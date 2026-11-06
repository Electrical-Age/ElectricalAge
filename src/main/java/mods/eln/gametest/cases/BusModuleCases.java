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

/**
 * The Bus I/O Module (mods.eln.bus): two modules on one bus carry a signal from a wire to a wire; two writers on one
 * channel, the last write wins and the other's comes back when it stops; its nine terminals' channels and directions survive a save and load and are claimed again;
 * its screen's packet tunes a terminal, and a value that is no channel leaves it unused; a brush tunes the terminal
 * nearest the click.
 */
public final class BusModuleCases {
    private BusModuleCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Across());
        cases.add(new LastWins());
        cases.add(new Saved());
        cases.add(new Tuned());
        cases.add(new Brushed());
    }

    static final int MODULE = 1 + (117 << 6);
    static final int SIGNAL = CablePart.tierOf("Signal Cable"), BUS = CablePart.BUS, DOWN = 0, WEST = 4, EAST = 5;
    /** The terminal at the middle of the back edge (offset 0): it meets a cable along the block's centre line. */
    static final int BACK_MIDDLE = 4;
    /** Bus channels by their dye names (BusChannel). */
    static final int WHITE = 0, ORANGE = 1, LIGHT_BLUE = 3, YELLOW = 4, LIME = 5, CYAN = 9, BLUE = 11, RED = 14, BLACK = 15,
        UNUSED = mods.eln.bus.BusChannel.UNUSED;

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
     * A Signal Source at 0.6 SVU (x 0) through a signal cable (x 1) to module A's back middle terminal, set in (x 2); bus
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
                source(c, c.at(0, 1, 1), Direction.XP, 0.6 * mods.eln.Eln.SVU);
                across(c, c.at(1, 1, 1), SIGNAL);
                a = module(c, c.at(2, 1, 1), Direction.XP);
                across(c, c.at(3, 1, 1), BUS);
                across(c, c.at(4, 1, 1), BUS);
                b = module(c, c.at(5, 1, 1), Direction.XN);
                across(c, c.at(6, 1, 1), SIGNAL);
                a.setChannel(BACK_MIDDLE, LIGHT_BLUE);
                b.setChannel(BACK_MIDDLE, LIGHT_BLUE);
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
            Wp9cCases.within(c, "bus module: A's in terminal sees the source [V]", a.volts(BACK_MIDDLE), 0.6 * mods.eln.Eln.SVU, 0.02 * mods.eln.Eln.SVU);
            Wp9cCases.within(c, "bus module: the channel carries it [0..1]", a.port.read(LIGHT_BLUE), 0.6, 0.01);
            Wp9cCases.within(c, "bus module: B's out terminal drives it [V]", b.volts(BACK_MIDDLE), 0.6 * mods.eln.Eln.SVU, 0.02 * mods.eln.Eln.SVU);
            Object[] k = ((VoxelNode) BusCases.node(c, 6, 1, 1)).cableAt(8, 0, 8);
            double u = k == null || k[0] == null ? Double.NaN : ((mods.eln.sim.ElectricalLoad) k[0]).getU();
            Wp9cCases.within(c, "bus module: the signal wire past B reads it [V]", u, 0.6 * mods.eln.Eln.SVU, 0.02 * mods.eln.Eln.SVU);
            var w = a.getWaila();
            c.check("bus module: Jade shows the terminal, its channel's number and name, direction and volts",
                w.containsKey("Terminal 5") && w.get("Terminal 5").startsWith("3 light_blue, in"), w.toString());
            c.check("bus module: Jade has no line for an unused terminal", !w.containsKey("Terminal 1"), w.toString());
            c.check("bus module: Jade shows the bus", w.containsKey("Bus") && !w.get("Bus").equals("no bus"), w.toString());
        }
    }

    /**
     * Two modules write one channel, from sources at 0.2 SVU and 0.8 SVU: the last write wins, and the other's comes back
     * when it stops (Baughn 2026-11-05; this was bus_module_highest_writer_wins, which pinned the highest of a tick's
     * writes winning). A starts alone; B joins at tick 20 and leaves at tick 50.
     */
    static final class LastWins implements TestCase {
        BusModuleElement a, b;
        String why = "";
        double alone = -1, both = -1, bothOut = -1;
        int writersBoth = -1;

        public String id() {
            return "bus_module_last_writer_wins";
        }

        public int ticks() {
            return 80;
        }

        public void build(TestContext c) {
            try {
                source(c, c.at(0, 1, 1), Direction.XP, 0.2 * mods.eln.Eln.SVU);
                across(c, c.at(1, 1, 1), SIGNAL);
                a = module(c, c.at(2, 1, 1), Direction.XP);
                across(c, c.at(3, 1, 1), BUS);
                b = module(c, c.at(4, 1, 1), Direction.XN);
                across(c, c.at(5, 1, 1), SIGNAL);
                source(c, c.at(6, 1, 1), Direction.XN, 0.8 * mods.eln.Eln.SVU);
                a.setChannel(BACK_MIDDLE, RED);
                // A also reads it back on a free terminal
                a.setChannel(0, RED);
                a.setOut(0, true);
            } catch (RuntimeException e) {
                why = String.valueOf(e.getMessage());
                return;
            }
            c.after(18, () -> alone = a.port.bus() == null ? -1 : a.port.bus().value(RED));
            c.after(20, () -> b.setChannel(BACK_MIDDLE, RED));
            c.after(45, () -> {
                var bus = a.port.bus();
                both = bus == null ? -1 : bus.value(RED);
                writersBoth = bus == null ? -1 : bus.writers(RED);
                bothOut = a.volts(0);
            });
            c.after(50, () -> b.setChannel(BACK_MIDDLE, UNUSED));
        }

        public void measure(TestContext c) {
            c.check("bus module last: built", why.isEmpty(), why);
            if (!why.isEmpty()) return;
            var bus = a.port.bus();
            c.check("bus module last: one bus", bus != null && bus == b.port.bus(), bus + " " + b.port.bus());
            if (bus == null) return;
            Wp9cCases.within(c, "bus module last: A alone puts its 0.2 SVU on it [0..1]", alone, 0.2, 0.01);
            Wp9cCases.within(c, "bus module last: B wrote last, 0.8 SVU [0..1]", both, 0.8, 0.01);
            c.check("bus module last: two writers", writersBoth == 2, "" + writersBoth);
            Wp9cCases.within(c, "bus module last: an out terminal on it gave 0.8 SVU [V]", bothOut, 0.8 * mods.eln.Eln.SVU, 0.02 * mods.eln.Eln.SVU);
            Wp9cCases.within(c, "bus module last: B stopped, A's 0.2 SVU is back [0..1]", bus.value(RED), 0.2, 0.01);
            c.check("bus module last: one writer", bus.writers(RED) == 1, "" + bus.writers(RED));
            Wp9cCases.within(c, "bus module last: the 0.2 SVU wire stays at 0.2 SVU [V]", a.volts(BACK_MIDDLE), 0.2 * mods.eln.Eln.SVU, 0.02 * mods.eln.Eln.SVU);
        }
    }

    static final class Saved implements TestCase {
        BusModuleElement m;
        static final int[] CHANNELS = {WHITE, UNUSED, LIME, LIGHT_BLUE, BLACK, UNUSED, CYAN, WHITE, RED};

        public String id() {
            return "bus_module_saved";
        }

        public void build(TestContext c) {
            m = module(c, c.at(2, 1, 1), Direction.XP);
            across(c, c.at(3, 1, 1), BUS);
            for (int i = 0; i < 9; i++) {
                m.setChannel(i, CHANNELS[i]);
                m.setOut(i, i % 2 == 1);
            }
        }

        static String settings(BusModuleElement e) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < 9; i++) s.append(e.channel(i)).append(e.isOut(i) ? ">" : "<").append(' ');
            return s.toString();
        }

        public void measure(TestContext c) {
            String before = settings(m);
            int claims = m.port.claims();
            int want = 1 << WHITE | 1 << LIME | 1 << LIGHT_BLUE | 1 << BLACK | 1 << CYAN | 1 << RED;
            c.check("bus module saved: its channels claimed", claims == want, Integer.toBinaryString(claims));
            Reload r;
            try {
                r = Reload.node(c.level(), c.at(2, 1, 1));
            } catch (ReflectiveOperationException e) {
                c.check("bus module saved: reloads", false, e.toString());
                return;
            }
            var fresh = (BusModuleElement) ((SixNode) r.fresh).sideElementList[Direction.YN.getInt()];
            c.check("bus module saved: channels and directions come back", settings(fresh).equals(before), before + " -> " + settings(fresh));
            c.check("bus module saved: claimed again on load", fresh.port.claims() == claims, Integer.toBinaryString(fresh.port.claims()));
            BusNet.refresh();
            var bus = fresh.port.bus();
            boolean all = bus != null;
            for (int ch = 0; ch < 16 && all; ch++) all = bus.inUse(ch) == ((claims >> ch & 1) != 0);
            c.check("bus module saved: back on its bus, its channels in use", all, bus == null ? "no bus" : "");
        }
    }

    /**
     * Its screen's packet tunes a terminal to a channel; a value that is no channel, too high or negative, leaves it
     * unused; a terminal past the ninth is ignored. Broken, the module's port leaves the bus.
     */
    static final class Tuned implements TestCase {
        BusModuleElement m;

        public String id() {
            return "bus_module_tuned";
        }

        public void build(TestContext c) {
            m = module(c, c.at(2, 1, 1), Direction.XP);
        }

        /** What the screen sends for terminal i, channel ch. */
        void send(TestContext c, int i, int ch) {
            m.networkUnserialize(c.stream(o -> {
                o.writeByte(BusModuleElement.setChannelId);
                o.writeByte(i);
                o.writeByte(ch);
            }));
        }

        public void measure(TestContext c) {
            BusPort other = new BusPort();
            BusNet.link(other, m.port);
            BusNet.refresh();
            send(c, 2, BLUE);
            c.check("bus module tuned: the screen tunes terminal 3 to 11 blue", m.channel(2) == BLUE && m.port.claims() == 1 << BLUE,
                m.channel(2) + " " + m.port.claims());
            c.check("bus module tuned: its bus has the channel in use", m.port.bus() != null && m.port.bus().inUse(BLUE), "" + m.port.bus());
            c.check("bus module tuned: Jade says it by number and name", m.getWaila().getOrDefault("Terminal 3", "").startsWith("11 blue, in"), m.getWaila().toString());
            send(c, 2, 16);
            c.check("bus module tuned: 16 is no channel, unused", m.channel(2) == UNUSED && m.port.claims() == 0, "" + m.channel(2));
            send(c, 4, ORANGE);
            send(c, 4, -3);
            c.check("bus module tuned: a negative value, unused", m.channel(4) == UNUSED, "" + m.channel(4));
            send(c, 9, RED);
            boolean none = true;
            for (int i = 0; i < 9; i++) none &= m.channel(i) == UNUSED;
            c.check("bus module tuned: no tenth terminal", none, "");
            // broken: its port leaves the bus
            m.sixNode.deleteSlot(null, m.slot);
            BusNet.refresh();
            c.check("bus module tuned: broken, its port is off the bus", m.port.bus() == null && other.bus() == null, m.port.bus() + " " + other.bus());
        }
    }

    /**
     * EA brushes clicked on a module (floor, bus edge east) tune the terminal nearest the click: a Red Brush near the
     * back edge's middle tunes terminal 5 to 14 red (EA's brushes number the dyes the other way round), a Light Blue
     * Brush where terminal 4 meets its cable tunes it to 3 light_blue. Paint is used only for a change; a dry brush
     * tunes nothing; a brush click opens no screen.
     */
    static final class Brushed implements TestCase {
        BusModuleElement m;

        public String id() {
            return "bus_module_brushed";
        }

        public void build(TestContext c) {
            m = module(c, c.at(2, 1, 1), Direction.XP);
        }

        static int life(net.minecraft.world.item.ItemStack s) {
            var t = mods.eln.generic.ItemNbt.get(s);
            return t == null ? -1 : t.getIntOr("life", -1);
        }

        public void measure(TestContext c) {
            // its own fake player: the shared one's hand is other tests' too
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(c.level(),
                new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("eln_bus_brush".getBytes()), "eln_bus_brush"));
            var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
            try {
                var red = mods.eln.Eln.findItemStack("Red Brush", 1);
                player.setItemInHand(hand, red);
                // the back edge is west (x 0); its middle terminal's pin root is 5 voxels west of the centre
                boolean used = m.onBlockActivated(player, m.side, 0.15f, 0.05f, 0.5f);
                c.check("bus module brushed: a click with a brush is taken", used, "");
                c.check("bus module brushed: a Red Brush tunes terminal 5 to 14 red", m.channel(BACK_MIDDLE) == RED, "" + m.channel(BACK_MIDDLE));
                int after = life(player.getMainHandItem());
                m.onBlockActivated(player, m.side, 0.15f, 0.05f, 0.5f);
                c.check("bus module brushed: the same colour again uses no paint", life(player.getMainHandItem()) == after, after + " -> " + life(player.getMainHandItem()));
                // terminal 4: where it meets a cable on the west edge (along), a little in from the edge
                int z = along(m, 3);
                player.setItemInHand(hand, mods.eln.Eln.findItemStack("Light Blue Brush", 1));
                m.onBlockActivated(player, m.side, 0.12f, 0.05f, z / 16f);
                c.check("bus module brushed: a Light Blue Brush at terminal 4 tunes it to 3 light_blue", m.channel(3) == LIGHT_BLUE && m.channel(BACK_MIDDLE) == RED,
                    m.channel(3) + " " + m.channel(BACK_MIDDLE) + " at z " + z);
                c.check("bus module brushed: claimed", m.port.claims() == (1 << RED | 1 << LIGHT_BLUE), "" + m.port.claims());
                // a dry brush tunes nothing
                var dry = mods.eln.Eln.findItemStack("Black Brush", 1);
                ((mods.eln.item.BrushDescriptor) mods.eln.item.BrushDescriptor.getDescriptor(dry)).setLife(dry, 0);
                player.setItemInHand(hand, dry);
                m.onBlockActivated(player, m.side, 0.15f, 0.05f, 0.5f);
                c.check("bus module brushed: a dry brush tunes nothing", m.channel(BACK_MIDDLE) == RED, "" + m.channel(BACK_MIDDLE));
            } finally {
                player.setItemInHand(hand, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }
}
