package mods.eln.node.voxel;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The cable router on synthetic worlds of whole solid blocks. */
public class CableRouterTest {
    static final int LV = 1;   // (CablePart.TIERS: Low Voltage, 2 x 1)

    static CableRouter.Space world(Set<List<Long>> solid) {
        return new CableRouter.Space() {
            @Override
            public boolean blocked(long x0, long y0, long z0, long x1, long y1, long z1) {
                for (long bx = Math.floorDiv(x0, 16); bx <= Math.floorDiv(x1 - 1, 16); bx++)
                    for (long by = Math.floorDiv(y0, 16); by <= Math.floorDiv(y1 - 1, 16); by++)
                        for (long bz = Math.floorDiv(z0, 16); bz <= Math.floorDiv(z1 - 1, 16); bz++)
                            if (solid.contains(List.of(bx, by, bz))) return true;
                return false;
            }

            @Override
            public boolean solid(long x0, long y0, long z0, long x1, long y1, long z1) {
                for (long bx = Math.floorDiv(x0, 16); bx <= Math.floorDiv(x1 - 1, 16); bx++)
                    for (long by = Math.floorDiv(y0, 16); by <= Math.floorDiv(y1 - 1, 16); by++)
                        for (long bz = Math.floorDiv(z0, 16); bz <= Math.floorDiv(z1 - 1, 16); bz++)
                            if (!solid.contains(List.of(bx, by, bz))) return false;
                return true;
            }
        };
    }

    static Set<List<Long>> floor(int x0, int x1, int z0, int z1) {
        Set<List<Long>> s = new HashSet<>();
        for (long x = x0; x <= x1; x++) for (long z = z0; z <= z1; z++) s.add(List.of(x, -1L, z));
        return s;
    }

    /** A point on the floor of block (bx, 0, bz), at its centre. */
    static CableRouter.End onFloor(long bx, long bz) {
        return new CableRouter.End(bx * 16 + 8, 0, bz * 16 + 8, 0, -1);
    }

    /** Every piece is a valid cable part in its block, lies against a solid block, and isn't inside one. */
    static void check(Set<List<Long>> solid, CableRouter.Result r) {
        assertNull(r.error);
        for (CableRouter.Piece p : CableRouter.pieces(r.steps, 2, 1)) {
            CablePart part = new CablePart(LV, p.face(), p.points());
            assertTrue(part.valid(), "piece " + p + " " + java.util.Arrays.toString(p.points()));
            assertFalse(solid.contains(List.of(p.bx(), p.by(), p.bz())), "a piece inside a solid block: " + p);
            if (p.face() >= 0) {
                int[] d = CableRouter.DV[p.face()];
                assertTrue(solid.contains(List.of(p.bx() + d[0], p.by() + d[1], p.bz() + d[2])), "a piece in mid-air: " + p + " "
                    + java.util.Arrays.toString(p.points()));
            }
        }
    }

    @Test
    void straightAlongAFloor() {
        var solid = floor(0, 9, 0, 2);
        var r = CableRouter.route(world(solid), 2, 1, onFloor(1, 1), List.of(), onFloor(6, 1));
        check(solid, r);
        assertEquals(80, r.length());
        assertEquals(2, r.steps.size(), "a straight line");
        assertEquals(6, CableRouter.pieces(r.steps, 2, 1).size(), "half a block in the first and last, whole ones between");
    }

    @Test
    void goesRoundAHoleInTheFloor() {
        var solid = floor(0, 9, 0, 2);
        solid.remove(List.of(4L, -1L, 1L));
        var r = CableRouter.route(world(solid), 2, 1, onFloor(1, 1), List.of(), onFloor(7, 1));
        check(solid, r);
        assertTrue(r.length() > 96, "detour " + r.length());
    }

    @Test
    void climbsAWall() {
        var solid = floor(0, 9, 0, 2);
        for (long y = 0; y < 3; y++) solid.add(List.of(5L, y, 1L));
        // to the middle of the wall's west face, two blocks up
        var end = new CableRouter.End(5 * 16, 2 * 16 + 8, 24, 5, -1);
        var r = CableRouter.route(world(solid), 2, 1, onFloor(1, 1), List.of(), end);
        check(solid, r);
        assertTrue(r.steps.stream().anyMatch(s -> s.attach() == 5), "ends on the wall");
        assertTrue(r.steps.stream().noneMatch(CableRouter.Step::outer));
    }

    @Test
    void goesOverABump() {
        // a one-block-wide floor with a bump: over it (two outer corners, two inner), knuckles round the top edges
        var solid = floor(0, 9, 1, 1);
        solid.add(List.of(4L, 0L, 1L));
        var r = CableRouter.route(world(solid), 2, 1, onFloor(1, 1), List.of(), onFloor(7, 1));
        check(solid, r);
        long knuckles = CableRouter.pieces(r.steps, 2, 1).stream().filter(p -> p.face() < 0).count();
        assertTrue(knuckles >= 2, "knuckles " + knuckles + ", steps " + r.steps);
    }

    @Test
    void givesUpInsideASealedBox() {
        Set<List<Long>> solid = new HashSet<>();
        for (long x = -1; x <= 1; x++)
            for (long y = -1; y <= 1; y++)
                for (long z = -1; z <= 1; z++) if (x != 0 || y != 0 || z != 0) solid.add(List.of(x, y, z));
        solid.addAll(floor(-5, 8, -5, 5));
        long t0 = System.nanoTime();
        var r = CableRouter.route(world(solid), 2, 1, onFloor(0, 0), List.of(), onFloor(5, 3));
        assertNotNull(r.error);
        assertTrue((System.nanoTime() - t0) / 1e6 < 1000, "gave up in time");
    }
}
