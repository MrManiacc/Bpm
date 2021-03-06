package me.jraynor.logic.event

/**Allows for receiving of event**/
interface IEventReceiver {
    /**Called when the event is fired**/
    fun onEvent(event: Event<*, *>): Any?
}

