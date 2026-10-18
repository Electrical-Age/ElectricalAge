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
 * slow processes. Two ports couple when they sit on neighbouring blocks, open on the faces that touch, in the same
 * level: a boiler set on a turbine feeds it. Devices join and leave with their EA node (connectJob / disconnectJob), so
 * a node Stasis keeps frozen is out of its network; the networks are rebuilt when that set changes.
 */
public final class FluidWorld implements IProcess {
    public static final FluidWorld INSTANCE = new FluidWorld();

    private final Set<FluidDevice> devices = new LinkedHashSet<>();
    private final List<FluidNetwork> networks = new ArrayList<>();
    private final List<FluidDevice.Vent> vents = new ArrayList<>();
    private boolean dirty;

    private FluidWorld() {
    }

    public static void add(FluidDevice d) {
        if (INSTANCE.devices.add(d)) INSTANCE.dirty = true;
    }

    public static void remove(FluidDevice d) {
        if (INSTANCE.devices.remove(d)) INSTANCE.dirty = true;
    }

    /** Server start and stop: forget everything (the simulator's process list is cleared too). */
    public static FluidWorld reset() {
        INSTANCE.devices.clear();
        INSTANCE.networks.clear();
        INSTANCE.vents.clear();
        INSTANCE.dirty = false;
        return INSTANCE;
    }

    /** For tests and meters: the network a node is in (null if none). */
    public static FluidNetwork networkOf(FluidNode n) {
        INSTANCE.rebuildIfDirty();
        for (FluidNetwork net : INSTANCE.networks) if (net.nodes.contains(n)) return net;
        return null;
    }

    /** The edges at a node this step (its ports' couplings and vents). */
    public static List<FluidEdge> edgesOf(FluidNode n) {
        List<FluidEdge> out = new ArrayList<>();
        FluidNetwork net = networkOf(n);
        if (net != null) for (FluidEdge e : net.edges) if (e.a == n || e.b == n) out.add(e);
        return out;
    }

    @Override
    public void process(double time) {
        rebuildIfDirty();
        for (FluidDevice d : devices) d.beforeFluidStep(time);
        for (FluidDevice.Vent v : vents) v.edge.open = v.open;
        for (FluidNetwork n : networks) n.step(time);
        for (FluidDevice d : devices) d.afterFluidStep(time);
    }

    private void rebuildIfDirty() {
        if (!dirty) return;
        dirty = false;
        networks.clear();
        vents.clear();
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
        }
        List<Object[]> couplings = new ArrayList<>();   // {portA, portB}
        for (FluidDevice d : devices)
            for (FluidDevice.FluidPort p : d.fluidPorts()) {
                Direction f = Direction.from3DDataValue(p.face());
                long other = BlockPos.of(p.pos()).relative(f).asLong();
                var there = at.getOrDefault(d.fluidLevel(), Map.of()).getOrDefault(other, List.of());
                for (FluidDevice.FluidPort q : there) {
                    if (q.face() != f.getOpposite().get3DDataValue() || q.node() == p.node()) continue;
                    if (p.pos() > q.pos()) continue;   // (each pair once)
                    couplings.add(new Object[]{p, q});
                    union(p.node(), q.node(), parent);
                }
            }
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
            byRoot.get(find(p.node(), parent)).connect(p.node(), p.draw(), q.node(), q.draw(), Math.min(p.k(), q.k()));
        }
        Map<FluidNetwork, FluidNode> air = new IdentityHashMap<>();
        for (FluidDevice d : devices)
            for (FluidDevice.Vent v : d.fluidVents()) {
                FluidNetwork net = byRoot.get(find(v.node, parent));
                FluidNode atm = air.computeIfAbsent(net, k -> k.add(FluidNode.atmosphere(v.node.z + v.node.height)));
                v.edge = net.connect(v.node, v.draw, atm, 0, v.k);
                v.edge.open = v.open;
                vents.add(v);
            }
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
