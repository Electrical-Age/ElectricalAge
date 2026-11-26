package mods.eln.robotics.motion;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** A straight path from origin along a block axis, length metres long (an elevator's mast, a floor rail). */
public record StraightPath(Vec3 origin, Direction axis, double length) implements Path {
    @Override
    public Vec3 at(double s) {
        return origin.add(axis.getStepX() * s, axis.getStepY() * s, axis.getStepZ() * s);
    }

    @Override
    public Vec3 tangent(double s) {
        return new Vec3(axis.getStepX(), axis.getStepY(), axis.getStepZ());
    }

    @Override
    public boolean axisAligned() {
        return true;
    }
}
