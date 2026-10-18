package mods.eln.node.voxel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Cable routing (notes/cables/PLAN.md step 3; Baughn: cables share the pipes' pathfinder, but run against a face, never
 * in mid-air). A* over states (world voxel point on a block face plane, the face it lies against, the last move): a
 * cable lies on the plane between a solid block (its support) and the block it is in. Moves: one lattice step along
 * the face (the support must go on: solid all the way), an inner corner (up a wall met at a block's edge), an outer
 * corner (over the support's edge onto its next face, with a knuckle in the block round the corner). Cost = length + a
 * penalty per bend + a little per block entered. Directions are net.minecraft.core.Direction 3D values (0 down, 1 up,
 * 2 north, 3 south, 4 west, 5 east). Pure Java: the world is behind Space.
 */
public final class CableRouter {
    public static final int LATTICE = 2;
    static final int BEND = 4, BLOCK_ENTERED = 1;
    static final int MAX_EXPANSIONS = 400_000, MARGIN = 16 * 6;
    /** Air kept round a cable (voxels, across and above it): two cables never look touching unless joined (Baughn). */
    public static final int CLEARANCE = 1;

    /** The world: what's in the way (a box in world voxels), and whether every voxel of a box is solid enough to lay a
     *  cable on (full blocks, and the shapes of imitated decorative blocks: a stair's step). */
    public interface Space extends PipeRouter.Space {
        boolean solid(long x0, long y0, long z0, long x1, long y1, long z1);
    }

    /** A routing end: a point on a face plane, the face its cable lies against (3D value), and, for a device or cable
     *  end, the direction the cable must arrive along (or leave along, for a start); -1 for a free point. */
    public record End(long x, long y, long z, int attach, int dir) {
    }

    /** One step of a route: the point, the face the cable lies against there, and whether it came round an outer
     *  corner (then the previous step's face was another). */
    public record Step(long x, long y, long z, int attach, boolean outer) {
    }

    public static final class Result {
        public final List<Step> steps;
        public final String error;

        Result(List<Step> steps, String error) {
            this.steps = steps;
            this.error = error;
        }

        /** Length in voxels (corners add nothing). */
        public int length() {
            int n = 0;
            for (int i = 1; i < steps.size(); i++) {
                Step a = steps.get(i - 1), b = steps.get(i);
                n += (int) (Math.abs(a.x - b.x) + Math.abs(a.y - b.y) + Math.abs(a.z - b.z));
            }
            return n;
        }
    }

    static final int[][] DV = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

    static int axis(int dir) {
        return dir < 2 ? 1 : dir < 4 ? 2 : 0;
    }

    static int reverse(int dir) {
        return dir ^ 1;
    }

    public static Result route(Space space, int width, int height, End start, List<End> waypoints, End end) {
        return route(space, width, height, start, waypoints, end, PipeRouter.TIME_BUDGET_MS);
    }

    public static Result route(Space space, int width, int height, End start, List<End> waypoints, End end, long budgetMs) {
        long deadline = System.nanoTime() + budgetMs * 1_000_000;
        List<End> stops = new ArrayList<>();
        stops.add(start);
        stops.addAll(waypoints);
        stops.add(end);
        List<Step> out = new ArrayList<>();
        int lastDir = start.dir();
        try {
            for (int leg = 0; leg + 1 < stops.size(); leg++) {
                End a = stops.get(leg), b = stops.get(leg + 1);
                int attach = out.isEmpty() ? a.attach() : out.get(out.size() - 1).attach();
                int arrive = leg + 2 == stops.size() ? b.dir() : -1;
                List<Step> p = leg(space, width, height, a, attach, lastDir, b, arrive, deadline);
                if (p == null) return new Result(null, stops.size() == 2 ? "no way along faces" : "no way along faces on leg " + (leg + 1));
                if (!out.isEmpty()) p.remove(0);
                out.addAll(p);
                if (out.size() >= 2) {
                    Step x = out.get(out.size() - 2), y = out.get(out.size() - 1);
                    lastDir = dirOf(x, y);
                }
            }
        } catch (PipeRouter.GaveUp e) {
            return new Result(null, "gave up: too far or too tangled to find a way (try waypoints)");
        }
        return new Result(simplify(out), null);
    }

    static int dirOf(Step a, Step b) {
        long[] d = {b.x - a.x, b.y - a.y, b.z - a.z};
        for (int v = 0; v < 6; v++) {
            int ax = axis(v);
            if (d[ax] != 0 && Long.signum(d[ax]) == (v % 2 == 0 ? -1 : 1)) return v;
        }
        return -1;
    }

    /** Keep corners, face changes and ends. */
    static List<Step> simplify(List<Step> s) {
        List<Step> out = new ArrayList<>();
        for (int i = 0; i < s.size(); i++) {
            if (i > 0 && i + 1 < s.size()) {
                Step a = s.get(i - 1), b = s.get(i), c = s.get(i + 1);
                if (a.attach == b.attach && b.attach == c.attach && !b.outer && !c.outer && dirOf(a, b) == dirOf(b, c) && dirOf(a, b) >= 0) continue;
            }
            out.add(s.get(i));
        }
        return out;
    }

    private record Key(long x, long y, long z, int attach, int dir) {
    }

    private record Open(Key k, long g, long f) implements Comparable<Open> {
        @Override
        public int compareTo(Open o) {
            return Long.compare(f, o.f);
        }
    }

    /** The block a cable at p on attach's plane lies in, on its in-plane axes using `toward` to pick the side of a
     *  boundary the point sits on (-1: floor). */
    static long[] cell(long[] p, int attach, int toward) {
        long[] c = new long[3];
        int n = axis(attach);
        for (int a = 0; a < 3; a++) {
            // (on a block's face plane: the block on the cable's side; on a plane inside a block, a step's top: that block)
            if (a == n) c[a] = Math.floorDiv(p[a], 16) - (attach % 2 == 1 && Math.floorMod(p[a], 16) == 0 ? 1 : 0);
            else if (toward >= 0 && axis(toward) == a && Math.floorMod(p[a], 16) == 0 && toward % 2 == 0) c[a] = Math.floorDiv(p[a], 16) - 1;
            else c[a] = Math.floorDiv(p[a], 16);
        }
        return c;
    }

    /** The cable's cross-section swept from p one step along d (world voxels), with its clearance. */
    static boolean stepBlocked(Space space, int width, int height, long[] p, int attach, int d) {
        long[] q = {p[0] + DV[d][0] * LATTICE, p[1] + DV[d][1] * LATTICE, p[2] + DV[d][2] * LATTICE};
        int n = axis(attach), along = axis(d), w = 3 - n - along;
        long[] lo = new long[3], hi = new long[3];
        lo[along] = Math.min(p[along], q[along]);
        hi[along] = Math.max(p[along], q[along]);
        lo[w] = p[w] - width / 2 - CLEARANCE;
        hi[w] = p[w] + (width - width / 2) + CLEARANCE;
        if (attach % 2 == 0) {
            lo[n] = p[n];
            hi[n] = p[n] + height + CLEARANCE;
        } else {
            lo[n] = p[n] - height - CLEARANCE;
            hi[n] = p[n];
        }
        return space.blocked(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
    }

    /** Is what's under a step from p along d solid (one voxel deep on attach's side, the cable's width)? */
    static boolean supported(Space space, int width, long[] p, int attach, int d) {
        int n = axis(attach), along = axis(d), w = 3 - n - along;
        long[] lo = new long[3], hi = new long[3];
        lo[along] = Math.min(p[along], p[along] + DV[d][along] * LATTICE);
        hi[along] = Math.max(p[along], p[along] + DV[d][along] * LATTICE);
        lo[w] = p[w] - width / 2;
        hi[w] = p[w] + (width - width / 2);
        lo[n] = attach % 2 == 0 ? p[n] - 1 : p[n];
        hi[n] = lo[n] + 1;
        return space.solid(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
    }

    /** One voxel beyond p along e, the cable's width across: on the cable's side (a wall there?) or the support's side
     *  (does the support go on?). */
    static boolean solidBeyond(Space space, int width, int height, long[] p, int attach, int e, boolean supportSide) {
        int n = axis(attach), ea = axis(e), w = 3 - n - ea;
        long[] lo = new long[3], hi = new long[3];
        lo[ea] = e % 2 == 1 ? p[ea] : p[ea] - 1;
        hi[ea] = lo[ea] + 1;
        lo[w] = p[w] - width / 2;
        hi[w] = p[w] + (width - width / 2);
        if (supportSide) {
            lo[n] = attach % 2 == 0 ? p[n] - 1 : p[n];
            hi[n] = lo[n] + 1;
        } else if (attach % 2 == 0) {
            lo[n] = p[n];
            hi[n] = p[n] + height;
        } else {
            lo[n] = p[n] - height;
            hi[n] = p[n];
        }
        return space.solid(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
    }

    static List<Step> leg(Space space, int width, int height, End a, int attach0, int lastDir, End b, int arriveDir, long deadline) {
        long[] s = {a.x(), a.y(), a.z()}, t = {b.x(), b.y(), b.z()};
        for (int i = 0; i < 3; i++) if (Math.floorMod(s[i], LATTICE) != 0 || Math.floorMod(t[i], LATTICE) != 0) return null;
        long[] lo = new long[3], hi = new long[3];
        for (int i = 0; i < 3; i++) {
            lo[i] = Math.min(s[i], t[i]) - MARGIN;
            hi[i] = Math.max(s[i], t[i]) + MARGIN;
        }
        PriorityQueue<Open> open = new PriorityQueue<>();
        Map<Key, Long> best = new HashMap<>();
        Map<Key, Key> from = new HashMap<>();
        Map<Key, Boolean> outer = new HashMap<>();
        Key k0 = new Key(s[0], s[1], s[2], attach0, lastDir);
        boolean leavePort = a.dir() >= 0 && lastDir == a.dir();
        best.put(k0, 0L);
        open.add(new Open(k0, 0, h(s, t)));
        int expansions = 0;
        while (!open.isEmpty()) {
            if (++expansions > MAX_EXPANSIONS || System.nanoTime() > deadline) throw new PipeRouter.GaveUp();
            Open o = open.poll();
            Key k = o.k;
            if (o.g > best.getOrDefault(k, Long.MAX_VALUE)) continue;
            long[] p = {k.x(), k.y(), k.z()};
            if (p[0] == t[0] && p[1] == t[1] && p[2] == t[2] && (b.attach() < 0 || k.attach() == b.attach()) && (arriveDir < 0 || k.dir() == arriveDir)) {
                List<Step> path = new ArrayList<>();
                for (Key c = k; c != null; c = from.get(c)) path.add(0, new Step(c.x(), c.y(), c.z(), c.attach(), outer.getOrDefault(c, false)));
                return path;
            }
            int n = axis(k.attach());
            // along the face
            for (int d = 0; d < 6; d++) {
                if (axis(d) == n) continue;
                if (k.equals(k0) && leavePort && d != a.dir()) continue;   // (leave a device straight out)
                if (k.dir() >= 0 && d == reverse(k.dir())) continue;
                long[] q = {p[0] + DV[d][0] * LATTICE, p[1] + DV[d][1] * LATTICE, p[2] + DV[d][2] * LATTICE};
                if (out(q, lo, hi)) continue;
                // a cable stays inside its block: it crosses a block's edge, never runs along one (it would straddle two)
                if (Math.floorMod(p[3 - n - axis(d)], 16) == 0) continue;
                if (stepBlocked(space, width, height, p, k.attach(), d) || !supported(space, width, p, k.attach(), d)) continue;
                long[] cp = cell(p, k.attach(), d), cq = cell(q, k.attach(), reverse(d));
                long g = o.g + LATTICE + (k.dir() >= 0 && k.dir() != d ? BEND : 0)
                    + (cp[0] != cq[0] || cp[1] != cq[1] || cp[2] != cq[2] ? BLOCK_ENTERED : 0);
                push(open, best, from, outer, k, new Key(q[0], q[1], q[2], k.attach(), d), g, t, false);
            }
            // corners, toward e (across the face): a wall met here (inner), or the support ending here (outer); at a block's
            // edge or at a step inside a block (a stair's riser, a slab's edge)
            for (int e = 0; e < 6; e++) {
                if (axis(e) == n) continue;
                if (solidBeyond(space, width, height, p, k.attach(), e, false)) {
                    // inner corner: up the wall met here
                    push(open, best, from, outer, k, new Key(p[0], p[1], p[2], e, -1), o.g + BEND, t, false);
                } else {
                    // outer corner: round the support's edge onto its face toward e
                    if (solidBeyond(space, width, height, p, k.attach(), e, true)) continue;
                    if (!solidBeyond(space, width, height, p, k.attach(), reverse(e), true)) continue;
                    if (knuckleBlocked(space, width, height, p, k.attach(), e)) continue;
                    push(open, best, from, outer, k, new Key(p[0], p[1], p[2], reverse(e), -1), o.g + BEND, t, true);
                }
            }
        }
        return null;
    }

    /** The knuckle round an outer corner at p (a box at the edge, in the block round the corner) with clearance. */
    static boolean knuckleBlocked(Space space, int width, int height, long[] p, int attach, int e) {
        int n = axis(attach), ea = axis(e), w = 3 - n - ea;
        long[] lo = new long[3], hi = new long[3];
        lo[w] = p[w] - width / 2 - CLEARANCE;
        hi[w] = p[w] + (width - width / 2) + CLEARANCE;
        if (attach % 2 == 0) { lo[n] = p[n]; hi[n] = p[n] + height + CLEARANCE; }
        else { lo[n] = p[n] - height - CLEARANCE; hi[n] = p[n]; }
        if (e % 2 == 1) { lo[ea] = p[ea]; hi[ea] = p[ea] + height + CLEARANCE; }
        else { lo[ea] = p[ea] - height - CLEARANCE; hi[ea] = p[ea]; }
        return space.blocked(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
    }

    private static boolean out(long[] q, long[] lo, long[] hi) {
        for (int i = 0; i < 3; i++) if (q[i] < lo[i] || q[i] > hi[i]) return true;
        return false;
    }

    private static void push(PriorityQueue<Open> open, Map<Key, Long> best, Map<Key, Key> from, Map<Key, Boolean> outer, Key k, Key nk,
                             long g, long[] t, boolean isOuter) {
        if (g >= best.getOrDefault(nk, Long.MAX_VALUE)) return;
        best.put(nk, g);
        from.put(nk, k);
        if (isOuter) outer.put(nk, true);
        else outer.remove(nk);
        open.add(new Open(nk, g, g + h(new long[]{nk.x(), nk.y(), nk.z()}, t)));
    }

    static long h(long[] p, long[] t) {
        return Math.abs(p[0] - t[0]) + Math.abs(p[1] - t[1]) + Math.abs(p[2] - t[2]);
    }

    /** A cable piece to lay: block, face (attach), local polyline; or a knuckle (face -1, one point). */
    public record Piece(long bx, long by, long bz, int face, int[] points) {
    }

    /**
     * The route as pieces per block: each run on one face cut at block boundaries, local voxels (0..16), and a knuckle
     * in the block round each outer corner.
     */
    public static List<Piece> pieces(List<Step> steps) {
        List<Piece> out = new ArrayList<>();
        // densify: a point at every block boundary crossed along a run
        List<Step> dense = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            Step b = steps.get(i);
            if (i > 0) {
                Step a = steps.get(i - 1);
                if (a.attach == b.attach && !b.outer) {
                    int d = dirOf(a, b);
                    if (d >= 0) {
                        int ax = axis(d);
                        long[] pa = {a.x, a.y, a.z}, pb = {b.x, b.y, b.z};
                        long step = pb[ax] > pa[ax] ? 1 : -1;
                        long next = step > 0 ? Math.floorDiv(pa[ax], 16) * 16 + 16 : Math.floorDiv(pa[ax] - 1, 16) * 16;
                        while (step > 0 ? next < pb[ax] : next > pb[ax]) {
                            long[] c = pa.clone();
                            c[ax] = next;
                            dense.add(new Step(c[0], c[1], c[2], a.attach, false));
                            next += 16 * step;
                        }
                    }
                }
            }
            dense.add(b);
        }
        // group segments by (block, face)
        Map<List<Long>, List<long[]>> cur = new LinkedHashMap<>();
        List<Long> curKey = null;
        List<long[]> poly = null;
        for (int i = 1; i < dense.size(); i++) {
            Step a = dense.get(i - 1), b = dense.get(i);
            if (b.outer && !(Math.floorMod(new long[]{b.x, b.y, b.z}[axis(reverse(b.attach))], 16) == 0
                && Math.floorMod(new long[]{b.x, b.y, b.z}[axis(a.attach)], 16) == 0)) {
                // round a step inside a block (or across one block face): no knuckle, the faces just change
                flush(out, curKey, poly);
                curKey = null;
                poly = null;
                continue;
            }
            if (b.outer) {
                // knuckle in the block round the corner: the cable's block before the corner, beyond the edge
                int e = reverse(b.attach);
                long[] c = cell(new long[]{a.x, a.y, a.z}, a.attach, reverse(e));
                long[] w = {c[0] + DV[e][0], c[1] + DV[e][1], c[2] + DV[e][2]};
                flush(out, curKey, poly);
                curKey = null;
                poly = null;
                out.add(new Piece(w[0], w[1], w[2], -1, new int[]{(int) (b.x - w[0] * 16), (int) (b.y - w[1] * 16), (int) (b.z - w[2] * 16)}));
                continue;
            }
            if (a.attach != b.attach) {   // inner corner: same point, new face
                flush(out, curKey, poly);
                curKey = null;
                poly = null;
                continue;
            }
            if (a.x == b.x && a.y == b.y && a.z == b.z) continue;
            long[] mid = {a.x + b.x, a.y + b.y, a.z + b.z};
            for (int k = 0; k < 3; k++) mid[k] = Math.floorDiv(mid[k], 2);
            long[] c = cell(mid, a.attach, -1);
            List<Long> key = List.of(c[0], c[1], c[2], (long) a.attach);
            if (!key.equals(curKey)) {
                flush(out, curKey, poly);
                curKey = key;
                poly = new ArrayList<>();
                poly.add(new long[]{a.x, a.y, a.z});
            }
            poly.add(new long[]{b.x, b.y, b.z});
        }
        flush(out, curKey, poly);
        return out;
    }

    private static void flush(List<Piece> out, List<Long> key, List<long[]> poly) {
        if (key == null || poly == null || poly.size() < 2) return;
        List<long[]> simple = new ArrayList<>();
        for (int i = 0; i < poly.size(); i++) {
            if (i > 0 && i + 1 < poly.size()) {
                long[] a = poly.get(i - 1), b = poly.get(i), c = poly.get(i + 1);
                boolean straight = true;
                for (int k = 0; k < 3; k++) if (Long.signum(b[k] - a[k]) != Long.signum(c[k] - b[k])) straight = false;
                if (straight) continue;
            }
            simple.add(poly.get(i));
        }
        int[] local = new int[simple.size() * 3];
        for (int i = 0; i < simple.size(); i++)
            for (int a = 0; a < 3; a++) local[3 * i + a] = (int) (simple.get(i)[a] - key.get(a) * 16);
        out.add(new Piece(key.get(0), key.get(1), key.get(2), key.get(3).intValue(), local));
    }
}
