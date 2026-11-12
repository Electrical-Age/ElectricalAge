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

    @Test
    void aBridgeOverOpenGroundStaysCheap() {
        // the GameTest's islands over a test floor over open ground: the air opens all that ground to the search; air
        // stretches being straight keeps it to ~15k expansions (bends in the air: 74k, past the click budget)
        Set<List<Long>> solid = new HashSet<>();
        for (long x = -6; x < 16; x++) for (long z = -6; z < 9; z++) solid.add(List.of(x, -1L, z));
        for (long x = 0; x < 10; x++) for (long z = 0; z < 3; z++) solid.add(List.of(x, 0L, z));
        for (long x = 0; x < 10; x++) if (x != 4) solid.add(List.of(x, 2L, 1L));
        var r = CableRouter.route(CableRouterTest.world(solid), LV, new CableRouter.End(1 * 16 + 8, 48, 24, 0, -1), List.of(),
            new CableRouter.End(6 * 16 + 8, 48, 24, 0, -1), 60_000);
        assertNull(r.error);
        int exp = Router.LAST_EXPANSIONS.get();
        assertTrue(exp < 30_000, exp + " expansions");
    }

    /** Lowest y of the hangs' chains, and the y where a chain passes x (on a line along x at the islands' top). */
    static double lowest(List<CableHang.Hang> hs) {
        double low = Double.MAX_VALUE;
        for (var h : hs) for (double[] q : h.pts()) low = Math.min(low, q[1]);
        return low;
    }

    static double yAtX(CableHang.Hang h, double x) {
        var p = h.pts();
        for (int i = 1; i < p.size(); i++) {
            double a = p.get(i - 1)[0], b = p.get(i)[0];
            if ((x - a) * (x - b) <= 0 && a != b) return p.get(i - 1)[1] + (p.get(i)[1] - p.get(i - 1)[1]) * (x - a) / (b - a);
        }
        return Double.NaN;
    }

    @Test
    void aBridgeHangsFromItsEdges() {
        // the gap is x 48..64 at the islands' top (y 0): the cable leaves the edge along the floor and bends down from
        // there (it began a voxel into the gap with its two end links held straight: Baughn 1570524112013623437), hangs a
        // couple of voxels at the middle, stays out of the blocks; only the piece over the gap draws it
        var solid = strip(0, 1, 2, 4, 5, 6);
        var sp = CableRouterTest.world(solid);
        var r = route(solid, LV, 1, 5);
        assertNull(r.error);
        long t0 = System.nanoTime();
        var hung = CableHang.laid(sp, LV, r.steps);
        long us = (System.nanoTime() - t0) / 1000;
        assertEquals(1, hung.hangs().size(), "one span");
        var h = hung.hangs().get(0);
        double low = lowest(hung.hangs());
        System.out.println("CableHang bridge: hangs " + (-low) + " voxels, " + us + " us, y at the edge+1 " + yAtX(h, 49) + ", +2 " + yAtX(h, 50));
        assertTrue(low < -1.0 && low > -4, "the middle hangs " + (-low) + " voxels");
        assertTrue(yAtX(h, 49) < -0.02, "already below the line a voxel past the edge: " + yAtX(h, 49));
        assertTrue(yAtX(h, 49) > -1.0, "but only a little (it leaves the edge along the floor): " + yAtX(h, 49));
        for (int i = 0; i < h.pts().size(); i++)
            assertFalse(CableHang.blocked(sp, h.pts().get(i), h.nrm().get(i)), "a node inside a block: " + java.util.Arrays.toString(h.pts().get(i)));
        int drawn = 0;
        for (CableRouter.Piece p : CableRouter.pieces(r.steps, 2, 1)) {
            if (p.face() < 0) continue;
            int[] hang = CableHang.encode(p, hung.route(), hung.hangs());
            if (hang == null) continue;
            CablePart part = new CablePart(CableRouterTest.LV, p.face(), p.points());
            part.hang2 = hang;
            assertFalse(part.drawn().isEmpty());
            if (p.bx() != 3) continue;
            drawn++;
            double[] lowest = {Double.MAX_VALUE};
            VoxelGeometry.model(List.of(part), null).emit(f -> {
                for (int i = 1; i < f.xyz.length; i += 3) lowest[0] = Math.min(lowest[0], f.xyz[i]);
            });
            assertEquals(low, lowest[0], 0.3, "the drawn cable's lowest point");
        }
        assertEquals(1, drawn, "the piece over the gap");
    }

    @Test
    void aDropPastAnAirBlockEasesOntoTheFloor() {
        // a pillar (x 0..16, y 16..64) ending a block over the floor (y 0): down its east face, off its foot, straight
        // down the air block and onto the floor eastward: the drawn cable rounds the corner at the bottom instead of a
        // hard turn (Baughn: it should bow or take a short run, by energy, not a perfect vertical and a right angle)
        java.util.Set<List<Long>> solid = new java.util.HashSet<>();
        for (long y = 1; y <= 3; y++) solid.add(List.of(0L, y, 0L));
        for (long x = -2; x <= 6; x++) solid.add(List.of(x, -1L, 0L));
        var sp = CableRouterTest.world(solid);
        var r = CableRouter.route(sp, LV, new CableRouter.End(16, 56, 8, 4, -1), List.of(), new CableRouter.End(4 * 16 + 8, 0, 8, 0, -1), 10_000);
        assertNull(r.error, "the route");
        var hung = CableHang.laid(sp, LV, r.steps);
        assertFalse(hung.hangs().isEmpty(), "a hung span: " + r.steps);
        var h = hung.hangs().get(0);
        // off the routed L (the line x 16 down to y 0, then along y 0): how far the chain cuts or bows the corner
        double most = 0;
        for (int i = 0; i < h.pts().size(); i++) {
            double[] q = h.pts().get(i);
            assertFalse(CableHang.blocked(sp, q, h.nrm().get(i)), "a node inside a block: " + java.util.Arrays.toString(q));
            double off = Math.min(Math.abs(q[0] - 16) + Math.max(0, -q[1]), Math.abs(q[1]) + Math.max(0, 16 - q[0]));
            most = Math.max(most, off);
        }
        System.out.println("CableHang drop: off the routed corner by up to " + most + " voxels; steps " + r.steps);
        assertTrue(most > 1.0, "the corner is eased, not a right angle: " + most);
        // (deterministic: the same again)
        var again = CableHang.laid(sp, LV, r.steps).hangs().get(0);
        for (int i = 0; i < h.pts().size(); i++) assertArrayEquals(h.pts().get(i), again.pts().get(i), 1e-12);
    }

    @Test
    void reachesADeviceFaceInTheAir() {
        // a wall (x 0..16), and a device block a block away (x 32..48) hanging in the air at y 16..32; its connector on the
        // west face's bottom edge: the cable comes down the wall, lifts off and crosses straight to it, arriving on the
        // edge's face (attach down) moving east, as a cable on a floor there would (EA's edge rule, VoxelNode.legacy)
        Set<List<Long>> solid = new HashSet<>();
        for (long y = 0; y < 3; y++) solid.add(List.of(0L, y, 0L));
        solid.add(List.of(2L, 1L, 0L));
        var r = CableRouter.route(CableRouterTest.world(solid), LV, new CableRouter.End(16, 40, 8, 4, -1), List.of(),
            new CableRouter.End(32, 16, 8, 0, 5), 10_000);
        assertNull(r.error);
        CableRouter.Step last = r.steps.get(r.steps.size() - 1);
        assertEquals(0, last.attach(), "arrives on the edge's face: " + r.steps);
        assertTrue(longestAir(CableRouterTest.world(solid), LV, r.steps) > 0, "through the air: " + r.steps);
    }
}

