package mods.eln.misc;

import com.google.common.base.MoreObjects;
import net.minecraft.util.math.Vec3d;


public class BoundingBox {
    public final Vec3d min, max;

    public BoundingBox(float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
        min = new Vec3d(xMin, yMin, zMin);
        max = new Vec3d(xMax, yMax, zMax);
    }

    public static BoundingBox mergeIdentity() {
        return new BoundingBox(
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY
        );
    }

    public BoundingBox merge(BoundingBox other) {
        return new BoundingBox(
            (float) Math.min(min.x, other.min.x),
            (float) Math.max(max.x, other.max.x),
            (float) Math.min(min.y, other.min.y),
            (float) Math.max(max.y, other.max.y),
            (float) Math.min(min.z, other.min.z),
            (float) Math.max(max.z, other.max.z)
        );
    }

    public Vec3d centre() {
        return new Vec3d(
            min.x + (max.x - min.x) / 2,
            min.y + (max.y - min.y) / 2,
            min.z + (max.z - min.z) / 2
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("min", min)
            .add("max", max)
            .toString();
    }
}
