package mods.eln.gametest.cases;

import mods.eln.bus.BusNet;
import mods.eln.bus.BusPort;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNode;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.sixnode.busmodule.BusModuleElement;
import mods.eln.sixnode.electricalmath.ElectricalMathElement;
import net.minecraft.core.BlockPos;

import java.util.List;

import static mods.eln.gametest.cases.BusModuleCases.BACK_MIDDLE;
import static mods.eln.gametest.cases.BusModuleCases.BUS;
import static mods.eln.gametest.cases.BusModuleCases.DOWN;
import static mods.eln.gametest.cases.BusModuleCases.SIGNAL;
import static mods.eln.gametest.cases.BusModuleCases.CYAN;
import static mods.eln.gametest.cases.BusModuleCases.LIGHT_BLUE;
import static mods.eln.gametest.cases.BusModuleCases.LIME;
import static mods.eln.gametest.cases.BusModuleCases.ORANGE;
import static mods.eln.gametest.cases.BusModuleCases.RED;
import static mods.eln.gametest.cases.BusModuleCases.UNUSED;
import static mods.eln.gametest.cases.BusModuleCases.WHITE;
import static mods.eln.gametest.cases.BusModuleCases.YELLOW;

/**
 * The signal processor and the data logger on the field bus (mods.eln.bus): a processor reads a channel a Bus I/O
 * Module puts on the bus and writes another a module drives a wire from, its output wire still working beside its bus
 * pin; a program ending with a bus definition; only the 16 dye names are channels; a logger channel recording a bus
 * channel; both devices' settings kept over a save and load.
 */
