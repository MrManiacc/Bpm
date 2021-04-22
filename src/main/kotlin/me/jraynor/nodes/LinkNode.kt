package me.jraynor.nodes

import imgui.*
import imgui.flag.*
import imgui.type.*
import me.jraynor.api.core.impl.*
import me.jraynor.api.core.*
import me.jraynor.api.utilities.*
import me.jraynor.api.utilities.enums.*
import me.jraynor.logic.event.*
import me.jraynor.logic.select.*
import me.jraynor.logic.vars.*
import net.minecraft.client.*
import net.minecraft.nbt.*
import net.minecraft.util.*
import net.minecraft.util.math.*
import net.minecraft.util.math.vector.*
import java.lang.Float.*

/**This will render a node that has the ability to select a real world block**/
class LinkNode : ContentNode("Link Node"), ISelectable, IEventReceiver {
    /**The currently selected block**/
    private var posBuffer: IntArray = IntArray(3)

    /**This will be updated via the [posBuffer]*0*/
    private var selectedBlock: BlockPos = BlockPos.ZERO

    /**The currently selected face**/
    private var selectedFace: Direction = Direction.NORTH

    /**This is used a flag of sorts to determine when to show the popup**/
    private var selectingFace = false

    /**This should be updated frequently**/
    private val blockNameBuffer = ImString()

    /**IF header content spacing is less than 0, we don't render the header content*/
    override val headerContentSpacing: Float = 5f

    /**When true we want to show the face**/
    private var shown = ImBoolean(false)

    /**The show color**/
    private var color: FloatArray = floatArrayOf(0.25882352941f, 0.7098039f, 0.7803921f, 0.85f)

    /**Used as a way for the screen to detect and close the screen when approiate**/
    override var shouldClose: Boolean = false

    /**Tracking the status of the overlay/show face**/
    private var overlaying = false

    /**Keeps track of the last shown block.**/
    private var shownBlock: Pair<BlockPos, Direction> = Pair(linkedPos, linkedFace)

    /**Keeps track of the last shown color**/
    private var shownColor: Vector4f = this.showColor

    /*================== Usable data ==================*/

    /**This should get the show color of the node**/
    val showColor: Vector4f
        get() = varOrDefault("color") { Vector4f(color[0], color[1], color[2], color[3]) }

    /**This should get the current shown status.**/
    val isShown: Boolean
        get() = varOrDefault("show") { shown.get() }

    /**This will get the currently linked face**/
    val linkedFace: Direction
        get() = varOrDefault("face") { this.selectedFace }

    /**This will get the currently selected block**/
    val linkedPos: BlockPos
        get() = varOrDefault("pos") { this.selectedBlock }

    /*===============================================*/

    init {
        addPin(
            VarRefPin(
                type = VarType.Bool,
                label = "show",
            )
        )
        addPin(
            VarRefPin(
                type = VarType.Color,
                label = "color",
            )
        )
        addPin(
            VarRefPin(
                type = VarType.Face,
                label = "face",
            )
        )
        addPin(
            VarRefPin(
                type = VarType.BlockPos,
                label = "pos",
            )
        )
        addPin(
            PinAdapter(
                label = "insert",
                io = InputOutput.Input,
                type = PinType.SQUARE,
                baseColor = ImColor.rgbToColor("#2A9D8F")
            )
        )
        addPin(
            PinAdapter(
                label = "extract",
                io = InputOutput.Output,
                type = PinType.SQUARE,
                baseColor = ImColor.rgbToColor("#F4A261")
            )
        )
    }

    /**Called when the player finishes a block selection**/
    override fun onFinishSelection(selection: Selection) {
        this.selectedBlock = selection.blockPos
        this.selectedFace = selection.blockFace
        pushUpdate()
    }

    /**We allow a link between the two pins only if this method passes**/
    override fun canLink(from: IPin, to: IPin): Boolean {
        val node = graph.findNode(to.nodeId)
        if (from.label == "extract" && node is ExtractNode) return to.label == "extract"
        return false
    }

    /**Renders stuff inside the node's header**/
    override fun renderHeaderContent() {
        if (ImGui.button("select##${this.nodeId}")) {
            SelectionHelper.hide(shownBlock)
            startSelection(this.showColor)
        }
    }

