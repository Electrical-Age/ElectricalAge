package mods.eln.node.voxel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * The one router for cables and pipes (Baughn 2026-11-11): an A* core shared by every kind, with the moves a kind's
 * placement allows. SURFACE kinds (cables) move along face planes against a solid support, round inner and outer
 * corners; FREE kinds (pipes) move through the middle of blocks, never in a face plane, paying for steps that rest
 * against nothing. Both search on their kind's lattice, with its bend and block costs, inside a margin round each leg,
 * under one wall-clock deadline. Stateless (the route preview calls it from a worker thread). Pure Java: the world is
 * behind the Space interfaces. CableRouter and PipeRouter are the kinds' entry points and output stages.
 */
public final class Router {
    private Router() {
    }

    // --- the core ---

    /** Where a search may step next from a state. */
    interface Expander<K> {
        /** Push each next state of k (reached at cost g) through push. */
        void expand(K k, long g, Push<K> push);
    }

    interface Push<K> {
        /** Offer nk at cost g, reached from k; false if it was already reached as cheaply. */
        boolean push(K k, K nk, long g);
    }

    interface Heuristic<K> {
        long h(K k);
    }

    interface Goal<K> {
        boolean at(K k);
    }

    private record Open<K>(K k, long g, long f) implements Comparable<Open<K>> {
        @Override
        public int compareTo(Open<K> o) {
            return Long.compare(f, o.f);
        }
    }

    /**
     * A* from k0 until goal; the goal state, its predecessors in `from`, or null if there is none. Throws
     * PipeRouter.GaveUp past the expansion limit or the deadline.
     */
    static <K> K search(K k0, Heuristic<K> h, Goal<K> goal, Expander<K> expander, Map<K, K> from, int maxExpansions, long deadline) {
        PriorityQueue<Open<K>> open = new PriorityQueue<>();
        Map<K, Long> best = new HashMap<>();
        best.put(k0, 0L);
        open.add(new Open<>(k0, 0, h.h(k0)));
        Push<K> push = (k, nk, g) -> {
            if (g >= best.getOrDefault(nk, Long.MAX_VALUE)) return false;
            best.put(nk, g);
            from.put(nk, k);
            open.add(new Open<>(nk, g, g + h.h(nk)));
            return true;
        };
        int expansions = 0;
        try {
        while (!open.isEmpty()) {
            if (++expansions > maxExpansions || System.nanoTime() > deadline) throw new PipeRouter.GaveUp();
            Open<K> o = open.poll();
            K k = o.k;
            if (o.g > best.getOrDefault(k, Long.MAX_VALUE)) continue;
            if (goal.at(k)) return k;
            expander.expand(k, o.g, push);
        }
        return null;
        } finally {
            LAST_EXPANSIONS.set(expansions);
        }
    }

    /** The expansions of this thread's last search (tests and benchmarks). */
    static final ThreadLocal<Integer> LAST_EXPANSIONS = ThreadLocal.withInitial(() -> 0);

    static long h(long[] p, long[] t) {
        return Math.abs(p[0] - t[0]) + Math.abs(p[1] - t[1]) + Math.abs(p[2] - t[2]);
    }

    private static boolean out(long[] q, long[] lo, long[] hi) {
        for (int i = 0; i < 3; i++) if (q[i] < lo[i] || q[i] > hi[i]) return true;
        return false;
    }

    // --- SURFACE kinds (cables): directions are net.minecraft.core.Direction 3D values ---

