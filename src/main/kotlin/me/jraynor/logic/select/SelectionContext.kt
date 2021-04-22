package me.jraynor.logic.select

import me.jraynor.api.core.*
import me.jraynor.api.network.*
import net.minecraft.client.*
import net.minecraft.entity.player.*
import net.minecraft.util.*
import net.minecraft.util.math.*
import net.minecraft.util.math.vector.*
import net.minecraft.world.*
import net.minecraftforge.fml.*
import net.minecraftforge.fml.network.*
import java.util.*
import kotlin.collections.HashSet

/*Stores the current selection data**/
class SelectionContext(
    var node: ISelectable? = null,
    var activeColor: Vector4f? = null,
    var currentBlock: BlockPos? = null,
    var currentFace: Direction? = null
) {
    /**Used to disable block punching on the server.**/
    internal val selectingPlayers = HashSet<UUID>()

    /**This is true if the selection is ready to be used**/
    val valid: Boolean get() = node != null && !node!!.graph.isEmpty() && activeColor != null

    /**The local player, which is used for the block selection**/
    val player: PlayerEntity? get() = Minecraft.getInstance().player

    /**This will get the local world instance**/
    val world: World? get() = Minecraft.getInstance().world

    /**This is true if the selection is read to be used**/
    val ready: Boolean get() = valid && currentBlock != null && currentFace != null

    /***
     * This will be fired on the server when the client sends an update packet
     */
    @NetEvent(LogicalSide.SERVER, checkSide = false)
    fun onBlockSelect(packet: PacketSelection, context: NetworkEvent.Context): Boolean {
        selectingPlayers.add(packet.uuid!!)
        println("Got start selection ${packet.uuid}")
        return true
    }

    /**When we're done with the selection, we invalidate.**/
    fun invalidate() {
        node = null
        currentBlock = null
        currentFace = null
        activeColor = null
    }

}