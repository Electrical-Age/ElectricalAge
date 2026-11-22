package mods.eln.node.voxel;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The old run a route starts on (continues) or finishes on (joins), near the join point (todo #516; Baughn: "let the
 * pathfinder pass through same-type voxels of the cable within 4-5 voxels of the waypoint"). Within REACH voxels along
 * the old run from the join point (across block faces too), its voxels are freed for the router (freeing); a route that runs
 * inside it there is cut where it leaves the old run (trimHead, trimTail), so nothing is laid on top of the old run and
 * the new run starts or ends at that point: a splice or, at the old run's end, its continuation. Without it a route
 * from a point just short of a cable's end laid a piece over the old one's last voxels, and a pipe's hopped over its
 * stub. Both kinds use it: the pieces of the old run are the kind's (same cable on the same face, or any pipe).
 */
final class JoinRun {
    /** How far along the old run (voxels from the join point) a route may run inside it. */
    static final int REACH = 6;
    /** How far across the centre line (voxels) the old run's voxels are freed: past any cable's or pipe's half width. */
    private static final int ACROSS = 5;

    /** The old run's voxels freed for the router, by block (block-local, VoxelNode.voxel). */
    final Map<BlockPos, BitSet> freed;
    /**
     * The old run's voxels within REST_REACH of the join (a pipe's; a cable's within REACH): a route joining it doesn't
     * rest against them (PipeRouter.Space.rests). Else the pipe router's soft support drew it along the old pipe, a
     * block or more right beside it, which looked doubled (AdventurAgent, a short loop).
     */
    final Map<BlockPos, BitSet> unrest;
    /** How far along an old pipe from the join (voxels) a route joining it doesn't rest against it: two blocks. */
    static final int REST_REACH = 32;
    /** The old run's centre line near the join: unit edges (world voxels) a route may run along unlaid. */
    private final Set<Edge> edges;

    /** A unit edge from (x, y, z) one voxel along axis a (world voxels). */
    private record Edge(long x, long y, long z, int a) {
    }
    /** The face a cable route must lie on to run inside it (-1: a pipe). */
    private final int face;

    private JoinRun(Map<BlockPos, BitSet> freed, Map<BlockPos, BitSet> unrest, Set<Edge> edges, int face) {
        this.freed = freed;
        this.unrest = unrest;
        this.edges = edges;
        this.face = face;
    }

    /** The cable of tier and spec at join j (world voxels) lying on face attach, or null if the view has none there. */
    static JoinRun cable(RouteView view, long[] j, int attach, int tier, CableSpec spec) {
        if (attach < 0) return null;
        return of(view, j, attach, p -> p instanceof CablePart cp && !cp.isKnuckle() && cp.face == attach && cp.tier == tier
            && java.util.Objects.equals(cp.spec, spec), REACH);
    }

    /** The pipe (any tier: pipes of every tier join) at join j, or null. Fittings and junctions are not run through. */
    static JoinRun pipe(RouteView view, long[] j) {
        return of(view, j, -1, p -> p instanceof PipePart, REST_REACH);
    }

    /** The block an edge from m one voxel along axis a lies in: a cable's on face's plane (CableRouter.cell), a pipe's. */
    private static BlockPos blockOf(long[] m, int a, int face) {
        int n = face < 0 ? -1 : CableRouter.axis(face);
        long[] c = new long[3];
        for (int k = 0; k < 3; k++)
            c[k] = Math.floorDiv(m[k], 16) - (k == n && face % 2 == 1 && Math.floorMod(m[k], 16) == 0 ? 1 : 0);
        return new BlockPos((int) c[0], (int) c[1], (int) c[2]);
    }

    /** A piece of a block's run, with its block. */
    private record Owned(BlockPos block, VoxelPart part) {
    }

    /**
     * The run's centre line from j (world voxels) along the run's pieces (same), far voxels either way at most (REACH of it
     * run through and freed), across
     * block faces too, not past a pipe's fitting or junction; and the voxels of those pieces round it. Null if no piece
     * of the run passes j.
     */
    private static JoinRun of(RouteView view, long[] j, int face, Predicate<VoxelPart> same, int far) {
        Map<BlockPos, List<VoxelPart>> runs = new HashMap<>();
        Map<BlockPos, List<VoxelPart>> all = new HashMap<>();
        java.util.function.Function<BlockPos, List<VoxelPart>> runOf = b -> runs.computeIfAbsent(b, bb -> {
            List<VoxelPart> parts = view.parts(bb), run = new ArrayList<>();
            for (VoxelPart p : parts) if (same.test(p)) run.add(p);
            all.put(bb, parts);
            return run;
        });
        // breadth first along the run's segments, a voxel at a time
        Map<List<Long>, Integer> dist = new HashMap<>();
        Set<Edge> edges = new HashSet<>(), seen = new HashSet<>();
        List<Owned> owners = new ArrayList<>();   // (the piece each edge lies on, in edges' order of finding)
        List<long[]> found = new ArrayList<>();   // {x, y, z, axis}: the edges, from their lower end
        ArrayDeque<long[]> todo = new ArrayDeque<>();
        dist.put(List.of(j[0], j[1], j[2]), 0);
        todo.add(j.clone());
        while (!todo.isEmpty()) {
            long[] p = todo.poll();
            int d = dist.get(List.of(p[0], p[1], p[2]));
            if (d >= far || d > 0 && face < 0 && stopped(all, p)) continue;
            for (int a = 0; a < 3; a++)
                for (int s = -1; s <= 1; s += 2) {
                    long[] q = p.clone();
                    q[a] += s;
                    long[] m = s < 0 ? q : p;
                    BlockPos b = blockOf(m, a, face);
                    List<VoxelPart> run = runOf.apply(b);
                    long ox = b.getX() * 16L, oy = b.getY() * 16L, oz = b.getZ() * 16L;
                    int owner = along(run, new long[]{p[0] - ox, p[1] - oy, p[2] - oz}, new long[]{q[0] - ox, q[1] - oy, q[2] - oz});
                    if (owner < 0) continue;
                    if (seen.add(new Edge(m[0], m[1], m[2], a))) {
                        if (d < REACH) edges.add(new Edge(m[0], m[1], m[2], a));
                        found.add(new long[]{m[0], m[1], m[2], a, d});
                        owners.add(new Owned(b, run.get(owner)));
                    }
                    if (dist.containsKey(List.of(q[0], q[1], q[2]))) continue;
                    dist.put(List.of(q[0], q[1], q[2]), d + 1);
                    todo.add(q);
                }
        }
        if (edges.isEmpty()) return null;
        // the voxels of the piece each edge lies on, along the edge (a pipe's past it by its half bore: its box runs past
        // its end) and as far across as the piece reaches; not another run's beside it
        Map<BlockPos, BitSet> freed = new HashMap<>(), unrest = new HashMap<>();
        for (int i = 0; i < found.size(); i++) {
            long[] e = found.get(i);
            boolean near = e[4] < REACH;
            int a = (int) e[3];
            BlockPos b = owners.get(i).block();
            VoxelPart p = owners.get(i).part();
            long[] le = {e[0] - b.getX() * 16L, e[1] - b.getY() * 16L, e[2] - b.getZ() * 16L};
            int cap = p instanceof PipePart pp ? pp.bore() / 2 : 0;
            BitSet f = freed.computeIfAbsent(b, bb -> new BitSet(4096)), u = unrest.computeIfAbsent(b, bb -> new BitSet(4096));
            for (int[] bx : p.boxes()) {
                int[] lo = new int[3], hi = new int[3];
                for (int k = 0; k < 3; k++) {
                    lo[k] = (int) Math.max(bx[k], k == a ? le[k] - cap : le[k] - ACROSS);
                    hi[k] = (int) Math.min(bx[k + 3], k == a ? le[k] + 1 + cap : le[k] + ACROSS + 1);
                }
                for (int x = lo[0]; x < hi[0]; x++)
                    for (int y = lo[1]; y < hi[1]; y++)
                        for (int z = lo[2]; z < hi[2]; z++) {
                            if (near) f.set(VoxelNode.voxel(x, y, z));
                            u.set(VoxelNode.voxel(x, y, z));
                        }
            }
        }
        return new JoinRun(freed, unrest, edges, face);
    }

    /** Is a pipe's fitting (VoxelNode.fittingAt) or a junction at p (world voxels), in a block looked at so far? */
    private static boolean stopped(Map<BlockPos, List<VoxelPart>> all, long[] p) {
        for (var e : all.entrySet()) {
            BlockPos b = e.getKey();
            long x = p[0] - b.getX() * 16L, y = p[1] - b.getY() * 16L, z = p[2] - b.getZ() * 16L;
            if (x < 0 || y < 0 || z < 0 || x > 16 || y > 16 || z > 16) continue;
            if (VoxelNode.fittingAt(e.getValue(), (int) x, (int) y, (int) z)) return true;
            for (VoxelPart q : e.getValue()) if (q instanceof JunctionPart jp && jp.x == x && jp.y == y && jp.z == z) return true;
        }
        return false;
    }

    /** The index of the piece of the run on one of whose segments p and q (block-local, a voxel apart) lie, or -1. */
    private static int along(List<VoxelPart> run, long[] p, long[] q) {
        for (int r = 0; r < run.size(); r++) {
            VoxelPart part = run.get(r);
            int[] pts = part instanceof CablePart c ? c.points : ((PipePart) part).points;
            for (int i = 3; i < pts.length; i += 3) {
                boolean in = true;
                for (int a = 0; a < 3 && in; a++) {
                    long lo = Math.min(pts[i - 3 + a], pts[i + a]), hi = Math.max(pts[i - 3 + a], pts[i + a]);
                    in = p[a] >= lo && p[a] <= hi && q[a] >= lo && q[a] <= hi;
                }
                if (in) return r;
            }
        }
        return -1;
    }

    /** How far along the route from its cut (voxels) it may share the old run's voxels: a splice's end, a junction's arm. */
    private static final int AT_CUT = 4;

    /**
     * Does the route (its polyline pts, world voxels, from the start; attach: the face each point's run lies on, null for
     * a pipe) come back onto or across the old run anywhere but by the joins' own ends (its first point if it starts on
     * an old run, its last if it ends on one: AT_CUT voxels along it)? Its part there is taken as it is laid: lo and hi
     * voxels either side of its centre line across it (a pipe's half bore both ways) and, for a cable, height off its
     * face. Only the freed voxels, or the free cube round the join, let a route through the old run; the parts it lays
     * there would lie over it (a duplicate, todo #516) or be refused when laid.
     */
    boolean crosses(List<long[]> pts, List<Integer> attach, boolean startJoined, boolean endJoined, int lo, int hi, int height) {
        int total = 0;
        for (int i = 1; i < pts.size(); i++) total += dist(pts.get(i - 1), pts.get(i));
        int at = 0;
        for (int i = 1; i < pts.size(); i++) {
            long[] a = pts.get(i - 1), b = pts.get(i);
            int n = dist(a, b);
            for (int k = 0; k <= n; k++) {
                int along = at + k;
                if (startJoined && along <= AT_CUT || endJoined && total - along <= AT_CUT) continue;
                long[] p = {a[0] + Long.signum(b[0] - a[0]) * (long) k, a[1] + Long.signum(b[1] - a[1]) * (long) k, a[2] + Long.signum(b[2] - a[2]) * (long) k};
                if (onRun(p, attach == null ? -1 : attach.get(i), lo, hi, height)) return true;
            }
            at += n;
        }
        return false;
    }

    private static int dist(long[] a, long[] b) {
        return (int) (Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]));
    }

    /** Does the new part's cross-section at p (its own cells: [p - lo, p + hi) across, a cable's height off face's plane)
     *  take a freed voxel of the old run? */
    private boolean onRun(long[] p, int face, int lo, int hi, int height) {
        int n = face < 0 ? -1 : CableRouter.axis(face);
        long[] l = new long[3], h = new long[3];
        for (int k = 0; k < 3; k++) {
            if (k == n) {   // (a cable lies on the side of its plane away from its support: face 0 (down) above it)
                l[k] = face % 2 == 0 ? p[k] : p[k] - height;
                h[k] = face % 2 == 0 ? p[k] + height : p[k];
            } else {
                l[k] = p[k] - lo;
                h[k] = p[k] + hi;
            }
        }
        for (long x = l[0]; x < h[0]; x++)
            for (long y = l[1]; y < h[1]; y++)
                for (long z = l[2]; z < h[2]; z++) {
                    BitSet f = freed.get(new BlockPos((int) Math.floorDiv(x, 16), (int) Math.floorDiv(y, 16), (int) Math.floorDiv(z, 16)));
                    if (f != null && f.get(VoxelNode.voxel((int) Math.floorMod(x, 16), (int) Math.floorMod(y, 16), (int) Math.floorMod(z, 16))))
                        return true;
                }
        return false;
    }

    /** The view with the old run's voxels that a route doesn't rest against free (a pipe route's support, rests). */
    RouteView unresting(RouteView v) {
        return RouteView.freeing(v, unrest);
    }

    /** The view with the run's voxels near the join free. */
    RouteView freeing(RouteView v) {
        return RouteView.freeing(v, freed);
    }

    /** Is the unit move from p one voxel along axis a (sign s) on the run's centre line? */
    private boolean on(long[] p, int a, int s) {
        long[] m = p.clone();
        if (s < 0) m[a]--;
        return edges.contains(new Edge(m[0], m[1], m[2], a));
    }

    /**
     * How far (voxels) a straight move from a to b runs on the run's centre line from a, or -1 if a and b are the same
     * point. A move on another face (a cable) runs on none of it.
     */
    private int runs(long[] a, long[] b, int attach) {
        int ax = a[0] != b[0] ? 0 : a[1] != b[1] ? 1 : a[2] != b[2] ? 2 : -1;
        if (ax < 0) return -1;
        if (attach != face) return 0;
        int s = Long.signum(b[ax] - a[ax]);
        long n = Math.abs(b[ax] - a[ax]);
        long[] p = a.clone();
        int k = 0;
        while (k < n && on(p, ax, s)) {
            p[ax] += s;
            k++;
        }
        return k;
    }

    private static long[] toward(long[] a, long[] b, int k) {
        long[] p = a.clone();
        for (int i = 0; i < 3; i++) p[i] += Long.signum(b[i] - a[i]) * (long) k;
        return p;
    }

    /** A pipe route's points from its start, cut where it leaves the run (its first point is on it); null if all of it is. */
    List<long[]> trimHead(List<long[]> pts) {
        int i = 0;
        long[] at = pts.get(0);
        while (i + 1 < pts.size()) {
            int k = runs(at, pts.get(i + 1), -1);
            long[] next = pts.get(i + 1);
            if (k == Math.abs(next[0] - at[0]) + Math.abs(next[1] - at[1]) + Math.abs(next[2] - at[2])) {
                at = next;
                i++;
                continue;
            }
            if (k > 0) at = toward(at, next, k);
            break;
        }
        if (i + 1 >= pts.size()) return null;
        List<long[]> out = new ArrayList<>();
        out.add(at);
        out.addAll(pts.subList(i + 1, pts.size()));
        return out;
    }

    /** The same from its end (its last point on the run). */
    List<long[]> trimTail(List<long[]> pts) {
        List<long[]> rev = new ArrayList<>(pts);
        java.util.Collections.reverse(rev);
        List<long[]> t = trimHead(rev);
        if (t == null) return null;
        java.util.Collections.reverse(t);
        return t;
    }

    private static long[] xyz(CableRouter.Step s) {
        return new long[]{s.x(), s.y(), s.z()};
    }

    /**
     * A cable route's steps from its start, cut where it leaves the run (its first step is on it, on the run's face): it
     * then starts there on that face. Null if all of it is.
     */
    List<CableRouter.Step> trimStepsHead(List<CableRouter.Step> steps) {
        int i = 0;
        long[] at = xyz(steps.get(0));
        while (i + 1 < steps.size()) {
            CableRouter.Step next = steps.get(i + 1);
            long[] q = xyz(next);
            int k = runs(at, q, next.attach());
            long n = Math.abs(q[0] - at[0]) + Math.abs(q[1] - at[1]) + Math.abs(q[2] - at[2]);
            if (k >= 0 && k == n && !next.outer()) {
                at = q;
                i++;
                continue;
            }
            if (k > 0) at = toward(at, q, k);
            break;
        }
        if (i + 1 >= steps.size()) return null;
        if (i == 0 && java.util.Arrays.equals(at, xyz(steps.get(0)))) return steps;
        List<CableRouter.Step> out = new ArrayList<>();
        out.add(new CableRouter.Step(at[0], at[1], at[2], face, false));
        out.addAll(steps.subList(i + 1, steps.size()));
        return out;
    }

    /** The same from its end (its last step on the run, on its face): it then ends there. */
    List<CableRouter.Step> trimStepsTail(List<CableRouter.Step> steps) {
        int i = steps.size() - 1;
        long[] at = xyz(steps.get(i));
        while (i > 0) {
            CableRouter.Step cur = steps.get(i), prev = steps.get(i - 1);
            long[] q = xyz(prev);
            // (a move from prev to cur lies on cur's face; one round an outer corner, or a corner in place, ends the cut)
            int k = runs(at, q, cur.attach());
            long n = Math.abs(q[0] - at[0]) + Math.abs(q[1] - at[1]) + Math.abs(q[2] - at[2]);
            if (k >= 0 && k == n && !cur.outer() && prev.attach() == face) {
                at = q;
                i--;
                continue;
            }
            if (k <= 0) break;
            at = toward(at, q, k);
            break;
        }
        if (i == 0) return null;
        if (java.util.Arrays.equals(at, xyz(steps.get(i)))) return i == steps.size() - 1 ? steps : new ArrayList<>(steps.subList(0, i + 1));
        List<CableRouter.Step> out = new ArrayList<>(steps.subList(0, i));
        out.add(new CableRouter.Step(at[0], at[1], at[2], face, false));
        return out;
    }
}