    /** A route along faces from start through the waypoints to end. */
    static CableRouter.Result surface(CableRouter.Space space, RouteKind kind, CableRouter.End start, List<CableRouter.End> waypoints,
                                      CableRouter.End end, long budgetMs) {
        long deadline = System.nanoTime() + budgetMs * 1_000_000;
        List<CableRouter.End> stops = new ArrayList<>();
        stops.add(start);
        stops.addAll(waypoints);
        stops.add(end);
        List<CableRouter.Step> out = new ArrayList<>();
        int lastDir = start.dir();
        int[] air = {0};   // (voxels off a support at the end of the last leg: a waypoint in mid-air doesn't reset it)
        try {
            for (int leg = 0; leg + 1 < stops.size(); leg++) {
                CableRouter.End a = stops.get(leg), b = stops.get(leg + 1);
                int attach = out.isEmpty() ? a.attach() : out.get(out.size() - 1).attach();
                int arrive = leg + 2 == stops.size() ? b.dir() : -1;
                List<CableRouter.Step> p = surfaceLeg(space, kind, a, attach, lastDir, air, b, arrive, deadline);
                if (p == null) return new CableRouter.Result(null, stops.size() == 2 ? "no way along faces" : "no way along faces on leg " + (leg + 1));
                if (!out.isEmpty()) p.remove(0);
                out.addAll(p);
                if (out.size() >= 2) {
                    CableRouter.Step x = out.get(out.size() - 2), y = out.get(out.size() - 1);
                    lastDir = CableRouter.dirOf(x, y);
                }
            }
        } catch (PipeRouter.GaveUp e) {
            return new CableRouter.Result(null, "gave up: too far or too tangled to find a way (try waypoints)");
        }
        return new CableRouter.Result(CableRouter.simplify(out), null);
    }

    /** A surface search state: the point, the face it lies against, the last move (-1 just after a corner), and the
     *  voxels since the last support (0 on a support; only kinds with an air allowance leave it). */
    private record SKey(long x, long y, long z, int attach, int dir, int air) {
    }

