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
import java.util.Set;

import static mods.eln.gametest.cases.BusModuleCases.BACK_MIDDLE;
import static mods.eln.gametest.cases.BusModuleCases.BUS;
import static mods.eln.gametest.cases.BusModuleCases.DOWN;
import static mods.eln.gametest.cases.BusModuleCases.SIGNAL;

/**
 * The signal processor and the data logger on the field bus (mods.eln.bus): a processor reads a channel a Bus I/O
 * Module puts on the bus and writes another a module drives a wire from, its output wire still working beside its bus
 * pin; a program ending with a bus definition; a 65th channel refused; a logger channel recording a bus channel; both
 * devices' settings kept over a save and load.
 */
public final class BusSpCases {
    private BusSpCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new ReadWrite());
        cases.add(new EndsWithBus());
        cases.add(new Refused());
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
     * A Signal Source at 30 V (x 0) into module A's in terminal "level" (x 2); bus cable (x 3) on to module B (z 0),
     * whose out terminal takes "half", and to the processor (x 4, output west), whose bus pin is off its output's middle:
     * the cable bends to meet it. Program: bus.half = bus.level / 2, output bus.level, on a signal cable in x 3 that
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
                BusModuleCases.source(c, c.at(0, 1, 1), Direction.XP, 30);
                BusModuleCases.across(c, c.at(1, 1, 1), SIGNAL);
                a = BusModuleCases.module(c, c.at(2, 1, 1), Direction.XP);
                b = BusModuleCases.module(c, c.at(3, 1, 0), Direction.ZP);
                sp = processor(c, c.at(4, 1, 1), Direction.XN, "bus.half = bus.level / 2\nbus.level", 1);
                pz = busAlong(sp);
                CableCases.voxel(c, c.at(3, 1, 1),
                    List.of(new CablePart(BUS, DOWN, new int[]{0, 0, 8, 8, 0, 8, 8, 0, 0}),
                        new CablePart(BUS, DOWN, new int[]{8, 0, 8, 8, 0, pz, 16, 0, pz})),
                    List.of(new CablePart(SIGNAL, DOWN, new int[]{16, 0, 8, 12, 0, 8})));
                a.setName(BACK_MIDDLE, "level");
                b.setName(BACK_MIDDLE, "half");
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
            c.check("bus sp: claims what the program reads and writes", sp.port.claims().equals(Set.of("half", "level")), sp.port.claims().toString());
            Wp9cCases.within(c, "bus sp: reads the channel module A puts on [0..1]", sp.port.read("level"), 0.6, 0.01);
            Wp9cCases.within(c, "bus sp: writes half of it [0..1]", sp.port.read("half"), 0.3, 0.01);
            c.check("bus sp: one writer on its channel", sp.port.bus() != null && sp.port.bus().writers("half") == 1,
                sp.port.bus() == null ? "no bus" : "" + sp.port.bus().writers("half"));
            Wp9cCases.within(c, "bus sp: module B's out terminal drives it [V]", b.volts(BACK_MIDDLE), 15, 0.5);
            Object[] k = ((VoxelNode) BusCases.node(c, 3, 1, 1)).cableAt(14, 0, 8);
            double u = k == null || k[0] == null ? Double.NaN : ((mods.eln.sim.ElectricalLoad) k[0]).getU();
            Wp9cCases.within(c, "bus sp: its output wire beside the bus pin still gives the output [V]", u, 30, 0.5);
            c.check("bus sp: its output edge has no circuit link to the bus", sp.edgePins().get(1).load() == null, "");
            var w = sp.getWaila();
            c.check("bus sp: Jade shows its bus channels", "2".equals(w.get("Bus channels")), w.toString());
        }
    }

    /**
     * A program whose last line is a bus definition (x 1, output east) feeds module B's out terminal (x 3) through bus
     * cable (x 2) bent from the processor's bus pin to the module's: 0.4 on the channel, 20 V on the module's wire, and
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
                sp = processor(c, c.at(1, 1, 1), Direction.XP, "x = 0.4\nbus.const = x", 0);
                b = BusModuleCases.module(c, c.at(3, 1, 1), Direction.XN);
                bentTo(c, c.at(2, 1, 1), busAlong(sp));
                b.setName(BACK_MIDDLE, "const");
                b.setOut(BACK_MIDDLE, true);
            } catch (RuntimeException e) {
                why = String.valueOf(e);
            }
        }

        public void measure(TestContext c) {
            c.check("bus sp ends: built", why.isEmpty(), why);
            if (!why.isEmpty()) return;
            c.check("bus sp ends: a program ending with a bus definition compiles", valid(sp), error(sp));
            Wp9cCases.within(c, "bus sp ends: the channel has 0.4 [0..1]", sp.port.read("const"), 0.4, 1e-6);
            Wp9cCases.within(c, "bus sp ends: the module's out terminal gives 20 V [V]", b.volts(BACK_MIDDLE), 20, 0.5);
            Wp9cCases.within(c, "bus sp ends: no output expression, 0 V on the output [V]", Wp9cCases.out(sp), 0, 0.01);
        }
    }

    /**
     * On a bus with 64 channels (a bare port claims them), a program using one of them compiles, one adding a 65th is
     * refused with its message shown as a compile error, and claims nothing; a plain program claims nothing either.
     * Broken, the processor's port leaves the bus.
     */
    static final class Refused implements TestCase {
        ElectricalMathElement sp;

        public String id() {
            return "bus_sp_refused";
        }

        public void build(TestContext c) {
            sp = processor(c, c.at(2, 1, 1), Direction.XP, "1", 0);
        }

        public void measure(TestContext c) {
            BusPort full = new BusPort();
            List<String> many = new java.util.ArrayList<>();
            for (int i = 0; i < 64; i++) many.add("c" + i);
            full.claim(many);
            BusNet.link(full, sp.port);
            BusNet.refresh();
            c.check("bus sp refused: on the full bus", sp.port.bus() != null && sp.port.bus().names().size() == 64,
                sp.port.bus() == null ? "no bus" : "" + sp.port.bus().names().size());
            program(c, sp, "bus.c5 = bus.c6 > 0.5");
            c.check("bus sp refused: channels the bus has are fine", valid(sp) && sp.port.claims().equals(Set.of("c5", "c6")),
                error(sp) + " " + sp.port.claims());
            program(c, sp, "bus.c5 = bus.extra > 0.5");
            String e = error(sp);
            c.check("bus sp refused: a 65th channel", !valid(sp) && e.startsWith("Bus full"), e);
            c.check("bus sp refused: no parenthesis in the message", !e.contains("("), e);
            c.check("bus sp refused: a refused program claims nothing", sp.port.claims().isEmpty(), sp.port.claims().toString());
            program(c, sp, "A * 2");
            c.check("bus sp refused: a program without the bus is fine", valid(sp) && error(sp).isEmpty() && sp.port.claims().isEmpty(), error(sp));
            sp.sixNode.deleteSlot(null, sp.slot);
            BusNet.refresh();
            c.check("bus sp refused: broken, its port is off the bus", sp.port.bus() == null && full.bus() == null, sp.port.bus() + " " + full.bus());
        }
    }

    /** A processor with a bus program, saved and loaded: the same program, valid, its names claimed and on its bus again. */
    static final class SpSaved implements TestCase {
        ElectricalMathElement sp;
        static final String PROGRAM = "# bus\nlow = bus.tank_level < 0.3\nbus.pump = rs(low, bus.tank_level > 0.9)\nbus.pump";

        public String id() {
            return "bus_sp_saved";
        }

        public void build(TestContext c) {
            sp = processor(c, c.at(2, 1, 1), Direction.XP, PROGRAM, 1);
            bentTo(c, c.at(3, 1, 1), busAlong(sp));
        }

        public void measure(TestContext c) {
            c.check("bus sp saved: compiles", valid(sp), error(sp));
            Set<String> claims = Set.copyOf(sp.port.claims());
            c.check("bus sp saved: claims its channels", claims.equals(Set.of("tank_level", "pump")), claims.toString());
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
            c.check("bus sp saved: claimed again on load", Set.copyOf(fresh.port.claims()).equals(claims), fresh.port.claims().toString());
            BusNet.refresh();
            var bus = fresh.port.bus();
            c.check("bus sp saved: back on a bus with its channels", bus != null && bus.names().containsAll(claims),
                bus == null ? "no bus" : bus.names().toString());
        }
    }

    /**
     * A Three-Channel Data Logger (front north, in a voxel block) whose channel B records bus channel "lvl": module A's
     * in terminal puts a 20 V Signal Source on it, through bus cable to the logger's bus pin (x 4; the rest east or west
     * of it, as busEdge says). B records 0.4, A and C their open wires; saved and loaded, B still takes "lvl", claimed again.
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
                BusModuleCases.source(c, c.at(lx + 4 * s, 1, 1), s > 0 ? Direction.XN : Direction.XP, 20);
                a.setName(BACK_MIDDLE, "lvl");
                if (logger.setBus(1, "LVL ") != null) throw new IllegalStateException(logger.busMessage());
            } catch (RuntimeException e) {
                why = String.valueOf(e);
            }
        }

        public void measure(TestContext c) {
            c.check("bus logger: built", why.isEmpty(), why);
            if (!why.isEmpty()) return;
            c.check("bus logger: on module A's bus", logger.port.bus() != null && logger.port.bus() == a.port.bus(), logger.port.bus() + " " + a.port.bus());
            c.check("bus logger: the name is folded to lower case and claimed", "lvl".equals(logger.busName(1)) && logger.port.claims().equals(Set.of("lvl")),
                logger.busName(1) + " " + logger.port.claims());
            c.check("bus logger: its bus edge has no wire", logger.getElectricalLoad(logger.busEdge()) == null, "");
            var v = logger.view();
            c.check("bus logger: channel B records the bus channel, 0.4; A and C their open wires",
                v.size() > 0 && Math.abs(v.mean(1, 0) - 0.4) < 2e-3 && Math.abs(v.mean(0, 0)) < 2e-3 && Math.abs(v.mean(2, 0)) < 2e-3,
                v.size() > 0 ? v.mean(0, 0) + " " + v.mean(1, 0) + " " + v.mean(2, 0) : "no points");
            String bad = logger.setBus(0, "no spaces!");
            c.check("bus logger: a bad name is refused", bad != null && logger.busName(0).isEmpty() && !bad.contains("("), String.valueOf(bad));
            logger.setBus(0, "");
            Reload r;
            try {
                r = Reload.node(c.level(), c.at(lx, 1, 1));
            } catch (ReflectiveOperationException e) {
                c.check("bus logger: reloads", false, e.toString());
                return;
            }
            var fresh = (mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerElement) ((SixNode) r.fresh).sideElementList[Direction.YN.getInt()];
            c.check("bus logger: its channels' bus names come back", "lvl".equals(fresh.busName(1)) && fresh.busName(0).isEmpty() && fresh.busName(2).isEmpty(),
                fresh.busName(0) + "/" + fresh.busName(1) + "/" + fresh.busName(2));
            c.check("bus logger: claimed again on load", fresh.port.claims().equals(Set.of("lvl")), fresh.port.claims().toString());
            BusNet.refresh();
            c.check("bus logger: back on the bus", fresh.port.bus() != null && fresh.port.bus().names().contains("lvl"),
                fresh.port.bus() == null ? "no bus" : fresh.port.bus().names().toString());
        }
    }
}
