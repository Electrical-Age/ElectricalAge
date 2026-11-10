package mods.eln.fluid;

import mods.eln.sim.IProcess;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNetwork;
import mods.eln.sim.fluid.FluidNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The fluid devices in the simulation (notes/pipes/DESIGN.md phase 2), stepped once per tick as one of the simulator's
 * slow processes. Two ports couple when they sit on neighbouring blocks, open on the faces that touch, at the same
 * spot on that face, in the same level: a boiler set on a turbine feeds it. Devices join and leave with their EA node (connectJob / disconnectJob), so
 * a node Stasis keeps frozen is out of its network; the networks are rebuilt when that set changes.
 */
public final class FluidWorld implements IProcess {
    public static final FluidWorld INSTANCE = new FluidWorld();

    private final Set<FluidDevice> devices = new LinkedHashSet<>();
    private final List<FluidNetwork> networks = new ArrayList<>();
    private final List<FluidDevice.Vent> vents = new ArrayList<>();
    private final List<FluidDevice.Link> links = new ArrayList<>();
    /** Pipe runs lumped into one node each (DESIGN.md section 5): lump -> its members, and member -> lump. */
    private final Map<FluidNode, List<FluidNode>> lumps = new IdentityHashMap<>();
    private final Map<FluidNode, FluidNode> lumpOf = new IdentityHashMap<>();
    private boolean dirty;

    private FluidWorld() {
    }

    public static void add(FluidDevice d) {
        if (INSTANCE.devices.add(d)) INSTANCE.dirty = true;
    }

    public static void remove(FluidDevice d) {
        if (INSTANCE.devices.remove(d)) INSTANCE.dirty = true;
    }

    /** A device's ports changed (a pipe laid or taken out): rebuild the networks. */
    public static void changed(FluidDevice d) {
        INSTANCE.dirty = true;
    }

    /** Server start and stop: forget everything (the simulator's process list is cleared too). */
    public static FluidWorld reset() {
        INSTANCE.devices.clear();
        INSTANCE.networks.clear();
        INSTANCE.vents.clear();
        INSTANCE.links.clear();
        INSTANCE.lumps.clear();
        INSTANCE.lumpOf.clear();
        INSTANCE.dirty = false;
        return INSTANCE;
    }

    /** For tests and meters: the network a node is in (null if none). */
    public static FluidNetwork networkOf(FluidNode n) {
        INSTANCE.rebuildIfDirty();
        n = INSTANCE.lumpOf.getOrDefault(n, n);
        for (FluidNetwork net : INSTANCE.networks) if (net.nodes.contains(n)) return net;
        return null;
    }

    private final java.util.Set<FluidNode> burstThisStep = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    /** A run that burst less than this long ago (ticks) is venting through its crack: its other pieces hold. */
    public static final long BURST_QUIET = 20 * 30;

    /**
     * May this piece of pipe burst now? One per run per step, and not while the run is venting from a burst in the last
     * BURST_QUIET ticks (AdventurAgent: a whole run burst piece by piece while the first crack let the pressure down).
     */
    public static boolean claimBurst(FluidNode piece, long now) {
        FluidNode lump = INSTANCE.lumpOf.getOrDefault(piece, piece);
        var members = INSTANCE.lumps.get(lump);
        for (FluidNode m : members == null ? List.of(piece) : members) if (now - m.burstTick < BURST_QUIET) return false;
        if (!INSTANCE.burstThisStep.add(lump)) return false;
        piece.burstTick = now;
        return true;
    }

    /** The node standing for a piece of pipe this step (its run's lump), or the node itself. */
    public static FluidNode nodeFor(FluidNode n) {
        INSTANCE.rebuildIfDirty();
        return INSTANCE.lumpOf.getOrDefault(n, n);
    }

    /** Write a run's lump back into its pieces now (before saving a piece whose run changed since the step). */
    public static void writeBackOf(FluidNode piece) {
        FluidNode lump = INSTANCE.lumpOf.get(piece);
        if (lump == null) return;
        for (FluidNode m : INSTANCE.lumps.get(lump)) {
            m.setShareOf(lump, m.volume / lump.volume);
            m.update();
        }
    }

