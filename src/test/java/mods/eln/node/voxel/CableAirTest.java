package mods.eln.node.voxel;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cable air runs (Baughn 2026-11-11, D5): a cable may leave its support for up to 0.6 block at a medium cost and up to 1.2
 * blocks at a high one, never further. The scenes are islands over nothing, so no detour along faces exists and a route
 * that is found must have crossed the air.
 */
public class CableAirTest {
    static final RouteKind LV = RouteKind.cable("cable", 2, 1);

    /** A strip of floor blocks (y -1, z 0) at these x. */
    static Set<List<Long>> strip(int... xs) {
        Set<List<Long>> s = new HashSet<>();
        for (int x : xs) s.add(List.of((long) x, -1L, 0L));
        return s;
    }

    static CableRouter.Result route(Set<List<Long>> solid, RouteKind kind, long bx0, long bx1) {
        return CableRouter.route(CableRouterTest.world(solid), kind, CableRouterTest.onFloor(bx0, 0), List.of(), CableRouterTest.onFloor(bx1, 0),
            10_000);
    }

    /** The longest stretch of lattice steps off any support along the route (voxels). */
    static int longestAir(CableRouter.Space space, RouteKind kind, List<CableRouter.Step> steps) {
        int run = 0, most = 0;
        for (int i = 0; i + 1 < steps.size(); i++) {
            CableRouter.Step a = steps.get(i), b = steps.get(i + 1);
            int d = CableRouter.dirOf(a, b);
            if (d < 0) continue;   // (a corner: no length)
            long[] p = {a.x(), a.y(), a.z()};
            long len = Math.abs(b.x() - a.x()) + Math.abs(b.y() - a.y()) + Math.abs(b.z() - a.z());
            for (long t = 0; t < len; t += kind.lattice()) {
                if (Router.surfaceSupported(space, kind, p, b.attach(), d)) run = 0;
                else most = Math.max(most, run += kind.lattice());
                for (int j = 0; j < 3; j++) p[j] += CableRouter.DV[d][j] * kind.lattice();
            }
        }
        return most;
    }

    @Test
    void bridgesAOneBlockGap() {
        var solid = strip(0, 1, 2, 4, 5, 6);
        var r = route(solid, LV, 1, 5);
        assertNull(r.error, "a 1-block gap is within 1.2 blocks");
        int air = longestAir(CableRouterTest.world(solid), LV, r.steps);
        assertTrue(air > 0 && air <= 20, "air stretch " + air);
        for (CableRouter.Piece p : CableRouter.pieces(r.steps, 2, 1)) {
            assertTrue(new CablePart(CableRouterTest.LV, p.face(), p.points()).valid(), "piece " + p);
            assertFalse(solid.contains(List.of(p.bx(), p.by(), p.bz())), "a piece inside a solid block: " + p);
        }
    }

    @Test
    void refusesATwoBlockGap() {
        assertNotNull(route(strip(0, 1, 2, 5, 6, 7), LV, 1, 6).error, "2 blocks is past 1.2");
    }

    @Test
    void noAllowanceNoBridge() {
        assertNotNull(route(strip(0, 1, 2, 4, 5, 6), LV.withAir(RouteKind.NO_AIR), 1, 5).error);
    }

    @Test
    void prefersTheFloorWhereItReaches() {
        // a floor all the way: the same route as without an allowance
        var solid = strip(0, 1, 2, 3, 4, 5, 6);
        var a = route(solid, LV, 1, 5);
        var b = route(solid, LV.withAir(RouteKind.NO_AIR), 1, 5);
        assertNull(a.error);
        assertEquals(b.steps, a.steps);
        assertEquals(0, longestAir(CableRouterTest.world(solid), LV, a.steps));
    }

    @Test
    void climbsToALedgeThroughTheAir() {
        // a floor, and a ledge block one up and one across a gap: up the gap's far wall would need a face; with nothing
        // but the floor's end and the ledge, the cable lifts off and lands on the ledge top
        Set<List<Long>> solid = strip(0, 1, 2);
        solid.add(List.of(3L, 0L, 0L));   // (a step up right next to the floor: an inner corner, no air)
        var r = CableRouter.route(CableRouterTest.world(solid), LV, CableRouterTest.onFloor(1, 0), List.of(),
            new CableRouter.End(3 * 16 + 8, 16, 8, 0, -1), 10_000);
        assertNull(r.error);
        assertEquals(0, longestAir(CableRouterTest.world(solid), LV, r.steps), "a wall to climb: no air needed");
    }
}
