package mods.eln.misc;

import com.google.common.base.MoreObjects;
import net.minecraft.world.phys.Vec3;


public class BoundingBox {
    public final Vec3 min, max;

    public BoundingBox(float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
        min = new Vec3(xMin, yMin, zMin);
        max = new Vec3(xMax, yMax, zMax);
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

    public Vec3 centre() {
        return new Vec3(
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