    /** The edges at a node this step (its ports' couplings and vents). */
    public static List<FluidEdge> edgesOf(FluidNode n) {
        List<FluidEdge> out = new ArrayList<>();
        FluidNetwork net = networkOf(n);
        n = INSTANCE.lumpOf.getOrDefault(n, n);
        if (net != null) for (FluidEdge e : net.edges) if (e.a == n || e.b == n) out.add(e);
        return out;
    }

    /** A network's progress after a step (CoreStep), onto its nodes and their runs' pieces (FluidNode.stallNow). */
    public static void recordProgress(List<FluidNode> nodes, double stall, double dropped) {
        for (FluidNode n : nodes) {
            List<FluidNode> pieces = INSTANCE.lumps.get(n);
            if (pieces != null) for (FluidNode p : pieces) progress(p, stall, dropped);
            progress(n, stall, dropped);
        }
    }

    private static void progress(FluidNode n, double stall, double dropped) {
        n.stallNow = stall;
        n.longestStall = Math.max(n.longestStall, stall);
        n.droppedTime += dropped;
    }

    /** How many pieces n's run is made of (1 for a node in no run with others). */
    public static int runSize(FluidNode n) {
        INSTANCE.rebuildIfDirty();
        FluidNode lump = INSTANCE.lumpOf.get(n);
        List<FluidNode> ms = lump == null ? null : INSTANCE.lumps.get(lump);
        return ms == null ? 1 : ms.size();
    }

    @Override
    public void process(double time) {
        rebuildIfDirty();
        // (over a copy: a device may fail in its step, and its explosion take others out of the set: a lab server
        // crashed on a ConcurrentModificationException when a steam turbine's casing burst; the ones gone are skipped)
        List<FluidDevice> stepping = new ArrayList<>(devices);
        for (FluidDevice d : stepping) if (devices.contains(d)) d.beforeFluidStep(time);
        // a run's wall: its pieces' heat links summed, at their mean wall temperature
        for (var e : lumps.entrySet()) {
            double ua = 0, uat = 0;
            for (FluidNode m : e.getValue()) {
                ua += m.wallUA;
                uat += m.wallUA * m.wallT;
            }
            e.getKey().wallUA = ua;
            e.getKey().wallT = ua > 0 ? uat / ua : 0;
        }
        for (FluidDevice.Vent v : vents) v.edge.open = v.open;
        for (FluidDevice.Link l : links) {
            l.edge.open = l.open;
            l.edge.boost = l.boost;
            l.edge.speed = l.speed;
            l.edge.qMax = l.qMax;
            l.edge.h0 = l.h0;
            l.edge.drop = l.drop;
            l.edge.conductance = l.conductance;
        }
        for (FluidNetwork n : networks) n.step(time);
        if (traceSteps > 0 && traceNode != null) trace();
        if (scanSteps > 0) scan();
        if (watchAll) watch();
        writeBack();
        burstThisStep.clear();
        for (FluidDevice d : stepping) if (devices.contains(d)) d.afterFluidStep(time);
    }

    /** Keep a rolling record of every node's step-to-step flips (the GameTest harness's limit-cycle check). */
    public static boolean watchAll;
    /** Per node: {P(t-1), P(t-2), flip history bits (1: it turned back by over 0.05 bar that step), the step last seen}. */
    private static final Map<FluidNode, double[]> watched = new IdentityHashMap<>();
    private static long watchStep;

    private void watch() {
        watchStep++;
        // (nodes of removed devices: dropped once they have been gone 100 steps)
        if (watchStep % 100 == 0) watched.values().removeIf(r -> r[3] < watchStep - 100);
        for (FluidNetwork net : networks)
            for (FluidNode n : net.nodes) {
                if (n.fixed) continue;
                double p = n.P();
                double[] r = watched.computeIfAbsent(n, k -> new double[]{Double.NaN, Double.NaN, 0, 0});
                long bits = (long) r[2] << 1;
                if (!Double.isNaN(r[1])) {
                    double d1 = p - r[0], d0 = r[0] - r[1];
                    if (d1 * d0 < 0 && Math.abs(d1) > 5e3 && Math.abs(d0) > 5e3) bits |= 1;
                }
                r[2] = bits & 0xFFFFF;
                r[1] = r[0];
                r[0] = p;
                r[3] = watchStep;
            }
    }

