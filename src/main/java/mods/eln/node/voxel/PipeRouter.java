package mods.eln.node.voxel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Pipe routing (notes/pipes/DESIGN.md section 4): A* on a lattice of world voxels (every LATTICE voxels; the face
 * centres of blocks are on it), axis-aligned moves, cost = length + a penalty per bend + a little per block entered.
 * A move is allowed if the pipe's bore swept along it hits nothing (Space). Waypoints split the search into legs; the
 * result is one polyline of world voxel points (corners only). Pure Java: the world is behind Space.
 */
public final class PipeRouter {
    public static final int LATTICE = 4;
    static final int BEND = 6, BLOCK_ENTERED = 1;
    /** A step whose pipe rests against nothing (no block or part within REST voxels of its side) costs this much more:
     *  runs follow walls and floors rather than hang in the air (Baughn), even when a little longer. */
    static final int UNSUPPORTED = 5, REST = 3;
    /** Search limits: expansions, and how far outside the legs' box it may wander (voxels). */
    static final int MAX_EXPANSIONS = 400_000, MARGIN = 16 * 8;

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
        List<End> stops = new ArrayList<>();
        stops.add(start);
        stops.addAll(waypoints);
        stops.add(end);
        List<long[]> pts = new ArrayList<>();
        int arriveDir = -1;
        for (int leg = 0; leg + 1 < stops.size(); leg++) {
            End a = stops.get(leg), b = stops.get(leg + 1);
            // leave a port along its direction; a waypoint continues the way the last leg arrived (no forced bend)
            int leaveDir = leg == 0 ? a.dir() : -1;
            int arrive = leg + 2 == stops.size() && b.dir() >= 0 ? b.dir() ^ 1 : -1;   // (arrive into a port: opposite its out-direction)
            List<long[]> p = leg(space, bore, a, b, leaveDir, arriveDir, arrive);
            if (p == null) return new Result(null, leg == 0 && stops.size() == 2 ? "no free path" : "no free path on leg " + (leg + 1));
            if (!pts.isEmpty()) p.remove(0);
            pts.addAll(p);
            arriveDir = p.size() >= 2 ? dirOf(pts.get(pts.size() - 2), pts.get(pts.size() - 1)) : arriveDir;
        }
        return new Result(simplify(pts), null);
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

