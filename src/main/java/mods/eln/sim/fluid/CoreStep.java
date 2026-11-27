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
public final class CoreStep {
    /** The core steps every network (the legacy solve is gone; Baughn 2026-11-10: straight to the end state). */
    static final boolean ON = true;

    /** Audit: edges whose two ports are not at one height (each stream pays its own head over them), and the worst. */
    static long notLevel;
    static double worstDz;
    static String worstEdge = "";
    private static final java.util.Set<String> SEEN = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Steps that failed (the core's best iterate was kept), and how many were written out (fluidcore-fail/, at most 30). */
    static long failedSteps, discardedSteps, slowSteps;
    static int dumps;
    /** This network has been written out already (one dump each, so the 30 cover 30 networks). */
    private boolean dumped;
    /** Its steps in a row whose result wasn't finite (each put back): from the second on, a stall (Jade's alarm). */
    int nonFiniteRun;

    static {
        // (a sub-step failing at 1/256 of the tick ends the network's step at its last converged sub-step, the rest of the
        // tick's time dropped: a stiff moment, hot water flashing into a pipe stub, is crossed in small steps over a few
        // ticks; a best iterate taken instead could be no physical state, and put back, the next tick failed the same way)
        Network.MAX_DEPTH = 8;
        Network.CUT_SHORT = true;
    }

    /**
     * The most wall time one network's step may take in the game (ns; 0: no limit; Network.MAX_STEP_NANOS): a stiff
     * network once took a 60 s tick and the watchdog killed Incognito, in a loop, since its chunks are kept loaded (a
     * steam line with water in it, 2026-11-21). Off (0) until a server starts (Eln, GAME_BUDGET); unit tests run without
     * it, GameTests with it (ElnGameTests).
     */
    public static long budgetNanos = 0;
    /**
     * 250 ms (-Deln.fluidBudgetMs), with Network's hard stop at 1.5x inside a solve. 10 ms cut every network that
     * sub-stepped for a moment (Baughn 1573837175051522166: more or less every plant was broken; a boiler at 0 g/s needed
     * 10-25 ms a step), and 40 ms still cut 27 GameTests' start-up transients. It only bounds a network's first heavy
     * steps: one that stays heavy moves to the background (HEAVY_AFTER); taking 250 ms every tick, one put Incognito at
     * 4 TPS, and the watchdog killed it.
     */
    public static final long GAME_BUDGET = Long.getLong("eln.fluidBudgetMs", 250L) * 1_000_000L;

    final Network net = new Network();
    /** Its last step ran out of Network.MAX_STEP_NANOS (FluidWorld says where). */
    boolean overBudget;
    final List<FluidNode> nodes;
    final List<FluidEdge> edges;
    final Network.Node[] cn;
    /** Per FluidEdge its core edge (null: not joined, its ports could not be brought to one height). */
    final Network.Edge[] ce;
    /** Per FluidEdge: the first of an opening's two one-way edges, joined in the core as one pipe (the second has none). */
    final boolean[] pipeOfPair;
    /** For an opening: its second core pipe, from the opening's top edge (ce has the bottom one). */
    final Network.Edge[] ceTop;
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
        ceTop = new Network.Edge[ce.length];
        for (int k = 0; k < ce.length; k++) {
            FluidEdge e = edges.get(k);
            // (an opening's second edge, the way back in: the core's pipe carries both ways in one edge, its two streams)
            if (k > 0 && pipeOfPair[k - 1] && e.entrySpan > 0) continue;
            Network.Node a = of.get(e.a), b = of.get(e.b);
            double fa = e.portA, fb = e.portB, za = e.a.z + fa * e.a.height, zb = e.b.z + fb * e.b.height;
            if (e.kind == FluidEdge.Kind.FLOW) {
                // (a prescribed flow: no head between its ends, its far end the outside)
                ce[k] = net.connect(a, fa, b, fb, 0);
                ce[k].kind = Network.Kind.FLOW;
                continue;
            }
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
            FluidEdge next = k + 1 < ce.length ? edges.get(k + 1) : null;
            if (e.kind == FluidEdge.Kind.CHECK && e.spanA > 0 && next != null && next.entrySpan > 0 && next.a == e.b && next.b == e.a) {
                // (an opening: two pipes from its bottom and its top edge, so liquid can leave under the gas coming back,
                // each stream paying its own head over the opening's height)
                pipeOfPair[k] = true;
                ce[k] = net.connect(a, Math.max(0, fa - e.spanA / 2), b, fb, 0);
                ceTop[k] = net.connect(a, Math.min(1, fa + e.spanA / 2), b, fb, 0);
            } else {
                ce[k] = net.connect(a, fa, b, fb, 0);
                if (e.kind == FluidEdge.Kind.PUMP) ce[k].kind = Network.Kind.PUMP;
                else if (e.kind == FluidEdge.Kind.TURBINE) ce[k].kind = Network.Kind.TURBINE;
                else if (e.kind == FluidEdge.Kind.REGULATOR) ce[k].kind = Network.Kind.REGULATOR;
                else if (e.oneWay()) ce[k].kind = Network.Kind.CHECK;
            }
        }
    }

