package mods.eln.robotics.motion;

import net.minecraft.world.phys.Vec3;

/**
 * Where a servo carriage is at each distance along its track (Baughn 2026-11-26: one track system for elevators, farm
 * carriages and the like, straight first, curved later). A Segment says how far along the carriage is at each tick; a
 * path turns that distance (metres from its start) into a place and a direction. A pure function of s, so server and
 * clients place a carriage alike from the same segment and path.
 */
public interface Path {
    /** Its length (m); a carriage's travel is at most this. */
    double length();

    /** The carriage's reference point (its plate's bottom centre) at distance s along the path. */
    Vec3 at(double s);

    /** The unit direction of travel at distance s. */
    Vec3 tangent(double s);

    /** How steeply it climbs at s: the tangent's vertical share (1 straight up, 0 level). */
    default double grade(double s) {
        return tangent(s).y;
    }

    /** How far along the path (m, unclamped) the point p lies, measured near distance s: p's offset along the tangent there. */
    default double along(Vec3 p, double s) {
        return s + p.subtract(at(s)).dot(tangent(s));
    }

    /** Straight and along a block axis: a walled cabin, whose colliders can't turn, may ride it. */
    boolean axisAligned();
}
