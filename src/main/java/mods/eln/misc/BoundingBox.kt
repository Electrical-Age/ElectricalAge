package mods.eln.misc

import net.minecraft.util.math.Vec3d

@Deprecated("The Minecraft one should do fine.")
class BoundingBox(xMin: Float, xMax: Float, yMin: Float, yMax: Float, zMin: Float, zMax: Float) {
    val min: Vec3d = Vec3d(xMin.toDouble(), yMin.toDouble(), zMin.toDouble())
    val max: Vec3d = Vec3d(xMax.toDouble(), yMax.toDouble(), zMax.toDouble())

    fun merge(other: BoundingBox): BoundingBox {
        return BoundingBox(
                Math.min(min.x, other.min.x).toFloat(),
                Math.max(max.x, other.max.x).toFloat(),
                Math.min(min.y, other.min.y).toFloat(),
                Math.max(max.y, other.max.y).toFloat(),
                Math.min(min.z, other.min.z).toFloat(),
                Math.max(max.z, other.max.z).toFloat()
        )
    }

    fun centre(): Vec3d {
        return Vec3d(
                min.x + (max.x - min.x) / 2,
                min.y + (max.y - min.y) / 2,
                min.z + (max.z - min.z) / 2
        )
    }

    companion object {
        fun mergeIdentity(): BoundingBox {
            return BoundingBox(
                    java.lang.Float.POSITIVE_INFINITY,
                    java.lang.Float.NEGATIVE_INFINITY,
                    java.lang.Float.POSITIVE_INFINITY,
                    java.lang.Float.NEGATIVE_INFINITY,
                    java.lang.Float.POSITIVE_INFINITY,
                    java.lang.Float.NEGATIVE_INFINITY
            )
        }
    }

    override fun toString() = "min: $min, max: $max"
}
