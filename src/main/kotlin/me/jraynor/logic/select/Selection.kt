package me.jraynor.logic.select

import net.minecraft.util.*
import net.minecraft.util.math.*

/**Simply stores a face with a direction. This is what is passed to a node.**/
data class Selection(val blockPos: BlockPos, val blockFace: Direction)