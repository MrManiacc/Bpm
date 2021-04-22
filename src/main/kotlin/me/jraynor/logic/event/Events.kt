package me.jraynor.logic.event

import me.jraynor.api.core.*
import me.jraynor.nodes.*
import net.minecraft.util.*
import net.minecraft.util.math.*

/**Stores all of our node events**/
object Events {
    /**Used for when we are ticking**/
    class TickEvent(sender: IPin) : Event<Void, Void>(sender)

    /**Used to filter nodes upon extraction**/
    class FilterEvent(sender: IPin, data: FilterData) : Event<FilterEvent.FilterData, Boolean>(sender, data = data) {
        /**The data that will be sent from the [ExtractNode] to the [FilterNode]**/
        data class FilterData(val position: BlockPos, val face: Direction, val type: ExtractNode.ExtractType)
    }

    /**Called when the variable is update, so we and pass it to the [LinkNode] allowing for us to set the new color/show state**/
    class VarUpdateEvent(sender: IPin, data: VarNode) : Event<VarNode, Void>(sender, data = data)
}
