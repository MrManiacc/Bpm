package me.jraynor.nodes

import imgui.*
import imgui.type.*
import me.jraynor.api.core.impl.*
import me.jraynor.api.core.*
import me.jraynor.api.utilities.*
import me.jraynor.api.utilities.enums.*
import me.jraynor.logic.event.*
import me.jraynor.logic.vars.*
import net.minecraft.nbt.*
import kotlin.math.*

/**This node will be used to extract items and other things from tile entities**/
class ExtractNode : ContentNode(
    title = "Extraction Node",
    headerColor = "#2c83a3",
), IEventReceiver {
    /**If header content spacing is less than 0, we don't render the header content*/
    override val headerContentSpacing: Float = 5f

    /**Used so we can show the selecting type popup**/
    private var selectingType = false

    /**The amount of stuff to extract **/
    private val amount: ImInt = ImInt(1)

    /*================== Usable data ==================*/

    /**The amount of whatever our [extractType] to extract each tick**/
    val extractAmount: Int
        get() = varOrDefault("rate") { amount.get() }

    /**We don't want this value to be able to be updated from a variable, so it can only be set locally**/
    var extractType: ExtractType = ExtractType.Item
        private set

    /*===============================================*/

    /**We don't need to store a reference to the pin because it's an input pint**/
    init {
        addPin(EventPin("##extract", PinType.FLOW, baseColor = ImColor.rgbToColor("#99A1A6")))
        addPin(
            VarRefPin(
                type = VarType.Int,
                label = "rate"
            )
        )
        addPin(
            PinAdapter(
                io = InputOutput.Output,
                label = "filter",
                type = PinType.GRID,
                baseColor = ImColor.rgbToColor("#99A1A6")
            )
        )
        addPin(
            PinAdapter(
                label = "extract",
                io = InputOutput.Input,
                type = PinType.SQUARE,
                baseColor = ImColor.rgbToColor("#2A9D8F")
            )
        )
        addPin(
            PinAdapter(
                label = "insert",
                io = InputOutput.Output,
                type = PinType.SQUARE,
                baseColor = ImColor.rgbToColor("#F4A261")
            )
        )
    }

    /**We allow a link between the two pins only if this method passes**/
    override fun canLink(from: IPin, to: IPin): Boolean {
        val node = graph.findNode(to.nodeId)
        if (from.label == "filter" && node is FilterNode) return true
        if (from.label == "insert" && node is LinkNode) return to.label == "insert"
        return false
    }

    /**Called when the event is fired**/
    override fun onEvent(event: Event<*, *>) {
        if (event is Events.TickEvent) {
            println("ticking extract node")
        }
    }

    /**This should render anything that will be put inside the header**/
    override fun renderHeaderContent() {
        if (ImGui.button("${this.extractType.name.toLowerCase()}##${this.nodeId}"))
            selectingType = true
    }

    /**This will render our type popup**/
    override fun postProcess() {
        if (this.selectingType) {
            ImGui.openPopup("type_select")
            if (ImGui.beginPopup("type_select")) {
                ExtractType.values().forEach {
                    if (ImGui.menuItem(it.name.toLowerCase())) {
                        this.extractType = it
                        this.selectingType = false
                        ImGui.closeCurrentPopup()
                        pushUpdate()
                    }
                }
                ImGui.endPopup()
            }
        }
    }

    /**This will render the contents of the node. This should return the new content width, if it hasn't been updated we simply
     * return the input [contentWidth]**/
    override fun renderContent(contentWidth: Float): Float {
        val rate = getPin("rate")
        if (rate is VarRefPin && !rate.hasData(graph)) {
            ImGui.pushItemWidth(100f)
            if (ImGui.inputInt("rate##$nodeId", this.amount)) {
                this.amount.set(max(this.amount.get(), 0))
                pushUpdate()
            }
            return max(contentWidth, ImGui.getItemRectMaxX())
        }
        return contentWidth
    }

    /**Writes our speed and type**/
    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.putEnum("extract_type", extractType)
        tag.putInt("extract_speed", amount.get())
        return tag
    }

    /**Reads our speed and type**/
    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
        extractType = tag.getEnum("extract_type")
        amount.set(tag.getInt("extract_speed"))
    }

    /**Stores the type of extraction we wish to evaluate***/
    enum class ExtractType {
        Item, Energy, Liquid, Gas
    }

}