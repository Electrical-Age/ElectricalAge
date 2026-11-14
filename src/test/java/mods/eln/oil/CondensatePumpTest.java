package mods.eln.oil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The condensate pump's centrifugal curve (Baughn 2026-11-03) and the line the fluid solver steps it on. */
public class CondensatePumpTest {
    /** A 50V turbine's: 10 bar rated, ~2 g/s of condensate. */
    final CondensatePump p = CondensatePump.forTurbine(10e5, 0.002);

    @Test
    void shutsOffAtEighteenTenthsOfTheRating() {
        assertEquals(18e5, p.shutoffPa, 1);
        assertEquals(0, p.kgPerS(18e5), 1e-12);
        assertEquals(0, p.kgPerS(25e5), 1e-12);
        assertEquals(p.freeKgPerS, p.kgPerS(0), 1e-12);
        assertEquals(p.freeKgPerS, p.kgPerS(-1e5), 1e-12);
    }

    /** Boilers at their full mark shut their feed floats and the condensate line stands at the shut-off head over the
     *  condenser's vacuum: within what its pipe works to, so a sound plant's line doesn't smoke its warning (Baughn
     *  2026-11-14, from a player's plant: 20 bar on iron that worked to 10). */
    @Test
    void aDeadheadedLineIsWithinItsPipesWorkingPressure() {
        double air = mods.eln.sim.fluid.Water.P_ATM;
        assertTrue(p.shutoffPa + air < mods.eln.node.voxel.PipePart.WORKING_PA[0], "small turbine's on iron");
        double hp = CondensatePump.forTurbine(mods.eln.mechanical.SteamTurbineSpec.HP.nominalPa, 0.04).shutoffPa;
        assertTrue(hp + air < mods.eln.node.voxel.PipePart.WORKING_PA[2], "plant condenser's on large alloy: " + hp);
    }

    @Test
    void passesHalfAgainItsCondensateAtOneAndAHalfTimesTheRating() {
        assertEquals(1.5 * 0.002, p.kgPerS(15e5), 1e-9);
        // a governed turbine's boiler (~14 bar for a 10 bar turbine, AdventurAgent on live): its condensate and more
        assertTrue(p.kgPerS(14e5) > 1.5 * 0.002, "" + p.kgPerS(14e5));
        // a boiler at 17 bar (overclocked): short of the condensate at full power
        assertTrue(p.kgPerS(17e5) < 0.002, "" + p.kgPerS(17e5));
    }

    @Test
    void flowFallsWithBackPressureSteeperNearShutoff() {
        double last = Double.MAX_VALUE, lastDrop = 0;
        for (double dp = 0; dp <= p.shutoffPa; dp += 0.5e5) {
            double q = p.kgPerS(dp);
            assertTrue(q <= last, "monotonic at " + dp);
            if (last != Double.MAX_VALUE) {
                assertTrue(last - q >= lastDrop - 1e-12, "steeper at " + dp);
                lastDrop = last - q;
            }
            last = q;
        }
    }

    @Test
    void theSolversLineRunsFromTheCurveToTheShutoff() {
        double g0 = p.freeKgPerS / p.shutoffPa;
        for (double dp = -1e5; dp <= p.shutoffPa; dp += 0.25e5) {
            double s = p.lineShare(dp), g = s * g0;
            assertTrue(s > 0 && s <= CondensatePump.MAX_LINE_SHARE + 1e-9, "share " + s + " at " + dp);
            assertEquals(p.shutoffPa, p.boost(dp), 1e-6, "no flow at the shut-off, from " + dp);
            if (dp < p.shutoffPa - 0.01e5) assertEquals(p.kgPerS(dp), g * (p.boost(dp) - dp), 1e-9, "meets the curve at " + dp);
            // steeper than the curve's tangent wherever it runs (so a step can't overshoot)
            if (dp > 0.1e5 && dp < p.shutoffPa - 1e5) {
                double slope = (p.kgPerS(dp - 100) - p.kgPerS(dp + 100)) / 200;
                assertTrue(g >= slope, "steeper than the tangent at " + dp);
            }
        }
        // past the shut-off: the line through it, pushing back (the check valve shuts)
        assertTrue(p.lineShare(22e5) * g0 * (p.boost(22e5) - 22e5) < 0);
    }

    /**
     * Stepped against a liquid-full pipe that feeds a boiler through a tight valve, near the shut-off, the pump settles
     * on one pressure (Baughn 2026-11-03: the pipe from a condenser to its boiler swung 10.5..12.5 bar; the chord the
     * pump was stepped on overshot every step and the pipe flipped between two pressures).
     */
    @Test
    void settlesNearTheShutoffAgainstATightFeed() {
        for (double boilerPa : new double[]{11.5e5, 12.5e5, 17e5, 18.5e5}) {
            double shutoff = boilerPa < 15e5 ? 13e5 : 20e5;
            double[] r = PumpLoop.run(shutoff, boilerPa, 6e-15);
            assertTrue(r[1] - r[0] < 0.01e5, "pipe " + r[0] / 1e5 + ".." + r[1] / 1e5 + " bar into " + boilerPa / 1e5);
        }
    }
}
