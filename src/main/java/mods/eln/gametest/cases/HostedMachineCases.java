package mods.eln.gametest.cases;

import mods.eln.api.ElnApi;
import mods.eln.api.Machine;
import mods.eln.api.MachineSpec;
import mods.eln.api.MachineState;
import mods.eln.api.MachineView;
import mods.eln.api.RelativeFace;
import mods.eln.api.impl.ElementHost;
import mods.eln.api.impl.HostedMachines;
import mods.eln.api.impl.MachineNode;
import mods.eln.apitest.ApiTestMachineEntity;
import mods.eln.apitest.ApiTestMachines;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeConnection;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Machines at other mods' block entities (ElnApi.machines()), with the hidden eln:api_test_machine, written on the API
 * alone: on a real 50 V supply through an LV cable it draws through its back and works; turning it moves its power
 * face; its LIT toggling keeps its node, links and energy; breaking it takes the node away and its handle reads
 * DETACHED; its block entity saved, unloaded and loaded again gets its state back and its cable relinked; two in a
 * line with a cable between don't pass power through each other; its bus face links to a voxel bus cable; the
 * eln:machine capability answers for it and for an EA element machine; calls off the server thread throw; a spec
 * that burns calls its burn handler once and stays tripped, and one that burns without a handler is refused.
 */
