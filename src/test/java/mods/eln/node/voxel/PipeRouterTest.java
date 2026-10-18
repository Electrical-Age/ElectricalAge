package mods.eln.node.voxel;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The pipe router on synthetic spaces (whole solid blocks as obstacles). */
public class PipeRouterTest {
    /** Solid blocks (block coordinates); everything else free. */
    static PipeRouter.Space solid(Set<List<Long>> blocks) {
        return (x0, y0, z0, x1, y1, z1) -> {
            for (long bx = Math.floorDiv(x0, 16); bx <= Math.floorDiv(x1 - 1, 16); bx++)
                for (long by = Math.floorDiv(y0, 16); by <= Math.floorDiv(y1 - 1, 16); by++)
                    for (long bz = Math.floorDiv(z0, 16); bz <= Math.floorDiv(z1 - 1, 16); bz++)
                        if (blocks.contains(List.of(bx, by, bz))) return true;
            return false;
        };
    }

    static PipeRouter.End face(long bx, long by, long bz, int dir) {
        long x = bx * 16 + 8, y = by * 16 + 8, z = bz * 16 + 8;
        switch (dir) {
            case 0 -> x -= 8;
            case 1 -> x += 8;
            case 2 -> y -= 8;
            case 3 -> y += 8;
            case 4 -> z -= 8;
            default -> z += 8;
        }
        return new PipeRouter.End(x, y, z, dir);
    }

    @Test
    void straightAcrossOpenGround() {
        // from the east face of block (0,0,0) to the west face of block (4,0,0): 3 blocks of pipe
        var r = PipeRouter.route(solid(Set.of()), 4, face(0, 0, 0, 1), List.of(), face(4, 0, 0, 0));
        assertNull(r.error);
        assertEquals(48, r.length());
        assertEquals(2, r.points.size(), "a straight line: two points");
        var blocks = PipeRouter.perBlock(r.points);
        assertEquals(3, blocks.size());
        for (var e : blocks.entrySet()) assertArrayEquals(new int[]{0, 8, 8, 16, 8, 8}, e.getValue().get(0), "block " + e.getKey());
    }

    @Test
    void goesAroundAnObstacle() {
        var r = PipeRouter.route(solid(Set.of(List.of(2L, 0L, 0L))), 4, face(0, 0, 0, 1), List.of(), face(4, 0, 0, 0));
        assertNull(r.error);
        assertTrue(r.length() > 48, "detour " + r.length());
        for (var blk : PipeRouter.perBlock(r.points).keySet()) assertNotEquals(List.of(2L, 0L, 0L), blk);
        // every piece is a valid pipe inside its block
        for (var pieces : PipeRouter.perBlock(r.points).values())
            for (int[] p : pieces) assertTrue(new PipePart(0, p).valid(), java.util.Arrays.toString(p));
    }

    @Test
    void turnsUpIntoAPortAbove() {
        // from a top face up and over into the bottom face of a block 3 east and 2 up
        var r = PipeRouter.route(solid(Set.of()), 6, face(0, 0, 0, 3), List.of(), face(3, 3, 0, 2));
        assertNull(r.error);
        long[] last = r.points.get(r.points.size() - 1), before = r.points.get(r.points.size() - 2);
        assertEquals(before[0], last[0]);
        assertTrue(before[1] < last[1], "arrives going up into the port");
    }

    @Test
    void runsAlongTheFloorRatherThanInTheAir() {
        // a floor of solid blocks at y = -1: from a port on the top of block (0,0,0)'s east face... a straight run east
        // at the block's middle height floats 8 voxels above the floor; the router drops to the lane next to it
        java.util.Set<List<Long>> floor = new java.util.HashSet<>();
        for (long x = -2; x <= 8; x++) for (long z = -2; z <= 2; z++) floor.add(List.of(x, -1L, z));
        var r = PipeRouter.route(solid(floor), 4, face(0, 0, 0, 1), List.of(), face(6, 0, 0, 0));
        assertNull(r.error);
        long low = Long.MAX_VALUE;
        for (long[] p : r.points) low = Math.min(low, p[1]);
        assertEquals(4, low, "the run drops to y 4 voxels (resting on the floor): " + PipeRouter.perBlock(r.points).keySet());
    }

    @Test
    void boxedInHasNoPath() {
        java.util.Set<List<Long>> walls = new java.util.HashSet<>();
        for (long x = -1; x <= 1; x++) for (long y = -1; y <= 1; y++) for (long z = -1; z <= 1; z++)
            if (!(x == 0 && y == 0 && z == 0) && !(x == 1 && y == 0 && z == 0)) walls.add(List.of(x, y, z));
        walls.add(List.of(2L, 0L, 0L));
        var r = PipeRouter.route(solid(walls), 4, face(0, 0, 0, 1), List.of(), face(6, 0, 0, 0));
        assertNotNull(r.error);
    }

    @Test
    void waypointsAreVisited() {
        var wp = new PipeRouter.End(2 * 16 + 8, 3 * 16 + 8, 8, -1);
        var r = PipeRouter.route(solid(Set.of()), 4, face(0, 0, 0, 1), List.of(wp), face(4, 0, 0, 0));
        assertNull(r.error);
        boolean seen = false;
        for (int i = 1; i < r.points.size(); i++) {
            long[] a = r.points.get(i - 1), b = r.points.get(i);
            if (between(a, b, wp)) seen = true;
        }
        assertTrue(seen);
    }

    static boolean between(long[] a, long[] b, PipeRouter.End p) {
        long[] q = {p.x(), p.y(), p.z()};
        for (int i = 0; i < 3; i++) if (q[i] < Math.min(a[i], b[i]) || q[i] > Math.max(a[i], b[i])) return false;
        return true;
    }
}
