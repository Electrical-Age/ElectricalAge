package mods.eln.node.voxel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Pipe routing's entry point and output stage (notes/pipes/DESIGN.md section 4). The search is Router's, for a FREE
 * kind: a lattice of world voxels (every LATTICE voxels; the face centres of blocks are on it), axis-aligned moves,
 * cost = length + a penalty per bend + a little per block entered (+ a little where it rests against nothing). A move
 * is allowed if the pipe's bore swept along it hits nothing (Space). Waypoints split the search into legs; the result
 * is one polyline of world voxel points (corners only), which perBlock() cuts into per-block pieces. Pure Java.
 */
public final class PipeRouter {
    public static final int LATTICE = 4;
    static final int BEND = 6, BLOCK_ENTERED = 1;
    /** A step whose pipe rests against nothing (no block or part within REST voxels of its side) costs this much more:
     *  runs follow walls and floors rather than hang in the air (Baughn), even when a little longer. */
    static final int UNSUPPORTED = 5, REST = 3;
    /** Search limits: expansions, and how far outside the legs' box it may wander (voxels). */
    static final int MAX_EXPANSIONS = 400_000, MARGIN = 16 * 8;
    /** Wall-clock budget for one route (all its legs), ms (Baughn: it must give up in time; it runs on the server
     *  thread). */
    public static final long TIME_BUDGET_MS = 200;

    /** The search ran out of time or of expansions. */
    static final class GaveUp extends RuntimeException {
        GaveUp() {
            super(null, null, false, false);
        }
    }

    /** What's in the way. A box in world voxels [x0, x1) x [y0, y1) x [z0, z1). */
    public interface Space {
        boolean blocked(long x0, long y0, long z0, long x1, long y1, long z1);
    }

    /** A routing end: a point (world voxels) and, if it's a port, the direction the pipe must leave or arrive along
     *  (dir: 0..5 = -x +x -y +y -z +z; -1 for a free point). */
    public record End(long x, long y, long z, int dir) {
    }

    public static final class Result {
        public final List<long[]> points;
        public final String error;

        Result(List<long[]> points, String error) {
            this.points = points;
            this.error = error;
        }

        public int length() {
            int n = 0;
            for (int i = 1; i < points.size(); i++)
                for (int a = 0; a < 3; a++) n += (int) Math.abs(points.get(i)[a] - points.get(i - 1)[a]);
            return n;
        }
    }

    static final int[][] D = {{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};

    /** Route from start through the waypoints to end, for a pipe of this bore (voxels). */
    public static Result route(Space space, int bore, End start, List<End> waypoints, End end) {
        return route(space, bore, start, waypoints, end, TIME_BUDGET_MS);
    }

    public static Result route(Space space, int bore, End start, List<End> waypoints, End end, long budgetMs) {
        return Router.free(space, RouteKind.pipe("pipe", bore), start, waypoints, end, budgetMs);
    }

    static int dirOf(long[] a, long[] b) {
        for (int i = 0; i < 3; i++) {
            if (b[i] < a[i]) return 2 * i;
            if (b[i] > a[i]) return 2 * i + 1;
        }
        return -1;
    }

    /** Keep the corners and the ends. */
    static List<long[]> simplify(List<long[]> pts) {
        List<long[]> out = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            if (i > 0 && i + 1 < pts.size() && dirOf(pts.get(i - 1), pts.get(i)) == dirOf(pts.get(i), pts.get(i + 1))) continue;
            out.add(pts.get(i));
        }
        return out;
    }

    static long h(long[] p, long[] t) {
        return Math.abs(p[0] - t[0]) + Math.abs(p[1] - t[1]) + Math.abs(p[2] - t[2]);
    }

    /**
     * The route cut into pieces per block: block (x y z) -> polylines in that block's local voxels (0..16). A block
     * the route passes through twice gets two pieces.
     */
    public static Map<List<Long>, List<int[]>> perBlock(List<long[]> pts) {
        Map<List<Long>, List<int[]>> out = new java.util.LinkedHashMap<>();
        // densify at block boundaries, then group consecutive points by the block their segment lies in
        List<long[]> dense = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            if (i == 0) {
                dense.add(pts.get(0));
                continue;
            }
            long[] a = pts.get(i - 1), b = pts.get(i);
            int axis = 0;
            for (int x = 0; x < 3; x++) if (a[x] != b[x]) axis = x;
            long step = b[axis] > a[axis] ? 1 : -1;
            long next = step > 0 ? Math.floorDiv(a[axis], 16) * 16 + 16 : Math.floorDiv(a[axis] - 1, 16) * 16;
            while (step > 0 ? next < b[axis] : next > b[axis]) {
                long[] c = a.clone();
                c[axis] = next;
                dense.add(c);
                next += 16 * step;
            }
            dense.add(b);
        }
        List<Long> cur = null;
        List<long[]> piece = new ArrayList<>();
        for (int i = 1; i < dense.size(); i++) {
            long[] a = dense.get(i - 1), b = dense.get(i);
            // (the route never runs in a block's face plane: only across it, so the lower end decides the block)
            List<Long> blk = List.of(Math.floorDiv(Math.min(a[0], b[0]), 16), Math.floorDiv(Math.min(a[1], b[1]), 16),
                Math.floorDiv(Math.min(a[2], b[2]), 16));
            if (!blk.equals(cur)) {
                flush(out, cur, piece);
                cur = blk;
                piece = new ArrayList<>();
                piece.add(a);
            }
            piece.add(b);
        }
        flush(out, cur, piece);
        return out;
    }

    private static void flush(Map<List<Long>, List<int[]>> out, List<Long> blk, List<long[]> piece) {
        if (blk == null || piece.size() < 2) return;
        List<long[]> simple = simplify(piece);
        int[] local = new int[simple.size() * 3];
        for (int i = 0; i < simple.size(); i++)
            for (int a = 0; a < 3; a++) local[3 * i + a] = (int) (simple.get(i)[a] - blk.get(a) * 16);
        out.computeIfAbsent(blk, k -> new ArrayList<>()).add(local);
    }
}
