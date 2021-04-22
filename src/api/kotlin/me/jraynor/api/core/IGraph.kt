package me.jraynor.api.core

import me.jraynor.api.utilities.enums.*
import net.minecraft.nbt.*
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.common.util.*

/**
 * A graph has the ability to be serialized, it can save and load nth number of [INode]. It allows for overseeing of all operations
 * related to nodes.
 */
interface IGraph : INBTSerializable<CompoundNBT>, Iterable<INode> {

    /**The side in which this graph resides.**/
    var side: Side

    /**The parent of this graph, normally it will be a tile entity**/
    var parent: CapabilityProvider<*>?

    /**the center x position of the graph**/
    var centerX: Float

    /**the center y position of the graph**/
    var centerY: Float

    /**This will find the given node by the given id.
     * TODO: This should be cached. Save a node by it's id when it's accessed.**/
    fun findNode(nodeId: Int): INode

    /**This will return true if we have a node by teh given id**/
    fun hasNode(nodeId: Int): Boolean

    /**This should find the pin with the given [pinId] or return [IPin.Empty]
     * TODO: cache this shit**/
    fun findPin(pinId: Int): IPin

    /**This will return true if we have a pin by the given id**/
    fun hasPin(pinId: Int): Boolean

    /**This will attempt to find the node that has a pin with the given id or returns [INode.Empty].
     * It will use the io to mach the given type. If none is the [forIo] we won't check the InputOutput state
     * TODO: cache this**/
    fun findNodeByPin(pinId: Int, forIo: InputOutput = InputOutput.None): INode

    /**This will add the given node. The node shouldn't have an id when it's passed to this add method. This nodeId will be
     * added dynamically by this [IGraph]. The pins for the node too will have their id's set.
     * @return true if removed successfully**/
    fun addNode(node: INode): Boolean

    /**This will remove the given node. It will also remove the pins. It should also update the caches.
     * @return true if removed successfully**/
    fun removeNode(node: INode): Boolean

    /**True if the IGraph is empty**/
    fun isEmpty(): Boolean = false

    /*Gets the next id**/
    fun nextId(): Int

    companion object {
        /**Allows for null safety. We can always check if the [IGraph] is empty using this**/
        val Empty: IGraph = object : IGraph {
            override var side: Side = Side.Neither
            override var parent: CapabilityProvider<*>? = null
            override var centerX: Float = Float.MIN_VALUE
            override var centerY: Float = Float.MIN_VALUE
            override fun findNode(nodeId: Int): INode = INode.Empty
            override fun hasNode(nodeId: Int): Boolean = false
            override fun findPin(pinId: Int): IPin = IPin.Empty
            override fun hasPin(pinId: Int): Boolean = false
            override fun findNodeByPin(pinId: Int, forIo: InputOutput): INode = INode.Empty
            override fun addNode(node: INode): Boolean = false
            override fun removeNode(node: INode): Boolean = false
            override fun isEmpty(): Boolean = true
            override fun nextId(): Int = -1
            override fun serializeNBT(): CompoundNBT = CompoundNBT()
            override fun deserializeNBT(nbt: CompoundNBT?) = Unit
            override fun iterator(): Iterator<INode> = emptyArray<INode>().iterator()
        }
    }
}