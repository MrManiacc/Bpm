package me.jraynor.api.render

import com.mojang.blaze3d.matrix.*
import me.jraynor.api.capability.*
import me.jraynor.api.core.*
import me.jraynor.api.utilities.enums.*
import net.minecraft.client.gui.screen.*
import net.minecraft.util.text.*

/*This allows us to have a renderable screen on the api.**/
abstract class AbstractNodeScreen(val parent: GraphTileEntity, val graphIn: IGraph? = null) : Screen(StringTextComponent("")) {
    /**This keeps track of the correct window name**/
    protected val name = "instance_${currentInstance++}"

    /**public accessor for the graph**/
    protected val graph: IGraph
        get() = parent.getCapability(CapabilityGraphHandler.GRAPH_CAPABILITY)
            .orElseGet { graphIn ?: IGraph.Empty }

    /**This should update the graph on the server hopefully**/
    fun pushUpdate() = parent.pushUpdate(Side.Server)

    /**This will render our node screen**/
    override fun render(matrixStack: MatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        UserInterface.frame {
            UserInterface.dockspace(name, this::renderVariables, this::renderGraph)
        }
    }

    /**This will render our main content area.**/
    protected abstract fun renderGraph()

    /**This will render the side panel/properties window.**/
    protected abstract fun renderVariables()

    companion object {
        private var currentInstance = 0
    }
}