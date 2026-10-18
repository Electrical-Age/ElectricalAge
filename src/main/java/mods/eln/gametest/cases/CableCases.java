package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.PipeKitItem;
import mods.eln.sixnode.resistor.ResistorElement;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.gametest.cases.Wp9aCases.GROUND;
import static mods.eln.gametest.cases.Wp9aCases.LV_CABLE;
import static mods.eln.gametest.cases.Wp9aCases.POWER_RESISTOR;
import static mods.eln.gametest.cases.Wp9aCases.SOURCE;
import static mods.eln.gametest.cases.Wp9aCases.current;
import static mods.eln.gametest.cases.Wp9aCases.resistor;
import static mods.eln.gametest.cases.Wp9aCases.source;

/**
 * Voxel cables against six-node cables (notes/cables/PLAN.md step 1): each case builds a circuit twice, once with
 * six-node Low Voltage Cables and once with voxel cable pieces laid as the converter would lay them (CablePart.arms:
 * arms from each element's face centre to its connected edges, knuckles round outer corners), between the same
 * six-node devices (Electrical Source 50 V, Power Resistor about 1 ohm so the cables' share is visible, Ground Cable).
 * The resistor currents must agree to 0.1%.
 */
public final class CableCases {
    private CableCases() {
    }

    static final int LV = CablePart.tierOf("Low Voltage Cable");
    static final int DOWN = 0, UP = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;
    /** 24 core items: 0.01 * 10^(24/12) = 1 ohm. */
    static final int CORE = 24;

    public static void addTo(List<TestCase> cases) {
        cases.add(new BurnCase());
        List<TestCase> mine = new ArrayList<>();
        addLayouts(mine);
        cases.addAll(mine);
        // the same circuits built from six-node cables and converted (CableConverter)
        for (TestCase t : mine) if (t instanceof Case k) cases.add(new ConvertCase(k.name.replace("voxel cable", "converted cable"), k.layout));
    }

