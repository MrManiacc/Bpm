package me.jraynor.objects.screen

import com.mojang.blaze3d.matrix.*
import imgui.*
import imgui.extension.nodeditor.*
import imgui.extension.nodeditor.flag.*
import imgui.flag.*
import imgui.type.*
import me.jraynor.api.render.*
import me.jraynor.api.core.*
import me.jraynor.api.utilities.enums.*
import me.jraynor.logic.select.*
import me.jraynor.nodes.*
import me.jraynor.render.*

/**This is the implementation of our node screen. **/
class SingularityScreen(parent: GraphTileEntity, graphIn: IGraph? = null) : AbstractNodeScreen(parent, graphIn) {
    private var selectedNodes: LongArray = longArrayOf()
    private val nameBuffer = ImString()
    private val context = NodeEditorContext()

    /**It's not feasible to cache this inside the [IGraph]. Stores the from and to link state**/
    private val linkCache = HashMap<Int, Pair<IPin, IPin>>()

    /**The current link target**/
    private var targetLink: Long = -1

    /**Stores the addable nodes**/
    private val addableNodes: Map<String, () -> INode> =
        hashMapOf(
            Pair("TickNode") { TickNode().apply { graph = this@SingularityScreen.graph; updatePosition(this) } },
            Pair("ExtractNode") { ExtractNode().apply { graph = this@SingularityScreen.graph; updatePosition(this) } },
            Pair("LinkNode") { LinkNode().apply { graph = this@SingularityScreen.graph; updatePosition(this) } },
            Pair("VarNode") { VarNode().apply { graph = this@SingularityScreen.graph; updatePosition(this) } },
            Pair("FilterNode") { FilterNode().apply { graph = this@SingularityScreen.graph; updatePosition(this) } },
        )

    /**Keep track of whether or not we have centered our self**/
    private var hasCentered = false

    /**This will make sure our nodes are in the correct positions**/
    override fun init() {
        super.init()
        NodeEditor.setCurrentEditor(context)
        graph.forEach {
            if (it.graphX != Float.MIN_VALUE && it.graphY != Float.MIN_VALUE)
                NodeEditor.setNodePosition(it.nodeId.toLong(), it.graphX, it.graphY)
        }
    }

    /**This will render our main content area.**/
    override fun renderGraph() {
        NodeEditor.setCurrentEditor(context)
        UserInterface.nodeGraph(name) {
            NodeEditor.pushStyleVar(NodeEditorStyleVar.FlowSpeed, 75f)
            NodeEditor.pushStyleVar(NodeEditorStyleVar.FlowMarkerDistance, 50f)
            graph.forEach(NodeRenderer::render) //Renders all of the nodes
            NodeEditor.suspend()
            suspended()
            graph.forEach(INode::postProcess) //Process all of the suspended for the node
            NodeEditor.resume()
            renderLinks()
            processLinks()
            NodeEditor.popStyleVar(2)
        }
        centerGraph()
    }

    /**This will allow us to add remove and do all kinds of stuff relating to nodes.**/
    private fun suspended() {
        val nodeWithContextMenu = NodeEditor.getNodeWithContextMenu()
        var selected = 0
        if (nodeWithContextMenu != -1L) {
            val nodeSize = NodeEditor.getSelectedObjectCount()
            if (nodeSize > 0) {
                selectedNodes = LongArray(nodeSize)
                NodeEditor.getSelectedNodes(selectedNodes, nodeSize)
                ImGui.openPopup("node_multi_context")
                ImGui.getStateStorage().setInt(ImGui.getID("selected_node_size"), nodeSize)
                selectedNodes.forEach {
                    if (graph.hasNode(it.toInt()))
                        selected++
                }
            } else selectedNodes = longArrayOf()
            if (selected <= 1) {
                ImGui.openPopup("node_context")
                ImGui.getStateStorage().setInt(ImGui.getID("selected_node_id"), nodeWithContextMenu.toInt())
            }
        }
        val linkWithContextMenu = NodeEditor.getLinkWithContextMenu()
        if (linkWithContextMenu != -1L) {
            targetLink = linkWithContextMenu
            ImGui.openPopup("link_context")
        }
        if (NodeEditor.showBackgroundContextMenu())
            ImGui.openPopup("node_editor_context")

        addContext()
        nodeContext()
        multiNodeContext()
    }

    /**Shows our add context when requested**/
    private fun addContext() {
        if (ImGui.beginPopup("node_editor_context")) {
            ImGui.text("Add nodes")
            ImGui.separator()
            this.addableNodes.forEach { (name, supplier) ->
                if (ImGui.button(name)) {
                    val node = supplier()
                    if (graph.addNode(node)) {
                        NodeEditor.setNodePosition(node.nodeId.toLong(), node.graphX, node.graphY)
                        pushUpdate()
                    }
                    ImGui.closeCurrentPopup()
                }
            }
            ImGui.endPopup()
        }
    }

    /**This will open a popup for deleting the currently selected node. there is a callback in the node graph**/
    private fun multiNodeContext() {
        if (ImGui.isPopupOpen("node_multi_context")) {
            if (ImGui.beginPopup("node_multi_context")) {
                if (ImGui.button("Delete ${selectedNodes.size} node(s)")) {
                    for (nodeIn in this.selectedNodes) {
                        val toRemove = graph.findNode(nodeIn.toInt())
                        println(toRemove)
                        if (graph.removeNode(toRemove)) {
                            NodeEditor.deleteNode(nodeIn)
                            pushUpdate() //We want to inform the server graph of the deletion
                        }
                    }
                    ImGui.closeCurrentPopup()
                }
                ImGui.endPopup()
            }
        }
    }