    /** How many of a node's (its run's) last 20 steps it flipped, while watchAll. */
    public static int recentFlips(FluidNode n) {
        double[] r = watched.get(nodeFor(n));
        return r == null ? 0 : Long.bitCount((long) r[2]);
    }

    /** The devices in the world now (a copy). */
    public static List<FluidDevice> devices() {
        return new ArrayList<>(INSTANCE.devices);
    }

    /** /elnlab fluidscan: steps left to watch, and per node its last two pressures and flip / swing tallies. */
    public static int scanSteps;
    private static final Map<FluidNode, double[]> scanned = new IdentityHashMap<>();

    /** Start a scan of every network for n steps (a fresh tally). */
    public static void startScan(int n) {
        scanned.clear();
        scanSteps = n;
    }

    /**
     * Per node: {P(t-1), P(t-2), steps seen, flips (the pressure turned back each step by over 0.05 bar), biggest swing}.
     * A period-2 limit cycle (the boiler feed valve's of 2026-11-04, before check valves entered the solve open) flips every step.
     */
    private void scan() {
        scanSteps--;
        for (FluidNetwork net : networks)
            for (FluidNode n : net.nodes) {
                if (n.fixed) continue;
                double p = n.P();
                // (5 min, 6 max, 7 the last turning point's pressure, 8 its direction (+1 rising, -1 falling), 9 slow reversals
                // of over 0.1 bar: a hunting loop swings back and forth over many steps)
                double[] r = scanned.computeIfAbsent(n, k -> new double[]{Double.NaN, Double.NaN, 0, 0, 0, p, p, p, 0, 0});
                r[5] = Math.min(r[5], p);
                r[6] = Math.max(r[6], p);
                if (r[8] >= 0 && p < r[7] - 1e4) {
                    if (r[8] > 0) r[9]++;
                    r[8] = -1;
                    r[7] = p;
                } else if (r[8] <= 0 && p > r[7] + 1e4) {
                    if (r[8] < 0) r[9]++;
                    r[8] = 1;
                    r[7] = p;
                } else if (r[8] > 0 ? p > r[7] : p < r[7]) r[7] = p;
                if (!Double.isNaN(r[1])) {
                    double d1 = p - r[0], d0 = r[0] - r[1];
                    if (d1 * d0 < 0 && Math.abs(d1) > 5e3 && Math.abs(d0) > 5e3) r[3]++;
                    r[4] = Math.max(r[4], Math.abs(d1));
                }
                r[1] = r[0];
                r[0] = p;
                r[2]++;
            }
        if (scanSteps == 0) scanReport();
    }

    /** Log the nodes that flipped most steps, with where their devices are. */
    private void scanReport() {
        Map<FluidNode, String> where = new IdentityHashMap<>();
        for (FluidDevice d : devices)
            for (FluidDevice.FluidPort p : d.fluidPorts()) {
                var b = BlockPos.of(p.pos());
                where.putIfAbsent(nodeFor(p.node()), d.getClass().getSimpleName() + " " + b.getX() + "," + b.getY() + "," + b.getZ());
            }
        List<Map.Entry<FluidNode, double[]>> hits = new ArrayList<>();
        for (var e : scanned.entrySet()) {
            double[] r = e.getValue();
            if (r[2] > 4 && (r[3] / (r[2] - 2) > 0.5 || r[9] >= 4)) hits.add(e);
        }
        hits.sort((a, b) -> Double.compare(b.getValue()[4], a.getValue()[4]));
        StringBuilder b = new StringBuilder(String.format("fluidscan: %d nodes watched, %d flip most steps or hunt", scanned.size(), hits.size()));
        for (var e : hits)
            b.append(String.format("%n  %s: flips %.0f of %.0f steps (step swing up to %.3f bar), %.0f slow reversals over %.3f..%.3f bar, P now %.3f bar, V %.5f m3",
                where.getOrDefault(e.getKey(), "?"), e.getValue()[3], e.getValue()[2] - 2, e.getValue()[4] / 1e5, e.getValue()[9],
                e.getValue()[5] / 1e5, e.getValue()[6] / 1e5, e.getKey().P() / 1e5, e.getKey().volume));
        mods.eln.Eln.LOG.info(b.toString());
        scanned.clear();
    }

