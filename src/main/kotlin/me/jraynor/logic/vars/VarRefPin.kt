package me.jraynor.logic.vars

import imgui.*
import me.jraynor.api.core.impl.*
import me.jraynor.api.core.*
import me.jraynor.api.utilities.*
import me.jraynor.api.utilities.enums.*
import me.jraynor.nodes.*
import net.minecraft.nbt.*
import java.util.*

/***
 * This pin acts a reference to a variable.
 */
class VarRefPin(
    type: VarType = VarType.Bool,
    label: String = "##${UUID.randomUUID()}",
    baseColor: Int = ImColor.rgbToColor("#F49FBC")
) : PinAdapter(
    io = InputOutput.Input,
    label = label,
    type = PinType.ROUND_SQUARE,
    baseColor = baseColor
) {

    /**This needs to only be set upon a pin being created, and when we're serializing**/
    var varType: VarType = type
        private set

    /**Gets the variable data for **/
    fun getData(graph: IGraph): VarData? {
        val from = getFrom(0, graph)
        if (from.isEmpty()) return null
        val node = graph.findNode(from.nodeId)
        if (node !is VarNode) return null
        return node.data
    }

    /**Simply does a check to if we have the actual var data**/
    fun hasData(graph: IGraph): Boolean = getData(graph) != null

    override fun serializeNBT(): CompoundNBT {
        val tag = super.serializeNBT()
        tag.putEnum("var_ref_type", varType)
        return tag
    }

    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
        this.varType = tag.getEnum("var_ref_type")
    }

}