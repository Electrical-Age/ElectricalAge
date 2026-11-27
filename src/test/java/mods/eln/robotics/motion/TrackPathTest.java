package mods.eln.robotics.motion;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** TrackPath: straight legs with quarter-arc corners; arc-length s, smooth, stops on whole blocks of the straights. */
class TrackPathTest {
    final Vec3 o = new Vec3(10.5, 64, -3.5);

    static TrackPath.Leg leg(Direction d, int n) {
        return new TrackPath.Leg(d, n);
    }

    @Test
    void oneLegIsAStraightPath() {
        for (Direction d : Direction.values()) {
            var t = new TrackPath(o, List.of(leg(d, 12)), new double[0]);
            var s = new StraightPath(o, d, 12);
            for (double x : new double[]{0, 0.3, 7.25, 12})
                assertEquals(0, t.at(x).distanceTo(s.at(x)), 1e-12, d + " at " + x);
            assertEquals(12, t.length(), 1e-12);
            assertEquals(13, t.stops().length, "a stop every metre, both ends");
            assertTrue(t.axisAligned());
        }
    }

    @Test
    void aCornerIsAQuarterCircle() {
        // 6 east, then 5 south, corner radius 3: the arc from (o + 3 east) to (corner + 3 south), centred 3 south of its start
        var t = new TrackPath(o, List.of(leg(Direction.EAST, 6), leg(Direction.SOUTH, 5)), new double[]{3});
        assertEquals(3 + 3 * Math.PI / 2 + 2, t.length(), 1e-12);
        Vec3 c = o.add(3, 0, 3);
        for (double s = 3; s <= 3 + 3 * Math.PI / 2; s += 0.1)
            assertEquals(3, t.at(s).distanceTo(c), 1e-9, "on the circle at " + s);
        assertEquals(0, t.at(3 + 3 * Math.PI / 2).distanceTo(o.add(6, 0, 3)), 1e-9, "joins the second leg");
        assertEquals(3, t.minRadius(0, t.length()), 1e-12);
        assertEquals(Double.POSITIVE_INFINITY, t.minRadius(0, 2.9));
        assertFalse(t.axisAligned());
        // stops: the two ends only (a corner that turns is no stop)
        assertArrayEquals(new double[]{0, t.length()}, t.stops(), 1e-9);
    }

    /** Corners at any angle (a base not on the block grid): the arc is tangent to both legs, of the given radius. */
    @Test
    void obliqueCornersAndSlopes() {
        Vec3 c1 = o.add(8, 0, 0), c2 = c1.add(6, 1.5, 6);   // (a 45-degree turn that also starts a gentle climb)
        var t = new TrackPath(List.of(o, c1, c2), new double[]{4});
        double th = Math.acos(new Vec3(1, 0, 0).dot(c2.subtract(c1).normalize()));
        double cut = 4 * Math.tan(th / 2);
        assertEquals(8 - cut + 4 * th + c2.distanceTo(c1) - cut, t.length(), 1e-9);
        assertEquals(0, t.at(t.length()).distanceTo(c2), 1e-9, "ends at the last corner");
        assertEquals(0, t.tangent(8 - cut + 4 * th).subtract(c2.subtract(c1).normalize()).length(), 1e-9, "leaves the arc along the next leg");
        assertEquals(4, t.minRadius(0, t.length()), 1e-12);
        assertFalse(t.axisAligned());
        assertTrue(t.grade(t.length() - 0.5) > 0, "climbs on the second leg");
    }

    /** Stops are the ends and the points it runs straight through: clicked on block centres, at any heading. */
    @Test
    void stopsSitOnWholeBlocks() {
        Vec3 a = o.add(5, 0, 0), b = a.add(10, 0, 4), end = b.add(3, 0, -7);   // (corners at a and b turn)
        Vec3 mid = a.add(5, 0, 2);   // (a station half way along a-b: straight through)
        var t = new TrackPath(List.of(o, a, mid, b, end), new double[]{2, 0, 3});
        assertEquals(3, t.stops().length, "both ends and the station");
        assertEquals(0, t.at(t.stops()[1]).distanceTo(mid), 1e-9, "the station is where it was clicked");
        for (double s : t.stops()) {
            Vec3 p = t.at(s).subtract(o);
            for (double c : new double[]{p.x, p.y, p.z}) assertEquals(Math.rint(c), c, 1e-9, "stop at " + s + ": " + p);
        }
    }

    @Test
    void refusesWhatCantBeBuilt() {
        assertThrows(IllegalArgumentException.class, () -> new TrackPath(o, List.of(leg(Direction.EAST, 2), leg(Direction.SOUTH, 5)), new double[]{3}));
        assertThrows(IllegalArgumentException.class, () -> new TrackPath(o, List.of(leg(Direction.EAST, 6), leg(Direction.WEST, 5)), new double[]{3}));
        assertThrows(IllegalArgumentException.class, () -> new TrackPath(o, List.of(leg(Direction.EAST, 6), leg(Direction.SOUTH, 5)), new double[0]));
    }

    /** Random tracks of up to 6 legs: smooth (no jump, the tangent turns continuously), unit tangent, arc length honest. */
    @Property(tries = 300)
    void randomTracksAreSmooth(@ForAll @IntRange(min = 1, max = 6) int n, @ForAll @IntRange(min = 0, max = 1 << 20) int seed) {
        var rnd = new java.util.Random(seed);
        List<TrackPath.Leg> legs = new ArrayList<>();
        double[] radii = new double[n - 1];
        Direction d = Direction.values()[rnd.nextInt(6)];
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                Direction prev = d;
                do d = Direction.values()[rnd.nextInt(6)]; while (d.getAxis() == prev.getAxis());
                radii[i - 1] = 1 + rnd.nextInt(4);
            }
            legs.add(leg(d, 0));
        }
        for (int i = 0; i < n; i++) {   // (long enough for both corners)
            double need = (i > 0 ? radii[i - 1] : 0) + (i < n - 1 ? radii[i] : 0);
            legs.set(i, leg(legs.get(i).dir(), (int) Math.ceil(need) + rnd.nextInt(5)));
        }
        if (legs.get(0).blocks() == 0 && n == 1) legs.set(0, leg(legs.get(0).dir(), 1));
        var t = new TrackPath(o, legs, radii);
        double h = 0.01;
        Vec3 prev = t.at(0), prevT = t.tangent(0);
        for (double s = h; s <= t.length() + 1e-9; s += h) {
            Vec3 p = t.at(s), tg = t.tangent(s);
            assertEquals(1, tg.length(), 1e-9, "unit tangent at " + s);
            assertTrue(p.distanceTo(prev) <= h + 1e-9, "no jump at " + s);
            assertTrue(p.distanceTo(prev) >= h * 0.99 - 1e-9, "arc length honest at " + s);
            assertTrue(tg.dot(prevT) > Math.cos(h / 0.99 + 1e-6), "the tangent turns continuously at " + s);
            prev = p;
            prevT = tg;
        }
        double[] st = t.stops();
        assertEquals(0, st[0], 1e-12);
        assertEquals(t.length(), st[st.length - 1], 1e-9);
    }
}
