package mods.eln.oil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The condensate pump's centrifugal curve (Baughn 2026-11-03) and the chord the fluid solver steps it on. */
public class CondensatePumpTest {
    /** A 50V turbine's: 10 bar rated, ~2 g/s of condensate. */
    final CondensatePump p = CondensatePump.forTurbine(10e5, 0.002);

    @Test
    void shutsOffAtThirteenTenthsOfTheRating() {
        assertEquals(13e5, p.shutoffPa, 1);
        assertEquals(0, p.kgPerS(13e5), 1e-12);
        assertEquals(0, p.kgPerS(20e5), 1e-12);
        assertEquals(p.freeKgPerS, p.kgPerS(0), 1e-12);
        assertEquals(p.freeKgPerS, p.kgPerS(-1e5), 1e-12);
    }

    @Test
    void passesHalfAgainItsCondensateAtTheRating() {
        assertEquals(1.5 * 0.002, p.kgPerS(10e5), 1e-9);
        // a boiler at 12 bar (overclocked): short of the condensate at full power
        assertTrue(p.kgPerS(12e5) < 0.002, "" + p.kgPerS(12e5));
    }

    @Test
    void flowFallsWithBackPressureSteeperNearShutoff() {
        double last = Double.MAX_VALUE, lastDrop = 0;
        for (double dp = 0; dp <= 13e5; dp += 0.5e5) {
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
    void theSolversChordMeetsTheCurveWithABoundedSlope() {
        double g0 = p.freeKgPerS / p.shutoffPa;
        for (double dp = 0; dp <= 13e5; dp += 0.25e5) {
            double s = p.chordShare(dp), g = s * g0;
            assertTrue(s >= 0.5 - 1e-9 && s <= 1 + 1e-9, "share " + s + " at " + dp);
            assertEquals(p.kgPerS(dp), g * (p.boost(dp) - dp), 1e-9, "at " + dp);
            assertEquals(p.freeKgPerS, g * p.boost(dp), 1e-9, "through free flow at " + dp);
        }
        // past the shut-off: the line through it, pushing back (the check valve shuts)
        assertTrue(p.chordShare(15e5) * g0 * (p.boost(15e5) - 15e5) < 0);
    }
}
