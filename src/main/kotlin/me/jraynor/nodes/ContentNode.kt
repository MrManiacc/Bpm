package me.jraynor.nodes

import imgui.*
import me.jraynor.api.core.impl.*
import me.jraynor.logic.vars.*

/**This node is the base node for all tickable nodes.**/
abstract class ContentNode(
    override var title: String = "Content Node",
    override var headerColor: String = "#e36334"
) : NodeAdapter() {
    /**IF header content spacing is less than 0, we don't render the header content*/
    open val headerContentSpacing: Float = -1f

    /**This should render anything that will be put inside the header**/
    open fun renderHeaderContent() {
        ImGui.dummy(1f, 1f)
    }

    /**This will get the given value or else it will use the passed value.**/
    protected inline fun <reified T : Any> varOrDefault(pinLabel: String, default: () -> T): T {
        val pin = getPin(pinLabel)
        if (!pin.isEmpty() && pin is VarRefPin && pin.hasData(graph)) {
            val data = pin.getData(graph)!! //we know it's not null cause we just checked above
            return data.get() ?: default()
        }
        return default()
    }

    /**This will render the contents of the node. This should return the new content width, if it hasn't been updated we simply
     * return the input [contentWidth]**/
    abstract fun renderContent(contentWidth: Float): Float

}