    /** One leg from a to b; air[0] is the air run at a on entry and at b on return. */
    static List<CableRouter.Step> surfaceLeg(CableRouter.Space space, RouteKind kind, CableRouter.End a, int attach0, int lastDir,
                                             int[] air, CableRouter.End b, int arriveDir, long deadline) {
        int lattice = kind.lattice();
        long[] s = {a.x(), a.y(), a.z()}, t = {b.x(), b.y(), b.z()};
        for (int i = 0; i < 3; i++) if (Math.floorMod(s[i], lattice) != 0 || Math.floorMod(t[i], lattice) != 0) return null;
        long[] lo = new long[3], hi = new long[3];
        for (int i = 0; i < 3; i++) {
            lo[i] = Math.min(s[i], t[i]) - kind.margin();
            hi[i] = Math.max(s[i], t[i]) + kind.margin();
        }
        Map<SKey, SKey> from = new HashMap<>();
        Map<SKey, Boolean> outer = new HashMap<>();
        SKey k0 = new SKey(s[0], s[1], s[2], attach0, lastDir, air[0]);
        boolean leavePort = a.dir() >= 0 && lastDir == a.dir();
        // with an air allowance: a state reached as cheaply with no more air behind it makes this one pointless (it can do
        // everything this one can); without that pruning the air states multiplied the search past its budget wherever
        // the air opened new ground (a gap over a floor: 250 ms for a 5-block bridge)
        int airSlots = kind.airLimit() / lattice + 1;
        Map<SKey, long[]> byAir = new HashMap<>();
        SKey goal = search(k0, k -> h(new long[]{k.x(), k.y(), k.z()}, t),
            k -> k.x() == t[0] && k.y() == t[1] && k.z() == t[2] && (b.attach() < 0 || k.attach() == b.attach()) && (arriveDir < 0 || k.dir() == arriveDir),
            (k, g0, push0) -> {
                Push<SKey> push = airSlots == 1 ? push0 : (kk, nk, g) -> {
                    long[] best = byAir.computeIfAbsent(new SKey(nk.x(), nk.y(), nk.z(), nk.attach(), nk.dir(), 0), q -> {
                        long[] fresh = new long[airSlots];
                        java.util.Arrays.fill(fresh, Long.MAX_VALUE);
                        return fresh;
                    });
                    int i = nk.air() / lattice;
                    for (int j = 0; j <= i; j++) if (best[j] <= g) return false;
                    if (!push0.push(kk, nk, g)) return false;
                    best[i] = g;
                    return true;
                };
                long[] p = {k.x(), k.y(), k.z()};
                int n = CableRouter.axis(k.attach());
                // along the face
                for (int d = 0; d < 6; d++) {
                    if (CableRouter.axis(d) == n) continue;
                    if (k.equals(k0) && leavePort && d != a.dir()) continue;   // (leave a device straight out)
                    if (k.dir() >= 0 && d == CableRouter.reverse(k.dir())) continue;
                    // in the air a run goes straight on: an air stretch is one straight line from where it leaves a support
                    // (off an edge, or lifted off a face) to where it lands or ends. Bends and turns in the air made the
                    // search 10x dearer (a cloud of air states round every edge) for nothing a straight stretch can't do
                    if (k.air() > 0 && k.dir() >= 0 && d != k.dir()) continue;
                    long[] q = {p[0] + CableRouter.DV[d][0] * lattice, p[1] + CableRouter.DV[d][1] * lattice, p[2] + CableRouter.DV[d][2] * lattice};
                    if (out(q, lo, hi)) continue;
                    // a cable stays inside its block: it crosses a block's edge, never runs along one (it would straddle two)
                    if (Math.floorMod(p[3 - n - CableRouter.axis(d)], 16) == 0) continue;
                    // off a support (a gap, a ledge, lifted off a face): only within the kind's air allowance, at its
                    // tier's extra cost, and with clearance on the support side too (parts aren't support: an air run
                    // must not lie on a pipe or another cable). A supported step lands: the air run starts over
                    int nAir = 0, airCost = 0;
                    if (!surfaceSupported(space, kind, p, k.attach(), d)) {
                        nAir = k.air() + lattice;
                        airCost = kind.airCost(nAir);
                        if (airCost < 0 || surfaceStepBlocked(space, kind, p, k.attach(), d, true)) continue;
                    } else if (surfaceStepBlocked(space, kind, p, k.attach(), d, false)) continue;
                    long[] cp = CableRouter.cell(p, k.attach(), d), cq = CableRouter.cell(q, k.attach(), CableRouter.reverse(d));
                    long g = g0 + lattice + airCost + (k.dir() >= 0 && k.dir() != d ? kind.bend() : 0)
                        + (cp[0] != cq[0] || cp[1] != cq[1] || cp[2] != cq[2] ? kind.blockEntered() : 0);
                    SKey nk = new SKey(q[0], q[1], q[2], k.attach(), d, nAir);
                    if (push.push(k, nk, g)) outer.remove(nk);
                }
                // corners, toward e (across the face): a wall met here (inner), or the support ending here (outer); at a
                // block's edge or at a step inside a block (a stair's riser, a slab's edge). Not two in a row at one point:
                // that is a run of no length, and two inner corners turned a cable round to the far side of a plane, through
                // the edge where two blocks meet diagonally (the cable's width reached the far block and read as a wall)
                if (k.dir() < 0 && !k.equals(k0)) return;
                boolean airy = kind.airLimit() > 0;
                for (int e = 0; e < 6; e++) {
                    if (CableRouter.axis(e) == n) continue;
                    if (solidBeyond(space, kind, p, k.attach(), e, false)) {
                        // inner corner: up the wall met here
                        SKey nk = new SKey(p[0], p[1], p[2], e, -1, k.air());
                        if (push.push(k, nk, g0 + kind.bend())) outer.remove(nk);
                        continue;
                    }
                    // lifting off: a kind that may leave its support turns up off the face it lies on, anywhere on it, as
                    // if a wall stood straight ahead; the steps after it are air steps until it lands. Only from a support,
                    // and only ahead: toward another side the turn would wrap round the support's own edge, which is an
                    // outer corner (a knuckle), not this
                    if (airy && e == k.dir() && k.air() == 0) {
                        SKey nk = new SKey(p[0], p[1], p[2], e, -1, 0);
                        if (push.push(k, nk, g0 + kind.bend())) outer.remove(nk);
                    }
                    // outer corner: round the support's edge onto its face toward e
                    if (solidBeyond(space, kind, p, k.attach(), e, true)) continue;
                    if (!solidBeyond(space, kind, p, k.attach(), CableRouter.reverse(e), true)) continue;
                    if (knuckleBlocked(space, kind, p, k.attach(), e)) continue;
                    SKey nk = new SKey(p[0], p[1], p[2], CableRouter.reverse(e), -1, k.air());
                    if (push.push(k, nk, g0 + kind.bend())) outer.put(nk, true);
                }
            }, from, CableRouter.MAX_EXPANSIONS, deadline);
        if (goal == null) return null;
        air[0] = goal.air();
        List<CableRouter.Step> path = new ArrayList<>();
        for (SKey c = goal; c != null; c = from.get(c)) path.add(0, new CableRouter.Step(c.x(), c.y(), c.z(), c.attach(), outer.getOrDefault(c, false)));
        return path;
    }

