package me.jraynor.nodes

import imgui.*
import me.jraynor.api.core.impl.*
import me.jraynor.api.core.*
import me.jraynor.api.utilities.enums.*
import me.jraynor.logic.select.*
import me.jraynor.logic.vars.*
import me.jraynor.logic.vars.VarData
import net.minecraft.nbt.*
import net.minecraft.util.*
import java.lang.Float.*

/**This node is the base node for all tickable nodes.**/
class VarNode : ContentNode(
    title = "Variable",
    headerColor = "#eb346e"
), ISelectable {
    /**Keeps track of all of our variable values**/
    val data: VarData = VarData(type = VarType.Bool)

    /**Used to display the selecting type popup**/
    private var selectingType = false

    /**Used to display the selecting face popup**/
    private var selectingFace = false

    /**IF header content spacing is less than 0, we don't render the header content*/
    override val headerContentSpacing: Float = 5f

    /**Used as a way for the screen to detect and close the screen when approiate**/
    override var shouldClose: Boolean = false

    /**We store a hard reference to the do tick pin**/
    init {
        addPin(
            PinAdapter(
                io = InputOutput.Output,
                label = "##out",
                type = PinType.ROUND_SQUARE,
                baseColor = ImColor.rgbToColor("#169873")
            )
        )
    }

    /**Called when the player finishes a block selection**/
    override fun onFinishSelection(selection: Selection) {
        this.data.blockPos = selection.blockPos
        pushUpdate()
    }

    /**This will essentially clear the current links from the pin we're attempting to link to only if it's a refence node.
     * We do this because it wouldn't make sense having more than one variable input.**/
    override fun canLink(from: IPin, to: IPin): Boolean {
        if (to is VarRefPin && to.varType == this.data.type) {
            if (to.hasFromLinks())
                to.clearLinks(graph)
            return true
        }
        return false
    }

    /**This should render anything that will be put inside the header**/
    override fun renderHeaderContent() {
        if (ImGui.button("type##${this.nodeId}")) {
            ImGui.closeCurrentPopup()
            selectingType = true
            selectingFace = false
        }
    }

    /**This is used as a way to add code to the suspend block in a node**/
    override fun postProcess() {
        ImGui.closeCurrentPopup()
        if (selectingType) ImGui.openPopup("var_type_select##${nodeId}")
        if (ImGui.beginPopup("var_type_select##${nodeId}")) {
            VarType.values().forEach {
                if (ImGui.menuItem("${it.name}##${nodeId}")) {
                    if (this.data.type != it) {
                        this.data.type = it
                        getPin("##out").clearLinks(graph) //We remove any links we may have if we update the type
                        pushUpdate()
                    }
                    this.selectingType = false
                    ImGui.closeCurrentPopup()
                }
            }
            ImGui.endPopup()
        }
        if (selectingFace) ImGui.openPopup("var_face_select##${nodeId}")
        if (ImGui.beginPopup("var_face_select##${nodeId}")) {
            Direction.values().forEach {
                if (ImGui.menuItem("${it.name2.capitalize()}##$nodeId")) {
                    this.data.face = it
                    this.selectingFace = false
                    pushUpdate()
                    ImGui.closeCurrentPopup()
                }
            }
            ImGui.endPopup()
        }
    }

    /**This will render the contents of the node. This should return the new content width, if it hasn't been updated we simply
     * return the input [contentWidth]**/
    override fun renderContent(contentWidth: Float): Float {
        with(data) {
            val name = "${type.name}##$nodeId"
            when (type) {
                VarType.Bool -> {
                    ImGui.pushItemWidth(60f)
                    if (ImGui.checkbox(name, this.imBoolean))
                        pushUpdate()
                }
                VarType.Int -> {
                    ImGui.pushItemWidth(100f)
                    if (ImGui.inputInt(name, this.imInt))
                        pushUpdate()
                }
                VarType.Float -> {
                    ImGui.pushItemWidth(85f)
                    if (ImGui.inputFloat(name, this.imFloat))
                        pushUpdate()
                }
                VarType.Vec2 -> {
                    ImGui.pushItemWidth(120f)
                    if (ImGui.inputFloat2(name, this.imVec2))
                        pushUpdate()
                }
                VarType.Vec3 -> {
                    ImGui.pushItemWidth(120f)
                    if (ImGui.inputFloat3(name, this.imVec3))
                        pushUpdate()
                }
                VarType.BlockPos -> {
                    if (ImGui.button("select##${nodeId}_var")) {
                        startSelection(this.color)
                    }
                    ImGui.pushItemWidth(120f)
                    if (ImGui.inputInt3(name, this.imBlockPos))
                        pushUpdate()
                }
                VarType.Face -> {
                    if (ImGui.button("${data.face.name.toLowerCase()}##$nodeId")) {
                        selectingFace = true
                        selectingType = false
                        ImGui.closeCurrentPopup()
                    }
                    ImGui.sameLine()
                    ImGui.text("face")
                }
                VarType.Color -> {
                    ImGui.pushItemWidth(120f)
                    if (ImGui.colorPicker4(name, imColor))
                        pushUpdate()
                }
            }
            return max(contentWidth, ImGui.getItemRectMaxX())
        }
    }

    /**Writes the variable to file**/
    override fun serializeNBT(): CompoundNBT {
        val tag = super<ContentNode>.serializeNBT()
        tag.put("var_data", data.serializeNBT())
        return tag
    }

    /**Reads our data like the nodeId, and graphX/graphY and pins.**/
    override fun deserializeNBT(tag: CompoundNBT) {
        super<ContentNode>.deserializeNBT(tag)
        this.data.deserializeNBT(tag.getCompound("var_data"))
    }


}