    /** Does a pipe of this bore, moved one lattice step from p along dir, hit anything? */
    static boolean stepBlocked(Space space, int bore, long[] p, int dir) {
        long h = bore / 2;
        long[] q = {p[0] + D[dir][0] * LATTICE, p[1] + D[dir][1] * LATTICE, p[2] + D[dir][2] * LATTICE};
        long[] lo = new long[3], hi = new long[3];
        for (int a = 0; a < 3; a++) {
            // across the bore; along the move only the step itself (a pipe leaving a device's face doesn't reach into it)
            long e = a == dir / 2 ? 0 : h;
            lo[a] = Math.min(p[a], q[a]) - e;
            hi[a] = Math.max(p[a], q[a]) + e;
        }
        return space.blocked(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
    }

    private record Key(long x, long y, long z, int dir) {
    }

    private static final class Open implements Comparable<Open> {
        final Key k;
        final long g, f;

        Open(Key k, long g, long f) {
            this.k = k;
            this.g = g;
            this.f = f;
        }

        @Override
        public int compareTo(Open o) {
            return Long.compare(f, o.f);
        }
    }

    /** One leg: A* over (point, last direction). Returns the lattice points, start and end included, or null. */
    static List<long[]> leg(Space space, int bore, End a, End b, int leaveDir, int lastDir, int arriveDir) {
        long[] s = {a.x(), a.y(), a.z()}, t = {b.x(), b.y(), b.z()};
        for (int i = 0; i < 3; i++) if (Math.floorMod(s[i], LATTICE) != 0 || Math.floorMod(t[i], LATTICE) != 0) return null;
        long lo0 = Math.min(s[0], t[0]) - MARGIN, hi0 = Math.max(s[0], t[0]) + MARGIN;
        long lo1 = Math.min(s[1], t[1]) - MARGIN, hi1 = Math.max(s[1], t[1]) + MARGIN;
        long lo2 = Math.min(s[2], t[2]) - MARGIN, hi2 = Math.max(s[2], t[2]) + MARGIN;
        PriorityQueue<Open> open = new PriorityQueue<>();
        Map<Key, Long> best = new HashMap<>();
        Map<Key, Key> from = new HashMap<>();
        Key k0 = new Key(s[0], s[1], s[2], lastDir);
        best.put(k0, 0L);
        open.add(new Open(k0, 0, h(s, t)));
        int expansions = 0;
        while (!open.isEmpty() && expansions++ < MAX_EXPANSIONS) {
            Open o = open.poll();
            Key k = o.k;
            if (o.g > best.getOrDefault(k, Long.MAX_VALUE)) continue;
            long[] p = {k.x(), k.y(), k.z()};
            if (p[0] == t[0] && p[1] == t[1] && p[2] == t[2] && (arriveDir < 0 || k.dir() == arriveDir)) {
                List<long[]> path = new ArrayList<>();
                for (Key c = k; c != null; c = from.get(c)) path.add(0, new long[]{c.x(), c.y(), c.z()});
                return path;
            }
            for (int d = 0; d < 6; d++) {
                if (k.equals(k0) && leaveDir >= 0 && d != leaveDir) continue;   // (leave a port straight out)
                if (k.dir() >= 0 && d == (k.dir() ^ 1)) continue;               // (no doubling back)
                long[] q = {p[0] + D[d][0] * LATTICE, p[1] + D[d][1] * LATTICE, p[2] + D[d][2] * LATTICE};
                if (q[0] < lo0 || q[0] > hi0 || q[1] < lo1 || q[1] > hi1 || q[2] < lo2 || q[2] > hi2) continue;
                // a pipe stays inside blocks: it crosses a face plane, never runs in one (or it would straddle two)
                boolean inPlane = false;
                for (int x = 0; x < 3; x++) if (x != d / 2 && Math.floorMod(q[x], 16) == 0) inPlane = true;
                if (inPlane) continue;
                if (stepBlocked(space, bore, p, d)) continue;
                long g = o.g + LATTICE + (k.dir() >= 0 && k.dir() != d ? BEND : 0) + (supported(space, bore, q, d) ? 0 : UNSUPPORTED)
                    + (Math.floorDiv(q[0], 16) != Math.floorDiv(p[0], 16) || Math.floorDiv(q[1], 16) != Math.floorDiv(p[1], 16)
                    || Math.floorDiv(q[2], 16) != Math.floorDiv(p[2], 16) ? BLOCK_ENTERED : 0);
                Key nk = new Key(q[0], q[1], q[2], d);
                if (g >= best.getOrDefault(nk, Long.MAX_VALUE)) continue;
                best.put(nk, g);
                from.put(nk, k);
                open.add(new Open(nk, g, g + h(q, t)));
            }
        }
        return null;
    }

    /** Does a pipe at q (moving along d) rest against something on any side across its run? */
    static boolean supported(Space space, int bore, long[] q, int d) {
        long h = bore / 2;
        int along = d / 2;
        for (int side = 0; side < 3; side++) {
            if (side == along) continue;
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                long[] lo = new long[3], hi = new long[3];
                for (int a = 0; a < 3; a++) {
                    lo[a] = q[a] - h;
                    hi[a] = q[a] + h;
                }
                if (sgn < 0) {
                    lo[side] = q[side] - h - REST;
                    hi[side] = q[side] - h;
                } else {
                    lo[side] = q[side] + h;
                    hi[side] = q[side] + h + REST;
                }
                if (space.blocked(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2])) return true;
            }
        }
        return false;
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
