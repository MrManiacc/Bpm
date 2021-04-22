package me.jraynor.logic.vars

import imgui.type.*
import me.jraynor.api.utilities.*
import net.minecraft.nbt.*
import net.minecraft.util.*
import net.minecraft.util.math.*
import net.minecraft.util.math.vector.*
import net.minecraftforge.common.util.*

/**This is used to store the variable types**/
class VarData(
    internal val imBoolean: ImBoolean = ImBoolean(false),
    internal val imInt: ImInt = ImInt(0),
    internal val imFloat: ImFloat = ImFloat(0f),
    internal val imVec2: FloatArray = floatArrayOf(0f, 0f),
    internal val imVec3: FloatArray = floatArrayOf(0f, 0f, 0f),
    internal val imBlockPos: IntArray = intArrayOf(0, 0, 0),
    internal val imColor: FloatArray = floatArrayOf(0.25882352941f, 0.7098039f, 0.7803921f, 0.85f),
    internal var type: VarType
) : INBTSerializable<CompoundNBT> {
    /**Gets the value of the boolean**/
    var bool: Boolean = imBoolean.get()
        get() = imBoolean.get()
        set(value) {
            field = value.apply { imBoolean.set(this) }
        }

    /**Gets the value of the int**/
    var int: Int = imInt.get()
        get() = imInt.get()
        set(value) {
            field = value.apply { imInt.set(this) }
        }

    /**Gets the value of the float**/
    var float: Float = imFloat.get()
        get() = imFloat.get()
        set(value) {
            field = value.apply { imFloat.set(this) }
        }

    /**Gets the value of the vec2**/
    var vec2: Vector2f = Vector2f(this.imVec2[0], this.imVec2[1])
        get() = Vector2f(this.imVec2[0], this.imVec2[1])
        set(value) {
            field = value.apply { imVec2[0] = this.x; imVec2[1] = this.y }
        }

    /**Gets the value of the vec2**/
    var vec3: Vector3f = Vector3f(this.imVec3[0], this.imVec3[1], this.imVec3[2])
        get() = Vector3f(this.imVec3[0], this.imVec3[1], this.imVec3[2])
        set(value) {
            value.let { imVec3[0] = it.x; imVec3[1] = it.y; imVec3[2] = it.z }
            field = value
        }

    /**Gets the value of the vec2**/
    var blockPos: BlockPos = BlockPos(this.imBlockPos[0], this.imBlockPos[1], this.imBlockPos[2])
        get() = BlockPos(this.imBlockPos[0], this.imBlockPos[1], this.imBlockPos[2])
        set(value) {
            value.let { imBlockPos[0] = it.x; imBlockPos[1] = it.y; imBlockPos[2] = it.z }
            field = value
        }

    /**Gets the color value of the variable**/
    var color: Vector4f = Vector4f(this.imColor[0], this.imColor[1], this.imColor[2], this.imColor[3])
        get() = Vector4f(this.imColor[0], this.imColor[1], this.imColor[2], this.imColor[3])
        set(value) {
            value.let { imColor[0] = it.x; imColor[1] = it.y; imColor[2] = it.z; imColor[3] = it.w }
            field = value
        }

    /**Keeps track of the current face**/
    var face: Direction = Direction.NORTH

    /**Gets the value of type t or null if it's not a valid value.**/
    inline fun <reified T : Any> get(): T? {
        when (T::class) {
            Boolean::class -> return this.bool as T?
            Int::class -> return this.int as T?
            Float::class -> return this.float as T?
            Vector2f::class -> return this.vec2 as T?
            Vector3f::class -> return this.vec3 as T?
            BlockPos::class -> return this.blockPos as T?
            Vector4f::class -> return this.color as T?
        }
        return null
    }

    /**Writes all of our variable to a compound**/
    override fun serializeNBT(): CompoundNBT {
        val tag = CompoundNBT()
        tag.putEnum("var_type", type)
        tag.putBoolean("var_bool", this.bool)
        tag.putInt("var_int", this.int)
        tag.putFloat("var_float", this.float)
        tag.putVec2("var_vec2", this.vec2)
        tag.putVec3("var_vec3", this.vec3)
        tag.putBlockPos("var_blockpos", this.blockPos)
        tag.putEnum("var_face", this.face)
        tag.putVec4("var_color", this.color)
        return tag
    }

    /**Read all of our variables from the compound**/
    override fun deserializeNBT(tag: CompoundNBT) {
        this.type = tag.getEnum("var_type")
        this.bool = tag.getBoolean("var_bool")
        this.int = tag.getInt("var_int")
        this.float = tag.getFloat("var_float")
        this.vec2 = tag.getVec2("var_vec2")
        this.vec3 = tag.getVec3("var_vec3")
        this.blockPos = tag.getBlockPos("var_blockpos")
        this.face = tag.getEnum("var_face")
        this.color = tag.getVec4("var_color")
    }
}