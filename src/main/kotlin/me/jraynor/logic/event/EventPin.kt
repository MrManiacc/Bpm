package me.jraynor.logic.event

import imgui.*
import me.jraynor.api.core.impl.*
import me.jraynor.api.core.*
import me.jraynor.api.utilities.enums.*

/**This is used for setting up call backs for pinsl**/
class EventPin(
    eventName: String = "event",
    override var type: PinType = PinType.FLOW,
    override var baseColor: Int = ImColor.intToColor(255, 255, 255)
) :
    PinAdapter(label = eventName, io = InputOutput.Input) {

    /**This fires an event and returns the expected type**/
    inline fun <reified D : Any, reified R : Any> fireEvent(graph: IGraph, event: Event<D, R>): R? {
        val parent = graph.findNode(this.nodeId)
        if (parent.isEmpty()) return null
        if (parent is IEventReceiver) {
            parent.onEvent(event)
            return event.result
        }
        return null
    }


}