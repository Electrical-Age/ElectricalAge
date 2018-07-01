package mods.eln.misc;

import com.google.common.base.Objects;
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
            (float) Math.min(min.xCoord, other.min.xCoord),
            (float) Math.max(max.xCoord, other.max.xCoord),
            (float) Math.min(min.yCoord, other.min.yCoord),
            (float) Math.max(max.yCoord, other.max.yCoord),
            (float) Math.min(min.zCoord, other.min.zCoord),
            (float) Math.max(max.zCoord, other.max.zCoord)
        );
    }

    public Vec3d centre() {
        return new Vec3d(
            min.xCoord + (max.xCoord - min.xCoord) / 2,
            min.yCoord + (max.yCoord - min.yCoord) / 2,
            min.zCoord + (max.zCoord - min.zCoord) / 2
        );
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("min", min)
            .add("max", max)
            .toString();
    }
}
