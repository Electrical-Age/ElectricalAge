package mods.eln.gametest.cases;

import mods.eln.bus.BusNet;
import mods.eln.bus.BusPort;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNode;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.sixnode.busmodule.BusModuleElement;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Set;

/**
 * The Bus I/O Module (mods.eln.bus): two modules on one bus carry a signal from a wire to a wire; two writers on one
 * channel, the higher wins; its nine terminals' names and directions survive a save and load and are claimed again;
 * a bad name and a 65th channel are refused.
 */
public final class BusModuleCases {
    private BusModuleCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Across());
        cases.add(new HighestWins());
        cases.add(new Saved());
        cases.add(new Refused());
    }

    static final int MODULE = 1 + (117 << 6);
    static final int SIGNAL = CablePart.tierOf("Signal Cable"), BUS = CablePart.BUS, DOWN = 0, WEST = 4, EAST = 5;
    /** The terminal at the middle of the back edge (offset 0): it meets a cable along the block's centre line. */
    static final int BACK_MIDDLE = 4;

    /** A module on the floor of a voxel block at p, its bus edge toward dir. */
    static BusModuleElement module(TestContext c, BlockPos p, Direction dir) {
        var m = (BusModuleElement) CableCases.host(c, MODULE, p, Direction.YN);
        Wp9aCases.orient(m, l -> l, dir);
        return m;
    }

    /** A Signal Source at p on its floor, its output toward dir, at u volts. */
    static void source(TestContext c, BlockPos p, Direction dir, double u) {
        var s = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) c.placeSix(Wp9aCases.SIGNAL_SOURCE, p);
        Wp9aCases.orient(s, l -> l, dir);
        float v = (float) u;
        s.networkUnserialize(c.stream(o -> {
            o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
            o.writeFloat(v);
        }));
    }

    /** A floor cable of tier straight across the block at p, west to east. */
    static void across(TestContext c, BlockPos p, int tier) {
        CableCases.voxel(c, p, CablePart.arms(tier, DOWN, WEST, EAST));
    }

    /** Where along its edge (block voxels, z for an east or west edge) terminal i of m meets a cable. */
    static int along(BusModuleElement m, int i) {
        LRDU edge = m.edgeOf(i);
        int a = m.side.applyLRDU(edge.left()).toEnumFacing().get3DDataValue();
        int o = BusModuleElement.PIN_OFFSETS[i % 3];
        return 8 + (a % 2 == 0 ? -o : o);
    }

    /**
     * A Signal Source at 30 V (x 0) through a signal cable (x 1) to module A's back middle terminal, set in (x 2); bus
     * cable (x 3, 4) to module B (x 5), whose back middle terminal, set out on the same channel, drives a signal cable
     * (x 6).
     */
    static final class Across implements TestCase {
        BusModuleElement a, b;
        String why = "";

        public String id() {
            return "bus_module_wire_to_wire";
        }

        public void build(TestContext c) {
            try {
                source(c, c.at(0, 1, 1), Direction.XP, 30);
                across(c, c.at(1, 1, 1), SIGNAL);
                a = module(c, c.at(2, 1, 1), Direction.XP);
                across(c, c.at(3, 1, 1), BUS);
                across(c, c.at(4, 1, 1), BUS);
                b = module(c, c.at(5, 1, 1), Direction.XN);
                across(c, c.at(6, 1, 1), SIGNAL);
                a.setName(BACK_MIDDLE, "level");
                b.setName(BACK_MIDDLE, "level");
                b.setOut(BACK_MIDDLE, true);
            } catch (RuntimeException e) {
                why = String.valueOf(e.getMessage());
            }
        }

        public void measure(TestContext c) {
            c.check("bus module: built", why.isEmpty(), why);
            if (!why.isEmpty()) return;
            c.check("bus module: the back middle terminals are at the cables' z 8", along(a, BACK_MIDDLE) == 8 && along(b, BACK_MIDDLE) == 8,
                along(a, BACK_MIDDLE) + " " + along(b, BACK_MIDDLE));
            c.check("bus module: both on one bus", a.port.bus() != null && a.port.bus() == b.port.bus(), a.port.bus() + " " + b.port.bus());
            c.check("bus module: its bus edge has no circuit link", a.getElectricalLoad(a.busEdge()) == null, "");
            Wp9cCases.within(c, "bus module: A's in terminal sees the source [V]", a.volts(BACK_MIDDLE), 30, 0.5);
            Wp9cCases.within(c, "bus module: the channel carries it [0..1]", a.port.read("level"), 0.6, 0.01);
            Wp9cCases.within(c, "bus module: B's out terminal drives it [V]", b.volts(BACK_MIDDLE), 30, 0.5);
            Object[] k = ((VoxelNode) BusCases.node(c, 6, 1, 1)).cableAt(8, 0, 8);
            double u = k == null || k[0] == null ? Double.NaN : ((mods.eln.sim.ElectricalLoad) k[0]).getU();
            Wp9cCases.within(c, "bus module: the signal wire past B reads it [V]", u, 30, 0.5);
            var w = a.getWaila();
            c.check("bus module: Jade shows the terminal, its direction and volts", w.containsKey("5 level") && w.get("5 level").startsWith("in"), w.toString());
            c.check("bus module: Jade shows the bus", w.containsKey("Bus") && !w.get("Bus").equals("no bus"), w.toString());
        }
    }

    /** Two modules write one channel from sources at 10 V and 40 V: the bus has 40 V, from two writers. */
    static final class HighestWins implements TestCase {
        BusModuleElement a, b;
        String why = "";

        public String id() {
            return "bus_module_highest_writer_wins";
        }

        public void build(TestContext c) {
            try {
                source(c, c.at(0, 1, 1), Direction.XP, 10);
                across(c, c.at(1, 1, 1), SIGNAL);
                a = module(c, c.at(2, 1, 1), Direction.XP);
                across(c, c.at(3, 1, 1), BUS);
                b = module(c, c.at(4, 1, 1), Direction.XN);
                across(c, c.at(5, 1, 1), SIGNAL);
                source(c, c.at(6, 1, 1), Direction.XN, 40);
                a.setName(BACK_MIDDLE, "mix");
                b.setName(BACK_MIDDLE, "mix");
                // A also reads it back on a free terminal
                a.setName(0, "mix");
                a.setOut(0, true);
            } catch (RuntimeException e) {
                why = String.valueOf(e.getMessage());
            }
        }

        public void measure(TestContext c) {
            c.check("bus module highest: built", why.isEmpty(), why);
            if (!why.isEmpty()) return;
            var bus = a.port.bus();
            c.check("bus module highest: one bus", bus != null && bus == b.port.bus(), bus + " " + b.port.bus());
            if (bus == null) return;
            Wp9cCases.within(c, "bus module highest: the channel has the higher, 40 V [0..1]", bus.value("mix"), 0.8, 0.01);
            c.check("bus module highest: two writers", bus.writers("mix") == 2, "" + bus.writers("mix"));
            Wp9cCases.within(c, "bus module highest: an out terminal on it gives 40 V [V]", a.volts(0), 40, 0.5);
            Wp9cCases.within(c, "bus module highest: the 10 V wire stays at 10 V [V]", a.volts(BACK_MIDDLE), 10, 0.5);
        }
    }

    /** A module with all nine terminals set, saved and loaded: the same names and directions, claimed on its bus again. */
    static final class Saved implements TestCase {
        BusModuleElement m;
        static final String[] NAMES = {"a0", "", "pump_on", "level", "x", "", "valve_3", "a0", "z"};

        public String id() {
            return "bus_module_saved";
        }

        public void build(TestContext c) {
            m = module(c, c.at(2, 1, 1), Direction.XP);
            across(c, c.at(3, 1, 1), BUS);
            for (int i = 0; i < 9; i++) {
                m.setName(i, NAMES[i]);
                m.setOut(i, i % 2 == 1);
            }
        }

        static String settings(BusModuleElement e) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < 9; i++) s.append(e.name(i)).append(e.isOut(i) ? ">" : "<").append(' ');
            return s.toString();
        }

        public void measure(TestContext c) {
            String before = settings(m);
            Set<String> claims = Set.copyOf(m.port.claims());
            c.check("bus module saved: its names claimed", claims.equals(Set.of("a0", "pump_on", "level", "x", "valve_3", "z")), claims.toString());
            Reload r;
            try {
                r = Reload.node(c.level(), c.at(2, 1, 1));
            } catch (ReflectiveOperationException e) {
                c.check("bus module saved: reloads", false, e.toString());
                return;
            }
            var fresh = (BusModuleElement) ((SixNode) r.fresh).sideElementList[Direction.YN.getInt()];
            c.check("bus module saved: names and directions come back", settings(fresh).equals(before), before + " -> " + settings(fresh));
            c.check("bus module saved: claimed again on load", Set.copyOf(fresh.port.claims()).equals(claims), fresh.port.claims().toString());
            BusNet.refresh();
            var bus = fresh.port.bus();
            c.check("bus module saved: back on its bus with its channels", bus != null && bus.names().containsAll(claims),
                bus == null ? "no bus" : bus.names().toString());
        }
    }

    /**
     * Names refused: one the bus can't have, and a 65th channel (a bare port on the bus claims 64); a capitalised one is
     * folded to lower case; a name already on the bus costs nothing. Broken, the module's names leave the bus.
     */
    static final class Refused implements TestCase {
        BusModuleElement m;

        public String id() {
            return "bus_module_refused";
        }

        public void build(TestContext c) {
            m = module(c, c.at(2, 1, 1), Direction.XP);
        }

        public void measure(TestContext c) {
            String bad = m.setName(0, "no spaces!");
            c.check("bus module refused: a bad name", bad != null && m.name(0).isEmpty() && !m.message().isEmpty(), bad + " / " + m.name(0));
            String up = m.setName(2, "Tank_Level");
            c.check("bus module refused: capitals are folded", up == null && m.name(2).equals("tank_level"), up + " / " + m.name(2));
            m.setName(2, "");
            // a bare port with 64 channels, on the module's bus
            BusPort full = new BusPort();
            List<String> many = new java.util.ArrayList<>();
            for (int i = 0; i < 64; i++) many.add("c" + i);
            full.claim(many);
            BusNet.link(full, m.port);
            BusNet.refresh();
            c.check("bus module refused: on the full bus", m.port.bus() != null && m.port.bus().names().size() == 64,
                m.port.bus() == null ? "no bus" : "" + m.port.bus().names().size());
            String fine = m.setName(0, "c5");
            c.check("bus module refused: a name the bus has is fine", fine == null && m.name(0).equals("c5"), String.valueOf(fine));
            String more = m.setName(1, "extra");
            c.check("bus module refused: a 65th channel", more != null && m.name(1).isEmpty() && m.message().equals(more), more + " / " + m.name(1));
            c.check("bus module refused: no parenthesis in the messages", !bad.contains("(") && !more.contains("("), bad + " / " + more);
            c.check("bus module refused: Jade has no line for a refused name", !m.getWaila().containsKey("2 extra"), m.getWaila().toString());
            // broken: its port leaves the bus
            m.sixNode.deleteSlot(null, m.slot);
            BusNet.refresh();
            c.check("bus module refused: broken, its port is off the bus", m.port.bus() == null && full.bus() == null,
                m.port.bus() + " " + full.bus());
        }
    }
}