    static void addLayouts(List<TestCase> cases) {
        cases.add(new Case("voxel cable straight", (c, six, z) -> {
            source(c, SOURCE, 0, z, 50);
            for (int x = 1; x <= 6; x++) {
                if (six) c.placeSix(LV_CABLE, c.at(x, 1, z));
                else voxel(c, c.at(x, 1, z), CablePart.arms(LV, DOWN, WEST, EAST));
            }
            return List.of(end(c, 7, z));
        }, (c, z0, z1) -> {
            // twice its rating, the cable warms alike in both
            var six = (mods.eln.sixnode.electricalcable.ElectricalCableElement) ((mods.eln.node.six.SixNode) mods.eln.node.NodeManager.instance
                .getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(3, 1, z0), c.level()))).getElement(mods.eln.misc.Direction.YN);
            double ts = ((mods.eln.sim.ThermalLoad) mods.eln.gametest.LabCommands.field(six, "thermalLoad")).Tc;
            var vox = (mods.eln.node.voxel.VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(3, 1, z1), c.level()));
            double tv = vox.cableStates().get(0)[2];
            c.line(String.format("voxel cable straight: cable at x 3 warmed by %.3f C (six-node), %.3f C (voxel)", ts, tv));
            c.check("voxel cable straight: warms like a six-node cable (1%)", ts > 0.1 && Math.abs(ts - tv) <= 0.01 * ts, String.format("six-node %.3f, voxel %.3f", ts, tv));
        }));
        cases.add(new Case("voxel cable inner corners", (c, six, z) -> {
            // floor, up onto a wall to the south, along it, back down to the floor
            for (int x = 2; x <= 5; x++) c.floor(c.at(x, 1, z + 1));
            source(c, SOURCE, 0, z, 50);
            if (six) {
                for (int x : new int[]{1, 2, 5, 6}) c.placeSix(LV_CABLE, c.at(x, 1, z));
                for (int x = 2; x <= 5; x++) c.placeSix(LV_CABLE, c.at(x, 1, z), mods.eln.misc.Direction.ZP);
            } else {
                voxel(c, c.at(1, 1, z), CablePart.arms(LV, DOWN, WEST, EAST));
                voxel(c, c.at(2, 1, z), CablePart.arms(LV, DOWN, WEST, SOUTH), CablePart.arms(LV, SOUTH, EAST, DOWN));
                voxel(c, c.at(3, 1, z), CablePart.arms(LV, SOUTH, WEST, EAST));
                voxel(c, c.at(4, 1, z), CablePart.arms(LV, SOUTH, WEST, EAST));
                voxel(c, c.at(5, 1, z), CablePart.arms(LV, SOUTH, WEST, DOWN), CablePart.arms(LV, DOWN, SOUTH, EAST));
                voxel(c, c.at(6, 1, z), CablePart.arms(LV, DOWN, WEST, EAST));
            }
            return List.of(end(c, 7, z));
        }));
        cases.add(new Case("voxel cable outer corners", (c, six, z) -> {
            // over a one-block bump at x = 3
            c.floor(c.at(3, 1, z));
            source(c, SOURCE, 0, z, 50);
            if (six) {
                for (int x : new int[]{1, 2, 4, 5}) c.placeSix(LV_CABLE, c.at(x, 1, z));
                c.placeSix(LV_CABLE, c.at(2, 1, z), mods.eln.misc.Direction.XP);
                c.placeSix(LV_CABLE, c.at(3, 2, z));
                c.placeSix(LV_CABLE, c.at(4, 1, z), mods.eln.misc.Direction.XN);
            } else {
                voxel(c, c.at(1, 1, z), CablePart.arms(LV, DOWN, WEST, EAST));
                voxel(c, c.at(2, 1, z), CablePart.arms(LV, DOWN, WEST, EAST), CablePart.arms(LV, EAST, DOWN, UP));
                voxel(c, c.at(2, 2, z), List.of(CablePart.knuckle(LV, 16, 0, 8)));
                voxel(c, c.at(3, 2, z), CablePart.arms(LV, DOWN, WEST, EAST));
                voxel(c, c.at(4, 2, z), List.of(CablePart.knuckle(LV, 0, 0, 8)));
                voxel(c, c.at(4, 1, z), CablePart.arms(LV, WEST, UP, DOWN), CablePart.arms(LV, DOWN, WEST, EAST));
                voxel(c, c.at(5, 1, z), CablePart.arms(LV, DOWN, WEST, EAST));
            }
            return List.of(end(c, 6, z));
        }));
        cases.add(new Case("voxel cable tee", (c, six, z) -> {
            // a T at x = 3: on to a load at x = 5, and round by z + 2 to a second load
            source(c, SOURCE, 0, z, 50);
            if (six) {
                for (int x = 1; x <= 4; x++) c.placeSix(LV_CABLE, c.at(x, 1, z));
                c.placeSix(LV_CABLE, c.at(3, 1, z + 1));
                c.placeSix(LV_CABLE, c.at(3, 1, z + 2));
                c.placeSix(LV_CABLE, c.at(4, 1, z + 2));
            } else {
                voxel(c, c.at(1, 1, z), CablePart.arms(LV, DOWN, WEST, EAST));
                voxel(c, c.at(2, 1, z), CablePart.arms(LV, DOWN, WEST, EAST));
                voxel(c, c.at(3, 1, z), CablePart.arms(LV, DOWN, WEST, EAST, SOUTH));
                voxel(c, c.at(4, 1, z), CablePart.arms(LV, DOWN, WEST, EAST));
                voxel(c, c.at(3, 1, z + 1), CablePart.arms(LV, DOWN, NORTH, SOUTH));
                voxel(c, c.at(3, 1, z + 2), CablePart.arms(LV, DOWN, NORTH, EAST));
                voxel(c, c.at(4, 1, z + 2), CablePart.arms(LV, DOWN, WEST, EAST));
            }
            return List.of(end(c, 5, z), end(c, 5, z + 2));
        }));
    }

    /** A short through 6 cables: one burns (the weakest link, as BurnChainCases), in voxel cables too. */
    static final class BurnCase implements TestCase {
        public String name() {
            return "voxel cable burn chain";
        }

        public int width() {
            return 9;
        }

        public int ticks() {
            return 1200;
        }

        public void build(TestContext c) {
            source(c, SOURCE, 0, 1, 60);
            for (int x = 1; x <= 6; x++) voxel(c, c.at(x, 1, 1), CablePart.arms(LV, DOWN, WEST, EAST));
            c.placeSix(GROUND, c.at(7, 1, 1));
        }

        public void measure(TestContext c) {
            StringBuilder s = new StringBuilder();
            int burnt = 0;
            for (int x = 1; x <= 6; x++) {
                var n = (mods.eln.node.voxel.VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(x, 1, 1), c.level()));
                boolean b = n.cableBurnt();
                if (b) burnt++;
                s.append(b ? 'X' : '-');
            }
            c.check("voxel cable burn chain: exactly one cable of 6 burns", burnt == 1, "cables (source side first): " + s);
        }
    }

    /** Six-node circuits in both rows; the second row's cable blocks are converted at once. */
    static final class ConvertCase implements TestCase {
        final String name;
        final Layout layout;
        List<ResistorElement> six, conv;
        int converted;
        final List<String> skipped = new ArrayList<>();

        ConvertCase(String name, Layout layout) {
            this.name = name;
            this.layout = layout;
        }

        public String name() {
            return name;
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public int floorDepth() {
            return 10;
        }

        public int ticks() {
            return 60;
        }

        public void build(TestContext c) {
            six = layout.build(c, true, 1);
            conv = layout.build(c, true, 6);
            for (int x = 0; x < 10; x++)
                for (int y = 1; y < 4; y++)
                    for (int z = 6; z < 10; z++) {
                        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(x, y, z), c.level()));
                        if (!(n instanceof mods.eln.node.six.SixNode s)) continue;
                        String why = mods.eln.node.voxel.CableConverter.convert(s);
                        if (why == null) converted++;
                        else skipped.add(x + "," + y + "," + z + ": " + why);
                    }
        }

        public void measure(TestContext c) {
            c.line(name + ": converted " + converted + " blocks; kept " + String.join("; ", skipped));
            c.check(name + ": cable blocks converted", converted > 0, converted + "");
            for (String k : skipped)
                c.check(name + ": only devices kept as six-node", k.contains("holds a "), k);
            for (int i = 0; i < six.size(); i++) {
                double a = current(six.get(i)), b = current(conv.get(i));
                c.line(String.format("%s: load %d: six-node %.5f A, converted %.5f A", name, i, a, b));
                c.check(name + ": load " + i + ": converted current = six-node current (0.1%)", Math.abs(a) > 1 && Math.abs(a - b) <= 1e-3 * Math.abs(a),
                    String.format("six-node %.5f A, converted %.5f A", a, b));
            }
        }
    }

    /** A Power Resistor at x and a Ground Cable after it. */
    static ResistorElement end(TestContext c, int x, int z) {
        ResistorElement r = resistor(c, POWER_RESISTOR, x, z, CORE);
        c.placeSix(GROUND, c.at(x + 1, 1, z));
        return r;
    }

    @SafeVarargs
    static void voxel(TestContext c, BlockPos p, List<CablePart>... groups) {
        for (List<CablePart> g : groups)
            for (CablePart part : g) {
                String err = PipeKitItem.place(c.level(), p, part, null);
                if (err != null) throw new IllegalStateException("voxel cable at " + p + ": " + err);
            }
    }

    interface Layout {
        List<ResistorElement> build(TestContext c, boolean six, int z);
    }

    interface Extra {
        void check(TestContext c, int zSix, int zVoxel);
    }

    static final class Case implements TestCase {
        final String name;
        final Layout layout;
        final Extra extra;
        List<ResistorElement> six = new ArrayList<>(), vox = new ArrayList<>();

        Case(String name, Layout layout) {
            this(name, layout, null);
        }

        Case(String name, Layout layout, Extra extra) {
            this.name = name;
            this.layout = layout;
            this.extra = extra;
        }

        public String name() {
            return name;
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public int floorDepth() {
            return 10;
        }

        public int ticks() {
            return 60;
        }

        public void build(TestContext c) {
            six = layout.build(c, true, 1);
            vox = layout.build(c, false, 6);
        }

        public void measure(TestContext c) {
            for (int i = 0; i < six.size(); i++) {
                double a = current(six.get(i)), b = current(vox.get(i));
                c.line(String.format("%s: load %d: six-node %.5f A, voxel %.5f A", name, i, a, b));
                c.check(name + ": load " + i + " carries current", Math.abs(a) > 1, String.format("%.4f A", a));
                c.check(name + ": load " + i + ": voxel current = six-node current (0.1%)", Math.abs(a - b) <= 1e-3 * Math.abs(a),
                    String.format("six-node %.5f A, voxel %.5f A", a, b));
            }
            if (extra != null) extra.check(c, 1, 6);
        }
    }
}
