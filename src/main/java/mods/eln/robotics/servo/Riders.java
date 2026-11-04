package mods.eln.robotics.servo;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Who rides a carriage, and how far they go with it: pure box arithmetic, shared by the server (mobs, items) and the
 * client (its own player), so it is unit-tested without a game.
 */
public final class Riders {
    private Riders() {
    }

    /** A rider's feet are within this of the carriage top (m), or within a tick's step if that is more. */
    public static final double TOLERANCE = 0.15;
    /**
     * A player's position reaches the server a round trip late, so a moving car's own passenger stands up to this far
     * below (rising) or above (sinking) its top there (m; ~10 ticks at 2 m/s). The server never moves players: their
     * clients carry them.
     */
    public static final double PLAYER_LAG = 1.0;
    /** Horizontal overlap needed to stand on it (m). */
    static final double OVERLAP = 1e-3;

    /** The rider tolerance for a step of this length (m). */
    public static double tolerance(double step) {
        return Math.max(TOLERANCE, Math.abs(step));
    }

    /** Does box e stand on the carriage box car: its bottom within tol of the top, overlapping it across? */
    public static boolean onTop(AABB e, AABB car, double tol) {
        return e.minY >= car.maxY - tol && e.minY <= car.maxY + tol && across(e, car);
    }

    /** A player as the server sees it: on top, give or take PLAYER_LAG (a passenger whose moves are late). */
    public static boolean playerOnTop(AABB e, AABB car, double tol) {
        double lag = Math.max(tol, PLAYER_LAG);
        return e.minY >= car.maxY - lag && e.minY <= car.maxY + lag && across(e, car);
    }

    /** Do the boxes overlap in x and z? */
    public static boolean across(AABB a, AABB b) {
        return a.minX < b.maxX - OVERLAP && a.maxX > b.minX + OVERLAP && a.minZ < b.maxZ - OVERLAP && a.maxZ > b.minZ + OVERLAP;
    }

    /** One carry: whether the rider goes along, by how much, and whether it stays standing (onGround) afterwards. */
    public record Carry(boolean carried, Vec3 delta, boolean keepOnGround) {
        public static final Carry NONE = new Carry(false, Vec3.ZERO, false);
    }

    /**
     * The client's carry of its own player, after the player's tick, as the carriage goes from box car0 to car1 (moved
     * by delta along its axis). A standing rider lands exactly on the new top of a vertical carriage (no creeping gap or
     * sink); one in the air goes by the step. Not a rider, or no move: NONE.
     */
    public static Carry carry(AABB player, boolean onGround, AABB car0, AABB car1) {
        Vec3 step = new Vec3(car1.minX - car0.minX, car1.minY - car0.minY, car1.minZ - car0.minZ);
        if (step.lengthSqr() < 1e-12) return Carry.NONE;
        if (!onTop(player, car0, tolerance(step.length()))) return Carry.NONE;
        boolean vertical = Math.abs(step.x) < 1e-9 && Math.abs(step.z) < 1e-9;
        Vec3 d = vertical && onGround ? new Vec3(0, car1.maxY - player.minY, 0) : step;
        return new Carry(true, d, onGround);
    }

    /** A box of the rider's size standing on the carriage top, clipped across to the carriage (the server's sweep). */
    public static AABB standing(AABB e, AABB car) {
        double h = e.maxY - e.minY;
        return new AABB(Math.max(e.minX, car.minX), car.maxY, Math.max(e.minZ, car.minZ),
            Math.min(e.maxX, car.maxX), car.maxY + h, Math.min(e.maxZ, car.maxZ));
    }
}
