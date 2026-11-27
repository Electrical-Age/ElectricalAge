package mods.eln.node.voxel;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Track Kit's track from clicked points: radii as wide as the legs allow, stops kept in line, refusals. */
class TrackPlacerTest {
    static final Vec3 O = new Vec3(0.5, 64, 0.5);

    @Test
    void cornersAreAsWideAsTheLegsAllow() {
        // 12 east, then 12 south: one corner; each leg keeps 1.5 m straight at its end (a stop), the rest is the arc's
        var t = TrackPlacer.lay(List.of(O, O.add(12, 0, 0), O.add(12, 0, 12)), new boolean[]{true, false, true});
        assertEquals(10.5, t.path().radius(0), 1e-9, "12 - 1.5 kept at the end, a right angle: r = cut");
        double[] st = t.path().stops();
        assertArrayEquals(new double[]{0, t.path().length()}, st, 1e-9);
    }

    @Test
    void twoCornersShareTheirLeg() {
        var t = TrackPlacer.lay(List.of(O, O.add(10, 0, 0), O.add(10, 0, 8), O.add(20, 0, 8)), new boolean[]{true, false, false, true});
        assertEquals(4, t.path().radius(0), 1e-9, "the 8 m middle leg halved between its two corners");
        assertEquals(4, t.path().radius(1), 1e-9);
    }

    @Test
    void aStopKeepsTheNextLegInLine() {
        // a stop at (6, 0, 1): the point after it is put on the line from the start through the stop
        var t = TrackPlacer.lay(List.of(O, O.add(6, 0, 1), O.add(13, 0, 3), O.add(13, 0, 15)), new boolean[]{true, true, false, true});
        Vec3 u = new Vec3(6, 0, 1).normalize();
        Vec3 moved = t.points().get(2).subtract(O);
        assertEquals(0, moved.subtract(u.scale(moved.dot(u))).length(), 0.05, "the corner after the stop is on its line (to the voxel grid)");
        assertEquals(3, t.path().stops().length, "start, the stop, the end");
        assertEquals(0, t.path().at(t.path().stops()[1]).distanceTo(O.add(6, 0, 1)), 1e-9, "the stop where it was clicked");
    }

    @Test
    void refusesTightOrLongTracks() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> TrackPlacer.lay(List.of(O, O.add(3, 0, 0), O.add(3, 0, 3)), new boolean[]{true, false, true}));
        assertTrue(ex.getMessage().contains("too tight"), ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> TrackPlacer.lay(List.of(O, O.add(70, 0, 0)), new boolean[]{true, true}));
        assertThrows(IllegalArgumentException.class,
            () -> TrackPlacer.lay(List.of(O, O.add(8, 0, 0), O.add(0, 0, 0)), new boolean[]{true, false, true}));
    }

    /** What lay() makes is what the drive keeps and reloads: rebuilt from its own points, the same track (review: a
     *  diagonal stop's corner, off the grid, came back 2.8 mm shorter, and a car at the end then faulted). */
    @Test
    void aLaidTrackRebuildsExactly() {
        var t = TrackPlacer.lay(List.of(O, O.add(6, 0, 1), O.add(13, 0, 3), O.add(13, 0, 15)), new boolean[]{true, true, false, true});
        for (Vec3 p : t.points())
            for (double c : new double[]{p.x, p.y, p.z}) assertEquals(Math.rint(c * 16) / 16, c, 1e-12, "on the voxel grid: " + p);
        var again = TrackPlacer.build(t.points(), t.stops());
        assertEquals(t.path().length(), again.path().length(), 1e-12);
        assertArrayEquals(t.path().stops(), again.path().stops(), 1e-12);
    }

    /** A stop clicked again ends the track there (review: it ran a block past it); the end lands on a block centre. */
    @Test
    void clickingAStopAgainEndsThere() {
        Vec3 stop = O.add(9, 0, 3);
        var t = TrackPlacer.lay(List.of(O, stop, stop), new boolean[]{true, true, false});
        assertEquals(2, t.points().size());
        assertEquals(0, t.path().at(t.path().length()).distanceTo(stop), 1e-12);
        // a stop on a diagonal, then the end: the end goes on in line, onto a block centre
        var u = TrackPlacer.lay(List.of(O, O.add(5, 0, 2), O.add(9, 0, 4)), new boolean[]{true, true, true});
        Vec3 end = u.points().get(2);
        assertEquals(Math.floor(end.x) + 0.5, end.x, 1e-12);
        assertEquals(Math.floor(end.z) + 0.5, end.z, 1e-12);
    }

    @Test
    void refusesTooShortTracks() {
        assertThrows(IllegalArgumentException.class, () -> TrackPlacer.lay(List.of(O, O.add(1, 0, 0)), new boolean[]{true, true}));
    }
}
