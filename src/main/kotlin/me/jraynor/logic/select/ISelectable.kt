package me.jraynor.logic.select

import me.jraynor.api.core.*
import net.minecraft.util.math.vector.*

/**As a way to treat a node as a callback for when a selection is finished.**/
interface ISelectable : INode {

    /**Used as a way for the screen to detect and close the screen when approiate**/
    var shouldClose: Boolean

    /**This will start a selection context.**/
    fun startSelection(color: Vector4f = Vector4f(0.6834334f, 0.4422334f, 0.84447f, 0.88f)) =
        SelectionHelper.start(this, color)

    /**Called when the player finishes a block selection**/
    fun onFinishSelection(selection: Selection)
}