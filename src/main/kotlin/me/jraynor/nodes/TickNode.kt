package me.jraynor.nodes

import imgui.*
import imgui.type.*
import me.jraynor.api.core.*
import me.jraynor.api.core.impl.*
import me.jraynor.api.utilities.enums.*
import me.jraynor.logic.event.*
import me.jraynor.logic.vars.*
import net.minecraft.nbt.*
import kotlin.math.*

/**This node is the base node for all tickable nodes.**/
open class TickNode : ContentNode(
    title = "Ticking Node",
    headerColor = "#2c9160"
) {
    /**how many times we should output the tick per second**/
    private val tps: ImInt = ImInt(20)

    /**Used to locally keep track if we're enabled or not**/
    private val enabled: ImBoolean = ImBoolean(false)

    /**IF header content spacing is less than 0, we don't render the header content*/
    override val headerContentSpacing: Float = 0f

    /**This is used to keep track of the current tick. When this number is greater than or equal to [tickRate]
     * we call the [tick]**/
    private var tick = 0

    /*================== Usable data ==================*/

    /**The rate of which to send out ticks to linked nodes**/
    val tickRate: Int
        get() = varOrDefault("rate") { tps.get() }

    /**Whether or not to send out a tick event to linked nodes**/
    val isEnabled: Boolean
        get() = varOrDefault("enabled") { enabled.get() }

    /*===============================================*/


    /**We store a hard reference to the do tick pin**/
    init {
        addPin(
            VarRefPin(
                type = VarType.Bool,
                label = "enabled",
            )
        )
        addPin(
            PinAdapter(
                io = InputOutput.Output,
                label = "##tick",
                type = PinType.FLOW,
                baseColor = ImColor.rgbToColor("#99A1A6")
            )
        )

        addPin(
            VarRefPin(
                type = VarType.Int,
                label = "rate",
            )
        )
    }

    /**Sends a tick to the given pin**/
    fun tick() {
        if (tick++ == tickRate) {
            val tickPin = getPin("##tick")
            tickPin.forEachTo(graph) {
                if (it is EventPin)
                    it.fireEvent(graph, Events.TickEvent(tickPin))
            }
            tick = 0
        }
    }

    /**We allow a link between the two pins only if this method passes**/
    override fun canLink(from: IPin, to: IPin): Boolean {
        if (from.label == "##tick") return to.type == PinType.FLOW
        return super.canLink(from, to)
    }

    /**This will render the contents of the node. This should return the new content width, if it hasn't been updated we simply
     * return the input [contentWidth]**/
    override fun renderContent(contentWidth: Float): Float {
        val enabled = getPin("enabled")
        if (enabled is VarRefPin && !enabled.hasData(graph)) {
            if (ImGui.checkbox("enabled##$nodeId", this.enabled))
                pushUpdate()
        }
        val rate = getPin("rate")
        if (rate is VarRefPin && !rate.hasData(graph)) {
            ImGui.pushItemWidth(80f)
            if (ImGui.inputInt("rate##$nodeId", tps)) {
                tps.set(max(tps.get(), 0))
                pushUpdate()
            }
        }
        return max(ImGui.getItemRectMaxX(), contentWidth + 20)
    }

    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.putInt("tick_rate", this.tps.get())
        tag.putBoolean("tick_enable", this.enabled.get())
        return tag
    }

    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
        tps.set(tag.getInt("tick_rate"))
        enabled.set(tag.getBoolean("tick_enable"))
    }

}