package me.jraynor.api.core

import me.jraynor.api.capability.CapabilityGraphHandler.Companion.GRAPH_CAPABILITY
import me.jraynor.api.network.*
import me.jraynor.api.network.packets.*
import me.jraynor.api.core.impl.*
import me.jraynor.api.utilities.enums.*
import net.minecraft.block.*
import net.minecraft.nbt.*
import net.minecraft.tileentity.*
import net.minecraft.util.*
import net.minecraftforge.common.capabilities.*
import net.minecraftforge.common.util.*

/**This is a generic interface. It allows for synchronizing/ handling of [IGraph].**/
abstract class GraphTileEntity(type: TileEntityType<*>) : TileEntity(type), ITickableTileEntity {
    protected var intiailzied = false

    /**Keeps track of the graph capability**/
    protected var nodeGraph: LazyOptional<IGraph> = LazyOptional.of { GraphImpl() }

    /**This constructs a new sync packet from the current graph**/
    private val syncPacket: PacketSyncGraph
        get() {
            val packet = PacketSyncGraph()
            packet.position = this.pos
            packet.graph = this.nodeGraph.resolve().get()
            return packet
        }

    /**This constructs a new sync packet from the current graph**/
    private val syncRequestPacket: PacketSyncRequest
        get() {
            val packet = PacketSyncRequest()
            packet.position = this.pos
            return packet
        }

    /**We remove our listeners upon the tile being removed**/
    override fun remove() {
        this.intiailzied = false
        onUnload()
        nodeGraph.invalidate()
        super.remove()
    }

    /**This will be called when the world unloads.**/
    abstract fun onUnload()

    /**This will attempt to add the listeners*/
    protected fun installListeners() = Network.addListeners(this)

    /**Uninstalls listeners for this instance**/
    protected fun uninstallListeners() = Network.removeListeners(this)

    /**This should send the graph to the client upon initialization**/
    fun pushUpdate(to: Side) {
        if (to == Side.Client)
            Network.sendToAllClients(syncPacket)
        else
            Network.sendToServer(syncPacket)
    }

    /**This will request an update from the given side**/
    fun requestUpdate(from: Side) {
        if (from == Side.Client)
            this.world?.let { Network.sendToClientsWithBlockLoaded(syncRequestPacket, this.pos, it) }
        else
            Network.sendToServer(syncRequestPacket)
    }

    /**Gets the update tag**/
    override fun getUpdateTag(): CompoundNBT = with(nodeGraph.resolve()) {
        val tag = super.getUpdateTag()
        if (isEmpty) return tag
        tag.put("graph", get().serializeNBT())
        return tag
    }

    /**Attempts to read the graph from the given tag**/
    override fun read(state: BlockState, tag: CompoundNBT) {
        super.read(state, tag)
        nodeGraph.ifPresent { it.deserializeNBT(tag.getCompound("graph")) }
    }

    /**Attempts to write the graph**/
    override fun write(compound: CompoundNBT): CompoundNBT {
        val tag = super.write(compound)
        nodeGraph.ifPresent { tag.put("graph", it.serializeNBT()) }
        return tag
    }

    /**Gets the graph capability when needed**/
    override fun <T : Any?> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (cap == GRAPH_CAPABILITY)
            return nodeGraph.cast()
        return super.getCapability(cap, side)
    }


}