public final class HostedMachineCases {
    private HostedMachineCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Works());
        cases.add(new Turns());
        cases.add(new KeepsThroughLit());
        cases.add(new Breaks());
        cases.add(new Reloads());
        cases.add(new Leaves());
        cases.add(new Bus());
        cases.add(new Burns());
    }

    /** Sets source's volts. */
    static void volts(TestContext c, ElectricalSourceElement source, float volts) {
        source.networkUnserialize(c.stream(out -> {
            out.writeByte(ElectricalSourceElement.setVoltageId);
            out.writeFloat(volts);
        }));
    }

    /** One tick's rated work energy of the fixture, J (100 W, no idle). */
    static final double TICK_J = 100 * 0.05;

    /** The fixture at x (y 1, z 1) facing facing. */
    static BlockPos machine(TestContext c, int x, Direction facing) {
        BlockPos p = c.at(x, 1, 1);
        c.level().setBlock(p, ApiTestMachines.BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, facing), 3);
        return p;
    }

    static ApiTestMachineEntity entity(TestContext c, BlockPos p) {
        return c.level().getBlockEntity(p) instanceof ApiTestMachineEntity e ? e : null;
    }

    static NodeBase nodeAt(TestContext c, BlockPos p) {
        return NodeManager.instance.getNodeFromCoordonate(new Coordonate(p, c.level()));
    }

    /** An LV cable at x on the floor. */
    static SixNodeElement cable(TestContext c, int x) {
        return c.placeSix(Wp10aCases.LV_CABLE, c.at(x, 1, 1));
    }

    /** A 50 V source at x on the floor. */
    static void source(TestContext c, int x) {
        ElectricalSourceElement src = (ElectricalSourceElement) c.placeSix(Wp10aCases.SOURCE, c.at(x, 1, 1));
        src.networkUnserialize(c.stream(out -> {
            out.writeByte(ElectricalSourceElement.setVoltageId);
            out.writeFloat(50f);
        }));
    }

    /** The NodeConnections between a and b (each sits in both nodes' lists). */
    static int links(NodeBase a, NodeBase b) {
        if (a == null || b == null) return 0;
        int n = 0;
        for (NodeConnection k : a.nodeConnectionList) if (b.nodeConnectionList.contains(k)) n++;
        return n;
    }

    static boolean linked(NodeBase a, NodeBase b) {
        return links(a, b) > 0;
    }

    /** Source at 1, cable at 2, the fixture at 3 facing east (its back, a power face, toward the cable). */
    abstract static class Fed implements TestCase {
        BlockPos pos;
        SixNodeElement cable;

        public void build(TestContext c) {
            source(c, 1);
            cable = cable(c, 2);
            pos = machine(c, 3, Direction.EAST);
        }

        Machine m(TestContext c) {
            return ElnApi.machines().of(entity(c, pos));
        }
    }

    /** It draws through its back on a real supply and works at its rated power; the capability and the rules hold. */
    static final class Works extends Fed {
        double w0, w1;
        BlockPos furnace, macerator;

        public String id() {
            return "api_hosted_machine_works";
        }

        public int ticks() {
            return 60;
        }

        @Override
        public void build(TestContext c) {
            super.build(c);
            furnace = c.at(0, 1, 2);
            c.level().setBlock(furnace, Blocks.FURNACE.defaultBlockState(), 3);
            macerator = c.placeTransparent(Wp10aCases.MACHINES[0], c.at(6, 1, 1)).node.coordonate.pos();
            c.after(40, () -> w0 = entity(c, pos).workJoules());
        }

        public void measure(TestContext c) {
            ApiTestMachineEntity e = entity(c, pos);
            Machine m = ElnApi.machines().of(e);
            w1 = e.workJoules();
            c.check("hosted: it runs on a 50 V line", m.state() == MachineState.RUNNING, m.state() + " at " + m.volts() + " V");
            c.checkValue("hosted: 20 ticks of work at its rated 100 W [J]", w1 - w0, 20 * TICK_J);
            c.check("hosted: the line at the machine is near 50 V", Math.abs(m.volts() - 50) < 1, m.volts() + " V");
            MachineNode n = HostedMachines.INSTANCE.node(e);
            c.check("hosted: a node of its own at the block, linked to the cable", n != null && nodeAt(c, pos) == n
                && linked(n, nodeAt(c, c.at(2, 1, 1))), String.valueOf(n));
            c.check("hosted: it draws through its back", n != null && n.activeFace() == RelativeFace.BACK, n == null ? "-" : String.valueOf(n.activeFace()));
            c.check("hosted: it shows LIT while it works", c.level().getBlockState(pos).getValue(BlockStateProperties.LIT), "");
            c.check("hosted: of() is the same handle each time", m == ElnApi.machines().of(e), "");
            MachineView v = c.level().getCapability(ElnApi.MACHINE, pos, null);
            MachineView vSide = c.level().getCapability(ElnApi.MACHINE, pos, Direction.UP);
            c.check("capability: eln:machine at the block is its view", v != null && vSide != null && v.state() == m.state()
                && v.volts() == m.volts() && !(v instanceof Machine), String.valueOf(v));
            MachineView ea = c.level().getCapability(ElnApi.MACHINE, macerator, null);
            c.check("capability: eln:machine at an EA element machine (a macerator, unfed)", ea != null && ea.state() == MachineState.UNPOWERED,
                ea == null ? "null" : ea.state().name());
            c.check("capability: none at a block that has no machine", c.level().getCapability(ElnApi.MACHINE, furnace, null) == null, "");
            // the rules
            AtomicReference<Throwable> off = new AtomicReference<>(), offOf = new AtomicReference<>();
            Thread t = new Thread(() -> {
                try {
                    m.takeJoules(1);
                } catch (Throwable x) {
                    off.set(x);
                }
                try {
                    ElnApi.machines().of(e);
                } catch (Throwable x) {
                    offOf.set(x);
                }
            }, "eln-api-test-other-thread");
            t.start();
            try {
                t.join(5000);
            } catch (InterruptedException x) {
                Thread.currentThread().interrupt();
            }
            c.check("rules: a take off the server thread throws", off.get() instanceof IllegalStateException, String.valueOf(off.get()));
            c.check("rules: of() off the server thread throws", offOf.get() instanceof IllegalStateException, String.valueOf(offOf.get()));
            Machine stray = ElnApi.machines().of(c.level().getBlockEntity(furnace));
            c.check("rules: a type never registered gets an inert DETACHED machine", stray.state() == MachineState.DETACHED
                && stray.takeJoules(1) == 0 && !ElnApi.machines().registered(c.level().getBlockEntity(furnace).getType()), stray.toString());
            var lines = mods.eln.api.impl.MachineLines.at(c.level(), pos);
            c.check("jade: the machine's lines (voltage, power, state, buffer), by the capability", lines.size() == 4
                && "running".equals(lines.get("Machine")), lines.toString());
            c.check("jade: none for EA's element machine (its node's lines have them) nor for a plain furnace",
                mods.eln.api.impl.MachineLines.at(c.level(), macerator).isEmpty()
                    && mods.eln.api.impl.MachineLines.at(c.level(), furnace).isEmpty(), "");
            Throwable late = null;
            try {
                ElnApi.machines().register(ApiTestMachines.TYPE.get(), be -> ApiTestMachines.SPEC);
            } catch (Throwable x) {
                late = x;
            }
            c.check("rules: registering after capabilities are registered throws", late instanceof IllegalStateException
                && late.getMessage().contains("FMLCommonSetupEvent"), String.valueOf(late));
        }
    }

    /** Turned north its power faces leave the cable (it lands on the bus face); turned west its front takes it. */
    static final class Turns extends Fed {
        MachineNode n0;
        MachineState north;
        RelativeFace northFace;
        boolean sameNode;

        public String id() {
            return "api_hosted_machine_turns";
        }

        public int ticks() {
            return 90;
        }

        @Override
        public void build(TestContext c) {
            super.build(c);
            c.after(30, () -> {
                n0 = HostedMachines.INSTANCE.node(entity(c, pos));
                BlockState s = c.level().getBlockState(pos);
                c.level().setBlock(pos, s.setValue(BlockStateProperties.FACING, Direction.NORTH), 3);
            });
            c.after(50, () -> {
                MachineNode n = HostedMachines.INSTANCE.node(entity(c, pos));
                sameNode = n == n0 && n != null;
                north = m(c).state();
                northFace = n == null ? null : n.activeFace();
                BlockState s = c.level().getBlockState(pos);
                c.level().setBlock(pos, s.setValue(BlockStateProperties.FACING, Direction.WEST), 3);
            });
        }

        public void measure(TestContext c) {
            c.check("turned: the same node follows the block", sameNode, String.valueOf(n0));
            c.check("turned north: no power face on the cable, unpowered", north == MachineState.UNPOWERED && northFace == null,
                north + " " + northFace);
            MachineNode n = HostedMachines.INSTANCE.node(entity(c, pos));
            c.check("turned west: its front takes the cable and it runs", m(c).state() == MachineState.RUNNING
                && n != null && n.activeFace() == RelativeFace.FRONT, m(c).state() + " " + (n == null ? "-" : n.activeFace()));
        }
    }

    /**
     * LIT toggled (by the test, and by the machine itself as it works) changes nothing of the machine: the same node,
     * not reconnected, still linked to the cable through its back, running, and working every tick. (The EA cable
     * beside it reconnects on every block update next to it, EA's neighbour rule; that relinks, it doesn't reset.)
     */
    static final class KeepsThroughLit extends Fed {
        MachineNode n0;
        int connects0;
        double delivered0, work0;
        final List<String> bad = new ArrayList<>();

        public String id() {
            return "api_hosted_machine_keeps_through_lit";
        }

        public int ticks() {
            return 50;
        }

        @Override
        public void build(TestContext c) {
            super.build(c);
            c.after(40, () -> {
                n0 = HostedMachines.INSTANCE.node(entity(c, pos));
                connects0 = n0 == null ? -1 : n0.connects();
                delivered0 = m(c).joulesDelivered();
                work0 = entity(c, pos).workJoules();
            });
            for (int k = 0; k < 6; k++) {
                int tick = 40 + k;
                c.after(tick, () -> {
                    BlockState s = c.level().getBlockState(pos);
                    c.level().setBlock(pos, s.setValue(BlockStateProperties.LIT, !s.getValue(BlockStateProperties.LIT)), 3);
                });
                c.after(tick + 1, () -> {
                    MachineNode n = HostedMachines.INSTANCE.node(entity(c, pos));
                    Machine m = m(c);
                    double work = entity(c, pos).workJoules();
                    if (n != n0) bad.add(tick + ": a new node");
                    else if (n.connects() != connects0) bad.add(tick + ": the node reconnected");
                    else if (!linked(n, nodeAt(c, c.at(2, 1, 1))) || n.activeFace() != RelativeFace.BACK) bad.add(tick + ": unlinked");
                    if (m.state() != MachineState.RUNNING) bad.add(tick + ": " + m.state());
                    if (m.joulesDelivered() < delivered0) bad.add(tick + ": the books restarted");
                    if (Math.abs(work - work0 - TICK_J) > 0.05 * TICK_J) bad.add(tick + ": worked " + (work - work0) + " J");
                    work0 = work;
                });
            }
        }

        public void measure(TestContext c) {
            c.check("LIT: toggling it keeps the node, its links, its state and its energy", n0 != null && bad.isEmpty(), String.join("; ", bad));
        }
    }

    /** Broken: the node goes, the cable is unlinked, and the handle the owner kept reads DETACHED. */
    static final class Breaks extends Fed {
        Machine kept;
        int cableLinks, toMachine;

        public String id() {
            return "api_hosted_machine_breaks";
        }

        public int ticks() {
            return 45;
        }

        @Override
        public void build(TestContext c) {
            super.build(c);
            c.after(40, () -> {
                kept = m(c);
                NodeBase cn = nodeAt(c, c.at(2, 1, 1));
                cableLinks = cn.nodeConnectionList.size();
                toMachine = links(cn, nodeAt(c, pos));
                c.level().destroyBlock(pos, false);
            });
        }

        public void measure(TestContext c) {
            int now = nodeAt(c, c.at(2, 1, 1)).nodeConnectionList.size();
            c.check("broken: no node at the block", nodeAt(c, pos) == null, String.valueOf(nodeAt(c, pos)));
            c.check("broken: the cable lost its links to the machine, only those", toMachine > 0 && now == cableLinks - toMachine,
                cableLinks + " - " + toMachine + " -> " + now);
            c.check("broken: the kept handle reads DETACHED and gives nothing", kept != null && kept.state() == MachineState.DETACHED
                && kept.takeJoules(1) == 0 && !kept.tryTakeJoules(1), String.valueOf(kept));
        }
    }

    /**
     * Saved, unloaded (its block entity gone, the block and the cable stay: as a chunk unload leaves EA's cables) and
     * loaded from the save: its state comes back from the attachment (it runs at once, booking its buffer) and the cable
     * that stayed links to the new node.
     */
    static final class Reloads extends Fed {
        double buffer;
        CompoundTag saved;
        Machine old;
        boolean goneBetween, cableFreeBetween;
        int cableLinks, toMachine;
        BlockEntity fresh;
        MachineState first;
        double firstDelivered;
        mods.eln.sim.IProcess saver;

        public String id() {
            return "api_hosted_machine_reloads";
        }

        public int ticks() {
            return 50;
        }

        @Override
        public void build(TestContext c) {
            super.build(c);
            // saved right after a step, before the owner takes: a buffer to carry (a slow process added after the
            // machine's, so it runs after the machine books the step)
            c.after(39, () -> mods.eln.Eln.simulator.addSlowProcess(saver = dt -> {
                if (saved != null) return;
                ApiTestMachineEntity e = entity(c, pos);
                buffer = ElnApi.machines().of(e).bufferJoules();
                saved = e.saveWithFullMetadata(c.level().registryAccess());
            }));
            c.after(40, () -> {
                mods.eln.Eln.simulator.removeSlowProcess(saver);
                old = m(c);
                NodeBase cn = nodeAt(c, c.at(2, 1, 1));
                cableLinks = cn.nodeConnectionList.size();
                toMachine = links(cn, nodeAt(c, pos));
                c.level().removeBlockEntity(pos);
            });
            c.after(42, () -> {
                goneBetween = nodeAt(c, pos) == null && old.state() == MachineState.DETACHED;
                cableFreeBetween = toMachine > 0 && nodeAt(c, c.at(2, 1, 1)).nodeConnectionList.size() == cableLinks - toMachine;
                fresh = BlockEntity.loadStatic(pos, c.level().getBlockState(pos), saved, c.level().registryAccess());
                c.level().setBlockEntity(fresh);
            });
            c.after(43, () -> {
                Machine m = ElnApi.machines().of(fresh);
                first = m.state();
                firstDelivered = m.joulesDelivered();
            });
        }

        public void measure(TestContext c) {
            String key = mods.eln.api.impl.ElementHost.NBT_PREFIX + "bufferJ";
            CompoundTag att = saved.getCompoundOrEmpty("neoforge:attachments").getCompoundOrEmpty("eln:machine").getCompoundOrEmpty("state");
            c.check("reload: the save holds the machine's buffer in its eln:machine attachment", buffer > 0
                && Math.abs(att.getDoubleOr(key, -1) - buffer) < 1e-9, buffer + " vs " + att);
            c.check("reload: unloaded, the node goes and the kept handle reads DETACHED", goneBetween, "");
            c.check("reload: unloaded, the cable that stays links to nothing there", cableFreeBetween, "");
            MachineNode n = fresh == null ? null : HostedMachines.INSTANCE.node(fresh);
            c.check("reload: a new node for the loaded block entity, the cable linked to it", n != null && nodeAt(c, pos) == n
                && linked(n, nodeAt(c, c.at(2, 1, 1))), String.valueOf(n));
            c.check("reload: saved running, it runs at once (no half-second start)", first == MachineState.RUNNING, String.valueOf(first));
            c.check("reload: its restored buffer is booked as delivered", firstDelivered >= buffer - 1e-9, firstDelivered + " >= " + buffer);
            c.check("reload: it works on", entity(c, pos) == fresh && ((ApiTestMachineEntity) fresh).workJoules() > 0, "");
        }
    }

    /**
     * Source, cable A, M1 (facing east: back on A, front on B), cable B, M2 (back on B): M1 runs, but no power passes
     * through it, so cable B and M2 stay dead though B is linked to M1's front.
     */
    static final class Leaves implements TestCase {
        BlockPos m1, m2;
        SixNodeElement b;

        public String id() {
            return "api_hosted_machines_are_leaves";
        }

        public int ticks() {
            return 60;
        }

        public void build(TestContext c) {
            source(c, 0);
            cable(c, 1);
            m1 = machine(c, 2, Direction.EAST);
            b = cable(c, 3);
            m2 = machine(c, 4, Direction.EAST);
        }

        public void measure(TestContext c) {
            Machine a = ElnApi.machines().of(entity(c, m1)), z = ElnApi.machines().of(entity(c, m2));
            MachineNode na = HostedMachines.INSTANCE.node(entity(c, m1));
            c.check("leaves: the first machine runs", a.state() == MachineState.RUNNING, a.state().name());
            c.check("leaves: cable B is linked to the first machine's front", linked(na, nodeAt(c, c.at(3, 1, 1))), "");
            double ub = Wp10aCases.loadU(b);
            c.check("leaves: no power through it: cable B is dead", Math.abs(ub) < 0.01, ub + " V");
            c.check("leaves: the second machine is unpowered", z.state() == MachineState.UNPOWERED && z.volts() < 0.01, z.state() + " " + z.volts());
        }
    }

    /** The fixture facing east has its bus on the right (north): a voxel bus cable there carries its busy level. */
    static final class Bus extends Fed {
        public String id() {
            return "api_hosted_machine_bus";
        }

        public int ticks() {
            return 50;
        }

        @Override
        public void build(TestContext c) {
            CableCases.voxel(c, c.at(3, 1, 0), CablePart.arms(CablePart.BUS, 0, 2, 3));   // floor, north-south
            super.build(c);
        }

        public void measure(TestContext c) {
            VoxelNode v = (VoxelNode) nodeAt(c, c.at(3, 1, 0));
            var ports = v.busPorts();
            double level = ports.isEmpty() ? -1 : ports.get(0).read(ApiTestMachines.BUSY_CHANNEL);
            c.check("bus: the machine is running", m(c).state() == MachineState.RUNNING, m(c).state().name());
            c.check("bus: the bus cable beside its bus face reads its busy channel high", level == 1, String.valueOf(level));
            c.check("bus: a bus link, not a circuit link", !linked(v, HostedMachines.INSTANCE.node(entity(c, pos))), "");
        }
    }

    /**
     * Source, MV cable (an LV cable would burn at 80 V), the fixture with BURNING_SPEC: 80 V on it burns it, its
     * handler is called once with the trip and leaves the block, and back at 50 V it stays tripped past the cooldown,
     * also saved and loaded again (and its handler isn't called again).
     * Beside it, a fixture whose spec burns without a handler is refused as it is hosted (and removed at once, before
     * its own ticker asks for its machine).
     */
    static final class Burns implements TestCase {
        BlockPos pos;
        ElectricalSourceElement src;
        MachineState before;
        double workAtBurn = -1, workBeforeReload = -2;
        Throwable refused;
        List<mods.eln.api.TripCause> causesBeforeReload = List.of();

        public String id() {
            return "api_hosted_machine_burn_handler";
        }

        public int ticks() {
            return 160;
        }

        public void build(TestContext c) {
            src = (ElectricalSourceElement) c.placeSix(Wp10aCases.SOURCE, c.at(1, 1, 1));
            volts(c, src, 50);
            c.placeSix(MachineApiCases.MV_CABLE, c.at(2, 1, 1));
            pos = machine(c, 3, Direction.EAST);
            entity(c, pos).useSpec(ApiTestMachines.BURNING_SPEC);
            BlockPos q = machine(c, 6, Direction.EAST);
            entity(c, q).useSpec(MachineSpec.consumer(50).ratedWatts(100).burnsOnOvervoltage().build());
            try {
                ElnApi.machines().of(entity(c, q));
            } catch (Throwable x) {
                refused = x;
            }
            c.level().removeBlock(q, false);
            c.after(30, () -> {
                before = ElnApi.machines().of(entity(c, pos)).state();
                volts(c, src, 80);
            });
            c.after(40, () -> workAtBurn = entity(c, pos).workJoules());
            c.after(60, () -> volts(c, src, 50));
            c.after(70, () -> {   // saved burnt, unloaded and loaded again
                ApiTestMachineEntity e = entity(c, pos);
                causesBeforeReload = e.burnCauses();
                workBeforeReload = e.workJoules();
                CompoundTag saved = e.saveWithFullMetadata(c.level().registryAccess());
                c.level().removeBlockEntity(pos);
                BlockEntity fresh = BlockEntity.loadStatic(pos, c.level().getBlockState(pos), saved, c.level().registryAccess());
                ((ApiTestMachineEntity) fresh).useSpec(ApiTestMachines.BURNING_SPEC);
                c.level().setBlockEntity(fresh);
            });
        }

        public void measure(TestContext c) {
            c.check("burn: refused without a handler", refused instanceof IllegalStateException
                && refused.getMessage().contains("burnsOnOvervoltage(handler)"), String.valueOf(refused));
            ApiTestMachineEntity e = entity(c, pos);
            c.check("burn: the block and its block entity stay (the handler leaves them)", e != null, "");
            if (e == null) return;
            Machine m = ElnApi.machines().of(e);
            c.check("burn: it ran at 50 V first", before == MachineState.RUNNING, String.valueOf(before));
            c.check("burn: reloaded, a new block entity", HostedMachines.INSTANCE.node(e) != null && !causesBeforeReload.isEmpty()
                && e.burnCauses().isEmpty(), causesBeforeReload + " then " + e.burnCauses());
            List<mods.eln.api.TripCause> causes = causesBeforeReload;
            c.check("burn: the handler was called once, and not again on reload", causes.size() == 1 && e.burnCauses().isEmpty(),
                causes + " then " + e.burnCauses());
            if (causes.size() != 1) return;
            mods.eln.api.TripCause t = causes.get(0);
            c.check("burn: with the overvoltage that burnt it", t.state() == MachineState.OVERVOLTAGE_TRIPPED && t.burns()
                && Math.abs(t.limit() - 65) < 1e-9 && t.measured() > 65, t.toString());
            c.check("burn: reloaded, it stays tripped at 50 V past the cooldown", m.state() == MachineState.OVERVOLTAGE_TRIPPED
                && m.lastTrip().map(t::equals).orElse(false), m.state() + " " + m.lastTrip());
            c.check("burn: no work after the burn, nor after the reload", workAtBurn >= 0 && workBeforeReload == workAtBurn
                && e.workJoules() == 0 && m.amps() < 1e-3,
                "worked " + workAtBurn + " J, then " + workBeforeReload + " J; " + e.workJoules() + " J since the reload, " + m.amps() + " A");
        }
    }
}
