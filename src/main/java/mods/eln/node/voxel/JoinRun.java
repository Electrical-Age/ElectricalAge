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
    /** The old run's centre line near the join: unit edges (world voxels) a route may run along unlaid. */
    private final Set<Edge> edges;

    /** A unit edge from (x, y, z) one voxel along axis a (world voxels). */
    private record Edge(long x, long y, long z, int a) {
    }
    /** The face a cable route must lie on to run inside it (-1: a pipe). */
    private final int face;

    private JoinRun(Map<BlockPos, BitSet> freed, Set<Edge> edges, int face) {
        this.freed = freed;
        this.edges = edges;
        this.face = face;
    }

    /** The cable of tier and spec at join j (world voxels) lying on face attach, or null if the view has none there. */
    static JoinRun cable(RouteView view, long[] j, int attach, int tier, CableSpec spec) {
        if (attach < 0) return null;
        return of(view, j, attach, p -> p instanceof CablePart cp && !cp.isKnuckle() && cp.face == attach && cp.tier == tier
            && java.util.Objects.equals(cp.spec, spec));
    }

    /** The pipe (any tier: pipes of every tier join) at join j, or null. Fittings and junctions are not run through. */
    static JoinRun pipe(RouteView view, long[] j) {
        return of(view, j, -1, p -> p instanceof PipePart);
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
     * The run's centre line from j (world voxels) along the run's pieces (same), REACH voxels either way at most, across
     * block faces too, not past a pipe's fitting or junction; and the voxels of those pieces round it. Null if no piece
     * of the run passes j.
     */
    private static JoinRun of(RouteView view, long[] j, int face, Predicate<VoxelPart> same) {
        Map<BlockPos, List<VoxelPart>> runs = new HashMap<>();
        Map<BlockPos, List<long[]>> stops = new HashMap<>();
        java.util.function.Function<BlockPos, List<VoxelPart>> runOf = b -> runs.computeIfAbsent(b, bb -> {
            List<VoxelPart> run = new ArrayList<>();
            List<long[]> st = new ArrayList<>();
            long ox = bb.getX() * 16L, oy = bb.getY() * 16L, oz = bb.getZ() * 16L;
            for (VoxelPart p : view.parts(bb)) {
                if (same.test(p)) run.add(p);
                int[] f = p instanceof ValvePart v ? new int[]{v.x, v.y, v.z} : p instanceof SafetyValvePart v ? new int[]{v.x, v.y, v.z}
                    : p instanceof PipeSensorPart v ? new int[]{v.x, v.y, v.z} : p instanceof JunctionPart v ? new int[]{v.x, v.y, v.z} : null;
                if (f != null && face < 0) st.add(new long[]{ox + f[0], oy + f[1], oz + f[2]});
            }
            stops.put(bb, st);
            return run;
        });
        // breadth first along the run's segments, a voxel at a time
        Map<List<Long>, Integer> dist = new HashMap<>();
        Set<Edge> edges = new HashSet<>();
        List<Owned> owners = new ArrayList<>();   // (the piece each edge lies on, in edges' order of finding)
        List<long[]> found = new ArrayList<>();   // {x, y, z, axis}: the edges, from their lower end
        ArrayDeque<long[]> todo = new ArrayDeque<>();
        dist.put(List.of(j[0], j[1], j[2]), 0);
        todo.add(j.clone());
        while (!todo.isEmpty()) {
            long[] p = todo.poll();
            int d = dist.get(List.of(p[0], p[1], p[2]));
            if (d >= REACH || d > 0 && stopped(stops.values(), p)) continue;
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
                    if (edges.add(new Edge(m[0], m[1], m[2], a))) {
                        found.add(new long[]{m[0], m[1], m[2], a});
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
        Map<BlockPos, BitSet> freed = new HashMap<>();
        for (int i = 0; i < found.size(); i++) {
            long[] e = found.get(i);
            int a = (int) e[3];
            BlockPos b = owners.get(i).block();
            VoxelPart p = owners.get(i).part();
            long[] le = {e[0] - b.getX() * 16L, e[1] - b.getY() * 16L, e[2] - b.getZ() * 16L};
            int cap = p instanceof PipePart pp ? pp.bore() / 2 : 0;
            BitSet f = freed.computeIfAbsent(b, bb -> new BitSet(4096));
            for (int[] bx : p.boxes()) {
                int[] lo = new int[3], hi = new int[3];
                for (int k = 0; k < 3; k++) {
                    lo[k] = (int) Math.max(bx[k], k == a ? le[k] - cap : le[k] - ACROSS);
                    hi[k] = (int) Math.min(bx[k + 3], k == a ? le[k] + 1 + cap : le[k] + ACROSS + 1);
                }
                for (int x = lo[0]; x < hi[0]; x++)
                    for (int y = lo[1]; y < hi[1]; y++)
                        for (int z = lo[2]; z < hi[2]; z++) f.set(VoxelNode.voxel(x, y, z));
            }
        }
        return new JoinRun(freed, edges, face);
    }

    private static boolean stopped(java.util.Collection<List<long[]>> stops, long[] p) {
        for (List<long[]> l : stops) for (long[] s : l) if (s[0] == p[0] && s[1] == p[1] && s[2] == p[2]) return true;
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