    /** The cable's cross-section swept from p one step along d (world voxels), with its clearance (on the support side
     *  too for a step in the air). */
    static boolean surfaceStepBlocked(CableRouter.Space space, RouteKind kind, long[] p, int attach, int d, boolean inAir) {
        int lattice = kind.lattice(), width = kind.width(), height = kind.height(), clearance = kind.clearance();
        long[] q = {p[0] + CableRouter.DV[d][0] * lattice, p[1] + CableRouter.DV[d][1] * lattice, p[2] + CableRouter.DV[d][2] * lattice};
        int n = CableRouter.axis(attach), along = CableRouter.axis(d), w = 3 - n - along;
        long[] lo = new long[3], hi = new long[3];
        lo[along] = Math.min(p[along], q[along]);
        hi[along] = Math.max(p[along], q[along]);
        lo[w] = p[w] - width / 2 - clearance;
        hi[w] = p[w] + (width - width / 2) + clearance;
        int under = inAir ? clearance : 0;
        if (attach % 2 == 0) {
            lo[n] = p[n] - under;
            hi[n] = p[n] + height + clearance;
        } else {
            lo[n] = p[n] - height - clearance;
            hi[n] = p[n] + under;
        }
        return space.blocked(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
    }

    /** Is what's under a step from p along d solid (one voxel deep on attach's side, the cable's width)? */
    static boolean surfaceSupported(CableRouter.Space space, RouteKind kind, long[] p, int attach, int d) {
        int width = kind.width(), lattice = kind.lattice();
        int n = CableRouter.axis(attach), along = CableRouter.axis(d), w = 3 - n - along;
        long[] lo = new long[3], hi = new long[3];
        lo[along] = Math.min(p[along], p[along] + CableRouter.DV[d][along] * lattice);
        hi[along] = Math.max(p[along], p[along] + CableRouter.DV[d][along] * lattice);
        lo[w] = p[w] - width / 2;
        hi[w] = p[w] + (width - width / 2);
        lo[n] = attach % 2 == 0 ? p[n] - 1 : p[n];
        hi[n] = lo[n] + 1;
        return space.solid(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
    }

    /** One voxel beyond p along e, the cable's width across: on the cable's side (a wall there?) or the support's side
     *  (does the support go on?). */
    static boolean solidBeyond(CableRouter.Space space, RouteKind kind, long[] p, int attach, int e, boolean supportSide) {
        int width = kind.width(), height = kind.height();
        int n = CableRouter.axis(attach), ea = CableRouter.axis(e), w = 3 - n - ea;
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

    /** The knuckle round an outer corner at p (a box at the edge, in the block round the corner) with clearance. */
    static boolean knuckleBlocked(CableRouter.Space space, RouteKind kind, long[] p, int attach, int e) {
        int width = kind.width(), height = kind.height(), clearance = kind.clearance();
        int n = CableRouter.axis(attach), ea = CableRouter.axis(e), w = 3 - n - ea;
        long[] lo = new long[3], hi = new long[3];
        lo[w] = p[w] - width / 2 - clearance;
        hi[w] = p[w] + (width - width / 2) + clearance;
        if (attach % 2 == 0) { lo[n] = p[n]; hi[n] = p[n] + height + clearance; }
        else { lo[n] = p[n] - height - clearance; hi[n] = p[n]; }
        if (e % 2 == 1) { lo[ea] = p[ea]; hi[ea] = p[ea] + height + clearance; }
        else { lo[ea] = p[ea] - height - clearance; hi[ea] = p[ea]; }
        return space.blocked(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
    }

    // --- FREE kinds (pipes): directions 0..5 = -x +x -y +y -z +z (PipeRouter.D) ---

    /** A route through the middle of blocks from start through the waypoints to end; off-lattice ports jog onto it. */
    static PipeRouter.Result free(PipeRouter.Space space, RouteKind kind, PipeRouter.End start, List<PipeRouter.End> waypoints,
                                  PipeRouter.End end, long budgetMs) {
        long deadline = System.nanoTime() + budgetMs * 1_000_000;
        try {
            // (a port off the lattice, a device's drawn flange between its points: straight out, then across onto it)
            List<long[]> head = jog(space, kind, start), tail = jog(space, kind, end);
            if (head != null) {
                long[] r = head.get(head.size() - 1);
                start = new PipeRouter.End(r[0], r[1], r[2], -1);
            }
            if (tail != null) {
                long[] r = tail.get(tail.size() - 1);
                end = new PipeRouter.End(r[0], r[1], r[2], -1);
            }
            PipeRouter.Result inner = freeLegs(space, kind, start, waypoints, end, deadline);
            if (inner.error != null || head == null && tail == null) return inner;
            List<long[]> pts = new ArrayList<>();
            if (head != null) pts.addAll(head.subList(0, head.size() - 1));
            pts.addAll(inner.points);
            if (tail != null) for (int i = tail.size() - 2; i >= 0; i--) pts.add(tail.get(i));
            return new PipeRouter.Result(PipeRouter.simplify(pts), null);
        } catch (PipeRouter.GaveUp e) {
            return new PipeRouter.Result(null, "gave up: too far or too tangled to find a way (try waypoints)");
        }
    }

    /**
     * For a port off the lattice (only across its direction): the points from it one lattice step straight out, then
     * across to the nearest lattice point a run of this bore can reach, ending there; null if it is on the lattice (or
     * no such point is free: the route then fails as before).
     */
    static List<long[]> jog(PipeRouter.Space space, RouteKind kind, PipeRouter.End e) {
        if (e.dir() < 0) return null;
        int lattice = kind.lattice();
        long[] p = {e.x(), e.y(), e.z()};
        int along = e.dir() / 2;
        boolean off = false;
        for (int a = 0; a < 3; a++) if (Math.floorMod(p[a], lattice) != 0) off = true;
        if (!off || Math.floorMod(p[along], lattice) != 0) return null;
        long[] q = {p[0] + PipeRouter.D[e.dir()][0] * lattice, p[1] + PipeRouter.D[e.dir()][1] * lattice, p[2] + PipeRouter.D[e.dir()][2] * lattice};
        if (segmentBlocked(space, kind, p, q)) return null;
        // the cross axes' lattice points either side, nearest first
        int[] cross = new int[2];
        int n = 0;
        for (int a = 0; a < 3; a++) if (a != along) cross[n++] = a;
        List<long[]> options = new ArrayList<>();
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++) {
                long[] r = q.clone();
                for (int c = 0; c < 2; c++) {
                    int a = cross[c];
                    long m = Math.floorMod(r[a], lattice);
                    if (m == 0) continue;
                    r[a] = (c == 0 ? i : j) == 0 ? r[a] - m : r[a] - m + lattice;
                }
                options.add(r);
            }
        options.sort(java.util.Comparator.comparingLong(r -> Math.abs(r[cross[0]] - q[cross[0]]) + Math.abs(r[cross[1]] - q[cross[1]])));
        for (long[] r : options) {
            long[] mid = q.clone();
            mid[cross[0]] = r[cross[0]];
            if (segmentBlocked(space, kind, q, mid) || segmentBlocked(space, kind, mid, r)) continue;
            List<long[]> out = new ArrayList<>(List.of(p, q));
            if (!java.util.Arrays.equals(mid, q) && !java.util.Arrays.equals(mid, r)) out.add(mid);
            if (!java.util.Arrays.equals(r, q)) out.add(r);
            return out;
        }
        return null;
    }

    /** Does a run of this bore along the straight segment a..b hit anything (across the bore; along it, the segment)? */
    static boolean segmentBlocked(PipeRouter.Space space, RouteKind kind, long[] a, long[] b) {
        if (java.util.Arrays.equals(a, b)) return false;
        long h = kind.width() / 2;
        int axis = a[0] != b[0] ? 0 : a[1] != b[1] ? 1 : 2;
        long[] lo = new long[3], hi = new long[3];
        for (int i = 0; i < 3; i++) {
            long e = i == axis ? 0 : h;
            lo[i] = Math.min(a[i], b[i]) - e;
            hi[i] = Math.max(a[i], b[i]) + e;
        }
        return space.blocked(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
    }

    static PipeRouter.Result freeLegs(PipeRouter.Space space, RouteKind kind, PipeRouter.End start, List<PipeRouter.End> waypoints,
                                      PipeRouter.End end, long deadline) {
        List<PipeRouter.End> stops = new ArrayList<>();
        stops.add(start);
        stops.addAll(waypoints);
        stops.add(end);
        List<long[]> pts = new ArrayList<>();
        int arriveDir = -1;
        for (int leg = 0; leg + 1 < stops.size(); leg++) {
            PipeRouter.End a = stops.get(leg), b = stops.get(leg + 1);
            // leave a port along its direction; a waypoint continues the way the last leg arrived (no forced bend)
            int leaveDir = leg == 0 ? a.dir() : -1;
            int arrive = leg + 2 == stops.size() && b.dir() >= 0 ? b.dir() ^ 1 : -1;   // (arrive into a port: opposite its out-direction)
            List<long[]> p = freeLeg(space, kind, a, b, leaveDir, arriveDir, arrive, deadline);
            if (p == null) return new PipeRouter.Result(null, leg == 0 && stops.size() == 2 ? "no free path" : "no free path on leg " + (leg + 1));
            if (!pts.isEmpty()) p.remove(0);
            pts.addAll(p);
            arriveDir = p.size() >= 2 ? PipeRouter.dirOf(pts.get(pts.size() - 2), pts.get(pts.size() - 1)) : arriveDir;
        }
        return new PipeRouter.Result(PipeRouter.simplify(pts), null);
    }

    /** A free search state: the point and the last move. */
    private record FKey(long x, long y, long z, int dir) {
    }

    /** One leg: A* over (point, last direction). The lattice points, start and end included, or null. */
    static List<long[]> freeLeg(PipeRouter.Space space, RouteKind kind, PipeRouter.End a, PipeRouter.End b, int leaveDir, int lastDir,
                                int arriveDir, long deadline) {
        int lattice = kind.lattice();
        long[] s = {a.x(), a.y(), a.z()}, t = {b.x(), b.y(), b.z()};
        for (int i = 0; i < 3; i++) if (Math.floorMod(s[i], lattice) != 0 || Math.floorMod(t[i], lattice) != 0) return null;
        long[] lo = new long[3], hi = new long[3];
        for (int i = 0; i < 3; i++) {
            lo[i] = Math.min(s[i], t[i]) - kind.margin();
            hi[i] = Math.max(s[i], t[i]) + kind.margin();
        }
        Map<FKey, FKey> from = new HashMap<>();
        FKey k0 = new FKey(s[0], s[1], s[2], lastDir);
        FKey goal = search(k0, k -> h(new long[]{k.x(), k.y(), k.z()}, t),
            k -> k.x() == t[0] && k.y() == t[1] && k.z() == t[2] && (arriveDir < 0 || k.dir() == arriveDir),
            (k, g0, push) -> {
                long[] p = {k.x(), k.y(), k.z()};
                for (int d = 0; d < 6; d++) {
                    if (k.equals(k0) && leaveDir >= 0 && d != leaveDir) continue;   // (leave a port straight out)
                    if (k.dir() >= 0 && d == (k.dir() ^ 1)) continue;               // (no doubling back)
                    long[] q = {p[0] + PipeRouter.D[d][0] * lattice, p[1] + PipeRouter.D[d][1] * lattice, p[2] + PipeRouter.D[d][2] * lattice};
                    if (out(q, lo, hi)) continue;
                    // a run stays inside blocks: it crosses a face plane, never runs in one (or it would straddle two)
                    boolean inPlane = false;
                    for (int x = 0; x < 3; x++) if (x != d / 2 && Math.floorMod(q[x], 16) == 0) inPlane = true;
                    if (inPlane) continue;
                    if (freeStepBlocked(space, kind, p, d)) continue;
                    long g = g0 + lattice + (k.dir() >= 0 && k.dir() != d ? kind.bend() : 0) + (freeSupported(space, kind, q, d) ? 0 : kind.unsupported())
                        + (Math.floorDiv(q[0], 16) != Math.floorDiv(p[0], 16) || Math.floorDiv(q[1], 16) != Math.floorDiv(p[1], 16)
                        || Math.floorDiv(q[2], 16) != Math.floorDiv(p[2], 16) ? kind.blockEntered() : 0);
                    push.push(k, new FKey(q[0], q[1], q[2], d), g);
                }
            }, from, PipeRouter.MAX_EXPANSIONS, deadline);
        if (goal == null) return null;
        List<long[]> path = new ArrayList<>();
        for (FKey c = goal; c != null; c = from.get(c)) path.add(0, new long[]{c.x(), c.y(), c.z()});
        return path;
    }

    /** Does a run of this bore, moved one lattice step from p along dir, hit anything? */
    static boolean freeStepBlocked(PipeRouter.Space space, RouteKind kind, long[] p, int dir) {
        long h = kind.width() / 2;
        int lattice = kind.lattice();
        long[] q = {p[0] + PipeRouter.D[dir][0] * lattice, p[1] + PipeRouter.D[dir][1] * lattice, p[2] + PipeRouter.D[dir][2] * lattice};
        long[] lo = new long[3], hi = new long[3];
        for (int a = 0; a < 3; a++) {
            // across the bore; along the move only the step itself (a pipe leaving a device's face doesn't reach into it)
            long e = a == dir / 2 ? 0 : h;
            lo[a] = Math.min(p[a], q[a]) - e;
            hi[a] = Math.max(p[a], q[a]) + e;
        }
        return space.blocked(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
    }

    /** Does a run at q (moving along d) rest against something on any side across its run? */
    static boolean freeSupported(PipeRouter.Space space, RouteKind kind, long[] q, int d) {
        long h = kind.width() / 2;
        int rest = kind.rest();
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
                    lo[side] = q[side] - h - rest;
                    hi[side] = q[side] - h;
                } else {
                    lo[side] = q[side] + h;
                    hi[side] = q[side] + h + rest;
                }
                if (space.blocked(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2])) return true;
            }
        }
        return false;
    }
}
