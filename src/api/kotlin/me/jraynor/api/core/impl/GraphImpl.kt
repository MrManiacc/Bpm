package me.jraynor.api.core.impl

import me.jraynor.api.core.*
import me.jraynor.api.utilities.*
import me.jraynor.api.utilities.enums.*
import net.minecraft.nbt.*
import net.minecraftforge.common.capabilities.*

/***
 * The internal implementation for a graph
 */
class GraphImpl() :
    IGraph {
    /**Keep track of the current side we're on.**/
    override var side: Side = Side.Neither

    /**The parent of this graph, normally it will be a tile entity**/
    override var parent: CapabilityProvider<*>? = null

    /**the center x position of the graph**/
    override var centerX: Float = Float.MIN_VALUE

    /**the center y position of the graph**/
    override var centerY: Float = Float.MIN_VALUE

    /**Our internal list of nodes. **/
    private val nodes: MutableList<INode> = ArrayList()

    /**Allows us to keep track of the last used id**/
    private var nextId: Int = 0

    /**Caches node to their node id**/
    private val nodeIdCache = HashMap<Int, INode>()

    /**Caches pins with their pin id**/
    private val pinIdCache = HashMap<Int, IPin>()

    /**Caches all of the nodes based upon the pin id. **/
    private val pinIdToNodeCache = HashMap<Int, INode>()

    /**Caches all ids.**/
    private val activeIdCache = HashSet<Int>()

    /**This will find the given node by the given id.**/
    override fun findNode(nodeId: Int): INode {
        if (nodeId < 0) return INode.Empty
        if (nodeIdCache.containsKey(nodeId)) return nodeIdCache[nodeId]!!
        for (node in nodes) {
            nodeIdCache[node.nodeId] = node
            if (node.nodeId == nodeId)
                return node
        }
        return INode.Empty
    }

    /**This will return true if we have a node by teh given id**/
    override fun hasNode(nodeId: Int): Boolean {
        if (nodeId < 0) return false
        if (nodeIdCache.containsKey(nodeId)) return true
        for (node in nodes) {
            nodeIdCache[node.nodeId] = node
            if (node.nodeId == nodeId)
                return !node.isEmpty()
        }
        return false
    }

    /**This should find the pin with the given [pinId] or return [IPin.Empty] **/
    override fun findPin(pinId: Int): IPin {
        if (pinId < 0) return IPin.Empty
        if (pinIdCache.containsKey(pinId)) return pinIdCache[pinId]!!
        for (node in nodes) {
            val pin = node.getPin(pinId)
            pin.nodeId = node.nodeId
            pinIdCache[pin.pinId] = pin
            if (!pin.isEmpty())
                return pin
        }
        return IPin.Empty
    }

    /**This will return true if we have a pin by the given id**/
    override fun hasPin(pinId: Int): Boolean {
        if (pinId < 0) return false
        if (pinIdCache.containsKey(pinId)) return true
        for (node in nodes) {
            val pin = node.getPin(pinId)
            pinIdCache[pin.pinId] = pin
            if (!pin.isEmpty())
                return true
        }
        return false
    }

    /**This will attempt to find the node that has a pin with the given id or returns [INode.Empty].
     * It will use the io to mach the given type. If none is the [forIo] we won't check the InputOutput state
     * TODO: cache this**/
    override fun findNodeByPin(pinId: Int, forIo: InputOutput): INode {
        if (pinIdToNodeCache.containsKey(pinId)) return pinIdToNodeCache[pinId]!!
        for (node in nodes) {
            val pin = node.getPin(pinId)
            this.pinIdToNodeCache[pin.pinId] = node
            if (!pin.isEmpty() && pin.io == forIo)
                return node
        }
        return INode.Empty
    }

    /**This will check to see if the given id is in use or not. This can become quite an expensive**/
    private fun isIdUsed(id: Int): Boolean {
        if (this.activeIdCache.contains(id)) return true
        if (this.pinIdCache.containsKey(id) || nodeIdCache.containsKey(id) || pinIdToNodeCache.containsKey(id)) {
            activeIdCache.add(id)
            return true
        }
        for (node in nodes) {
            nodeIdCache[node.nodeId] = node
            if (node.nodeId == id) {
                activeIdCache.add(id)
                return true
            }
            for (pin in node) {
                pin.nodeId = node.nodeId
                pinIdCache[pin.pinId] = pin
                if (pin.pinId == id) {
                    activeIdCache.add(id)
                    return true
                }
            }
        }
        return false
    }

    /**This will return the next available id. **/
    override fun nextId(): Int {
        while (isIdUsed(nextId)) {
            nextId++
        }
        return nextId
    }

    /**This will add the given node. The node shouldn't have an id when it's passed to this add method. This nodeId will be
     * added dynamically by this [IGraph]. The pins for the node too will have their id's setl.
     * @return true if removed successfully**/
    override fun addNode(node: INode): Boolean {
        if (node.isEmpty()) return false
        if (!node.onAdd()) return false //allow skipping of the addition of this node, if onAdd is false (defaults to true)
        if (this.nodes.add(node)) {
            node.graph = this
            node.nodeId = nextId()
            nodeIdCache[node.nodeId] = node
            for (pin in node) {
                pin.pinId = nextId()
                pin.nodeId = node.nodeId
                pinIdCache[pin.pinId] = pin
            }
            return true
        }
        return false
    }

    /**This will remove the given node. It will also remove the pins. It should also update the caches.
     * @return true if removed successfully**/
    override fun removeNode(node: INode): Boolean {
        if (!node.onRemove()) return false //allow for overriding of removal when onRemove is false
        for (pin in node) {
            pin.clearLinks(this)
            pinIdCache.remove(pin.pinId)
            activeIdCache.remove(pin.pinId)
            pinIdToNodeCache.remove(pin.pinId)
        }
        activeIdCache.remove(node.nodeId)
        nodeIdCache.remove(node.nodeId)
        return nodes.remove(node)
    }

    /**This will write all of our [nodes], as well as the [pos]. It will also write the nextId**/
    override fun serializeNBT(): CompoundNBT = with(CompoundNBT()) {
        putDeepList("nodes", nodes)
        putFloatArray("center", floatArrayOf(centerX, centerY))
        return this
    }

    /**Reads our position, and our nodes**/
    override fun deserializeNBT(tag: CompoundNBT) {
        with(this.nodes) {
            clear()
            addAll(tag.getDeepList("nodes"))
        }
        tag.getFloatArray("center").let { this.centerX = it[0]; this.centerY = it[1] }
    }

    /**Allows for iteration over the nodes**/
    override fun iterator(): Iterator<INode> = this.nodes.listIterator()

    override fun toString(): String = "Graph(nodes=$nodes)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphImpl

        if (side != other.side) return false
        if (parent != other.parent) return false
        if (nodes != other.nodes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = side.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        result = 31 * result + nodes.hashCode()
        return result
    }


}