    /** /elnlab fluidtrace: the node whose network is logged after each step, for traceSteps more steps. */
    public static FluidNode traceNode;
    public static int traceSteps;

    /** One line per node (pressure, z, volume, water, air, T) and per edge (ends, kind, open, boost, flow) of traceNode's network. */
    private void trace() {
        FluidNetwork net = networkOf(nodeFor(traceNode));
        traceSteps--;
        if (net == null) {
            mods.eln.Eln.LOG.info("fluidtrace: the node is in no network");
            return;
        }
        StringBuilder b = new StringBuilder("fluidtrace step (" + traceSteps + " left)");
        for (int i = 0; i < net.nodes.size(); i++) {
            FluidNode n = net.nodes.get(i);
            b.append(String.format("%n  n%d%s P=%.4f bar z=%.2f h=%.2f V=%.5f w=%.6f a=%.3g E=%.6g T=%.2f", i, n.fixed ? " fixed" : "", n.P() / 1e5,
                n.z, n.height, n.volume, n.mWater, n.mAir, n.energy, n.T() - 273.15));
        }
        for (FluidEdge e : net.edges)
            b.append(String.format("%n  e n%d->n%d %s open=%.4f boost=%.3f bar flow=%.5f liq=%.5f k=%.4g pA=%.3f pB=%.3f spanA=%.3f spanB=%.3f %s",
                net.nodes.indexOf(e.a), net.nodes.indexOf(e.b), e.kind, e.open, e.boost / 1e5, e.flow, e.fLiquid, e.k, e.portA, e.portB,
                e.spanA, e.spanB, e.debug()));
        mods.eln.Eln.LOG.info(b.toString());
    }

    /** Each lumped run's contents back into its members, by volume (they are what is saved and what is rebuilt from). */
    private void writeBack() {
        for (var e : lumps.entrySet()) {
            FluidNode lump = e.getKey();
            for (FluidNode m : e.getValue()) {
                m.setShareOf(lump, m.volume / lump.volume);
                m.update();
                // (the heat through the run's walls, back to each piece's wall by its share of the link)
                m.wallJ = lump.wallUA > 0 ? lump.wallJ * m.wallUA / lump.wallUA : 0;
            }
        }
    }

