package mods.eln.gametest.cases;

import mods.eln.bus.BusNet;
import mods.eln.bus.BusPort;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.VoxelNode;

import java.util.List;

/**
 * The field bus (mods.eln.bus): Field Bus Cable laid across voxel blocks makes one bus, with no circuit links; a run
 * not touching it is another; breaking the middle splits it. Jade lists a bus cable's channels in use by number and name.
 */
public final class BusCases {
    private BusCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Across());
        cases.add(new KitOwnTier());
    }

    static final int BUS = CablePart.BUS, DOWN = 0, WEST = 4, EAST = 5;

    static VoxelNode node(TestContext c, int x, int y, int z) {
        return (VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(x, y, z), c.level()));
    }

    /** The bus of a voxel block's first bus port, or null. */
    static mods.eln.bus.Bus bus(VoxelNode v) {
        List<BusPort> ps = v.busPorts();
        return ps.isEmpty() ? null : ps.get(0).bus();
    }

    /** Three blocks of bus cable in a row (x 1-3), a fourth apart (x 5). */
    static final class Across implements TestCase {
        public String id() {
            return "bus_cable_across_blocks";
        }

        public int ticks() {
            return 10;
        }

        public void build(TestContext c) {
            for (int x = 1; x <= 3; x++) CableCases.voxel(c, c.at(x, 1, 1), CablePart.arms(BUS, DOWN, WEST, EAST));
            CableCases.voxel(c, c.at(5, 1, 1), CablePart.arms(BUS, DOWN, WEST, EAST));
        }

        public void measure(TestContext c) {
            VoxelNode a = node(c, 1, 1, 1), b = node(c, 2, 1, 1), d = node(c, 3, 1, 1), far = node(c, 5, 1, 1);
            c.check("bus: three blocks of bus cable are one bus", bus(a) != null && bus(a) == bus(b) && bus(b) == bus(d),
                bus(a) + " " + bus(b) + " " + bus(d));
            c.check("bus: a run apart is on no bus of theirs", bus(far) != bus(a), "" + bus(far));
            c.check("bus: no circuit links between bus cables", a.nodeConnectionList.isEmpty() && b.nodeConnectionList.isEmpty(),
                a.nodeConnectionList.size() + " " + b.nodeConnectionList.size());
            var w = b.getWaila(true);
            c.check("bus: Jade names the cable", w.values().stream().anyMatch(s -> s.startsWith("Field Bus Cable")), w.toString());
            // a device's port on it claiming 3 light_blue: Jade lists that channel, by number and name, and no other
            BusPort dev = new BusPort();
            dev.claim(1 << 3);
            BusNet.link(dev, b.busPorts().get(0));
            BusNet.refresh();
            w = b.getWaila(true);
            c.check("bus: Jade lists a channel in use by number and name", w.containsKey("  3 light_blue") && w.get("  3 light_blue").contains("V"), w.toString());
            c.check("bus: and only the channels in use", w.keySet().stream().filter(k -> k.startsWith("  ")).count() == 1, w.toString());
            BusNet.detach(List.of(dev));
            // the middle block gone: the ends are apart
            b.disconnect();
            BusNet.refresh();
            c.check("bus: the middle gone, the ends are apart", bus(a) == null || bus(a) != bus(d), bus(a) + " " + bus(d));
            b.connect();
            BusNet.refresh();
            c.check("bus: back again, one bus", bus(a) != null && bus(a) == bus(d), bus(a) + " " + bus(d));
        }
    }

    /** A bus kit clicked on a signal cable crossing a bus cable's block starts on the bus cable, not as a branch of the
     *  signal one (AdventurAgent: it began a separate bus there and said "Laid"). */
    static final class KitOwnTier implements TestCase {
        public String id() {
            return "bus_kit_starts_on_its_own_tier";
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext c) {
            CableCases.voxel(c, c.at(1, 1, 1), List.of(new CablePart(BUS, DOWN, new int[]{0, 0, 4, 16, 0, 4})),
                List.of(new CablePart(CablePart.tierOf("Signal Cable"), DOWN, new int[]{12, 0, 8, 12, 0, 16})));
        }

        public void measure(TestContext c) {
            var p = c.at(1, 1, 1);
            var hit = new net.minecraft.world.phys.Vec3(p.getX() + 12.5 / 16, p.getY() + 0.5 / 16, p.getZ() + 12 / 16.0);
            long[][] join = new long[1][];
            var e = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), p, net.minecraft.core.Direction.UP, hit, true, join, BUS);
            c.check("bus kit: starts on the bus cable, not the signal cable it was clicked on",
                e != null && e.z() == p.getZ() * 16L + 4 && join[0] != null, String.valueOf(e));
            var s = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), p, net.minecraft.core.Direction.UP, hit, true, new long[1][],
                CablePart.tierOf("Signal Cable"));
            c.check("signal kit: still branches off the signal cable there", s != null && s.x() == p.getX() * 16L + 12, String.valueOf(s));
        }
    }
}
