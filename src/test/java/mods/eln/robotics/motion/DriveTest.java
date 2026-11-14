package mods.eln.robotics.motion;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The drive's speed rule (the line's voltage) and its energy rule (never further than the energy got pays for). */
class DriveTest {
    final Drive lift = new Drive(200, 2.0, 1.0, 50, 0.8, 100);

    @Test
    void speedFollowsTheLine() {
        assertEquals(2.0, lift.vmaxAt(1), 1e-9);
        assertEquals(1.0, lift.vmaxAt(0.5), 1e-9, "half voltage, half speed");
        assertEquals(2.2, lift.vmaxAt(1.5), 1e-9, "a little over nominal at most");
        assertEquals(0, lift.vmaxAt(0.2), "under the stall share it holds");
        assertEquals(0, lift.vmaxAt(0), "not running");
    }

    @Test
    void liftingCostsWork() {
        assertEquals(50 + 9.81 * 100 * 2 / 0.8, lift.ratedWatts(), 1e-9, "idle plus its rated load at full speed");
        assertEquals(1000 * 0.1 / 0.8, lift.joules(0.1, 1000), 1e-12, "up against a net weight");
        assertEquals(1000 * 0.1 / 0.8, lift.joules(-0.1, -1000), 1e-12, "down, lifting the counterweight");
        assertEquals(0, lift.joules(-0.1, 1000), "lowering a net weight costs nothing");
        assertEquals(0.05, lift.paidMetres(0.1, 1000, 1000 * 0.05 / 0.8), 1e-12, "half the energy, half the way");
        assertEquals(-0.1, lift.paidMetres(-0.1, 1000, 0), "lowering needs none");
    }

    /** A move cut short by a starved supply: the axis stops where the energy ran out (from rest it stands still). */
    @Test
    void starvedFromRestItWaits() {
        Segment s = new Segment(0, 0, 0, 6, 0.1, 0.0025);
        Segment n = lift.afterPaying(s, 0, 1000, 0, 0);
        assertNotSame(s, n);
        assertEquals(1, n.startTick());
        assertEquals(0, n.from());
        assertEquals(0, n.v0());
        assertEquals(6, n.to());
        assertEquals(0.1, n.vmax(), "it goes on with its limits once paid");
    }

    /** Cut on an empty buffer, its new top speed is what the supply keeps delivering, not the tick's crumbs. */
    @Test
    void cutToWhatTheSupplyKeepsPaying() {
        Segment s = new Segment(0, 0, 0.05, 6, 0.05, 0.0025);   // cruising at 1 m/s
        double perTick = 1000 * 0.025 / 0.8;   // the line pays for half of it
        Segment n = lift.afterPaying(s, 10, 1000, 1e-6, perTick);
        assertEquals(1e-6 * 0.8 / 1000, n.v0(), 1e-15, "this tick: the crumbs");
        assertEquals(0.025, n.vmax(), 1e-12, "then: what the line keeps paying for");
    }

    /** Paid in full, the segment is unchanged; short, the new one starts where the paid step ends, no faster than paid. */
    @Property(tries = 1000)
    void neverFurtherThanPaid(@ForAll @DoubleRange(min = -10, max = 10) double to, @ForAll @DoubleRange(min = -0.2, max = 0.2) double v0,
                              @ForAll @DoubleRange(min = 0.005, max = 0.15) @net.jqwik.api.constraints.Scale(4) double vmax, @ForAll @IntRange(min = 0, max = 300) int at,
                              @ForAll @DoubleRange(min = -3000, max = 3000) double force, @ForAll @DoubleRange(min = 0, max = 1.2) double share,
                              @ForAll @DoubleRange(min = 0, max = 1.5) double sustainShare) {
        Segment s = new Segment(0, 0, v0, to, vmax, 0.0025);
        double p = s.position(at), dx = s.position(at + 1) - p;
        double need = lift.joules(dx, force), got = need * share;
        double paid = lift.paidMetres(dx, force, got);
        assertTrue(Math.abs(paid) <= Math.abs(dx) + 1e-15 && paid * dx >= 0, "within the planned step, same way: " + paid + " of " + dx);
        assertTrue(lift.joules(paid, force) <= got * (1 + 1e-9) + 1e-12, "paid for: " + lift.joules(paid, force) + " > " + got);
        if (force * dx <= 0) assertEquals(dx, paid, "lowering (or holding) costs nothing");
        Segment n = lift.afterPaying(s, at, force, got, need * sustainShare);
        if (share >= 1) assertEquals(dx, paid, "paid in full");
        if (paid == dx) {   // (also a cost under the buffer's rounding: a settling step of 1e-17 m)
            assertSame(s, n, "paid in full: no new segment");
            return;
        }
        assertEquals(at + 1, n.startTick());
        assertEquals(p + paid, n.position(0), 1e-12, "continuous: it starts where the paid step ends");
        assertEquals(paid, n.velocity(0), 1e-12, "at the speed paid for");
        assertEquals(s.to(), n.to(), "toward the same target");
        double sustained = Math.abs(lift.paidMetres(dx, force, need * sustainShare));
        if (paid != 0 || sustained != 0) {
            assertTrue(n.vmax() <= Math.max(Math.abs(paid), sustained) + 1e-15, "its new top speed is no more than the supply pays for");
            assertTrue(n.vmax() <= s.vmax() + 1e-15, "nor its old one");
        } else assertEquals(s.vmax(), n.vmax(), "paid nothing: it waits with its limits");
        SegmentPropertyTest.checkProfile(n, "after a shortfall");
    }
}