    private void rebuildIfDirty() {
        if (!dirty) return;
        dirty = false;
        networks.clear();
        vents.clear();
        links.clear();
        lumps.clear();
        lumpOf.clear();
        // ports by (level, block, face)
        Map<Object, Map<Long, List<FluidDevice.FluidPort>>> at = new HashMap<>();
        for (FluidDevice d : devices)
            for (FluidDevice.FluidPort p : d.fluidPorts())
                at.computeIfAbsent(d.fluidLevel(), k -> new HashMap<>()).computeIfAbsent(p.pos(), k -> new ArrayList<>()).add(p);
        // union-find over nodes: device nodes joined by coupled ports
        Map<FluidNode, FluidNode> parent = new IdentityHashMap<>();
        List<FluidNode> all = new ArrayList<>();
        Map<FluidNode, Object> levelOf = new IdentityHashMap<>();
        for (FluidDevice d : devices) {
            for (FluidDevice.FluidPort p : d.fluidPorts()) addNode(p.node(), parent, all, levelOf, d.fluidLevel());
            for (FluidDevice.Vent v : d.fluidVents()) addNode(v.node, parent, all, levelOf, d.fluidLevel());
            for (FluidDevice.Link l : d.fluidLinks()) {
                addNode(l.a, parent, all, levelOf, d.fluidLevel());
                addNode(l.b, parent, all, levelOf, d.fluidLevel());
            }
        }
        List<FluidDevice.Link> allLinks = new ArrayList<>();
        for (FluidDevice d : devices) allLinks.addAll(d.fluidLinks());
        List<Object[]> couplings = new ArrayList<>();   // {portA, portB}
        for (FluidDevice d : devices)
            for (FluidDevice.FluidPort p : d.fluidPorts()) {
                Direction f = Direction.from3DDataValue(p.face());
                long other = BlockPos.of(p.pos()).relative(f).asLong();
                var there = at.getOrDefault(d.fluidLevel(), Map.of()).getOrDefault(other, List.of());
                for (FluidDevice.FluidPort q : there) {
                    if (q.face() != f.getOpposite().get3DDataValue() || q.node() == p.node() || q.at() != p.at()) continue;
                    if (p.pos() > q.pos()) continue;   // (each pair once)
                    couplings.add(new Object[]{p, q});
                    union(p.node(), q.node(), parent);
                }
            }
        // pipe runs: pipe nodes coupled to each other become one node (their volumes, contents and height span)
        Map<FluidNode, FluidNode> runParent = new IdentityHashMap<>();
        for (FluidNode n : all) if (n.lumpable) runParent.put(n, n);
        for (Object[] c : couplings) {
            var p = (FluidDevice.FluidPort) c[0];
            var q = (FluidDevice.FluidPort) c[1];
            if (p.node().lumpable && q.node().lumpable) union(p.node(), q.node(), runParent);
        }
        List<List<FluidNode>> joins = new ArrayList<>();
        for (FluidDevice d : devices) joins.addAll(d.fluidJoins());
        for (List<FluidNode> g : joins)
            for (int i = 1; i < g.size(); i++)
                if (runParent.containsKey(g.get(0)) && runParent.containsKey(g.get(i))) union(g.get(0), g.get(i), runParent);
        Map<FluidNode, List<FluidNode>> runs = new IdentityHashMap<>();
        for (FluidNode n : runParent.keySet()) runs.computeIfAbsent(find(n, runParent), r -> new ArrayList<>()).add(n);
        Map<FluidNode, Double> runR = new IdentityHashMap<>();
        for (List<FluidNode> ms : runs.values()) {
            double V = 0, zLo = Double.MAX_VALUE, zHi = -Double.MAX_VALUE, R = 0;
            for (FluidNode m : ms) {
                V += m.volume;
                zLo = Math.min(zLo, m.z);
                zHi = Math.max(zHi, m.z + m.height);
                R += m.resistance;
            }
            FluidNode lump = new FluidNode(V, zLo, Math.max(0.01, zHi - zLo), false);
            for (FluidNode m : ms) lump.addShareOf(m, 1);   // (every species: the run's contents summed)
            lump.update();
            lumps.put(lump, ms);
            for (FluidNode m : ms) lumpOf.put(m, lump);
            runR.put(lump, R);
        }
        java.util.function.Function<FluidNode, FluidNode> repr = n -> lumpOf.getOrDefault(n, n);
        // the networks: over the representatives (a run's lump stands for its pipes)
        Map<FluidNode, FluidNode> parent2 = new IdentityHashMap<>();
        List<FluidNode> reps = new ArrayList<>();
        for (FluidNode n : all) {
            FluidNode r = repr.apply(n);
            if (!parent2.containsKey(r)) {
                parent2.put(r, r);
                reps.add(r);
            }
        }
        for (Object[] c : couplings) union(repr.apply(((FluidDevice.FluidPort) c[0]).node()), repr.apply(((FluidDevice.FluidPort) c[1]).node()), parent2);
        for (FluidDevice.Link l : allLinks) union(repr.apply(l.a), repr.apply(l.b), parent2);
        parent = parent2;
        all = reps;
        Map<FluidNode, FluidNetwork> byRoot = new IdentityHashMap<>();
        for (FluidNode n : all) {
            FluidNetwork net = byRoot.computeIfAbsent(find(n, parent), r -> {
                FluidNetwork x = new FluidNetwork();
                networks.add(x);
                return x;
            });
            net.add(n);
        }
        for (Object[] c : couplings) {
            var p = (FluidDevice.FluidPort) c[0];
            var q = (FluidDevice.FluidPort) c[1];
            FluidNode a = repr.apply(p.node()), b = repr.apply(q.node());
            if (a == b) continue;   // (inside one run)
            // a run's end: its draw on the lump, and half the run's resistance instead of the piece's own
            double rA = a != p.node() ? runR.get(a) / 2 : 1 / p.k(), rB = b != q.node() ? runR.get(b) / 2 : 1 / q.k();
            double sA = spanOn(a, p.node(), p.span()), sB = spanOn(b, q.node(), q.span());
            double dA = drawOn(a, p.node(), p.draw()), dB = drawOn(b, q.node(), q.draw()), k = 1 / (rA + rB);   // (in series)
            var net = byRoot.get(find(a, parent));
            if ((sA > 0) != (sB > 0)) {
                // an opening (a pipe's open end) on a point port: two one-way edges, so liquid can run out under it while
                // gas comes back over it (Baughn 2026-11-03: counter-flow; one stream per edge stopped a sealed part-full
                // pipe draining). Out of the opening: its wetted mix at the mean pressure over it. Into it: what the
                // other side's port gives: liquid meeting the opening as the outgoing liquid does, gas at the opening's
                // height as a point (FluidEdge.entrySpan). Usually one of the two carries a one-way flow; both run only
                // as liquid goes out and gas comes back. Vents and links stay single edges.
                FluidNode o = sA > 0 ? a : b, d = sA > 0 ? b : a;
                double dO = sA > 0 ? dA : dB, dD = sA > 0 ? dB : dA, span = Math.max(sA, sB);
                net.connect(o, dO, d, dD, k).kind(FluidEdge.Kind.CHECK).spanA = span;
                net.connect(d, dD, o, dO, k).kind(FluidEdge.Kind.CHECK).entrySpan = span;
            } else {
                var edge = net.connect(a, dA, b, dB, k);
                edge.spanA = sA;
                edge.spanB = sB;
            }
        }
        for (FluidDevice.Link l : allLinks) {
            FluidNode a = repr.apply(l.a), b = repr.apply(l.b);
            if (a == b) continue;   // (both sides in one run: the valve can't hold anything back)
            double r = 1 / l.k + (a != l.a ? runR.get(a) / 2 : 0) + (b != l.b ? runR.get(b) / 2 : 0);
            l.edge = byRoot.get(find(a, parent)).connect(a, drawOn(a, l.a, l.drawA), b, drawOn(b, l.b, l.drawB), 1 / r).kind(l.kind);
            l.edge.area = l.area;
            l.edge.open = l.open;
            l.edge.boost = l.boost;
            l.edge.qMax = l.qMax;
            l.edge.h0 = l.h0;
            l.edge.speed = l.speed;
            l.edge.eta = l.eta;
            l.edge.nozzle = l.nozzle;
            l.edge.drop = l.drop;
            l.edge.conductance = l.conductance;
            l.edge.band = l.band;
            links.add(l);
        }
        Map<FluidNetwork, FluidNode> air = new IdentityHashMap<>();
        for (FluidDevice d : devices)
            for (FluidDevice.Vent v : d.fluidVents()) {
                FluidNode vn = repr.apply(v.node);   // (a burst pipe vents its whole run)
                FluidNetwork net = byRoot.get(find(vn, parent));
                FluidNode atm = air.computeIfAbsent(net, k -> k.add(FluidNode.atmosphere(vn.z + vn.height)));
                v.edge = net.connect(vn, drawOn(vn, v.node, v.draw), atm, 0, v.k);
                v.edge.open = v.open;
                vents.add(v);
            }
    }

