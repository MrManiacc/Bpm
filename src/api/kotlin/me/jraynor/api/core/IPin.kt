package me.jraynor.api.core

import me.jraynor.api.utilities.enums.*
import me.jraynor.api.utilities.*
import net.minecraft.nbt.*
import net.minecraftforge.common.util.*

/**
 * This represents a connection that is stored within a node. It can be serialized allowing the ability to be sent
 * over the network.
 */
interface IPin : INBTSerializable<CompoundNBT> {
    /**We can allow our self to keep a reference to the parent node**/
    var nodeId: Int

    /**This should be unique. Technically the id should be set from the client, after the client had synchronized the
     * server's last id's. Then sent to server**/
    var pinId: Int

    /**This is rendered on the client side. It is also used as key for the pin to be indexed upon inside the [INode]**/
    var label: String

    /**The type of icon this pin will render**/
    var type: PinType

    /**The color of the pin label**/
    var labelColor: Int

    /**The base color of the pin**/
    var baseColor: Int

    /**The inner color of the pin**/
    var innerColor: Int

    /**The type of input/output this pin is**/
    var io: InputOutput

    /**This should get the pinId of the link at index of [linkIdx]. If it's outside of the bounds, we return -1**/
    fun getFrom(linkIdx: Int): Int

    /**This should get a reference to the link index of [linkIdx]. If it's outside of the bounds we return [IPin.Empty]**/
    fun getFrom(linkIdx: Int, graph: IGraph): IPin = graph.findPin(getFrom(linkIdx))

    /**This should get the pinId of the link at index of [linkIdx]. If it's outside of the bounds, we return -1**/
    fun getTo(linkIdx: Int): Int

    /**This should get a reference to the link index of [linkIdx]. If it's outside of the bounds we return [IPin.Empty]**/
    fun getTo(linkIdx: Int, graph: IGraph): IPin = graph.findPin(getTo(linkIdx))

    /**Adds a pin with the given pin id. The graph is required so we can set the from link since we don't have an
     *  actual pin reference**/
    fun addLink(otherId: Int, graph: IGraph): Int

    /**This will add a link to the [other] IPin.
     * Returns the index of the link **/
    fun addLink(other: IPin): Int

    /**True if the other pin is removed from this**/
    fun removeLink(other: IPin): Boolean

    /**removes another pin using just the pin id and graph reference.**/
    fun removeLink(otherId: Int, graph: IGraph): Boolean

    /***This will call the [removeLink] method for each of the toLinks inside this pin, that means it will attempt to remove
     * the reference the other pin has to us, as well as the reference we have to it.**/
    fun clearLinks(graph: IGraph)

    /**return true if we have outgoing links**/
    fun hasToLinks(): Boolean

    /**True if other pins are linked to us.**/
    fun hasFromLinks(): Boolean

    /**True if we're linked to the given link**/
    fun isLinkedTo(other: IPin): Boolean

    /**Checks if we're linked to the given [otherId]**/
    fun isLinkedTo(otherId: Int, graph: IGraph): Boolean

    /**true if we're an input and the [other] pin has us as one of their links*/
    fun isLinkedFrom(other: IPin): Boolean

    /**true if we're an input and the [otherId] pin has us as one of their links*/
    fun isLinkedFrom(otherId: Int, graph: IGraph): Boolean

    /**This iterates over each of the toLinks **/
    fun forEachTo(iterator: (pinId: Int) -> Unit)

    /**This iterates over each of the toLinks **/
    fun forEachTo(graph: IGraph, iterator: (pin: IPin) -> Unit): Unit = forEachTo { iterator(graph.findPin(it)) }

    /**This iterates over each of the fromLinks **/
    fun forEachFrom(iterator: (pinId: Int) -> Unit)

    /**This iterates over each of the fromLinks **/
    fun forEachFrom(graph: IGraph, iterator: (pin: IPin) -> Unit) = forEachFrom { iterator(graph.findPin(it)) }

    /**True if the IPin is empty**/
    fun isEmpty(): Boolean = false

    /**This will write out all  of our pin data**/
    override fun serializeNBT(): CompoundNBT = with(CompoundNBT()) {
        putInt("nodeId", nodeId)
        putInt("pinId", pinId)
        putString("label", label)
        putEnum("icon", this@IPin.type)
        putInt("labelColor", labelColor)
        putInt("innerColor", innerColor)
        putInt("baseColor", baseColor)
        putEnum("io", io)
        return this
    }

    /**reads our link data and other stuff**/
    override fun deserializeNBT(tag: CompoundNBT) {
        nodeId = tag.getInt("nodeId")
        pinId = tag.getInt("pinId")
        label = tag.getString("label")
        type = tag.getEnum("icon")
        io = tag.getEnum("io")
        labelColor = tag.getInt("labelColor")
        innerColor = tag.getInt("innerColor")
        baseColor = tag.getInt("baseColor")

    }

    companion object {
        /**Allows for null safety. We can always check if the [IPin] is empty using this**/
        val Empty: IPin = object : IPin {
            override var nodeId = -1
            override var pinId: Int = -1
            override var label: String = ""
            override var type: PinType = PinType.NONE
            override var labelColor: Int = 0
            override var baseColor: Int = 0
            override var innerColor: Int = 0
            override fun isEmpty(): Boolean = true
            override var io: InputOutput = InputOutput.None
            override fun getFrom(linkIdx: Int): Int = -1
            override fun getTo(linkIdx: Int): Int = -1
            override fun addLink(otherId: Int, graph: IGraph): Int = 0
            override fun addLink(other: IPin): Int = 0
            override fun removeLink(other: IPin): Boolean = false
            override fun removeLink(otherId: Int, graph: IGraph): Boolean = false
            override fun clearLinks(graph: IGraph) = Unit
            override fun hasToLinks(): Boolean = false
            override fun hasFromLinks(): Boolean = false
            override fun isLinkedTo(other: IPin): Boolean = false
            override fun isLinkedTo(otherId: Int, graph: IGraph): Boolean = false
            override fun isLinkedFrom(other: IPin): Boolean = false
            override fun isLinkedFrom(otherId: Int, graph: IGraph): Boolean = false
            override fun forEachTo(iterator: (pinId: Int) -> Unit) = Unit
            override fun forEachFrom(iterator: (pinId: Int) -> Unit) = Unit
        }
    }
}