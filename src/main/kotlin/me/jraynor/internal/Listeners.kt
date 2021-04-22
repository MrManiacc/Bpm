package me.jraynor.internal

import me.jraynor.*
import me.jraynor.api.capability.*
import me.jraynor.api.core.*
import me.jraynor.api.network.*
import me.jraynor.api.render.*
import me.jraynor.api.utilities.*
import me.jraynor.logic.select.*
import net.minecraft.client.*
import net.minecraft.world.*
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent
import net.minecraftforge.fml.event.server.*
import thedarkcolour.kotlinforforge.eventbus.KotlinEventBus

/**
 * This will listener to the client/server specific events.
 */
internal object Listeners {

    /**
     * This will register the listeners
     */
    fun register(modBus: KotlinEventBus, forgeBus: KotlinEventBus) {
        modBus.addListener(::onCommonSetup)
        modBus.addListener(::onLoadComplete)
        forgeBus.addListener(::onServerStopping)
        SelectionHelper.register(forgeBus)
    }

    /**
     * This will create the imgui instance
     */
    private fun onLoadComplete(event: FMLLoadCompleteEvent) {
        whenClient(logical = false) {
            runOnRender {
                UserInterface.init()
            }
        }
        SelectionHelper.registerCtx()
    }

    /**
     * This is for initializing anything that's shared acorss the client
     * and server.
     */
    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        Network.initialize(Bpm.ID, PacketSelection::class)
        CapabilityGraphHandler.register()
    }

    /**This will call the [GraphTileEntity.onUnload] for each tile in each world. This isn't the most efficient method,
     *  but it should only be called upon unloading of the world **/
    private fun onServerStopping(event: FMLServerStoppingEvent) {
        if (physicalClient) Minecraft.getInstance().world?.let { unloadFor(it) } //Do unload client sided ONLY when we're on a client distribution.
        event.server.worlds.forEach(::unloadFor)
        SelectionHelper.hideAll()
    }

    /**Does an unload of all [GraphTileEntity] for the given [World]**/
    private fun unloadFor(world: World) =
        world.loadedTileEntityList.filterIsInstance<GraphTileEntity>().forEach(GraphTileEntity::onUnload)

}