    /** A port's draw on its node, as a draw on the node standing for it (the same height in the world). */
    private static double drawOn(FluidNode rep, FluidNode n, double draw) {
        if (rep == n) return draw;
        double z = n.z + draw * n.height;
        return Math.max(0, Math.min(1, (z - rep.z) / rep.height));
    }

    /** An opening's height as a share of its run's lump, from its share of its own piece. */
    private static double spanOn(FluidNode rep, FluidNode n, double span) {
        if (rep == n || span <= 0) return span;
        return Math.min(1, span * n.height / rep.height);
    }

    private static void addNode(FluidNode n, Map<FluidNode, FluidNode> parent, List<FluidNode> all, Map<FluidNode, Object> levelOf, Object level) {
        if (parent.containsKey(n)) return;
        parent.put(n, n);
        all.add(n);
        levelOf.put(n, level);
    }

    private static FluidNode find(FluidNode n, Map<FluidNode, FluidNode> parent) {
        FluidNode p = parent.get(n);
        if (p == n) return n;
        FluidNode r = find(p, parent);
        parent.put(n, r);
        return r;
    }

    private static void union(FluidNode a, FluidNode b, Map<FluidNode, FluidNode> parent) {
        FluidNode ra = find(a, parent), rb = find(b, parent);
        if (ra != rb) parent.put(ra, rb);
    }
}