    /**
     * A source's mix in the core's order (each core species' share, then J/kg: Network.Edge.supply), into into if it fits;
     * null for none (a sink, or a source with no mix: it gives its far end's draw).
     */
    double[] supply(FluidEdge e, double[] into) {
        if (e.shares == null) return null;
        int S = ids == null ? 2 : ids.length;
        double[] out = into != null && into.length == S + 1 ? into : new double[S + 1];
        double h = 0;
        var reg = SpeciesRegistry.get();
        for (int j = 0; j < S; j++) {
            int id = ids == null ? j : ids[j];
            double x = id < e.shares.length ? e.shares[id] : 0;
            out[j] = x;
            if (x != 0) h += x * reg.byId(id).hVapour(e.kelvin);
        }
        out[S] = h;
        return out;
    }

    /** A stream's conductance (kg/s/Pa) carrying packet pk at drive dp: rho k open / mu, or a valve's orifice law. */
    static double conductance(FluidEdge e, FluidNetwork.Packet pk, double dp, double open) {
        return e.area > 0
            ? FluidEdge.CD * e.area * open * Math.sqrt(2 * Math.max(pk.rho, 1e-6) / Math.max(Math.abs(dp), FluidEdge.DP_LINEAR))
            : pk.rho * e.k * open / mu(e, pk);
    }

    /**
     * The viscosity a stream from pk's node meets along e: pk's, its liquid part taken share by share when e knows its ends'
     * shares of its resistance (FluidEdge.shareA, shareB; option A'): the downstream end's share for the same liquid at
     * the downstream temperature, the rest at the upstream's. A downstream end with no liquid of its own gives none of its
     * temperature (what flows in is the upstream liquid as it comes). Water's viscosity is a constant: unchanged, bit for bit.
     */
    static double mu(FluidEdge e, FluidNetwork.Packet pk) {
        if (Double.isNaN(e.shareA) || pk.liqMu <= 0 || pk.node == null) return pk.mu;
        boolean fromA = pk.node == e.a;
        FluidNode down = fromA ? e.b : e.a;
        double share = fromA ? e.shareB : e.shareA;
        if (!(share > 0) || down.mLiquid() <= 0) return pk.mu;
        double there = pk.liqMuAt(down.T());
        if (there == pk.liqMu || !(there > 0) || !Double.isFinite(there)) return pk.mu;
        return pk.mu + share * (there - pk.liqMu);
    }

    /** The network's core, built or rebuilt as needed. */
    static CoreStep of(FluidNetwork fn) {
        CoreStep c = fn.core;
        int[] ids = speciesOf(fn);
        if (c == null || c.nodes.size() != fn.nodes.size() || c.edges.size() != fn.edges.size() || !java.util.Arrays.equals(c.ids, ids)
            || !same(c.nodes, fn.nodes) || !same(c.edges, fn.edges)) {
            if (c != null) c.abandon();   // (a background step of the core it replaces)
            c = new CoreStep(fn, ids);
            fn.core = c;
            fn.builds++;
        }
        return c;
    }

    private static <T> boolean same(List<T> a, List<T> b) {
        for (int i = 0; i < a.size(); i++) if (a.get(i) != b.get(i)) return false;
        return true;
    }

