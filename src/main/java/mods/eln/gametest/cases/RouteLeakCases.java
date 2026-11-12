package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeConnection;
import mods.eln.node.NodeManager;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.CablePlacer;
import mods.eln.node.voxel.CableRouter;
import mods.eln.node.voxel.PipePlacer;
import mods.eln.node.voxel.PipeRouter;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelNodeEntity;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Routes over time (Baughn 2026-11-11: removed and re-added, at the same place or elsewhere, with no leaks): rounds of
 * laying random cable and pipe routes on a pad, breaking random pieces or whole runs, and laying again. Everything the
 * removed blocks put into the simulation (loads, thermal loads, processes, watchdogs, node connections) and into the
 * fluid world (pipe nodes) must be gone from the simulator's and fluid world's collections, no node may stay in the
 * node manager for a removed block, and the nodes still standing may hold no connection to a node that's gone.
 */
public final class RouteLeakCases {
    private RouteLeakCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new LayBreakRelay());
    }

    static final class LayBreakRelay implements TestCase {
        static final int ROUNDS = 14, W = 9;
        static final int[] ISLANDS = {2, 3, 5, 6};
        final Set<Object> gone = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<NodeBase> goneNodes = Collections.newSetFromMap(new IdentityHashMap<>());
        int laid, broken, refused, liveSeen, liveSampled, junctions, airLaid;
        final java.util.TreeMap<String, int[]> coverage = new java.util.TreeMap<>();

        public String id() {
            return "routes_lay_break_relay_leak_nothing";
        }

        public int width() {
            return W;
        }

        public int ticks() {
            return 20;
        }

        VoxelNode node(TestContext c, BlockPos p) {
            return c.level().getBlockEntity(p) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode v ? v : null;
        }

        List<BlockPos> padBlocks(TestContext c) {
            List<BlockPos> out = new ArrayList<>();
            for (int x = 0; x < W; x++)
                for (int y = 1; y < 4; y++)
                    for (int z = 0; z < 3; z++) out.add(c.at(x, y, z));
            return out;
        }

        /** Remove a voxel block's part i the way breaking does; when the block empties, its node goes too. */
        void breakPart(TestContext c, BlockPos p, VoxelNode v, int i) {
            // (what it had before minus what it has after: a rebuild's old loads must go as well as a removed block's)
            Set<Object> before = Collections.newSetFromMap(new IdentityHashMap<>());
            before.addAll(v.simObjectsForTests());
            boolean empty = v.removePart(i, false);
            if (empty) {
                goneNodes.add(v);
                v.removeEmptyBlock();
            } else {
                Set<Object> after = Collections.newSetFromMap(new IdentityHashMap<>());
                after.addAll(v.simObjectsForTests());
                before.removeAll(after);
            }
            gone.addAll(before);
        }

        public void build(TestContext c) {
            Random r = new Random(1112);
            BlockPos o = c.at(0, 0, 0);
            // (routing phase 2: two islands floating a block over the floor, a block apart, for cables across the air)
            for (int x : ISLANDS) c.floor(c.at(x, 2, 1));
            for (int round = 0; round < ROUNDS; round++) {
                // lay one or two routes
                for (int k = 0; k < 1 + r.nextInt(2); k++) {
                    int ax = r.nextInt(W), bx = r.nextInt(W), az = r.nextInt(3), bz = r.nextInt(3);
                    String err;
                    if (r.nextBoolean()) {
                        int tier = CablePart.tierOf("Low Voltage Cable");
                        // (on the floor's top, off the block edges, on the cable lattice)
                        var a = new CableRouter.End((o.getX() + ax) * 16L + 2 + 2 * r.nextInt(7), (o.getY() + 1) * 16L, (o.getZ() + az) * 16L + 2 + 2 * r.nextInt(7), 0, -1);
                        var b = new CableRouter.End((o.getX() + bx) * 16L + 2 + 2 * r.nextInt(7), (o.getY() + 1) * 16L, (o.getZ() + bz) * 16L + 2 + 2 * r.nextInt(7), 0, -1);
                        err = CablePlacer.routeAndLayForTests(c.level(), tier, a, b);
                    } else {
                        var a = new PipeRouter.End((o.getX() + ax) * 16L + 8, (o.getY() + 1) * 16L + 8, (o.getZ() + az) * 16L + 8, -1);
                        var b = new PipeRouter.End((o.getX() + bx) * 16L + 8, (o.getY() + 1 + r.nextInt(2)) * 16L + 8, (o.getZ() + bz) * 16L + 8, -1);
                        err = PipePlacer.routeForTests(c.level(), r.nextInt(2), a, b);
                    }
                    if (err == null) laid++;
                    else refused++;
                }
                // a cable across the gap between the islands (its piece over the gap hangs in the air)
                if (r.nextBoolean()) {
                    int tier = CablePart.tierOf("Low Voltage Cable");
                    int ax = ISLANDS[r.nextInt(2)], bx = ISLANDS[2 + r.nextInt(2)];
                    var a = new CableRouter.End((o.getX() + ax) * 16L + 2 + 2 * r.nextInt(7), (o.getY() + 3) * 16L, (o.getZ() + 1) * 16L + 4 + 2 * r.nextInt(5), 0, -1);
                    var b = new CableRouter.End((o.getX() + bx) * 16L + 2 + 2 * r.nextInt(7), (o.getY() + 3) * 16L, (o.getZ() + 1) * 16L + 4 + 2 * r.nextInt(5), 0, -1);
                    String err = CablePlacer.routeAndLayForTests(c.level(), tier, a, b);
                    if (err == null) {
                        laid++;
                        VoxelNode gap = node(c, c.at(4, 3, 1));
                        if (gap != null) for (var part : gap.parts) if (part instanceof CablePart cp && !CablePlacer.supported(c.level(), c.at(4, 3, 1), cp)) airLaid++;
                    } else refused++;
                }
                // a branch onto a pipe standing on the pad: up from a point of its centre line to the block's top (a junction)
                for (BlockPos p : padBlocks(c)) {
                    VoxelNode v = node(c, p);
                    if (v == null || r.nextInt(4) != 0) continue;
                    for (var part : new ArrayList<>(v.parts)) {
                        if (!(part instanceof mods.eln.node.voxel.PipePart pipe) || pipe.points.length < 6) continue;
                        int[] q = pipe.points;
                        int mx = (q[0] + q[3]) / 2, my = (q[1] + q[4]) / 2, mz = (q[2] + q[5]) / 2;
                        if (mx % 4 != 0 || my % 4 != 0 || mz % 4 != 0 || my >= 12) continue;
                        var up = new mods.eln.node.voxel.PipePart(pipe.tier, new int[]{mx, my, mz, mx, 16, mz});
                        if (v.joinError(up, mx, my, mz) != null) continue;
                        if (v.addJoined(up, mx, my, mz) == null) junctions++;
                        break;
                    }
                }
                // break: some random pieces, or (every few rounds) everything
                boolean all = round % 4 == 3;   // (the last rounds leave blocks standing, for the census check)
                for (BlockPos p : padBlocks(c)) {
                    VoxelNode v = node(c, p);
                    if (v == null) continue;
                    for (int i = v.parts.size() - 1; i >= 0; i--) {
                        if (!all && r.nextInt(3) != 0) continue;
                        breakPart(c, p, v, i);
                        broken++;
                        if (node(c, p) == null) break;
                    }
                }
            }
        }

        /** Every object reachable from the simulator's and the fluid world's collections (through the sim's own
         *  containers, a few levels down). */
        static Set<Object> reachable() {
            Set<Object> out = Collections.newSetFromMap(new IdentityHashMap<>());
            walk(Eln.simulator, out, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
            walk(mods.eln.fluid.FluidWorld.INSTANCE, out, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
            return out;
        }

        static void walk(Object holder, Set<Object> out, int depth, Set<Object> seen) {
            if (holder == null || depth > 4 || !seen.add(holder)) return;
            for (Class<?> k = holder.getClass(); k != null && k != Object.class; k = k.getSuperclass())
                for (Field f : k.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
                    Object v;
                    try {
                        f.setAccessible(true);
                        v = f.get(holder);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        continue;
                    }
                    collect(v, out, depth, seen);
                }
        }

        static void collect(Object v, Set<Object> out, int depth, Set<Object> seen) {
            if (v == null) return;
            if (v instanceof Collection<?> col) for (Object e : new ArrayList<>(col)) element(e, out, depth, seen);
            else if (v instanceof Map<?, ?> m) for (var e : new ArrayList<>(m.entrySet())) {
                element(e.getKey(), out, depth, seen);
                element(e.getValue(), out, depth, seen);
            } else if (v.getClass().isArray() && !v.getClass().getComponentType().isPrimitive())
                for (int i = 0; i < Array.getLength(v); i++) element(Array.get(v, i), out, depth, seen);
            else if (v.getClass().getName().startsWith("mods.eln.sim.mna") || v.getClass().getName().startsWith("mods.eln.fluid"))
                walk(v, out, depth + 1, seen);
        }

        static void element(Object e, Set<Object> out, int depth, Set<Object> seen) {
            if (e == null) return;
            out.add(e);
            if (e instanceof Collection<?> || e instanceof Map<?, ?>) collect(e, out, depth + 1, seen);
            else if (e.getClass().getName().startsWith("mods.eln.sim.mna") || e.getClass().getName().startsWith("mods.eln.fluid")) walk(e, out, depth + 1, seen);
        }

        public void measure(TestContext c) {
            Set<Object> live = reachable();
            // (the census sees what the standing blocks hold, after the fluid world rebuilt: the check isn't vacuous)
            for (BlockPos p : padBlocks(c)) {
                VoxelNode v = node(c, p);
                if (v == null) continue;
                for (Object x : v.simObjectsForTests()) {
                    if (x instanceof NodeConnection) continue;
                    liveSampled++;
                    int[] cv = coverage.computeIfAbsent(x.getClass().getSimpleName().replaceAll("\\$\\$Lambda.*", "\\$\\$Lambda"), k -> new int[2]);
                    cv[1]++;
                    if (live.contains(x)) {
                        liveSeen++;
                        cv[0]++;
                    }
                }
            }
            int leaked = 0;
            StringBuilder what = new StringBuilder();
            for (Object g : gone)
                if (live.contains(g)) {
                    leaked++;
                    if (what.length() < 300) what.append(g.getClass().getSimpleName()).append(' ');
                }
            int stayed = 0;
            List<NodeBase> nodes = new ArrayList<>(NodeManager.instance.getNodes());
            Set<NodeBase> present = Collections.newSetFromMap(new IdentityHashMap<>());
            present.addAll(nodes);
            for (NodeBase n : goneNodes) if (present.contains(n)) stayed++;
            int stale = 0;
            for (BlockPos p : padBlocks(c)) {
                VoxelNode v = node(c, p);
                if (v == null) continue;
                for (NodeConnection nc : new ArrayList<>(v.nodeConnectionList))
                    if (nc.other(v) != null && !present.contains(nc.other(v))) stale++;
            }
            c.line("lay/break/relay: " + laid + " routes laid, " + refused + " refused, " + broken + " pieces broken, " + goneNodes.size() + " blocks gone, "
                + gone.size() + " objects to be gone");
            c.check("lay/break/relay: routes were laid and blocks went", laid >= ROUNDS && goneNodes.size() >= 20, laid + " laid, " + goneNodes.size() + " gone");
            c.line("lay/break/relay: " + junctions + " pipe junctions made, " + airLaid + " cable pieces laid over the islands' gap");
            c.check("lay/break/relay: junctions and air runs came and went too (routing phase 2)", junctions > 0 && airLaid > 0, junctions + " junctions, " + airLaid + " air pieces");
            StringBuilder cov = new StringBuilder();
            for (var e : coverage.entrySet()) cov.append(e.getKey()).append(' ').append(e.getValue()[0]).append('/').append(e.getValue()[1]).append(", ");
            c.line("lay/break/relay: census coverage of standing blocks (seen/held): " + cov);
            c.check("lay/break/relay: the census finds standing blocks' sim objects (not vacuous)", liveSeen > 0, liveSeen + " of " + liveSampled);
            c.check("lay/break/relay: nothing of a removed block stays in the simulation or fluid world", leaked == 0, leaked + " left: " + what);
            c.check("lay/break/relay: no removed block's node stays in the node manager", stayed == 0, stayed + " stayed");
            c.check("lay/break/relay: no standing node keeps a connection to a removed one", stale == 0, stale + " stale");
        }
    }
}
