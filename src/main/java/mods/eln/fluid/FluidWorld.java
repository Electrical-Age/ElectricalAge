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
            double share = m.volume / lump.volume;
            m.mWater = lump.mWater * share;
            m.mAir = lump.mAir * share;
            m.energy = lump.energy * share;
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

    @Override
    public void process(double time) {
        rebuildIfDirty();
        for (FluidDevice d : devices) d.beforeFluidStep(time);
        for (FluidDevice.Vent v : vents) v.edge.open = v.open;
        for (FluidDevice.Link l : links) l.edge.open = l.open;
        for (FluidNetwork n : networks) n.step(time);
        writeBack();
        burstThisStep.clear();
        for (FluidDevice d : devices) d.afterFluidStep(time);
    }

    /** Each lumped run's contents back into its members, by volume (they are what is saved and what is rebuilt from). */
    private void writeBack() {
        for (var e : lumps.entrySet()) {
            FluidNode lump = e.getKey();
            for (FluidNode m : e.getValue()) {
                double share = m.volume / lump.volume;
                m.mWater = lump.mWater * share;
                m.mAir = lump.mAir * share;
                m.energy = lump.energy * share;
                m.update();
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
            double V = 0, w = 0, a = 0, u = 0, zLo = Double.MAX_VALUE, zHi = -Double.MAX_VALUE, R = 0;
            for (FluidNode m : ms) {
                V += m.volume;
                w += m.mWater;
                a += m.mAir;
                u += m.energy;
                zLo = Math.min(zLo, m.z);
                zHi = Math.max(zHi, m.z + m.height);
                R += m.resistance;
            }
            FluidNode lump = new FluidNode(V, zLo, Math.max(0.01, zHi - zLo), false);
            lump.mWater = w;
            lump.mAir = a;
            lump.energy = u;
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
            byRoot.get(find(a, parent)).connect(a, drawOn(a, p.node(), p.draw()), b, drawOn(b, q.node(), q.draw()), 1 / (rA + rB));   // (in series)
        }
        for (FluidDevice.Link l : allLinks) {
            FluidNode a = repr.apply(l.a), b = repr.apply(l.b);
            if (a == b) continue;   // (both sides in one run: the valve can't hold anything back)
            double r = 1 / l.k + (a != l.a ? runR.get(a) / 2 : 0) + (b != l.b ? runR.get(b) / 2 : 0);
            l.edge = byRoot.get(find(a, parent)).connect(a, drawOn(a, l.a, l.drawA), b, drawOn(b, l.b, l.drawB), 1 / r);
            l.edge.open = l.open;
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