public final class BusSpCases {
    private BusSpCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new ReadWrite());
        cases.add(new EndsWithBus());
        cases.add(new BadChannel());
        cases.add(new SpSaved());
        cases.add(new LoggerOnBus());
    }

    /** A signal processor in a voxel block at p, its output toward dir, with this program and chips. */
    static ElectricalMathElement processor(TestContext c, BlockPos p, Direction dir, String program, int chips) {
        var m = (ElectricalMathElement) CableCases.host(c, Wp9cCases.SIGNAL_PROCESSOR, p, Direction.YN);
        Wp9aCases.orient(m, l -> l, dir);
        program(c, m, program);
        if (chips > 0) {
            m.getInventory().setItem(0, mods.eln.Eln.findItemStack("Cheap Chip", chips));
            m.getInventory().setChanged();
        }
        return m;
    }

    /** Its program, as its screen's Save sends it. */
    static void program(TestContext c, ElectricalMathElement m, String program) {
        m.networkUnserialize(c.stream(o -> {
            o.writeByte(1); // ElectricalMathElement.setExpressionId
            o.writeUTF(program);
        }), Wp9cCases.player(c));
    }

    /** Where along its output edge (block voxels) the processor's bus pin is. */
    static int busAlong(ElectricalMathElement m) {
        int a = m.side.applyLRDU(m.front.left()).toEnumFacing().get3DDataValue();
        return 8 + (a % 2 == 0 ? -ElectricalMathElement.BUS_OFFSET : ElectricalMathElement.BUS_OFFSET);
    }

    /** Bus cable across the floor of p, west to east, from z on its west edge (a processor's bus pin) to the middle
     *  of its east edge. */
    static void bentTo(TestContext c, BlockPos p, int z) {
        CableCases.voxel(c, p, List.of(new CablePart(BUS, DOWN, new int[]{0, 0, z, 8, 0, z, 8, 0, 8}), new CablePart(BUS, DOWN, new int[]{8, 0, 8, 16, 0, 8})));
    }

    static String error(ElectricalMathElement m) {
        return Batch1Cases.field(m, "error");
    }

    static boolean valid(ElectricalMathElement m) {
        return Batch1Cases.field(m, "equationIsValid");
    }

    /**
     * A Signal Source at 0.6 SVU (x 0) into module A's in terminal on 4 yellow (x 2); bus cable (x 3) on to module B (z 0),
     * whose out terminal takes 1 orange, and to the processor (x 4, output west), whose bus pin is off its output's middle:
     * the cable bends to meet it. Program: bus.orange = bus.yellow / 2, output bus.yellow, on a signal cable in x 3 that
     * meets the output terminal.
     */
    static final class ReadWrite implements TestCase {
        BusModuleElement a, b;
        ElectricalMathElement sp;
        int pz;
        String why = "";

        public String id() {
            return "bus_sp_read_write";
        }

        public void build(TestContext c) {
            try {
                BusModuleCases.source(c, c.at(0, 1, 1), Direction.XP, 0.6 * mods.eln.Eln.SVU);
                BusModuleCases.across(c, c.at(1, 1, 1), SIGNAL);
                a = BusModuleCases.module(c, c.at(2, 1, 1), Direction.XP);
                b = BusModuleCases.module(c, c.at(3, 1, 0), Direction.ZP);
                sp = processor(c, c.at(4, 1, 1), Direction.XN, "bus.orange = bus.yellow / 2\nbus.yellow", 1);
                pz = busAlong(sp);
                CableCases.voxel(c, c.at(3, 1, 1),
                    List.of(new CablePart(BUS, DOWN, new int[]{0, 0, 8, 8, 0, 8, 8, 0, 0}),
                        new CablePart(BUS, DOWN, new int[]{8, 0, 8, 8, 0, pz, 16, 0, pz})),
                    List.of(new CablePart(SIGNAL, DOWN, new int[]{16, 0, 8, 12, 0, 8})));
                a.setChannel(BACK_MIDDLE, YELLOW);
                b.setChannel(BACK_MIDDLE, ORANGE);
                b.setOut(BACK_MIDDLE, true);
            } catch (RuntimeException e) {
                why = String.valueOf(e);
            }
        }

        public void measure(TestContext c) {
            c.check("bus sp: built", why.isEmpty(), why);
            if (!why.isEmpty()) return;
            c.check("bus sp: its bus pin is 4 voxels off the output", pz == 4 || pz == 12, "" + pz);
            c.check("bus sp: the program compiles", valid(sp), error(sp));
            c.check("bus sp: on the modules' bus", sp.port.bus() != null && sp.port.bus() == a.port.bus() && a.port.bus() == b.port.bus(),
                sp.port.bus() + " " + a.port.bus() + " " + b.port.bus());
            c.check("bus sp: claims what the program reads and writes", sp.port.claims() == (1 << ORANGE | 1 << YELLOW), "" + sp.port.claims());
            Wp9cCases.within(c, "bus sp: reads the channel module A puts on [0..1]", sp.port.read(YELLOW), 0.6, 0.01);
            Wp9cCases.within(c, "bus sp: writes half of it [0..1]", sp.port.read(ORANGE), 0.3, 0.01);
            c.check("bus sp: one writer on its channel", sp.port.bus() != null && sp.port.bus().writers(ORANGE) == 1,
                sp.port.bus() == null ? "no bus" : "" + sp.port.bus().writers(ORANGE));
            Wp9cCases.within(c, "bus sp: module B's out terminal drives it [V]", b.volts(BACK_MIDDLE), 0.3 * mods.eln.Eln.SVU, 0.02 * mods.eln.Eln.SVU);
            Object[] k = ((VoxelNode) BusCases.node(c, 3, 1, 1)).cableAt(14, 0, 8);
            double u = k == null || k[0] == null ? Double.NaN : ((mods.eln.sim.ElectricalLoad) k[0]).getU();
            Wp9cCases.within(c, "bus sp: its output wire beside the bus pin still gives the output [V]", u, 0.6 * mods.eln.Eln.SVU, 0.02 * mods.eln.Eln.SVU);
            c.check("bus sp: its output edge has no circuit link to the bus", sp.edgePins().get(1).load() == null, "");
            // a bus kit aimed at the bus pin ends there, a signal kit at the output beside it (AdventurAgent: a bus kit
            // found no place on the processor)
            BlockPos at = c.at(4, 1, 1);
            var node = (VoxelNode) BusCases.node(c, 4, 1, 1);
            var busEnd = mods.eln.node.voxel.CablePlacer.deviceEnd(node, at, sp.side.getInt(), new double[]{0.05, 0, pz / 16.0}, false, BUS);
            c.check("bus sp: a bus kit ends at the bus pin", busEnd != null && busEnd.x() == at.getX() * 16L && busEnd.z() == at.getZ() * 16L + pz,
                String.valueOf(busEnd));
            var sigEnd = mods.eln.node.voxel.CablePlacer.deviceEnd(node, at, sp.side.getInt(), new double[]{0.05, 0, pz / 16.0}, false, SIGNAL);
            c.check("bus sp: a signal kit there ends at the output", sigEnd != null && sigEnd.z() == at.getZ() * 16L + 8, String.valueOf(sigEnd));
            var w = sp.getWaila();
            c.check("bus sp: Jade shows its bus channels by number and name", "1 orange, 4 yellow".equals(w.get("Bus channels")), w.toString());
        }
    }

    /**
     * A program whose last line is a bus definition (x 1, output east) feeds module B's out terminal (x 3) through bus
     * cable (x 2) bent from the processor's bus pin to the module's: 0.4 on the channel, 0.4 SVU on the module's wire, and
     * 0 V on the processor's output, which has no expression.
     */
    static final class EndsWithBus implements TestCase {
        ElectricalMathElement sp;
        BusModuleElement b;
        String why = "";

        public String id() {
            return "bus_sp_ends_with_bus";
        }

        public void build(TestContext c) {
            try {
                sp = processor(c, c.at(1, 1, 1), Direction.XP, "x = 0.4\nbus.lime = x", 0);
                b = BusModuleCases.module(c, c.at(3, 1, 1), Direction.XN);
                bentTo(c, c.at(2, 1, 1), busAlong(sp));
                b.setChannel(BACK_MIDDLE, LIME);
                b.setOut(BACK_MIDDLE, true);
            } catch (RuntimeException e) {
                why = String.valueOf(e);
            }
        }

        public void measure(TestContext c) {
            c.check("bus sp ends: built", why.isEmpty(), why);
            if (!why.isEmpty()) return;
            c.check("bus sp ends: a program ending with a bus definition compiles", valid(sp), error(sp));
            Wp9cCases.within(c, "bus sp ends: the channel has 0.4 [0..1]", sp.port.read(LIME), 0.4, 1e-6);
            Wp9cCases.within(c, "bus sp ends: the module's out terminal gives 0.4 SVU [V]", b.volts(BACK_MIDDLE), 0.4 * mods.eln.Eln.SVU, 0.02 * mods.eln.Eln.SVU);
            Wp9cCases.within(c, "bus sp ends: no output expression, 0 V on the output [V]", Wp9cCases.out(sp), 0, 0.01);
        }
    }

    /**
     * Only the 16 dye names are channels: bus.teal is a compile error that names them, and claims nothing; the dye names
     * compile and are claimed; a plain program claims nothing. Broken, the processor's port leaves the bus.
     */
    static final class BadChannel implements TestCase {
        ElectricalMathElement sp;

        public String id() {
            return "bus_sp_bad_channel";
        }

        public void build(TestContext c) {
            sp = processor(c, c.at(2, 1, 1), Direction.XP, "1", 0);
        }

        public void measure(TestContext c) {
            BusPort other = new BusPort();
            BusNet.link(other, sp.port);
            BusNet.refresh();
            program(c, sp, "bus.red = bus.white > 0.5");
            c.check("bus sp channels: dye names are channels", valid(sp) && sp.port.claims() == (1 << RED | 1 << WHITE),
                error(sp) + " " + sp.port.claims());
            program(c, sp, "bus.red = bus.teal > 0.5");
            String e = error(sp);
            c.check("bus sp channels: bus.teal is no channel", !valid(sp) && e.contains("bus.teal is not a bus channel"), e);
            c.check("bus sp channels: the error names the 16", e.contains("white") && e.contains("light_blue") && e.contains("black"), e);
            c.check("bus sp channels: no parenthesis in the message", !e.contains("("), e);
            c.check("bus sp channels: a program that fails claims nothing", sp.port.claims() == 0, "" + sp.port.claims());
            program(c, sp, "A * 2");
            c.check("bus sp channels: a program without the bus is fine", valid(sp) && error(sp).isEmpty() && sp.port.claims() == 0, error(sp));
            sp.sixNode.deleteSlot(null, sp.slot);
            BusNet.refresh();
            c.check("bus sp channels: broken, its port is off the bus", sp.port.bus() == null && other.bus() == null, sp.port.bus() + " " + other.bus());
        }
    }

    /** A processor with a bus program, saved and loaded: the same program, valid, its names claimed and on its bus again. */
    static final class SpSaved implements TestCase {
        ElectricalMathElement sp;
        static final String PROGRAM = "# bus\nlow = bus.cyan < 0.3\nbus.red = rs(low, bus.cyan > 0.9)\nbus.red";

        public String id() {
            return "bus_sp_saved";
        }

        public void build(TestContext c) {
            sp = processor(c, c.at(2, 1, 1), Direction.XP, PROGRAM, 1);
            bentTo(c, c.at(3, 1, 1), busAlong(sp));
        }

        public void measure(TestContext c) {
            c.check("bus sp saved: compiles", valid(sp), error(sp));
            int claims = sp.port.claims();
            c.check("bus sp saved: claims its channels", claims == (1 << CYAN | 1 << RED), "" + claims);
            c.check("bus sp saved: on a bus", sp.port.bus() != null, "no bus");
            Reload r;
            try {
                r = Reload.node(c.level(), c.at(2, 1, 1));
            } catch (ReflectiveOperationException e) {
                c.check("bus sp saved: reloads", false, e.toString());
                return;
            }
            var fresh = (ElectricalMathElement) ((SixNode) r.fresh).sideElementList[Direction.YN.getInt()];
            String expr = Batch1Cases.field(fresh, "expression");
            c.check("bus sp saved: the program comes back", PROGRAM.equals(expr) && valid(fresh), expr + " / " + error(fresh));
            c.check("bus sp saved: claimed again on load", fresh.port.claims() == claims, "" + fresh.port.claims());
            BusNet.refresh();
            var bus = fresh.port.bus();
            c.check("bus sp saved: back on a bus with its channels in use", bus != null && bus.inUse(CYAN) && bus.inUse(RED), "" + bus);
        }
    }

    /**
     * A Three-Channel Data Logger (front north, in a voxel block) whose channel B records bus channel 3 light_blue: module A's
     * in terminal puts a 0.4 SVU Signal Source on it, through bus cable to the logger's bus pin (x 4; the rest east or west
     * of it, as busEdge says). B records 0.4, A and C their open wires; saved and loaded, B still takes it, claimed again.
     */
    static final class LoggerOnBus implements TestCase {
        mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerElement logger;
        BusModuleElement a;
        String why = "";
        int lx;

        public String id() {
            return "bus_logger_channel";
        }

        public int width() {
            return 9;
        }

        public void build(TestContext c) {
            try {
                // which way its bus pin faces decides the side the rest goes
                logger = (mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerElement) CableCases.host(c, Wp9cCases.THREE_CHANNEL_LOGGER, c.at(4, 1, 1),
                    Direction.YN);
                Wp9aCases.orient(logger, l -> l, Direction.ZN);
                Direction d = logger.side.applyLRDU(logger.busEdge());
                if (d != Direction.XP && d != Direction.XN) throw new IllegalStateException("bus pin toward " + d);
                int s = d == Direction.XP ? 1 : -1;
                lx = 4;
                BusModuleCases.across(c, c.at(lx + s, 1, 1), BUS);
                a = BusModuleCases.module(c, c.at(lx + 2 * s, 1, 1), s > 0 ? Direction.XN : Direction.XP);
                BusModuleCases.across(c, c.at(lx + 3 * s, 1, 1), SIGNAL);
                BusModuleCases.source(c, c.at(lx + 4 * s, 1, 1), s > 0 ? Direction.XN : Direction.XP, 0.4 * mods.eln.Eln.SVU);
                a.setChannel(BACK_MIDDLE, LIGHT_BLUE);
                // as its screen sends it
                logger.networkUnserialize(c.stream(o -> {
                    o.writeByte(mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerElement.setBusId);
                    o.writeByte(1);
                    o.writeByte(LIGHT_BLUE);
                }), Wp9cCases.player(c));
            } catch (RuntimeException e) {
                why = String.valueOf(e);
            }
        }

        public void measure(TestContext c) {
            c.check("bus logger: built", why.isEmpty(), why);
            if (!why.isEmpty()) return;
            c.check("bus logger: on module A's bus", logger.port.bus() != null && logger.port.bus() == a.port.bus(), logger.port.bus() + " " + a.port.bus());
            c.check("bus logger: channel B takes 3 light_blue, claimed", logger.busChannel(1) == LIGHT_BLUE && logger.port.claims() == 1 << LIGHT_BLUE,
                logger.busChannel(1) + " " + logger.port.claims());
            c.check("bus logger: its bus edge has no wire", logger.getElectricalLoad(logger.busEdge()) == null, "");
            var v = logger.view();
            c.check("bus logger: channel B records the bus channel, 0.4; A and C their open wires",
                v.size() > 0 && Math.abs(v.mean(1, 0) - 0.4) < 2e-3 && Math.abs(v.mean(0, 0)) < 2e-3 && Math.abs(v.mean(2, 0)) < 2e-3,
                v.size() > 0 ? v.mean(0, 0) + " " + v.mean(1, 0) + " " + v.mean(2, 0) : "no points");
            var w = logger.getWaila();
            c.check("bus logger: Jade shows B's channel by number and name", w.getOrDefault("Bus B", "").startsWith("3 light_blue"), w.toString());
            logger.setBus(0, 16);
            c.check("bus logger: 16 is no channel, A records its wire", logger.busChannel(0) == UNUSED, "" + logger.busChannel(0));
            Reload r;
            try {
                r = Reload.node(c.level(), c.at(lx, 1, 1));
            } catch (ReflectiveOperationException e) {
                c.check("bus logger: reloads", false, e.toString());
                return;
            }
            var fresh = (mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerElement) ((SixNode) r.fresh).sideElementList[Direction.YN.getInt()];
            c.check("bus logger: its channels' bus channels come back",
                fresh.busChannel(1) == LIGHT_BLUE && fresh.busChannel(0) == UNUSED && fresh.busChannel(2) == UNUSED,
                fresh.busChannel(0) + "/" + fresh.busChannel(1) + "/" + fresh.busChannel(2));
            c.check("bus logger: claimed again on load", fresh.port.claims() == 1 << LIGHT_BLUE, "" + fresh.port.claims());
            BusNet.refresh();
            c.check("bus logger: back on the bus", fresh.port.bus() != null && fresh.port.bus().inUse(LIGHT_BLUE), "" + fresh.port.bus());
        }
    }
}
