package me.jraynor.api.core.impl

import me.jraynor.api.network.*
import me.jraynor.api.network.packets.*
import me.jraynor.api.core.*
import me.jraynor.api.utilities.*
import me.jraynor.api.utilities.enums.*
import net.minecraft.nbt.*
import net.minecraft.tileentity.*
import kotlin.reflect.*

/**This is the base node. All nodes should in theory extend from this however, they do not need to. **/
open class NodeAdapter(
    override var graph: IGraph = IGraph.Empty,
    override var nodeId: Int = 0,
    override var title: String = "",
    override var graphX: Float = Float.MIN_VALUE,
    override var graphY: Float = Float.MIN_VALUE,
    override var titleColor: String = "#ffffff",
    override var headerColor: String = "#8c8c8c"
) : INode {
    /**Caches all of the pins by their id**/
    private val pinIdCache = HashMap<Int, IPin>()

    /**The storage method of pins should be internal. The interface does not need a reference.**/
    private val pins: MutableList<IPin> = ArrayList()

    /**Caches pins by their label**/
    private val pinLabelCache = HashMap<String, IPin>()

    /**Caches the given pin types**/
    private val pinTypeCache = HashMap<KClass<out IPin>, IPin>()

    /**Adds the given pin**/
    protected fun addPin(pin: IPin): IPin {
        pin.nodeId = this.nodeId
        pins.add(pin)
        return pin
    }

    /**This should return the total number of pins**/
    override val count: Int get() = this.pins.count()

    /**Allows you to get a pin at the specific pin's index locally. [pinIndex] is NOT a pinId, its an index into. This number
     * should be in the range of 0 through ([count] - 1)**/
    override fun get(pinIndex: Int): IPin {
        if (pinIndex < 0 || pinIndex >= count) return IPin.Empty
        return pins[pinIndex]
    }

    /**True if we have any pins**/
    override fun hasPins(): Boolean = pins.isNotEmpty()

    /**True if the pin with the given name is present**/
    override fun hasPin(pinId: Int): Boolean {
        if (pinIdCache.containsKey(pinId)) return true
        for (pin in pins) {
            pin.nodeId = nodeId
            pinIdCache[pin.pinId] = pin
            if (pin.pinId == pinId)
                return true
        }
        return false
    }

    /**True if the pin with the given name is present**/
    override fun hasPin(name: String): Boolean {
        if (pinLabelCache.containsKey(name)) return true
        for (pin in pins) {
            pin.nodeId = nodeId
            pinLabelCache[pin.label] = pin
            if (pin.label == name)
                return true
        }
        return false
    }

    /**True if the pin with the given name is present**/
    override fun hasPin(type: KClass<out IPin>): Boolean {
        if (pinTypeCache.containsKey(type)) return true
        for (pin in pins) {
            pin.nodeId = nodeId
            pinTypeCache[pin::class] = pin
            if (type.isInstance(pin))
                return true
        }
        return false
    }

    /**Gets the pin with the given name**/
    override fun getPin(pinId: Int): IPin {
        if (pinIdCache.containsKey(pinId)) return pinIdCache[pinId]!!
        for (pin in pins) {
            pin.nodeId = nodeId
            pinIdCache[pin.pinId] = pin
            if (pin.pinId == pinId) {
                return pin
            }
        }
        return IPin.Empty
    }

    /**Gets the pin with the given name**/
    override fun getPin(name: String): IPin {
        if (pinLabelCache.containsKey(name)) return pinLabelCache[name]!!
        for (pin in pins) {
            pin.nodeId = nodeId
            pinLabelCache[pin.label] = pin
            if (pin.label == name) {
                return pin
            }
        }
        return IPin.Empty
    }

    /**Gets the pin with the given name**/
    override fun <T : IPin> getPin(type: KClass<T>): T {
        if (pinTypeCache.containsKey(type)) return pinTypeCache[type]!! as T
        for (pin in pins) {
            pin.nodeId = nodeId
            pinTypeCache[pin::class] = pin
            if (type.isInstance(pin))
                return pin as T
        }
        return IPin.Empty as T
    }

    /**True if one of our pins has the given node type linked**/
    override fun <T : INode> hasLinkedNode(type: KClass<T>): Boolean {
        pins.filter { it.io == InputOutput.Output }.forEach { output ->
            var has = false
            output.forEachTo(graph) { link ->
                val node = graph.findNode(link.nodeId)
                if (!node.isEmpty() && type.isInstance(node)) {
                    has = true
                    return@forEachTo
                }
            }
            if (has) return true
        }
        return false
    }

    /**This should find and get the linked node or null**/
    override fun <T : INode> getLinkedNode(type: KClass<T>): T? {
        pins.filter { it.io == InputOutput.Output }.forEach { output ->
            var outNode: INode? = null
            output.forEachTo(graph) { link ->
                val node = graph.findNode(link.nodeId)
                if (!node.isEmpty() && type.isInstance(node)) {
                    outNode = node
                    return@forEachTo
                }
            }
            if (outNode != null) return outNode as T
        }
        return null
    }

    /**This will push an update for the specific node to the opposite graph. I.E if on client we will push an update
     * to the server for this node, and vice verses.**/
    override fun pushUpdate() {
        val parent = graph.parent
        if (parent is TileEntity)
            when (graph.side) {
                Side.Client -> Network.sendToServer(PacketSyncNode().apply {
                    this.node = this@NodeAdapter; this.position = parent.pos
                })
                Side.Server -> Network.sendToClientsWithBlockLoaded(PacketSyncNode().apply {
                    this.node = this@NodeAdapter; this.position = parent.pos
                }, parent.pos, parent.world!!)
                else -> error("Attempted to push update for node ${this.javaClass.simpleName}(title=$title, nodeId=$nodeId), but the graph's side is set to neither!")
            }
        else
            error("Tried to push update for unsupported graph parent. Currently only tile entities can hold graph capabilities")
    }

    /**Used to allow iteration over each of the pins**/
    override fun iterator(): Iterator<IPin> = this.pins.listIterator()

    override fun serializeNBT(): CompoundNBT = with(super.serializeNBT()) {
        putDeepList("pins", pins)
        return this
    }

    override fun deserializeNBT(tag: CompoundNBT) {
        super.deserializeNBT(tag)
        pins.clear()
        pins.addAll(tag.getDeepList("pins"))
    }

    override fun toString(): String =
        "${javaClass.simpleName}(nodeId=$nodeId, title='$title', graphX=$graphX, graphY=$graphY, pins=$pins)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeAdapter) return false

        if (nodeId != other.nodeId) return false
        if (title != other.title) return false
        if (graphX != other.graphX) return false
        if (graphY != other.graphY) return false
        if (titleColor != other.titleColor) return false
        if (headerColor != other.headerColor) return false
        if (pins != other.pins) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nodeId
        result = 31 * result + title.hashCode()
        result = 31 * result + graphX.hashCode()
        result = 31 * result + graphY.hashCode()
        result = 31 * result + titleColor.hashCode()
        result = 31 * result + headerColor.hashCode()
        result = 31 * result + pins.hashCode()
        return result
    }


}