    /**This will render the contents of the node. This should return the new content width, if it hasn't been updated we simply
     * return the input [contentWidth]**/
    override fun renderContent(contentWidth: Float): Float {
        var hasFace = false
        val face = getPin("face")
        if (face is VarRefPin && !face.hasData(graph)) {
            if (ImGui.button("${this.selectedFace.name.toLowerCase()}##${this.nodeId}"))
                selectingFace = true
            ImGui.sameLine()
            ImGui.text("face")
            hasFace = true
        }
        val show = getPin("show")
        if (show is VarRefPin && !show.hasData(graph)) {
            if (hasFace) ImGui.sameLine()
            if (ImGui.checkbox("show##${this.nodeId}", this.shown)) {
                pushUpdate()
            }
        }

        val pos = getPin("pos")
        if (pos is VarRefPin && !pos.hasData(graph)) {
            ImGui.setNextItemWidth(120f)
            this.posBuffer.let { it[0] = selectedBlock.x; it[1] = selectedBlock.y; it[2] = selectedBlock.z }
            if (ImGui.inputInt3("pos##${this.nodeId}", this.posBuffer)) pushUpdate()
            this.selectedBlock = BlockPos(posBuffer[0], posBuffer[1], posBuffer[2])
        }
        ImGui.setNextItemWidth(120f)
        if (ImGui.inputText("name##${this.nodeId}", blockName(), ImGuiInputTextFlags.ReadOnly)) pushUpdate()
        if (this.isShown) {
            val color = getPin("color")
            if (color is VarRefPin && !color.hasData(graph)) {
                ImGui.setNextItemWidth(120f)
                if (ImGui.colorPicker4("color##${this.nodeId}", this.color)) {
                    pushUpdate()
                }
            }
        }
        return max(ImGui.getItemRectMaxX(), contentWidth)
    }

    /**Gets the current block name**/
    private fun blockName(): ImString {
        blockNameBuffer.set(Minecraft.getInstance().world?.getBlockState(this.linkedPos)?.block?.translatedName?.string ?: "Undefined")
        return blockNameBuffer
    }

    /**This is used as a way to add code to the suspend block in a node**/
    override fun postProcess() {
        if (this.selectingFace) {
            ImGui.openPopup("face_select")
            if (ImGui.beginPopup("face_select")) {
                Direction.values().forEach {
                    if (ImGui.menuItem(it.name.toLowerCase())) {
                        this.selectedFace = it
                        this.selectingFace = false
                        ImGui.closeCurrentPopup()
                        pushUpdate()
                    }
                }
                ImGui.endPopup()
            }
        }

        //We simply are checking if the [shownBlock] has changed, if it has we update the overlaying state
        if (this.linkedPos != shownBlock.first || this.linkedFace != shownBlock.second || this.shownColor != showColor)
            overlaying = false

        if (isShown && !overlaying) {
            this.shownBlock = Pair(this.linkedPos, this.linkedFace)
            this.shownColor = this.showColor
            SelectionHelper.show(shownBlock, this.showColor)
            overlaying = true
        }

        if (!isShown && overlaying) {
            this.shownBlock = Pair(this.linkedPos, this.linkedFace)
            SelectionHelper.hide(shownBlock)
            overlaying = false
        }
    }

    override fun serializeNBT(): CompoundNBT {
        val tag = super<ContentNode>.serializeNBT()
        tag.putEnum("face", this.selectedFace)
        tag.putBoolean("shown", this.shown.get())
        tag.putBlockPos("block", this.selectedBlock)
        tag.putFloatArray("color", this.color)
        return tag
    }

    override fun deserializeNBT(tag: CompoundNBT) {
        super<ContentNode>.deserializeNBT(tag)
        this.selectedFace = tag.getEnum("face")
        this.shown.set(tag.getBoolean("shown"))
        tag.getBlockPos("block").let { this.selectedBlock = it }
        this.color = tag.getFloatArray("color")
    }

    /**Called when the event is fired**/
    override fun onEvent(event: Event<*, *>): Any? {
        if (event is Events.VarUpdateEvent) {
            if (event.hasData) {
                val data = event.data!!

            }
        }
        return null
    }

}