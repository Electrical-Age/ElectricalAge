package mods.eln.bike;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Dye aiming (AdventurAgent: side-on frame clicks missed, a saddle click painted the trunk): a look ray at each group's
 * middle, from every side it can be seen from, picks that group, whichever way the bike faces.
 */
public class BikeColorsTest {
    static final Vec3 POS = new Vec3(10.5, 64, -3.5);

    /** The world point at (side, up, forward) of a bike facing yaw. */
    static Vec3 world(float yaw, double side, double up, double forward) {
        double yr = Math.toRadians(yaw), fx = -Math.sin(yr), fz = Math.cos(yr);
        return POS.add(fx * forward + fz * side, up, fz * forward - fx * side);
    }

    /** Look at the bike-frame point from (ds, du, df) away from it (in the bike's frame), 2.5 blocks off. */
    static int look(float yaw, double[] at, double ds, double du, double df) {
        Vec3 target = world(yaw, at[0], at[1], at[2]);
        Vec3 from = world(yaw, at[0] + ds, at[1] + du, at[2] + df);
        Vec3 dir = target.subtract(from).normalize();
        Vec3 eye = target.subtract(dir.scale(2.5));
        return BikeColors.pick(eye, dir, POS, yaw);
    }

    static final double[] SADDLE = {0, 1.0, -0.17}, TRUNK = {0, 1.0, -0.63}, FORK = {0, 0.55, 0.6}, FRAME = {0, 0.6, 0.15};

    @Test
    void eachPartPicksItselfFromWhereItCanBeSeen() {
        for (float yaw : new float[]{0, 90, 217, -45}) {
            String y = " (yaw " + yaw + ")";
            // from either side, level: everything
            for (int s : new int[]{1, -1}) {
                assertEquals(BikeColors.SADDLE, look(yaw, SADDLE, s, 0, 0), "saddle from the side, not the trunk" + y);
                assertEquals(BikeColors.TRUNK, look(yaw, TRUNK, s, 0, 0), "trunk from the side" + y);
                assertEquals(BikeColors.FORK, look(yaw, FORK, s, 0, 0), "fork from the side" + y);
                assertEquals(BikeColors.FRAME, look(yaw, FRAME, s, 0, 0), "frame from the side" + y);
                // and a standing player's view, from the side and above
                assertEquals(BikeColors.FRAME, look(yaw, FRAME, s, 0.7, 0), "frame from the side, looking down" + y);
                assertEquals(BikeColors.SADDLE, look(yaw, SADDLE, s, 0.8, 0), "saddle from the side, looking down" + y);
            }
            // from above
            assertEquals(BikeColors.SADDLE, look(yaw, SADDLE, 0, 1, 0), "saddle from above" + y);
            assertEquals(BikeColors.TRUNK, look(yaw, TRUNK, 0, 1, 0), "trunk from above" + y);
            assertEquals(BikeColors.FORK, look(yaw, FORK, 0, 1, 0), "fork from above" + y);
            assertEquals(BikeColors.FRAME, look(yaw, FRAME, 0, 1, 0), "frame (top tube) from above" + y);
            // from in front and from behind
            assertEquals(BikeColors.FORK, look(yaw, FORK, 0, 0, 1), "fork from in front" + y);
            assertEquals(BikeColors.TRUNK, look(yaw, TRUNK, 0, 0, -1), "trunk from behind" + y);
            assertEquals(BikeColors.SADDLE, look(yaw, SADDLE, 0, 1.8, -1), "saddle from behind, over the trunk" + y);
            assertEquals(BikeColors.SADDLE, look(yaw, SADDLE, 0, 1.2, 1), "saddle from in front, over the bars" + y);
            // looking past it: nothing
            assertEquals(-1, look(yaw, new double[]{1.5, 1.0, 0}, 0, 0, 1), "a ray past the bike's side, along it" + y);
        }
    }
}
