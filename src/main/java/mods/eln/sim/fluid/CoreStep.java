package mods.eln.sim.fluid;

import mods.eln.fluidcore.Network;

import java.util.ArrayList;
import java.util.List;

/**
 * The fluid core (fluidcore/, board #61) stepping a FluidNetwork in place of its own solve, behind the switch ON.
 * FluidNode stays the authority: each step copies every node's masses and energy into the core (devices write them
 * freely between steps), steps, and copies them back with what each edge carried. The core is rebuilt when the
 * network's nodes or edges change, or a species turns up that it doesn't carry.
 * <p>
 * This is the first, thin form: an edge's conductance is taken at the step's start from its upstream port, as the
 * legacy solve's is (rho k open / mu, or a valve's orifice law at the starting drop), and held for the step; the port
 * windows, the core's own thermodynamics for the nodes' reads and the legacy save values are not yet joined up.
 */
final class CoreStep {
    /** The switch: -Deln.fluidCore=true or ELN_FLUID=core (M5.1; a config option comes with the full adapter). */
    static boolean ON = Boolean.getBoolean("eln.fluidCore") || "core".equals(System.getenv("ELN_FLUID"));

    /** Audit: edges whose two ports are not at one height (each stream pays its own head over them), and the worst. */
    static long notLevel;
    static double worstDz;
    static String worstEdge = "";
    private static final java.util.Set<String> SEEN = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Steps that failed (the core's best iterate was kept), and how many were written out (fluidcore-fail/, at most 30). */
    static long failedSteps, discardedSteps;
    static int dumps;
    /** This network has been written out already (one dump each, so the 30 cover 30 networks). */
    private boolean dumped;

    static {
        // (in a game tick, 32 sub-steps at most: past that a failing network would stall the server; its step ends at the
        // best iterate, conservatively, and is counted and written out)
        Network.MAX_DEPTH = 5;
    }

    final Network net = new Network();
    final List<FluidNode> nodes;
    final List<FluidEdge> edges;
    final Network.Node[] cn;
    /** Per FluidEdge its core edge (null: not joined, its ports could not be brought to one height). */
    final Network.Edge[] ce;
    /** Per FluidEdge: the first of an opening's two one-way edges, joined in the core as one pipe (the second has none). */
    final boolean[] pipeOfPair;
    /** Core species index to registry id (null: water and air, the core's fast path). */
    final int[] ids;
    final int water, air;

    private CoreStep(FluidNetwork fn, int[] ids) {
        this.nodes = new ArrayList<>(fn.nodes);
        this.edges = new ArrayList<>(fn.edges);
        this.ids = ids;
        mods.eln.fluidcore.Species[] species = null;
        if (ids != null) {
            species = new mods.eln.fluidcore.Species[ids.length];
            for (int j = 0; j < ids.length; j++) species[j] = toCore(SpeciesRegistry.get().byId(ids[j]));
        }
        water = 0;
        air = 1;
        cn = new Network.Node[nodes.size()];
        java.util.Map<FluidNode, Network.Node> of = new java.util.IdentityHashMap<>();
        for (int i = 0; i < cn.length; i++) {
            FluidNode n = nodes.get(i);
            cn[i] = net.add(new Network.Node(n.volume, n.z, n.height, n.fixed, species));
            of.put(n, cn[i]);
        }
        ce = new Network.Edge[edges.size()];
        pipeOfPair = new boolean[ce.length];
        for (int k = 0; k < ce.length; k++) {
            FluidEdge e = edges.get(k);
            // (an opening's second edge, the way back in: the core's pipe carries both ways in one edge, its two streams)
            if (k > 0 && pipeOfPair[k - 1] && e.entrySpan > 0) continue;
            Network.Node a = of.get(e.a), b = of.get(e.b);
            double fa = e.portA, fb = e.portB, za = e.a.z + fa * e.a.height, zb = e.b.z + fb * e.b.height;
            if (Math.abs(za - zb) > 1e-6) {
                notLevel++;
                String what = String.format("dz %.3f: %s %s at %.3f -> %s %s at %.3f (z %.3f / %.3f, h %.3f / %.3f)", za - zb,
                    e.kind, e.a.fixed ? "fixed" : "node", fa, e.b.fixed ? "fixed" : "node", e.b.lumpable ? "lump" : "", fb, e.a.z, e.b.z, e.a.height, e.b.height);
                if (SEEN.add(what)) System.out.println("[fluidcore] edge not level, " + what);
                if (Math.abs(za - zb) > worstDz) {
                    worstDz = Math.abs(za - zb);
                    worstEdge = what;
                }
            }
            ce[k] = net.connect(a, fa, b, fb, 0);
            FluidEdge next = k + 1 < ce.length ? edges.get(k + 1) : null;
            if (e.kind == FluidEdge.Kind.CHECK && e.spanA > 0 && next != null && next.entrySpan > 0 && next.a == e.b && next.b == e.a)
                pipeOfPair[k] = true;
            else if (e.oneWay()) ce[k].kind = Network.Kind.CHECK;
        }
    }

    /** A stream's conductance (kg/s/Pa) carrying packet pk at drive dp: rho k open / mu, or a valve's orifice law. */
    private static double conductance(FluidEdge e, FluidNetwork.Packet pk, double dp, double open) {
        return e.area > 0
            ? FluidEdge.CD * e.area * open * Math.sqrt(2 * Math.max(pk.rho, 1e-6) / Math.max(Math.abs(dp), FluidEdge.DP_LINEAR))
            : pk.rho * e.k * open / pk.mu;
    }

    /** The network's core, built or rebuilt as needed. */
    static CoreStep of(FluidNetwork fn) {
        CoreStep c = fn.core;
        int[] ids = speciesOf(fn);
        if (c == null || c.nodes.size() != fn.nodes.size() || c.edges.size() != fn.edges.size() || !java.util.Arrays.equals(c.ids, ids)
            || !same(c.nodes, fn.nodes) || !same(c.edges, fn.edges)) {
            c = new CoreStep(fn, ids);
            fn.core = c;
        }
        return c;
    }

    private static <T> boolean same(List<T> a, List<T> b) {
        for (int i = 0; i < a.size(); i++) if (a.get(i) != b.get(i)) return false;
        return true;
    }

    /** Registry ids of every species held anywhere in the network (water and air first), or null for water and air alone. */
    static int[] speciesOf(FluidNetwork fn) {
        java.util.TreeSet<Integer> extra = new java.util.TreeSet<>();
        for (FluidNode n : fn.nodes)
            if (n.extra != null) for (int i = 0; i < n.extra.length; i++) if (n.extra[i] > 0) extra.add(i + 2);
        if (extra.isEmpty()) return null;
        int[] ids = new int[2 + extra.size()];
        ids[0] = SpeciesRegistry.WATER;
        ids[1] = SpeciesRegistry.AIR;
        int j = 2;
        for (int id : extra) ids[j++] = id;
        return ids;
    }

    /** The core's form of a game species. */
    static mods.eln.fluidcore.Species toCore(Species s) {
        if (s.id == SpeciesRegistry.WATER) return mods.eln.fluidcore.Species.WATER;
        if (s.id == SpeciesRegistry.AIR) return mods.eln.fluidcore.Species.AIR;
        return switch (s.kind) {
            case CURVE -> mods.eln.fluidcore.Species.curve(s.key, s.group, s.rhoL, s.cpL, s.cpV, s.molarMass, s.tb, s.latentTb);
            case NONE -> mods.eln.fluidcore.Species.none(s.key, s.group, s.rhoL, s.cpL, s.molarMass);
            case GAS -> mods.eln.fluidcore.Species.gas(s.key, s.cpV, s.molarMass);
            case WATER -> mods.eln.fluidcore.Species.WATER;
        };
    }

    /**
     * A step that failed: counted, the network put back as it was before the step (the core's best iterate can be far
     * from any physical state: nodes at 4000 K and 120 K followed such steps), and written out for a replay.
     */
    private void failed(double dt, double[][] m0, double[] u0, double[] h0) {
        failedSteps++;
        // (the best iterate is kept unless it is not a physical state: kept, a stub's first flooding goes on to converge
        // the next tick; put back, the next tick starts from the same contents and fails the same way for ever)
        boolean sane = true;
        for (Network.Node c : cn) {
            if (c.fixed) continue;
            double T = c.T();
            if (!Double.isFinite(c.U) || !Double.isFinite(c.P()) || !(T > 150 && T < 3000)) sane = false;
            for (double x : c.m) if (!Double.isFinite(x)) sane = false;
        }
        if (!sane) discardedSteps++;
        if (failedSteps == 1 || Long.bitCount(failedSteps) == 1)
            System.out.println("[fluidcore] failed steps so far: " + failedSteps + ", of them put back " + discardedSteps + " (this one: "
                + cn.length + " nodes, the first at z " + nodes.get(0).z + ")");
        if (dumped || dumps >= 30) {
            if (!sane) discard(m0, u0);
            return;
        }
        dumped = true;
        int n = ++dumps;
        double[][] m1 = new double[cn.length][];
        double[] u1 = new double[cn.length];
        for (int i = 0; i < cn.length; i++) {
            m1[i] = cn[i].m.clone();
            u1[i] = cn[i].U;
        }
        restore(m0, u0);
        for (int i = 0; i < cn.length; i++) cn[i].heat = h0[i];
        String text = mods.eln.fluidcore.NetworkDump.write(net, dt);
        if (sane) restore(m1, u1);
        else discard(m0, u0);
        try {
            java.nio.file.Path dir = java.nio.file.Path.of("fluidcore-fail");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path f = dir.resolve(n + ".txt");
            java.nio.file.Files.writeString(f, text);
            System.out.println("[fluidcore] a step failed (" + cn.length + " nodes, " + ce.length + " edges); written to " + f.toAbsolutePath());
        } catch (java.io.IOException e) {
            System.out.println("[fluidcore] a step failed; could not write it out: " + e);
        }
    }

    private void restore(double[][] m0, double[] u0) {
        for (int i = 0; i < cn.length; i++) {
            System.arraycopy(m0[i], 0, cn[i].m, 0, m0[i].length);
            cn[i].U = u0[i];
            cn[i].update();
        }
    }

    /** Back to the step's start, with nothing moved. */
    private void discard(double[][] m0, double[] u0) {
        restore(m0, u0);
        for (Network.Edge c : ce) if (c != null && c.moved != null) java.util.Arrays.fill(c.moved, 0);
    }

    void step(FluidNetwork fn, double dt) {
        // contents in (the authority is the FluidNode; the core wakes by its own signature when they change)
        for (int i = 0; i < cn.length; i++) {
            FluidNode n = nodes.get(i);
            Network.Node c = cn[i];
            c.m[0] = n.mWater;
            c.m[1] = n.mAir;
            for (int j = 2; j < c.m.length; j++) c.m[j] = n.extra != null && ids[j] - 2 < n.extra.length ? n.extra[ids[j] - 2] : 0;
            c.U = n.energy;
            c.heat = n.fixed ? 0 : n.heat - n.lossWPerK * (n.T() - Water.T_AMBIENT);
            n.heat = 0;
            c.update();
        }
        // each stream's conductance from its own port at the step's start (rho k open / mu, or a valve's orifice law at the
        // starting drop, its own head included), held for the step
        double[] xl = new double[ce.length];
        for (int k = 0; k < ce.length; k++) {
            Network.Edge c = ce[k];
            if (c == null) continue;
            FluidEdge e = edges.get(k);
            c.boost = e.boost;
            double dp = e.a.pressureOver(e.portA, e.spanA) - e.b.pressureOver(e.portB, e.spanB) + e.boost;
            double dz = (e.a.z + e.portA * e.a.height) - (e.b.z + e.portB * e.b.height);
            FluidNetwork.Packet pa = FluidNetwork.packet(e.a, e.portA, e.spanA), pb = FluidNetwork.packet(e.b, e.portB, e.spanB);
            boolean fromA = dp + pa.rhoHead * Network.G * dz >= 0 || (e.oneWay() && !pipeOfPair[k]);
            xl[k] = fromA ? pa.xl : pb.xl;
            double open = Math.max(0, e.open);
            c.g = conductance(e, pa, dp + pa.rhoHead * Network.G * dz, open);
            c.gBack = e.oneWay() && !pipeOfPair[k] ? Double.NaN : conductance(e, pb, dp + pb.rhoHead * Network.G * dz, open);
        }
        long slept = net.stats.slept, failures = net.stats.failures;
        double[][] m0 = new double[cn.length][];
        double[] u0 = new double[cn.length], h0 = new double[cn.length];
        for (int i = 0; i < cn.length; i++) {
            m0[i] = cn[i].m.clone();
            u0[i] = cn[i].U;
            h0[i] = cn[i].heat;
        }
        net.step(dt);
        if (net.stats.failures > failures) failed(dt, m0, u0, h0);
        boolean asleep = net.stats.slept > slept;
        // contents out
        for (int i = 0; i < cn.length; i++) {
            FluidNode n = nodes.get(i);
            if (n.fixed) continue;
            Network.Node c = cn[i];
            n.mWater = c.m[0];
            n.mAir = c.m[1];
            if (ids != null) {
                int need = 0;
                for (int j = 2; j < ids.length; j++) need = Math.max(need, ids[j] - 1);
                if (n.extra == null || n.extra.length < need) n.extra = n.extra == null ? new double[need] : java.util.Arrays.copyOf(n.extra, need);
                for (int j = 2; j < ids.length; j++) n.extra[ids[j] - 2] = c.m[j];
            }
            n.energy = c.U;
            n.update();
        }
        // what each edge carried
        int size = SpeciesRegistry.get().size();
        for (int k = 0; k < ce.length; k++) {
            FluidEdge e = edges.get(k);
            Network.Edge c = ce[k];
            double[] mv = c == null || asleep ? null : c.moved;
            e.flow = e.fLiquid = e.fVapour = e.fAir = e.fEnthalpy = 0;
            if (e.fSpecies != null) java.util.Arrays.fill(e.fSpecies, 0);
            if (mv == null) continue;
            int S = mv.length - 1;
            double w = mv[0] / dt;
            e.fLiquid = w * xl[k];
            e.fVapour = w * (1 - xl[k]);
            e.fAir = mv[1] / dt;
            e.fEnthalpy = mv[S] / dt;
            double total = 0;
            for (int j = 0; j < S; j++) total += mv[j];
            e.flow = total / dt;
            if (e.fSpecies == null || e.fSpecies.length < size) e.fSpecies = new double[size];
            for (int j = 0; j < S; j++) e.fSpecies[ids == null ? j : ids[j]] = mv[j] / dt;
        }
    }
}
