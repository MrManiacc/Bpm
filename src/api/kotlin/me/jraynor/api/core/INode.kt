package me.jraynor.api.core

import imgui.*
import net.minecraft.nbt.*
import net.minecraftforge.common.util.*
import kotlin.reflect.*

/**
 * This is the base node. Every single type of node will extend off of this. This will store critical methods for things
 * such as saving and loading, which allows for serialization from the server to the client. We also much have [ImGui]
 * rendering calls, but only for the client.
 */
interface INode : INBTSerializable<CompoundNBT>, Iterable<IPin> {
    /**A node shouldn't be created outside of [IGraph], so we should always have a reference**/
    var graph: IGraph

    /**This can be used as way to check if the node is properly serialized/deserialized. This should be unique and always present.**/
    var nodeId: Int

    /**The label of the node.**/
    var title: String

    /**The color of the title text**/
    var titleColor: String

    /**The color, in hex string of the current title color.**/
    var headerColor: String

    /**This is used to keep track of the x position on screen of the node.**/
    var graphX: Float

    /**This is used to keep track of the y position on screen of the node.**/
    var graphY: Float

    /**This should return the total number of pins**/
    val count: Int

    /**Allows you to get a pin at the specific pin's index locally. [pinIndex] is NOT a pinId, its an index into. This number
     * should be in the range of 0 through ([count] - 1)**/
    operator fun get(pinIndex: Int): IPin

    /**True if we have any pins**/
    fun hasPins(): Boolean

    /**True if the pin with the given name is present**/
    fun hasPin(pinId: Int): Boolean

    /**Gets the pin with the given name**/
    fun getPin(pinId: Int): IPin

    /**True if the pin with the given name is present**/
    fun hasPin(name: String): Boolean

    /**Gets the pin with the given name**/
    fun getPin(name: String): IPin

    /**True if the pin with the given name is present**/
    fun hasPin(type: KClass<out IPin>): Boolean

    /**Gets the pin with the given name**/
    fun <T : IPin> getPin(type: KClass<T>): T

    /**True if one of our pins has the given node type linked**/
    fun <T : INode> hasLinkedNode(type: KClass<T>): Boolean

    /**This should find and get the linked node or null**/
    fun <T : INode> getLinkedNode(type: KClass<T>): T?

    /**We allow a link between the two pins only if this method passes**/
    fun canLink(from: IPin, to: IPin): Boolean = true

    /**This will push an update for the specific node to the opposite graph. I.E if on client we will push an update
     * to the server for this node, and vice verses.**/
    fun pushUpdate()

    /**This is used as a way to add code to the suspend block in a node**/
    fun postProcess() = Unit

    /**True if the INode is empty**/
    fun isEmpty(): Boolean = false

    /**This is called when the node is added to the graph
     * @return true if we should add it, false if not**/
    fun onAdd(): Boolean = true

    /**This is called before the node is removed.
     * @return true if we should remove it, false if not**/
    fun onRemove(): Boolean = true

    /**This will write all of our base data.**/
    override fun serializeNBT(): CompoundNBT = with(CompoundNBT()) {
        putInt("nodeId", nodeId)
        putFloat("graphX", graphX)
        putFloat("graphY", graphY)
        putString("title", title)
        return this
    }

    /**Reads our data like the nodeId, and graphX/graphY and pins.**/
    override fun deserializeNBT(tag: CompoundNBT) {
        this.nodeId = tag.getInt("nodeId")
        this.graphX = tag.getFloat("graphX")
        this.graphY = tag.getFloat("graphY")
        this.title = tag.getString("title")
    }

    companion object {
        /**Allows for null safety. We can always check if the [INode] is empty using this**/
        val Empty: INode = object : INode {
            override var graph: IGraph = IGraph.Empty
            override var nodeId: Int = -1
            override var title: String = ""
            override var titleColor: String = "#ffffff"
            override var headerColor: String = "#ffffff"
            override var graphX: Float = -1f
            override var graphY: Float = -1f
            override val count: Int = -1
            override fun get(pinIndex: Int): IPin = IPin.Empty
            override fun hasPins(): Boolean = false
            override fun hasPin(pinId: Int): Boolean = false
            override fun hasPin(name: String): Boolean = false
            override fun hasPin(type: KClass<out IPin>): Boolean = false
            override fun getPin(pinId: Int): IPin = IPin.Empty
            override fun getPin(name: String): IPin = IPin.Empty
            override fun <T : IPin> getPin(type: KClass<T>): T = IPin.Empty as T
            override fun isEmpty(): Boolean = true
            override fun <T : INode> hasLinkedNode(type: KClass<T>): Boolean = false
            override fun <T : INode> getLinkedNode(type: KClass<T>): T? = null
            override fun pushUpdate() {}
            override fun iterator(): Iterator<IPin> = emptyArray<IPin>().iterator()
        }
    }
}