    /**
     * Registry ids of every species held anywhere in the network or declared by a source's mix (FluidEdge.shares, set or
     * not this step: a source turned on or off doesn't rebuild the core), water and air first; or null for water and air alone.
     */
    static int[] speciesOf(FluidNetwork fn) {
        java.util.TreeSet<Integer> extra = new java.util.TreeSet<>();
        for (FluidNode n : fn.nodes)
            if (n.extra != null) for (int i = 0; i < n.extra.length; i++) if (n.extra[i] > 0) extra.add(i + 2);
        for (FluidEdge e : fn.edges)
            if (e.kind == FluidEdge.Kind.FLOW && e.shares != null) for (int i = 2; i < e.shares.length; i++) if (e.shares[i] != 0) extra.add(i);
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
            // (a burst pipe's air venting from 50 bar cools towards 100 K as it expands: a physical state)
            if (!finite(c) || !Double.isFinite(c.P()) || !(T > 50 && T < 6000)) sane = false;
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

    /** Are a core node's energy and masses finite? */
    static boolean finite(Network.Node c) {
        if (!Double.isFinite(c.U)) return false;
        for (double x : c.m) if (!Double.isFinite(x)) return false;
        return true;
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
        for (Network.Edge c : ceTop) if (c != null && c.moved != null) java.util.Arrays.fill(c.moved, 0);
    }

    /** A step that took many sub-steps (or hit the cap): written out once per network, as failures are. */
    private void slow(double dt, double[][] m0, double[] u0, double[] h0, long substeps) {
        slowSteps++;
        if (dumped || dumps >= 30) return;
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
        restore(m1, u1);
        try {
            java.nio.file.Path dir = java.nio.file.Path.of("fluidcore-fail");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path f = dir.resolve(n + "-slow.txt");
            java.nio.file.Files.writeString(f, text);
            System.out.println("[fluidcore] a step took " + substeps + " sub-steps (" + cn.length + " nodes); written to " + f.toAbsolutePath());
        } catch (java.io.IOException e) {
            System.out.println("[fluidcore] a slow step; could not write it out: " + e);
        }
    }

    /** Time (s) a cut-short step left undone, carried into the next steps (one tick's worth more per tick, OWED_MAX at most). */
    private double owed;
    static final double OWED_MAX = 0.5;
    static final java.util.concurrent.atomic.AtomicLong CRAWLS = new java.util.concurrent.atomic.AtomicLong();
    private boolean stallDumped;

    /**
     * Background steps: a network whose steps took over heavyNanos (5 ms in the game; else a tenth of the budget,
     * SYNC_SHARE) per tick of step HEAVY_AFTER times more often than not (FluidNode.slowSteps, kept on the nodes over rebuilds) is stepped on a worker thread, one
     * step at a time, each started on a tick and applied on the first tick after it finishes, and each as long as the
     * ticks since the one before it began (MAX_SPAN), its result handed out over those ticks (handOut): it keeps real
     * time at a coarser step, and the server thread never waits for it. Dysnomia's plant (110 nodes, 2026-11-21) sat in a transient
     * of ~500 steps at up to a second each: on the server thread it took Incognito to 4 TPS, and under any per-tick
     * limit it never got through. Steps back under half that time bring it back to the server thread. (Baughn
     * 1573800740550344846: a thread pool.)
     */
    public static final int HEAVY_AFTER = 10, SYNC_SHARE = 10;
    /**
     * In the game, a step over this (ns) counts as heavy instead of a tenth of the budget (0: that tenth). 5 ms
     * (-Deln.fluidHeavyMs, Eln): a big running plant's 9-14 ms steps (Incognito 2026-11-27, 193 and 69 nodes, a quarter
     * of every tick between them) leave the server thread for the workers (Baughn 1575818571764858992: a stopgap).
     */
    public static long heavyNanos = 0;
    /** A background step's own budget (ns): generous, since nothing waits for it; a runaway one still ends. */
    static final long BACKGROUND_BUDGET = 20_000_000_000L;
    private static final java.util.concurrent.ExecutorService WORKER = java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Electrical Age fluid background");
        t.setDaemon(true);
        return t;
    });
    /** The background step under way and what finishing it needs. */
    private java.util.concurrent.Future<?> job;
    private Pending pending;
    /** How long the last finished step took (ms), and whether it ran in the background (FluidWorld's log, the stats). */
    public double lastMs;

    public boolean background;
    public int lastSpan = 1;

    /** A started step: what finish() needs once it has run. */
    static final class Pending {
        /** What the solve threw, on whatever thread it ran (complete rethrows it on the server thread). */
        Throwable error;
        /** The nodes' state is in the core (load), which took loadNanos; ticks: those since the last step began. */
        volatile boolean loaded;
        volatile long loadNanos;
        int ticks;
        double tick, dt;
        double[] xl, u0, h0, heat0;
        double[][] m0;
        long failures, sub0, cut0, crawls0;
        volatile long took;
        boolean background;
        int slow, span;
        long slept0;
    }

    /** Tests: a background step's result is taken no sooner than this many ticks after its start (a test server ticks as
     *  fast as it can, so a step that takes real time would span no ticks to speak of). */
    public static int testHoldTicks;
    private int waited;
    /**
     * A background step covers the real time since the one before it began (Baughn 1574029078598778991: fluid time must
     * not fall behind the devices', whose furnaces, timers and watchdogs run every tick: a network at a fraction of real
     * time had its boilers overheat, 2026-11-21): the ticks since the last one was started, MAX_SPAN at most (the rest is
     * dropped, and counts towards Jade's alarm). The cost of a transient doesn't grow with the step: its sub-steps do
     * the same work (Dysnomia's blow-down: 23-31 s of solving per 30 s simulated, at 0.05 s steps and at 0.8 s).
     */
    static final int MAX_SPAN = 20;
    private int ticksSince;
    /**
     * What the results so far still owe the devices, handed out evenly over the ticks their step stood for (ticksLeft),
     * so a device books each tick what moved in a tick, and all of it once: per edge {liquid, vapour, air, enthalpy, all,
     * work} (kg and J) and its species (kg), per node its wall heat (J).
     */
    private double[][] remE, remS;
    private double[] remWall;
    private int ticksLeft, behindTicks;
    /** The length (s) of the last step the core really ran: asleep, it keeps that step's moves, for a step of that length. */
    private double awakeDt;
    /** Tests: all the mass its steps moved over edges (kg, absolute sum), and the sum of what each tick handed out to devices as flows (kg, absolute). */
    public double movedKg, bookedSum;
    private static long failureLogged;

    boolean pending() {
        return job != null;
    }

    /** Its network is replaced or forgotten: a background step under way is cancelled, and the heat it took given back. */
    void abandon() {
        flush();
        if (job == null) return;
        net.cancelled = true;
        job.cancel(false);
        for (int i = 0; i < cn.length; i++) nodes.get(i).heat += pending.heat0[i];
        job = null;
        pending = null;
    }

    /**
     * Its core is replaced or forgotten: the wall heat still to be handed out stays with the nodes (FluidNode.wallCarry),
     * for the next core; else a shell would never be debited for heat its water already took. What its edges still owed
     * (flows, a turbine's work) is lost with it: devices book less than moved, never more.
     */
    private void flush() {
        if (remWall == null) return;
        for (int i = 0; i < remWall.length; i++) {
            if (remWall[i] != 0 && !nodes.get(i).fixed) mods.eln.fluid.FluidWorld.carryWall(nodes.get(i), remWall[i]);
            remWall[i] = 0;
        }
    }

    /**
     * The threads a tick's solves run on, the server thread among them (-Deln.fluidThreads; 1: the server thread alone).
     * Apart from the background workers, whose steps run for many ticks and would hold a tick's solves up.
     */
    private static final mods.eln.fluidcore.FluidPool POOL = new mods.eln.fluidcore.FluidPool(
        Integer.getInteger("eln.fluidThreads", Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2))));
    /** Solves expected to take less than this in all (ns, by their last) run on the server thread alone: waking the
     *  helpers costs about as much. */
    static final long POOL_FROM_NANOS = 300_000;

    public static int threads() {
        return POOL.threads();
    }

    /**
     * Run the solves of networks begun this tick (FluidNetwork.begin was true), and return when all are done: on the
     * pool's threads, the longest (by their last solve) first, or one by one here when there is little to do. True if
     * the pool ran them. What a solve throws is kept for its network's finish.
     */
    public static boolean solveAll(List<FluidNetwork> begun) {
        long expect = 0;
        for (FluidNetwork n : begun) expect += n.lastSolveNanos();
        if (begun.size() < 2 || POOL.threads() < 2 || expect < POOL_FROM_NANOS) {
            for (FluidNetwork n : begun) n.run();
            return false;
        }
        List<FluidNetwork> order = new ArrayList<>(begun);
        order.sort((a, b) -> Long.compare(b.lastSolveNanos(), a.lastSolveNanos()));
        List<Runnable> tasks = new ArrayList<>(order.size());
        for (FluidNetwork n : order) tasks.add(n::run);
        POOL.runAll(tasks);
        return true;
    }

    /** A whole step on this thread (begin, run, complete). */
    void step(FluidNetwork fn, double dt) {
        Pending p = begin(fn, dt);
        if (p != null) {
            run(p);
            complete(p);
        }
    }

    /**
     * The solve of a step begun on the server thread, on any thread (FluidWorld runs a tick's on the pool): this
     * network's state into the core (load), then the core's step. It touches nothing another network or the server
     * thread uses meanwhile; complete takes the results out.
     */
    void run(Pending p) {
        try {
            if (!p.loaded) load(p);
            long t0 = System.nanoTime();
            net.step(p.dt);
            p.took = System.nanoTime() - t0;
        } catch (Throwable t) {
            p.error = t;
        }
    }

    /** On the server thread, after run: the step's results onto the nodes and edges; what the solve threw is thrown here. */
    void complete(Pending p) {
        if (p.error instanceof RuntimeException r) throw r;
        if (p.error instanceof Error e) throw e;
        if (p.error != null) throw new IllegalStateException(p.error);
        finish(p);
    }

    /** How long its last solve on the tick's pool took (ns): FluidWorld hands the long ones out first. */
    long lastSolveNanos;

    /**
     * On the server thread: a tick of this network up to its solve. The nodes' and edges' state goes into the core; a
     * background step is started, waited for or applied. Returns the step to run and complete, or null when this tick
     * has no solve of its own (a background step runs, or its result was just applied).
     */
    Pending begin(FluidNetwork fn, double dt) {
        boolean applied = false;
        ticksSince++;
        if (job != null) {
            if (!job.isDone() || waited < testHoldTicks) {
                waited++;
                waiting(dt);
                return null;
            }
            Pending p = pending;
            java.util.concurrent.Future<?> j = job;
            job = null;
            pending = null;
            try {
                j.get();
            } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                // (its core's state is unknown: a new one is built from the nodes, which still hold the step's start; said
                // once a minute: on the server thread this would have been a crash, here it would be a line a tick)
                long now = System.currentTimeMillis();
                if (now - failureLogged > 60_000) {
                    failureLogged = now;
                    mods.eln.Eln.LOG.error("[fluidcore] a background step failed; its network starts over from its nodes", e);
                }
                for (int i = 0; i < cn.length; i++) nodes.get(i).heat += p.heat0[i];
                flush();
                fn.core = null;
                waiting(dt);
                return null;
            }
            finish(p);
            if (p.background) {   // (on the network: a rebuilt core starts its counters over)
                fn.backgroundNanos += p.took;
                fn.backgroundSteps++;
            }
            applied = true;   // (this tick has its results: the next step may start, but nothing more is applied or cleared)
        }
        int slow = 0;
        for (FluidNode n : nodes) slow = Math.max(slow, n.slowSteps);
        // (back on this thread only once the count is down to half: a plant whose steps straddle the band flipped at the
        // threshold, each flip a tick with no flow and one with two ticks' worth)
        boolean bg = budgetNanos > 0 && (slow >= HEAVY_AFTER || background && slow > HEAVY_AFTER / 2);
        if (applied && !bg) {   // (light again: back on this thread from the next tick, owing the ticks its last step took)
            return null;
        }
        // (a background step: as long as the ticks since the last one began)
        int ticks = Math.max(1, ticksSince), span = bg ? Math.min(ticks, MAX_SPAN) : 1;
        double droppedNow = bg ? (ticks - span) * dt : 0;
        if (!bg) owed = Math.min(owed + (ticks - 1) * dt, OWED_MAX);
        ticksSince = 0;
        // (at most one tick's worth caught up per tick: devices step their own processes by the tick, and a second of
        // fluid landing in one tick would be a spike they all see)
        // (a background step is span ticks long; its time isn't owed)
        double tick = dt, ask = bg ? span * dt : dt + Math.min(owed, dt);
        owed -= ask - dt;
        dt = ask;
        Pending p = new Pending();
        p.tick = tick;
        p.dt = dt;
        p.slow = slow;
        p.background = bg;
        p.ticks = ticks;
        // (a background step's state goes in now, before devices touch the nodes again; a tick's own in run, on its thread)
        if (bg) load(p);
        if (bg) {
            net.budget = BACKGROUND_BUDGET;
            net.span = span;
            behindTicks = 0;
            pending = p;
            double stepDt = dt;
            background = true;
            waited = 0;
            p.span = span;
            // (over MAX_SPAN ticks behind: that time is lost; waiting() counted it towards Jade's alarm tick by tick)
            if (droppedNow > 0) mods.eln.fluid.FluidWorld.recordProgress(nodes, stallNow(), droppedNow);
            job = WORKER.submit(() -> {
                long t0 = System.nanoTime();
                net.step(stepDt);
                p.took = System.nanoTime() - t0;
            });
            if (!applied) waiting(tick);
            return null;
        }
        p.span = 1;
        net.span = 1;
        net.budget = -1;
        Network.MAX_STEP_NANOS = budgetNanos;   // (the same for every network: set here, read by the solves)
        return p;
    }

    /**
     * The nodes' and edges' state into the core, for the step p (begin's second half, the costly one: every port's
     * packet). It reads and writes this network's own nodes, edges and core, nothing shared: for a tick's own step it
     * runs on the solve's thread (run).
     */
    private void load(Pending p) {
        long t0 = System.nanoTime();
        int ticks = p.ticks;
        double[] heat0 = new double[cn.length];
        // contents in (the authority is the FluidNode; the core wakes by its own signature when they change)
        for (int i = 0; i < cn.length; i++) {
            FluidNode n = nodes.get(i);
            Network.Node c = cn[i];
            if (!n.fixed && n.guardFinite(() -> mods.eln.fluid.FluidWorld.whereIs(n))) n.update();   // (a NaN put in by anything since the last step: healed, not stepped)
            c.m[0] = n.mWater;
            c.m[1] = n.mAir;
            for (int j = 2; j < c.m.length; j++) c.m[j] = n.extra != null && ids[j] - 2 < n.extra.length ? n.extra[ids[j] - 2] : 0;
            c.U = n.energy;
            // (devices add their heat (W) every tick: over the ticks since the last step began, their mean)
            c.heat = n.fixed ? 0 : n.heat / ticks - n.lossWPerK * (n.T() - Water.T_AMBIENT);
            heat0[i] = n.heat;
            n.heat = 0;
            c.wallUA = n.fixed ? 0 : n.wallUA;
            c.wallT = n.wallT;
            c.update();
        }
        // each stream's conductance from its own port at the step's start (rho k open / mu, or a valve's orifice law at the
        // starting drop, its own head included), held for the step
        double[] xl = new double[ce.length];
        for (int k = 0; k < ce.length; k++) {
            Network.Edge c = ce[k];
            if (c == null) continue;
            FluidEdge e = edges.get(k);
            if (e.kind == FluidEdge.Kind.FLOW) {
                // (a prescribed flow: its rate, gates and what it carries; no conductance)
                c.rate = e.rate;
                c.phase = e.phase;
                c.minPa = e.minPa;
                c.maxPa = e.maxPa;
                c.supply = supply(e, c.supply);
                xl[k] = e.shares != null ? 0 : e.phase == Network.Phase.LIQUID ? 1 : e.phase == Network.Phase.GAS ? 0
                    : FluidNetwork.packet(e.a, e.portA, 0).xl;
                continue;
            }
            c.boost = e.boost;
            c.conductance = e.conductance;
            c.band = e.band;
            double dp = e.a.pressureOver(e.portA, e.spanA) - e.b.pressureOver(e.portB, e.spanB) + e.boost;
            double dz = (e.a.z + e.portA * e.a.height) - (e.b.z + e.portB * e.b.height);
            FluidNetwork.Packet pa = FluidNetwork.packet(e.a, e.portA, e.spanA), pb = FluidNetwork.packet(e.b, e.portB, e.spanB);
            boolean fromA = dp + pa.rhoHead * Network.G * dz >= 0 || (e.oneWay() && !pipeOfPair[k]) || e.kind == FluidEdge.Kind.PUMP;
            xl[k] = fromA ? pa.xl : pb.xl;
            double open = Math.max(0, e.open);
            if (pipeOfPair[k]) {
                // (each of the opening's two pipes takes half its conductance, carrying what its own edge draws)
                for (int top = 0; top < 2; top++) {
                    Network.Edge h = top == 0 ? c : ceTop[k];
                    h.boost = e.boost;
                    FluidNetwork.Packet po = FluidNetwork.packet(e.a, h.fa, 0);
                    double dpo = e.a.pressureAt(h.fa) - e.b.pressureOver(e.portB, e.spanB) + e.boost;
                    h.g = 0.5 * conductance(e, po, dpo + po.rhoHead * Network.G * h.dz, open);
                    h.gBack = 0.5 * conductance(e, pb, dpo + pb.rhoHead * Network.G * h.dz, open);
                }
                continue;
            }
            if (e.kind == FluidEdge.Kind.TURBINE) {
                // (a turbine is its nozzle and its work in the core's solve: the device sets open and the drop)
                c.nozzle = e.nozzle;
                c.open = open;
                c.drop = e.drop;
                c.eta = e.eta;
                continue;
            }
            if (e.kind == FluidEdge.Kind.PUMP) {
                // (a pump is its curve in the core's solve: the device sets its speed; open scales it too)
                c.qMax = e.qMax;
                c.h0 = e.h0;
                c.gas = e.gas;
                c.anyPhase = e.anyPhase;
                c.eta = e.eta;
                c.speed = e.speed * open;
                continue;
            }
            // (a gas-only edge's conductance is its gas's: the port's packet blends in the liquid near the surface)
            FluidNetwork.Packet pq = e.gasOnly ? FluidNetwork.packet(e.a, 1.0, 0) : pa;
            c.gasOnly = e.gasOnly;
            c.g = conductance(e, pq, dp + pa.rhoHead * Network.G * dz, open);
            c.gBack = e.oneWay() ? Double.NaN : conductance(e, pb, dp + pb.rhoHead * Network.G * dz, open);
            // (a regulator: its set point and mode, the rest of its law in the core's solve)
            c.setPa = e.setPa;
            c.relief = e.relief;
        }
        p.xl = xl;
        p.heat0 = heat0;
        p.failures = net.stats.failures;
        p.slept0 = net.stats.slept;
        p.m0 = new double[cn.length][];
        p.u0 = new double[cn.length];
        p.h0 = new double[cn.length];
        for (int i = 0; i < cn.length; i++) {
            p.m0[i] = cn[i].m.clone();
            p.u0[i] = cn[i].U;
            p.h0[i] = cn[i].heat;
        }
        p.sub0 = net.stats.substeps;
        p.cut0 = net.stats.cut;
        p.crawls0 = net.stats.crawls;
        p.loaded = true;
        p.loadNanos = System.nanoTime() - t0;
    }

    /** A tick while a background step runs: the devices get this tick's share of the last results; the core isn't touched. */
    private void waiting(double tick) {
        overBudget = true;
        // (its last result is still being handed out: on time. With nothing left it is behind; the next step covers
        // that time, up to MAX_SPAN ticks)
        boolean behind = ticksLeft <= 0;
        if (behind) behindTicks++;
        handOut(tick);
        double before = stallNow();
        mods.eln.fluid.FluidWorld.recordProgress(nodes, behind ? before + tick : before, 0);
    }

    /** The longest time (s) any of its nodes has now been behind. */
    private double stallNow() {
        double s = 0;
        for (FluidNode n : nodes) s = Math.max(s, n.stallNow);
        return s;
    }

    /** This tick's share of what the results still owe, onto the edges and nodes (rates per second; work and wall heat in J). */
    private void handOut(double tick) {
        if (remE == null) {
            for (FluidEdge e : edges) clear(e);
            for (FluidNode n : nodes) if (!n.fixed) n.wallJ = 0;
            return;
        }
        int parts = Math.max(1, ticksLeft);
        for (int k = 0; k < remE.length; k++) {
            FluidEdge e = edges.get(k);
            double[] r = remE[k];
            double liquid = r[0] / parts, vapour = r[1] / parts, air = r[2] / parts, enthalpy = r[3] / parts, all = r[4] / parts, work = r[5] / parts;
            r[0] -= liquid;
            r[1] -= vapour;
            r[2] -= air;
            r[3] -= enthalpy;
            r[4] -= all;
            r[5] -= work;
            e.fLiquid = liquid / tick;
            e.fVapour = vapour / tick;
            e.fAir = air / tick;
            e.fEnthalpy = enthalpy / tick;
            e.flow = all / tick;
            bookedSum += Math.abs(all);
            e.work = work / tick;
            e.workJ = work;
            double[] s = remS[k];
            if (s == null) {
                if (e.fSpecies != null) java.util.Arrays.fill(e.fSpecies, 0);
                continue;
            }
            if (e.fSpecies == null || e.fSpecies.length < s.length) e.fSpecies = new double[s.length];
            for (int j = 0; j < s.length; j++) {
                double m = s[j] / parts;
                s[j] -= m;
                e.fSpecies[j] = m / tick;
            }
        }
        for (int i = 0; i < remWall.length; i++) {
            FluidNode n = nodes.get(i);
            if (n.fixed) continue;
            double q = remWall[i] / parts;
            remWall[i] -= q;
            n.wallJ = q;
        }
        if (ticksLeft > 0) ticksLeft--;
    }

    private static void clear(FluidEdge e) {
        e.flow = e.fLiquid = e.fVapour = e.fAir = e.fEnthalpy = 0;
        e.work = e.workJ = 0;
        if (e.fSpecies != null) java.util.Arrays.fill(e.fSpecies, 0);
    }

    /** After the core's step (on this thread, or a background one's first tick after it): its results onto nodes and edges. */
    /** Tests only: run on the core's network as a step's result is taken (to make that result what a solver bug would). */
    static java.util.function.Consumer<Network> testAfterStep;

    private void finish(Pending p) {
        if (testAfterStep != null) testAfterStep.accept(net);
        double tick = p.tick, dt = p.dt;
        if (remE == null) {
            remE = new double[ce.length][6];
            remS = new double[ce.length][];
            remWall = new double[cn.length];
        }
        double[] xl = p.xl, u0 = p.u0, h0 = p.h0;
        double[][] m0 = p.m0;
        long failures = p.failures, sub0 = p.sub0, cut0 = p.cut0, crawls0 = p.crawls0;
        // (a step the core slept through keeps the moves of its last real step, which may have been longer or shorter
        // than this one: scaled to this one's length)
        boolean slept = net.stats.slept > p.slept0;
        if (!slept) awakeDt = dt;
        double kept = slept && awakeDt > 0 ? dt / awakeDt : 1;
        boolean idle = p.background;   // (its time isn't owed)
        lastMs = p.took / 1e6;
        if (!p.background) lastSolveNanos = p.loadNanos + p.took;
        background = p.background;

        overBudget = net.overBudget || p.background;
        // (heavy: over heavyNanos, or a tenth of the budget, per tick of step (a background step stands for span ticks,
        // and its cost grows with them); light: under half that. The count moves by one a step, HEAVY_AFTER * 2 at most)
        long heavy = heavyNanos > 0 ? heavyNanos : budgetNanos / SYNC_SHARE, took = p.took / Math.max(1, p.span);
        int next = budgetNanos <= 0 ? 0 : took > heavy ? Math.min(p.slow + 1, 2 * HEAVY_AFTER) : took < heavy / 2 ? Math.max(p.slow - 1, 0) : p.slow;
        mods.eln.fluid.FluidWorld.recordSlow(nodes, next);
        // (solves accepted at the precision floor, counted to watch: a few thousand in the 28 steam GameTests)
        long crawled = CRAWLS.addAndGet(net.stats.crawls - crawls0);
        if (net.stats.crawls > crawls0 && Long.bitCount(crawled) == 1 && crawled >= 16) System.out.println("[fluidcore] solves accepted at the precision floor so far: " + crawled);
        // (a result that isn't finite is a failed step, written out and put back below, never healed into the nodes: it
        // did none of its time, which is owed; in a row, from the second on, its network is stalled)
        boolean finite = true;
        for (Network.Node c : cn) if (!c.fixed && !finite(c)) finite = false;
        nonFiniteRun = finite ? 0 : nonFiniteRun + 1;
        double done = finite ? net.done : 0;
        // (a step cut short covered less than dt: the rest is owed, so heat put in over it isn't lost and the network
        // catches up; flows below are what moved this tick, per tick, so what devices book from them balances)
        if (!idle) owed += Math.max(0, dt - done);
        // (the stall record lives on the nodes, so a network rebuilt mid-stall, as a burst rebuilds it, keeps it)
        double before = 0;
        for (FluidNode n : nodes) before = Math.max(before, n.stallNow);
        // (a background step: by its own length; what it left undone is dropped, and one that fell behind its ticks keeps
        // the stall its waiting ticks counted, so a network always behind reaches Jade's alarm)
        double stall = idle ? (done < 0.5 * dt ? before + dt : behindTicks > 0 ? before : 0) : done < 0.5 * tick ? before + tick : 0;
        if (nonFiniteRun == 1) stall = before;   // (one put back may be a fluke: the alarm from the second in a row)
        double dropped = Math.max(0, owed - OWED_MAX) + (idle ? Math.max(0, dt - done) : 0);
        // (a network stalled for half a second is written out once more: its first failure is often a harmless one)
        if (done == 0 && owed > OWED_MAX && !stallDumped) {
            stallDumped = true;
            dumped = false;
        }
        // (and what it owes is capped: a stall of seconds repaid at twice the rate gave turbines double power for as long,
        // and oversped one. Time dropped moves nothing: heat links pass heat by the time stepped, so the books still
        // balance)
        owed = Math.min(owed, OWED_MAX);
        mods.eln.fluid.FluidWorld.recordProgress(nodes, stall, dropped);
        // (written out with the step length asked for: a replay at the tick's length isn't the step that failed)
        if (net.stats.failures > failures || !finite) failed(dt, m0, u0, h0);
        if (!finite) {
            // (put back: its devices' heat goes to the next step, and its wall heat and work, maybe NaN, to nobody)
            for (int i = 0; i < cn.length; i++) {
                nodes.get(i).heat += p.heat0[i];
                cn[i].wallJ = 0;
            }
            for (Network.Edge c : ce) if (c != null) c.workJ = 0;
            for (Network.Edge c : ceTop) if (c != null) c.workJ = 0;
        }
        else if (net.stats.cut > cut0 || net.stats.substeps - sub0 > 64L * p.span) slow(dt, m0, u0, h0, net.stats.substeps - sub0);
        dt = tick;
        // contents out
        for (int i = 0; i < cn.length; i++) {
            FluidNode n = nodes.get(i);
            if (n.fixed) continue;
            Network.Node c = cn[i];
            // (what devices put into the node while a background step ran is carried over; on this thread it is nothing)
            double dW = n.mWater - m0[i][0], dA = n.mAir - m0[i][1], dU = n.energy - u0[i];
            n.mWater = Math.max(0, c.m[0] + dW);
            n.mAir = Math.max(0, c.m[1] + dA);
            if (ids != null) {
                int need = 0;
                for (int j = 2; j < ids.length; j++) need = Math.max(need, ids[j] - 1);
                if (n.extra == null || n.extra.length < need) n.extra = n.extra == null ? new double[need] : java.util.Arrays.copyOf(n.extra, need);
                for (int j = 2; j < ids.length; j++) n.extra[ids[j] - 2] = Math.max(0, c.m[j] + (n.extra[ids[j] - 2] - m0[i][j]));
            }
            n.energy = c.U + dU;
            // (asleep, the core keeps the last step's heat, as it keeps its edges' moves: a network sleeps in a steady
            // state, and a condenser's cooling or a boiler's shell passes heat through it all the same; read as 0, a
            // condenser asleep booked no heat)
            remWall[i] += c.wallJ * kept + mods.eln.fluid.FluidWorld.takeWallCarry(n);
            n.guardFinite(() -> mods.eln.fluid.FluidWorld.whereIs(n));   // (what a device put in during a background step)
            n.update();
        }
        // what each edge carried: added to what is still owed, and handed out over the ticks the step stood for
        int size = SpeciesRegistry.get().size();
        for (int k = 0; k < ce.length; k++) {
            Network.Edge c = ce[k];
            // (asleep, the core keeps the last step's moves: the state stands still, but a pump circulating round a loop
            // still circulates; reported as nothing, its loop flow read 0)
            double[] mv = c == null ? null : c.moved;
            if (mv != null && ceTop[k] != null && ceTop[k].moved != null) {
                double[] both = mv.clone();
                for (int j = 0; j < both.length; j++) both[j] += ceTop[k].moved[j];
                mv = both;
            }
            double[] r = remE[k];
            if (c != null) r[5] += c.workJ * kept;
            if (mv == null) continue;
            int S = mv.length - 1;
            if (kept != 1) {
                mv = mv.clone();
                for (int j = 0; j < mv.length; j++) mv[j] *= kept;
            }
            r[0] += mv[0] * xl[k];
            r[1] += mv[0] * (1 - xl[k]);
            r[2] += mv[1];
            r[3] += mv[S];
            if (remS[k] == null || remS[k].length < size) remS[k] = remS[k] == null ? new double[size] : java.util.Arrays.copyOf(remS[k], size);
            for (int j = 0; j < S; j++) {
                r[4] += mv[j];
                movedKg += Math.abs(mv[j]);
                remS[k][ids == null ? j : ids[j]] += mv[j];
            }
        }
        // (a remainder still being handed out keeps its ticks: a network just back on this thread doesn't get it at once)
        ticksLeft = Math.max(ticksLeft, p.span);
        lastSpan = p.span;
        handOut(tick);
    }
}
