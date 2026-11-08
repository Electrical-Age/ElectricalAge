package mods.eln.anim;

import mods.eln.misc.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Particles at a rate: {@link #step} gives how many to spawn this frame for perSecond (the parts of one carried over),
 * and {@link #vent} turns a point of the model (its frame: front XN, block units from the block's centre) to world
 * coordinates for the device's facing, so the puffs come out of the drawn vent whichever way it was placed.
 */
public final class Emitter {
    double due;

    /** How many to spawn for perSecond over dt (stepped in refresh(dt)); at most 50 a frame. */
    public int step(double dt, double perSecond) {
        if (perSecond <= 0 || dt <= 0) {
            due = Math.min(due, 1);
            return 0;
        }
        due += dt * perSecond;
        int n = (int) Math.min(50, Math.floor(due));
        due -= Math.floor(due);
        return n;
    }

    /** How far the model is turned about y for a device facing front (radians; as glRotateXnRef turns it): XN 0, ZP 90,
     *  XP 180, ZN 270 degrees. A model direction (dx, dz) is (dx cos + dz sin, -dx sin + dz cos) in the world. */
    public static double turn(Direction front) {
        return Math.toRadians(switch (front) {
            case XP -> 180;
            case ZP -> 90;
            case ZN -> 270;
            default -> 0;
        });
    }

    /** The model point (x, y, z: block units from the centre, model frame) in the world, for the block at pos facing front. */
    public static Vec3 vent(BlockPos pos, Direction front, double x, double y, double z) {
        double th = turn(front);
        double c = Math.cos(th), s = Math.sin(th);
        return new Vec3(pos.getX() + 0.5 + x * c + z * s, pos.getY() + 0.5 + y, pos.getZ() + 0.5 - x * s + z * c);
    }
}
