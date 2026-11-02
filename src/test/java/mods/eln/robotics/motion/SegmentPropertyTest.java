package mods.eln.robotics.motion;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;

import static org.junit.jupiter.api.Assertions.*;

/** The servo motion profile: continuous, within its limits, arriving on target; a retarget mid-move is seamless. */
class SegmentPropertyTest {
    static void checkProfile(Segment s, String what) {
        double dur = s.duration();
        assertTrue(Double.isFinite(dur) && dur >= 0, what + " duration " + dur);
        double step = Math.max(dur / 400, 1e-3);
        double prevP = s.position(0), prevV = s.velocity(0);
        double vlim = Math.max(s.vmax(), Math.abs(s.v0())) + 1e-9;
        for (double t = step; t <= dur + 5 * step; t += step) {
            double p = s.position(t), v = s.velocity(t);
            assertTrue(Math.abs(v) <= vlim, what + " speed " + v + " > " + vlim + " at " + t);
            assertTrue(Math.abs(v - prevV) <= s.accel() * step + 1e-9, what + " acceleration jump at " + t);
            assertTrue(Math.abs(p - prevP) <= vlim * step + 1e-9, what + " position jump at " + t);
            prevP = p;
            prevV = v;
        }
        assertEquals(s.to(), s.position(dur + 1), 1e-9, what + " arrives");
        assertEquals(0, s.velocity(dur + 1), 1e-12, what + " at rest");
    }

    @Property(tries = 500)
    void movesAreSmoothAndArrive(@ForAll @DoubleRange(min = -20, max = 20) double from, @ForAll @DoubleRange(min = -20, max = 20) double to,
                                 @ForAll @DoubleRange(min = -0.3, max = 0.3) double v0, @ForAll @DoubleRange(min = 0.01, max = 0.3) double vmax,
                                 @ForAll @DoubleRange(min = 0.002, max = 0.05) @net.jqwik.api.constraints.Scale(4) double accel) {
        checkProfile(new Segment(0, from, v0, to, vmax, accel), "move");
    }

    @Property(tries = 300)
    void retargetIsSeamless(@ForAll @DoubleRange(min = -10, max = 10) double to1, @ForAll @DoubleRange(min = -10, max = 10) double to2,
                            @ForAll @DoubleRange(min = 0, max = 200) double at, @ForAll @DoubleRange(min = 0.02, max = 0.2) double vmax2) {
        Segment a = new Segment(100, 0, 0, to1, 0.1, 0.01);
        long t = 100 + (long) at;
        Segment b = a.retarget(t, to2, vmax2, 0.01);
        assertEquals(a.position(t - 100), b.position(0), 1e-9, "same place");
        assertEquals(a.velocity(t - 100), b.velocity(0), 1e-9, "same speed");
        checkProfile(b, "retarget");
    }
}