    /**This will open a popup for deleting the currently selected node. there is a callback in the node graph**/
    private fun nodeContext() {
        var open = false
        if (ImGui.isPopupOpen("node_context")) {
            val targetNode = ImGui.getStateStorage().getInt(ImGui.getID("selected_node_id"))
            if (ImGui.beginPopup("node_context")) {
                if (ImGui.button("Delete node")) {
                    val toRemove = graph.findNode(targetNode)
                    if (graph.removeNode(toRemove)) {
                        NodeEditor.deleteNode(targetNode.toLong())
                        pushUpdate() //We want to inform the server graph of the deletion
                    }
                    ImGui.closeCurrentPopup()
                }
                if (ImGui.button("Copy node")) {
                    val toCopy = graph.findNode(targetNode)
                    if (graph.removeNode(toCopy)) {
                        pushUpdate() //We want to inform the server graph of the deletion
                    }
                    ImGui.closeCurrentPopup()
                }
                if (ImGui.button("Rename node")) {
                    ImGui.closeCurrentPopup()
                    open = true
                }
                ImGui.endPopup()
            }
        }
        if (open)
            ImGui.openPopup("Rename node")
        val targetNode = ImGui.getStateStorage().getInt(ImGui.getID("selected_node_id"))
        if (ImGui.beginPopup("Rename node")) {
            val toRename = graph.findNode(targetNode)
            nameBuffer.set(toRename.title)
            ImGui.setKeyboardFocusHere()
            ImGui.pushItemWidth(80f)
            if (ImGui.inputText("name", nameBuffer, ImGuiInputTextFlags.EnterReturnsTrue)) {
                toRename.title = nameBuffer.get()
                pushUpdate()
                ImGui.closeCurrentPopup()
            }
            ImGui.endPopup()
        }
        if (ImGui.isPopupOpen("link_context") && targetLink != -1L) {
            if (ImGui.beginPopup("link_context")) {
                if (ImGui.button("Delete link")) {
                    if (linkCache.containsKey(targetLink.toInt())) {
                        val pins = linkCache[targetLink.toInt()]!!
                        val from = pins.first
                        val to = pins.second
                        if (from.removeLink(to)) {
                            pushUpdate()
                            linkCache.remove(targetLink.toInt())
                            targetLink = -1
                        }
                    }
                    ImGui.closeCurrentPopup()
                }
                ImGui.endPopup()
            }
        }
    }

    /**This will create a new node of the given type and return it**/
    private inline fun <reified T : INode> updatePosition(node: T): T = node.apply {
        graphX = NodeEditor.toCanvasX(ImGui.getMousePosX())
        graphY = NodeEditor.toCanvasY(ImGui.getMousePosY())
        return this
    }

    /**Renders the links between nodes**/
    private fun renderLinks() {
        if (NodeEditor.beginCreate()) {
            val a = ImLong()
            val b = ImLong()
            if (NodeEditor.queryNewLink(a, b)) {
                val source = graph.findPin(a.get().toInt())
                val target = graph.findPin(b.get().toInt())
                val node = graph.findNode(source.nodeId)
                if (source.pinId != target.pinId && node.canLink(source, target) && NodeEditor.acceptNewItem()) {
                    if (source.io == InputOutput.Output && target.io == InputOutput.Input)
                        if (source.addLink(target) != -1)
                            pushUpdate()
                }
            }
        }
        NodeEditor.endCreate()
    }

    /**
     * This will render the links of the graph
     */
    private fun processLinks() {
        var linkID = 0L
        graph.forEach {
            it.forEach { pin ->
                pin.forEachTo(graph) { link ->
                    linkCache[linkID.toInt()] = Pair(pin, link)
                    NodeEditor.link(linkID, pin.pinId.toLong(), link.pinId.toLong())
                    if (it is TickNode && it.isEnabled)
                        NodeEditor.flow(linkID)
                    linkID++
                }
            }
        }
    }

    /**Handles the centering of the graph**/
    private fun centerGraph() {
        val center = UserInterface.getContentCenter()
        val graphX = NodeEditor.toCanvasX(center.x)
        val graphY = NodeEditor.toCanvasY(center.y)
        graph.centerX = graphX
        graph.centerY = graphY
        if (!hasCentered) {
            NodeEditor.navigateToContent(1f)
            hasCentered = true
        }
    }

    /**This will render the side panel/properties window.**/
    override fun renderVariables() {
        var count = 0
        graph.filterIsInstance<VarNode>().forEach {
            val name = if (it.title == "Variable") "Variable #$count" else it.title
            if (ImGui.treeNode("${name}##${it.nodeId}")) {
                it.renderContent(0f)
                ImGui.treePop()
            }
            count++
        }
    }

    /**This will render our node screen**/
    override fun render(matrixStack: MatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.render(matrixStack, mouseX, mouseY, partialTicks)
        this.graph.forEach {
            if (it is ISelectable && it.shouldClose) {
                //                onClose()
                this.closeScreen()
                it.shouldClose = false
            }
        }
    }

    /**Update the server of the graph when we close the screen**/
    override fun onClose() {
        val buffer = ImVec2()
        graph.forEach {
            NodeEditor.getNodePosition(it.nodeId.toLong(), buffer)
            it.graphX = buffer.x
            it.graphY = buffer.y
        }
        pushUpdate()
        context.destroy()
    }
}