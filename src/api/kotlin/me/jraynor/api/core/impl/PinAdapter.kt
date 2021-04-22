package me.jraynor.api.core.impl

import imgui.*
import me.jraynor.api.core.*
import me.jraynor.api.utilities.enums.*
import net.minecraft.nbt.*

/**The base pin. All other pins should be of this type**/
open class PinAdapter(
    override var pinId: Int = 0,
    override var label: String = "",
    override var type: PinType = PinType.SQUARE,
    override var io: InputOutput = InputOutput.None,
    override var labelColor: Int = ImColor.rgbToColor("#ffffff"),
    override var baseColor: Int = ImColor.rgbToColor("#cccccc"),
    override var innerColor: Int = ImColor.rgbToColor("#212121"),
) : IPin {
    /**We can allow our self to keep a reference to the parent node**/
    override var nodeId: Int = -1

    /**We want to not expose our links in the interface, simple hide them in the adapter and allow accessor methods**/
    private val toLinks: MutableList<Int> = ArrayList()

    /**We want to not expose our links in the interface, simple hide them in the adapter and allow accessor methods**/
    private val fromLinks: MutableList<Int> = ArrayList()

    /**This should get the pinId of the link at index of [linkIdx]. If it's outside of the bounds, we return -1**/
    override fun getFrom(linkIdx: Int): Int {
        if (linkIdx < 0 || linkIdx >= fromLinks.count()) return -1
        return fromLinks[linkIdx]
    }

    /**This should get the pinId of the link at index of [linkIdx]. If it's outside of the bounds, we return -1**/
    override fun getTo(linkIdx: Int): Int {
        if (linkIdx < 0 || linkIdx >= toLinks.count()) return -1
        return toLinks[linkIdx]
    }

    /**Adds a pin with the given pin id. The graph is required so we can set the from link since we don't have an
     *  actual pin reference**/
    override fun addLink(otherId: Int, graph: IGraph): Int {
        val other = graph.findPin(otherId)
        if (other.isEmpty()) return -1
        return addLink(other)
    }

    /**This will add a link to the [other] IPin.
     * Returns the index of the link **/
    override fun addLink(other: IPin): Int {
        if (this.toLinks.add(other.pinId)) {
            if (other is PinAdapter)
                other.fromLinks.add(this.pinId)
            return this.toLinks.size - 1
        }
        return -1
    }

    /**True if the other pin is removed from this**/
    override fun removeLink(other: IPin): Boolean {
        if (other is PinAdapter)
            return this.toLinks.remove(other.pinId) && other.fromLinks.remove(this.pinId)
        return this.toLinks.remove(other.pinId)
    }

    /**removes another pin using just the pin id and graph reference.**/
    override fun removeLink(otherId: Int, graph: IGraph): Boolean {
        val other = graph.findPin(otherId)
        if (other.isEmpty()) return false
        return removeLink(other)
    }

    /***Clears all outgoing links**/
    override fun clearLinks(graph: IGraph) {
        forEachTo(graph) {
            if (it is PinAdapter)
                it.fromLinks.remove(this.pinId)
        }
        forEachFrom(graph) {
            if (it is PinAdapter)
                it.toLinks.remove(this.pinId)
        }
        this.toLinks.clear()
        this.fromLinks.clear()
    }

    /**return true if we have outgoing links**/
    override fun hasToLinks(): Boolean = this.toLinks.isNotEmpty()

    /**True if other pins are linked to us.**/
    override fun hasFromLinks(): Boolean = this.fromLinks.isNotEmpty()

    /**True if we're linked to the given link**/
    override fun isLinkedTo(other: IPin): Boolean = this.toLinks.contains(other.pinId)

    /**Checks if we're linked to the given [otherId]**/
    override fun isLinkedTo(otherId: Int, graph: IGraph): Boolean {
        val other = graph.findPin(otherId)
        if (other.isEmpty()) return false
        return isLinkedTo(other)
    }

    /**true if we're an input and the [other] pin has us as one of their links*/
    override fun isLinkedFrom(other: IPin): Boolean {
        if (other !is PinAdapter) return false
        return other.fromLinks.contains(this.pinId)
    }

    /**true if we're an input and the [otherId] pin has us as one of their links*/
    override fun isLinkedFrom(otherId: Int, graph: IGraph): Boolean {
        val other = graph.findPin(otherId)
        if (other.isEmpty()) return false
        return isLinkedFrom(other)
    }

    /**This iterates over each of the toLinks **/
    override fun forEachTo(iterator: (pinId: Int) -> Unit) = this.toLinks.forEach(iterator)

    /**This iterates over each of the fromLinks **/
    override fun forEachFrom(iterator: (pinId: Int) -> Unit) = this.fromLinks.forEach(iterator)

    /**Serializes our private links**/
    override fun serializeNBT(): CompoundNBT = with(super.serializeNBT()) {
        putIntArray("toLinks", toLinks)
        putIntArray("fromLinks", fromLinks)
        return this
    }

    /**reads our link data and other stuff**/
    override fun deserializeNBT(tag: CompoundNBT) = with(tag) {
        super.deserializeNBT(this)
        toLinks.clear()
        fromLinks.clear()
        getIntArray("toLinks").forEach(toLinks::add)
        getIntArray("fromLinks").forEach(fromLinks::add)
    }

    override fun toString(): String =
        "${javaClass.simpleName}(pinId=$pinId, label='$label', icon=$type, inputOutput=$io, links=$toLinks)"

    /**we only care about the pin id and io state for the equals*/
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IPin) return false
        if (pinId != other.pinId) return false
        if (io != other.io) return false
        return true
    }

    /**we only want to use the pin id and io state for the hash*/
    override fun hashCode(): Int {
        var result = pinId
        result = 31 * result + io.hashCode()
        return result
    }

}