package me.jraynor.logic.select

import me.jraynor.api.network.*
import me.jraynor.api.select.render.*
import me.jraynor.api.utilities.*
import me.jraynor.objects.screen.*
import me.jraynor.objects.tile.*
import net.minecraft.client.*
import net.minecraft.util.*
import net.minecraft.util.math.*
import net.minecraft.util.math.vector.*
import net.minecraftforge.client.event.*
import net.minecraftforge.event.*
import net.minecraftforge.event.entity.player.*
import net.minecraftforge.eventbus.api.*
import thedarkcolour.kotlinforforge.eventbus.*
import java.util.HashSet
import kotlin.collections.HashMap

/**This object allows for easy selections of blocks**/
internal object SelectionHelper {
    /**Our internal renderer**/
    private val renderData = RenderData()

    /**Our current selection data. Keeps track of the node data and onSelect callback**/
    private val context: SelectionContext = SelectionContext()

    /**This is used to keep track of the currently rendered link nodes**/
    private val overlays = HashMap<Pair<BlockPos, Direction>, Vector4f>()

    /**allows for removing of the overlays when a tile is unloaded or removed.**/
    private val tileOverlays = HashMap<BlockPos, MutableList<Pair<BlockPos, Direction>>>()

    /**This will register both our network callback and our listeners**/
    fun register(forgeBus: KotlinEventBus) {
        whenClient(false) {
            forgeBus.addListener(EventPriority.HIGH, this::onTick)
            forgeBus.addListener(EventPriority.HIGH, this::onRender)
            forgeBus.addListener(EventPriority.HIGHEST, this::onClickAir)
        }
        forgeBus.addListener(EventPriority.HIGHEST, this::onClickBlock) //This should be registered on both the client and server.
    }

    /**This registers the context to the network**/
    internal fun registerCtx() = Network.addListeners(context)

    /**Starts a new selection context**/
    fun start(node: ISelectable, color: Vector4f) {
        Network.sendToServer(PacketSelection(Minecraft.getInstance().player?.uniqueID))
        context.node = node
        context.activeColor = color
        node.shouldClose = true
    }

    /**
     * This is called upon the client tick
     */
    private fun onTick(event: TickEvent.ClientTickEvent) {
        if (context.valid) {
            if (context.player == null || context.world == null)
                context.invalidate()
            else {
                val result = rayTraceBlock(context.player!!, context.world!!, 75)
                if (!result.isInside) {
                    context.currentBlock = result.pos
                    context.currentFace = result.face
                }
            }
        }
    }

    /**
     * This is called upon the client tick
     */
    private fun onRender(event: RenderWorldLastEvent) {
        renderData.start(event)
        if (context.ready)
            renderData.drawFaceOverlay(context.currentBlock!!, context.currentFace!!, context.activeColor!!)
        overlays.forEach {
            renderData.drawFaceOverlay(it.key.first, it.key.second, it.value)
        }
        renderData.stop()
    }

    /**
     * This is called upon the client tick
     */
    private fun onClickAir(event: PlayerInteractEvent.LeftClickEmpty) {
        if (context.ready) {
            finish()
        }
    }

    /**
     * This is called upon the client tick
     */
    private fun onClickBlock(event: PlayerInteractEvent.LeftClickBlock) {
        whenClient {
            if (context.ready) {
                finish()
                event.isCanceled = true
            }
        }
        whenServer {
            if (context.selectingPlayers.contains(event.player.uniqueID)) {
                event.isCanceled = true
                context.selectingPlayers.remove(event.player.uniqueID)
            }
        }
    }

    /**Finished the selection, and re-opens the screen with the correct tile/graph*/
    private fun finish() {
        context.node!!.onFinishSelection(Selection(context.currentBlock!!, context.currentFace!!))
        val tile = context.node!!.graph.parent
        if (tile is SingularityTile) {
            Minecraft.getInstance().displayGuiScreen(SingularityScreen(tile, context.node?.graph))
            if (!this.tileOverlays.containsKey(tile.pos))
                this.tileOverlays[tile.pos] = ArrayList()
            tileOverlays[tile.pos]!!.add(Pair(context.currentBlock!!, context.currentFace!!))
        }
        context.invalidate()
    }

    /**Shows the given block**/
    fun show(blockFace: Pair<BlockPos, Direction>, color: Vector4f) =
        this.overlays.apply { this[blockFace] = color }

    /**Hides the given block/face**/
    fun hide(blockFace: Pair<BlockPos, Direction>) =
        this.overlays.remove(blockFace)

    /**Clears all of the overlays.**/
    fun hideAll() = this.overlays.clear()

    /**Hides all of the overlays that have the [blockPos] as a key in the [tileOverlays]**/
    fun hideAll(blockPos: BlockPos) =
        this.tileOverlays[blockPos]?.forEach(::hide).also { this.tileOverlays.remove(blockPos) }


}