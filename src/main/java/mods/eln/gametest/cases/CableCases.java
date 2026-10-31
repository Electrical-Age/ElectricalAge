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
        cases.add(new RoutedCase());
        cases.add(new JoinedRouteCase());
        cases.add(new DeviceEndCase());
        cases.add(new WaypointEndCase());
        cases.add(new PreviewSnapshotCase());
        cases.add(new HostedCase());
        cases.add(new FootprintCase());
        cases.add(new SlotsCase());
        cases.add(new ChipsPackedCase());
        cases.add(new CableToChipCase());
        cases.add(new PinsAcrossCase());
        cases.add(new SmallDevicesAnywhereCase());
        cases.add(new BreakMixedCase());
        cases.add(new ThermalCase());
        cases.add(new MimicCase());
        cases.add(new StairsCase());
        cases.add(new ChiselCase());
        cases.add(new ChiselShapesCase());
        cases.add(new FenceJoinCase());
        // mixed blocks (a device and a cable in one block) and a device round an outer corner, converted
        Layout mixed = (c, six, z) -> {
            // source -> 2 cables -> resistor -> a block with a floor cable and a Ground Cable on its wall (inner corner)
            c.floor(c.at(4, 1, z + 1));
            source(c, SOURCE, 0, z, 50);
            for (int x = 1; x <= 2; x++) c.placeSix(LV_CABLE, c.at(x, 1, z));
            ResistorElement r = resistor(c, POWER_RESISTOR, 3, z, CORE);
            c.placeSix(LV_CABLE, c.at(4, 1, z));
            c.placeSix(GROUND, c.at(4, 1, z), mods.eln.misc.Direction.ZP);
            return List.of(r);
        };
        cases.add(new ConvertCase("converted mixed block", mixed, false));
        cases.add(new ConvertCase("reverted mixed block", mixed, true));
        Layout deviceCorner = (c, six, z) -> {
            // ... -> resistor -> floor cable up the side of a bump -> round its top edge -> a Ground Cable on top
            c.floor(c.at(5, 1, z));
            source(c, SOURCE, 0, z, 50);
            for (int x = 1; x <= 2; x++) c.placeSix(LV_CABLE, c.at(x, 1, z));
            ResistorElement r = resistor(c, POWER_RESISTOR, 3, z, CORE);
            c.placeSix(LV_CABLE, c.at(4, 1, z));
            c.placeSix(LV_CABLE, c.at(4, 1, z), mods.eln.misc.Direction.XP);
            c.placeSix(GROUND, c.at(5, 2, z));
            return List.of(r);
        };
        cases.add(new ConvertCase("converted device round a corner", deviceCorner, false));
        Layout wallCorners = (c, six, z) -> {
            // Incognito's spawn lamp (2026-10-18: dark after a live conversion): a device on a wall fed round two outer
            // corners through one air block: source on a wall -> a wall cable -> up round the top edge of a block onto
            // its top -> along it onto its side -> round the wall's edge to the device; a ground beyond the device
            for (int[] b : new int[][]{{6, 1, z + 1}, {5, 1, z + 1}, {4, 2, z + 1}, {3, 2, z + 1}}) c.floor(c.at(b[0], b[1], b[2]));
            mods.eln.misc.Direction wall = mods.eln.misc.Direction.ZP;
            var src = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) c.placeSix(SOURCE, c.at(6, 1, z), wall);
            src.networkUnserialize(c.stream(out -> {
                out.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                out.writeFloat(50);
            }));
            c.placeSix(LV_CABLE, c.at(5, 1, z), wall);                                       // C: on the block's -z face
            c.placeSix(LV_CABLE, c.at(5, 2, z + 1), mods.eln.misc.Direction.YN);              // B: on its top
            c.placeSix(LV_CABLE, c.at(5, 2, z + 1), mods.eln.misc.Direction.XN);              // B: on the wall's +x face
            ResistorElement r = (ResistorElement) c.placeSix(POWER_RESISTOR, c.at(4, 2, z), wall);   // L, by air at (5, 2, z)
            Wp9aCases.orient(r, mods.eln.misc.LRDU::right, mods.eln.misc.Direction.XN);
            r.getInventory().setItem(0, Wp9aCases.coal(CORE));
            r.getInventory().setChanged();
            c.placeSix(GROUND, c.at(3, 2, z), wall);
            return List.of(r);
        };
        cases.add(new ConvertCase("converted wall device round two corners", wallCorners, false));
        cases.add(new ConvertCase("converted wall device round two corners, backwards", wallCorners, false, true));
        // the three blocks round the air block in every order (the live conversion went by the node list's order)
        int[][] perms = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};
        for (int[] pm : perms) cases.add(new ConvertCase("converted wall device round two corners, order " + pm[0] + pm[1] + pm[2], wallCorners, false, false, pm));
        Layout wet = (c, six, z) -> {
            // a cable and the resistor under water (waterlogged six-node blocks): they convert and stay wet
            source(c, SOURCE, 0, z, 50);
            for (int x = 1; x <= 6; x++) {
                if (x >= 3) c.level().setBlock(c.at(x, 1, z), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
                if (x <= 5) c.placeSix(LV_CABLE, c.at(x, 1, z));
            }
            return List.of(end(c, 6, z));
        };
        cases.add(new ConvertCase("converted cable under water", wet, false));
        cases.add(new Case("hosted device between voxel cables", (c, six, z) -> {
            // source -> 3 cables -> resistor -> a cable -> ground: six-node row, and a voxel row where the resistor
            // sits in a voxel block (its edges meet voxel cable ports across the faces)
            source(c, SOURCE, 0, z, 50);
            for (int x = 1; x <= 3; x++) {
                if (six) c.placeSix(LV_CABLE, c.at(x, 1, z));
                else voxel(c, c.at(x, 1, z), CablePart.arms(LV, DOWN, WEST, EAST));
            }
            ResistorElement r;
            if (six) r = resistor(c, POWER_RESISTOR, 4, z, CORE);
            else {
                r = (ResistorElement) host(c, POWER_RESISTOR, c.at(4, 1, z), mods.eln.misc.Direction.YN);
                Wp9aCases.orient(r, mods.eln.misc.LRDU::right, mods.eln.misc.Direction.XN);
                r.getInventory().setItem(0, Wp9aCases.coal(CORE));
                r.getInventory().setChanged();
            }
            if (six) c.placeSix(LV_CABLE, c.at(5, 1, z));
            else voxel(c, c.at(5, 1, z), CablePart.arms(LV, DOWN, WEST, EAST));
            c.placeSix(GROUND, c.at(6, 1, z));
            return List.of(r);
        }));
        cases.add(new LampCornerCase(0));
        cases.add(new LampCornerCase(1));
        cases.add(new LampCornerCase(2));
        cases.add(new KitCase());
        List<TestCase> mine = new ArrayList<>();
        addLayouts(mine);
        cases.addAll(mine);
        // the same circuits built from six-node cables and converted (CableConverter)
        for (TestCase t : mine) if (t instanceof Case k) cases.add(new ConvertCase(k.name.replace("voxel cable", "converted cable"), k.layout, false));
        // and converted, then reverted: six-node again, no knuckles left
        for (TestCase t : mine) if (t instanceof Case k) cases.add(new ConvertCase(k.name.replace("voxel cable", "reverted cable"), k.layout, true));
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

        final boolean revert, backwards;
        int voxelLeft = -1;

        ConvertCase(String name, Layout layout, boolean revert) {
            this(name, layout, revert, false);
        }

        /** backwards: convert from the far end (x, y, z descending): the order blocks convert in changes the links. */
        /** order: the device block (0), the block with the two cables (1) and the wall cable below (2) first, in this
         *  order (wallCorners), then the rest. */
        int[] order;

        ConvertCase(String name, Layout layout, boolean revert, boolean backwards, int[] order) {
            this(name, layout, revert, backwards);
            this.order = order;
        }

        ConvertCase(String name, Layout layout, boolean revert, boolean backwards) {
            this.name = name;
            this.layout = layout;
            this.revert = revert;
            this.backwards = backwards;
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
            if (order != null) {
                int[][] key = {{4, 2, 6}, {5, 2, 7}, {5, 1, 6}};
                for (int o : order) {
                    var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(key[o][0], key[o][1], key[o][2]), c.level()));
                    if (n instanceof mods.eln.node.six.SixNode s && !(n instanceof mods.eln.node.voxel.VoxelNode)) {
                        String why = mods.eln.node.voxel.CableConverter.convert(s);
                        if (why == null) converted++;
                        else skipped.add(java.util.Arrays.toString(key[o]) + ": " + why);
                    }
                }
            }
            for (int i = 0; i < 10; i++)
                for (int j = 1; j < 4; j++)
                    for (int k = 6; k < 10; k++) {
                        int x = backwards ? 9 - i : i, y = backwards ? 4 - j : j, z = backwards ? 15 - k : k;
                        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(x, y, z), c.level()));
                        if (!(n instanceof mods.eln.node.six.SixNode s) || n instanceof mods.eln.node.voxel.VoxelNode) continue;
                        String why = mods.eln.node.voxel.CableConverter.convert(s);
                        if (why == null) converted++;
                        else skipped.add(x + "," + y + "," + z + ": " + why);
                    }
            if (!revert) return;
            List<mods.eln.node.voxel.VoxelNode> vs = new ArrayList<>();
            for (int x = 0; x < 10; x++)
                for (int y = 1; y < 4; y++)
                    for (int z = 6; z < 10; z++)
                        if (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(x, y, z), c.level())) instanceof mods.eln.node.voxel.VoxelNode v)
                            vs.add(v);
            for (var v : vs) mods.eln.node.voxel.CableConverter.revert(v);
            for (var v : vs) if (v.legacy == null) mods.eln.node.voxel.CableConverter.revertKnuckles(v);
            voxelLeft = 0;
            for (int x = 0; x < 10; x++)
                for (int y = 1; y < 4; y++)
                    for (int z = 6; z < 10; z++)
                        if (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(x, y, z), c.level())) instanceof mods.eln.node.voxel.VoxelNode)
                            voxelLeft++;
        }

        public void measure(TestContext c) {
            c.line(name + ": converted " + converted + " blocks; kept " + String.join("; ", skipped));
            c.check(name + ": blocks converted", converted > 0, converted + "");
            // (the devices were rebuilt in their voxel blocks: their elements now)
            List<ResistorElement> now = new ArrayList<>();
            for (ResistorElement r : conv) {
                var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(r.sixNode.coordonate);
                now.add(n instanceof mods.eln.node.six.SixNode s6 && s6.getElement(r.side) instanceof ResistorElement re ? re : r);
            }
            conv = now;
            if (revert) c.check(name + ": no voxel node left after the revert", voxelLeft == 0, voxelLeft + "");
            c.check(name + ": every six-node block converted (devices too)", skipped.isEmpty(), String.join("; ", skipped));
            for (int x = 0; x < 10; x++) {
                var st = c.level().getBlockState(c.at(x, 1, 6));
                if (st.getBlock() instanceof mods.eln.node.voxel.VoxelNodeBlock && c.level().getBlockState(c.at(x, 1, 1)).getFluidState().isSource())
                    c.check(name + ": a wet block stays wet at x " + x, st.getValue(mods.eln.node.voxel.VoxelNodeBlock.WATERLOGGED), st.toString());
            }
            for (int i = 0; i < six.size(); i++) {
                double a = current(six.get(i)), b = current(conv.get(i));
                c.line(String.format("%s: load %d: six-node %.5f A, converted %.5f A", name, i, a, b));
                c.check(name + ": load " + i + ": converted current = six-node current (0.1%)", Math.abs(a) > 1 && Math.abs(a - b) <= 1e-3 * Math.abs(a),
                    String.format("six-node %.5f A, converted %.5f A", a, b));
            }
        }
    }

    /**
     * A cable kit's route (CableRouter, CablePlacer): from the source's east edge to the resistor's west edge, past a
     * wall on a one-block-wide floor strip: over the wall, or round the strip's side or underside (outer corners,
     * knuckles), never through the air; the circuit conducts.
     */
    static final class RoutedCase implements TestCase {
        ResistorElement r;
        String err;
        int pieces;
        final StringBuilder where = new StringBuilder();

        public String name() {
            return "routed cable over a wall";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 3;
        }

        public int ticks() {
            return 60;
        }

        public void build(TestContext c) {
            source(c, SOURCE, 0, 1, 50);
            r = end(c, 7, 1);
            c.floor(c.at(4, 1, 1));
            c.floor(c.at(4, 2, 1));
            // (the floor strip's sides open: clear the rows beside it so the only way is over the wall)
            for (int x = 0; x < 10; x++) {
                c.level().setBlock(c.at(x, 0, 0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                c.level().setBlock(c.at(x, 0, 2), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
            var hitS = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(0, 1, 1)).add(1.0, 0.05, 0.5);
            var hitR = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(7, 1, 1)).add(0.0, 0.05, 0.5);
            long[][] j = new long[1][];
            var a = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(0, 1, 1), net.minecraft.core.Direction.EAST, hitS, true, j);
            var b = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(7, 1, 1), net.minecraft.core.Direction.WEST, hitR, false, j);
            err = mods.eln.node.voxel.CablePlacer.routeAndLayForTests(c.level(), LV, a, b);
            for (int x = 1; x < 7; x++)
                for (int y = 0; y < 4; y++)
                    for (int z = 0; z < 3; z++)
                        if (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(x, y, z), c.level())) instanceof mods.eln.node.voxel.VoxelNode v) {
                            pieces += v.parts.size();
                            where.append(x).append(',').append(y).append(',').append(z).append(' ');
                        }
        }

        public void measure(TestContext c) {
            c.check("routed cable: laid", err == null, String.valueOf(err));
            c.line("routed cable: pieces in blocks " + where);
            c.check("routed cable: round the wall (8+ pieces: corners, knuckles)", pieces >= 8, pieces + " pieces");
            double i = current(r);
            c.line(String.format("routed cable: %.4f A through the resistor", i));
            c.check("routed cable: the circuit conducts", Math.abs(i) > 1, String.format("%.4f A", i));
        }
    }

    /**
     * A cable routed to a device ends at the middle of the clicked edge, however far off it the click was (Baughn: snap
     * to the connector's centre); a floor cable ending at a terminal plate gets a lug in its mesh (Baughn: the steam
     * turbine showed a bare cable end).
     */
    static final class DeviceEndCase implements TestCase {
        ResistorElement r;
        String err;
        String endAt = "none";
        int plain, lugged;
        String ends = "", snapInfo = "";
        boolean snapOk;
        boolean endsOk = true;
        boolean trimOk;
        String trimEnds = "";

        public String name() {
            return "a cable to a device ends at its connector, with a lug";
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

        public int ticks() {
            return 60;
        }

        public void build(TestContext c) {
            source(c, SOURCE, 0, 1, 50);
            r = end(c, 7, 1);
            var hitS = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(0, 1, 1)).add(1.0, 0.05, 0.5);
            var hitR = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(7, 1, 1)).add(0.0, 0.05, 0.2);   // (off-centre)
            long[][] j = new long[1][];
            var a = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(0, 1, 1), net.minecraft.core.Direction.EAST, hitS, true, j);
            var b = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(7, 1, 1), net.minecraft.core.Direction.WEST, hitR, false, j);
            err = mods.eln.node.voxel.CablePlacer.routeAndLayForTests(c.level(), LV, a, b);
            // a click on the floor half a block from the device's port snaps to it (AdventurAgent, Baughn: never looks
            // connected without being so)
            var floorHit = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(6, 0, 1)).add(0.8, 1.0, 0.5);
            var snapped = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(6, 0, 1), net.minecraft.core.Direction.UP, floorHit, false, new long[1][]);
            snapInfo = snapped == null ? "none" : snapped + "";
            snapOk = snapped != null && snapped.dir() >= 0 && snapped.x() == b.x() && snapped.z() == b.z();
            // and a click in the middle of the floor cell beside a six-node source (in-world, hosted in a VoxelNode)
            // snaps to the source's port, the same end as clicking the source
            boolean conv = mods.eln.node.voxel.CableConverter.convertOnLoad;
            c.floor(c.at(0, 0, 6));
            c.floor(c.at(1, 0, 6));
            mods.eln.node.voxel.CableConverter.convertOnLoad = true;
            try {
                source(c, SOURCE, 0, 6, 50);
            } finally {
                mods.eln.node.voxel.CableConverter.convertOnLoad = conv;
            }
            var hitV = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(0, 1, 6)).add(0.65, 0.1, 0.5);   // (on its model, east of its middle)
            var viaSource = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(0, 1, 6), net.minecraft.core.Direction.UP, hitV, true, new long[1][]);
            var midV = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(1, 0, 6)).add(0.5, 1.0, 0.5);
            var sixV = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(1, 0, 6), net.minecraft.core.Direction.UP, midV, true, new long[1][]);
            snapInfo += " / voxel " + (sixV == null ? "none" : sixV) + " vs " + viaSource;
            snapOk &= viaSource != null && viaSource.equals(sixV);
            snapOk &= mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(0, 1, 6), c.level())) instanceof mods.eln.node.voxel.VoxelNode;
            var midHit = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(1, 0, 1)).add(0.5, 1.0, 0.5);
            var six = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(1, 0, 1), net.minecraft.core.Direction.UP, midHit, true, new long[1][]);
            snapInfo += " / six " + (six == null ? "none" : six) + " vs " + a;
            snapOk &= a.equals(six);
            // a hosted Signal Trimmer has its connector on its front only: a click near any side of its model ends there
            // (it used to take the nearest edge, which had no connector)
            c.floor(c.at(4, 0, 7));
            var tp = c.at(4, 1, 7);
            var trim = host(c, Wp9cCases.SIGNAL_TRIMMER, tp, mods.eln.misc.Direction.YN);
            Wp9aCases.orient(trim, x -> x, mods.eln.misc.Direction.XP);
            trimOk = true;
            for (double[] h : new double[][]{{0.35, 0.5}, {0.65, 0.5}, {0.5, 0.35}, {0.5, 0.65}}) {
                var hit = net.minecraft.world.phys.Vec3.atLowerCornerOf(tp).add(h[0], 0.1, h[1]);
                var e = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), tp, net.minecraft.core.Direction.UP, hit, true, new long[1][]);
                trimEnds += (e == null ? "none" : e.x() - tp.getX() * 16L + "," + (e.z() - tp.getZ() * 16L)) + " ";
                trimOk &= e != null && e.x() == tp.getX() * 16L + 16 && e.z() == tp.getZ() * 16L + 8;
            }
            // a 50V steam turbine: ends on its front and back (its terminal plates) and its left flank (the control signal's)
            var tb = c.placeTransparent(12 + (82 << 6), c.at(4, 1, 2));
            for (var d : new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH,
                net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST}) {
                var hit = net.minecraft.world.phys.Vec3.atCenterOf(c.at(4, 1, 2)).add(d.getStepX() * 0.5, -0.45, d.getStepZ() * 0.5);
                boolean got = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(4, 1, 2), d, hit, false, new long[1][]) != null;
                var ed = mods.eln.misc.Direction.fromEnumFacing(d);
                boolean want = ed == tb.front || ed == tb.front.back() || ed == tb.front.left();   // (left: its control signal)
                ends += d + (got ? "+ " : "- ");
                endsOk &= got == want;
            }
            if (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(6, 1, 1), c.level())) instanceof mods.eln.node.voxel.VoxelNode v)
                for (var part : v.parts)
                    if (part instanceof mods.eln.node.voxel.CablePart cp)
                        for (var port : cp.cablePorts())
                            if (port.face() == 5) {
                                endAt = port.x() + "," + port.y() + "," + port.z();
                                float[] lugs = new float[4];
                                lugs[5 - 2] = mods.eln.cable.CableTerminals.CONDUCTOR_HEIGHT_PX;
                                plain = mods.eln.node.voxel.VoxelGeometry.of(List.of(cp)).size();
                                lugged = mods.eln.node.voxel.VoxelGeometry.of(List.of(cp), lugs).size();
                            }
        }

        public void measure(TestContext c) {
            c.check("device end: laid", err == null, String.valueOf(err));
            c.check("device end: the cable meets the device at the middle of the edge (16,0,8)", "16,0,8".equals(endAt), endAt);
            c.check("device end: a lug at a terminal plate (3 more boxes)", lugged == plain + 18, plain + " -> " + lugged + " quads");
            c.check("device end: a turbine takes cables on its front, back and left (signal) only", endsOk, ends);
            c.check("device end: a click anywhere on a hosted trimmer ends at its front connector (16,8)", trimOk, trimEnds);
            c.check("device end: a floor click beside the device's port snaps to it", snapOk, snapInfo);
            double i = current(r);
            c.check("device end: the circuit conducts", Math.abs(i) > 1, String.format("%.4f A", i));
        }
    }

    /**
     * A run from one floor point to another (a waypoint clicked twice) has a piece in every block it crosses, and ends at
     * the clicked point: half-way into the last block.
     */
    static final class WaypointEndCase implements TestCase {
        String err;
        final StringBuilder where = new StringBuilder();
        boolean last;

        public String name() {
            return "a run ended at a waypoint reaches it";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 4;
        }

        public int depth() {
            return 3;
        }

        public int ticks() {
            return 10;
        }

        public void build(TestContext c) {
            var hitA = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(1, 0, 1)).add(0.5, 1.0, 0.5);
            var hitB = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(5, 0, 1)).add(0.5, 1.0, 0.5);
            long[][] j = new long[1][];
            var a = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(1, 0, 1), net.minecraft.core.Direction.UP, hitA, true, j);
            var b = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), c.at(5, 0, 1), net.minecraft.core.Direction.UP, hitB, false, j);
            err = mods.eln.node.voxel.CablePlacer.routeAndLayForTests(c.level(), LV, a, b);
            for (int x = 0; x < 8; x++)
                if (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(x, 1, 1), c.level())) instanceof mods.eln.node.voxel.VoxelNode v)
                    for (var part : v.parts) {
                        where.append(x).append(':');
                        if (part instanceof mods.eln.node.voxel.CablePart cp) {
                            where.append(cp.face).append(java.util.Arrays.toString(cp.points));
                            if (x == 5 && cp.valid() && !cp.boxes().isEmpty()) last = true;
                        }
                        where.append(' ');
                    }
        }

        public void measure(TestContext c) {
            c.check("waypoint end: laid", err == null, String.valueOf(err));
            c.line("waypoint end: parts " + where);
            c.check("waypoint end: a piece in the last block", last, where.toString());
        }
    }

    /**
     * A route started on an old cable joins it (Baughn: cable laid with two clicks wasn't connected): a cable from the
     * source to a free point on the floor, then a second one from a point on that cable to the resistor; the circuit
     * conducts.
     */
    static final class JoinedRouteCase implements TestCase {
        ResistorElement r;
        String err1, err2, joinAt;

        public String name() {
            return "a route from an old cable joins it";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 4;
        }

        public int depth() {
            return 3;
        }

        public int ticks() {
            return 60;
        }

        public void build(TestContext c) {
            source(c, SOURCE, 0, 1, 50);
            r = end(c, 7, 1);
            var level = c.level();
            var hitS = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(0, 1, 1)).add(1.0, 0.05, 0.5);
            long[][] j = new long[1][];
            var a = mods.eln.node.voxel.CablePlacer.endAtForTests(level, c.at(0, 1, 1), net.minecraft.core.Direction.EAST, hitS, true, j);
            var free = new mods.eln.node.voxel.CableRouter.End(c.at(3, 1, 1).getX() * 16L + 8, c.at(3, 1, 1).getY() * 16L, c.at(3, 1, 1).getZ() * 16L + 8, 0, -1);
            err1 = mods.eln.node.voxel.CablePlacer.routeAndLayForTests(level, LV, a, free);
            // a click on the laid cable's top, a block from its end
            var onCable = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(2, 1, 1)).add(0.5, 1.0 / 16 + 0.001, 0.5);
            long[][] j2 = new long[1][];
            var s2 = mods.eln.node.voxel.CablePlacer.endAtForTests(level, c.at(2, 1, 1), net.minecraft.core.Direction.UP, onCable, true, j2);
            joinAt = j2[0] == null ? "no join" : java.util.Arrays.toString(j2[0]);
            var hitR = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(7, 1, 1)).add(0.0, 0.05, 0.5);
            long[][] j3 = new long[1][];
            var b = mods.eln.node.voxel.CablePlacer.endAtForTests(level, c.at(7, 1, 1), net.minecraft.core.Direction.WEST, hitR, false, j3);
            err2 = s2 == null ? "no start on the cable" : mods.eln.node.voxel.CablePlacer.routeAndLayJoinedForTests(level, LV, s2, j2[0], b, j3[0]);
        }

        public void measure(TestContext c) {
            c.check("joined route: the first cable laid", err1 == null, String.valueOf(err1));
            c.check("joined route: the second starts on it (a join)", !"no join".equals(joinAt), joinAt);
            c.check("joined route: the second laid", err2 == null, String.valueOf(err2));
            double i = current(r);
            c.check("joined route: the circuit conducts through the join", Math.abs(i) > 1, String.format("%.4f A", i));
        }
    }

    /**
     * The route preview runs on its own thread over a snapshot of the blocks (Baughn: the preview was slow): over stairs,
     * a slab and a fence it shows exactly the route the live world gives.
     */
    static final class PreviewSnapshotCase implements TestCase {
        String result;
        boolean same;

        public String name() {
            return "route preview from a snapshot";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 4;
        }

        public int depth() {
            return 3;
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext c) {
            var level = c.level();
            level.setBlock(c.at(3, 1, 1), net.minecraft.world.level.block.Blocks.OAK_STAIRS.defaultBlockState(), 3);
            level.setBlock(c.at(5, 1, 1), net.minecraft.world.level.block.Blocks.OAK_SLAB.defaultBlockState(), 3);
            level.setBlock(c.at(6, 1, 0), net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState(), 3);
            var a = new mods.eln.node.voxel.CableRouter.End(c.at(1, 1, 1).getX() * 16L + 8, c.at(1, 1, 1).getY() * 16L, c.at(1, 1, 1).getZ() * 16L + 8, 0, -1);
            var b = new mods.eln.node.voxel.CableRouter.End(c.at(8, 1, 1).getX() * 16L + 8, c.at(8, 1, 1).getY() * 16L, c.at(8, 1, 1).getZ() * 16L + 8, 0, -1);
            var lo = c.at(0, 0, 0);
            var live = mods.eln.node.voxel.KitPreview.answer(mods.eln.node.voxel.RouteView.live(level), LV, a, null, List.of(), b, null, Integer.MAX_VALUE);
            var snap = mods.eln.node.voxel.KitPreview.answer(mods.eln.node.voxel.RouteView.snapshot(level, lo.getX() - 8, lo.getY() - 8, lo.getZ() - 8,
                lo.getX() + 18, lo.getY() + 12, lo.getZ() + 12), LV, a, null, List.of(), b, null, Integer.MAX_VALUE);
            result = live.message() + " / " + snap.message();
            same = live.ok() && snap.ok() && live.boxes().size() == snap.boxes().size();
            for (int i = 0; same && i < live.boxes().size(); i++) same = java.util.Arrays.equals(live.boxes().get(i), snap.boxes().get(i));
        }

        public void measure(TestContext c) {
            c.check("route preview: the snapshot gives the live route", same, result);
        }
    }

    /** Cable kits from coils and back, merging; breaking a laid piece gives its length back. */
    static final class KitCase implements TestCase {
        public String name() {
            return "cable kits";
        }

        /** Real crafts in a crafting table: {tier, plates in one slot} (AdventurAgent: 4 alloy plates left 2, not 1). */
        static final int[][] TABLE = {{1, 4}, {1, 7}, {0, 5}};
        net.minecraft.world.inventory.CraftingMenu[] menus = new net.minecraft.world.inventory.CraftingMenu[TABLE.length];
        net.minecraft.world.item.ItemStack[] results = new net.minecraft.world.item.ItemStack[TABLE.length];

        public void build(TestContext c) {
            for (int q = 0; q < TABLE.length; q++) {
                int[] t = TABLE[q];
                var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());
                var cm = new net.minecraft.world.inventory.CraftingMenu(7, fp.getInventory(),
                    net.minecraft.world.inventory.ContainerLevelAccess.create(c.level(), net.minecraft.core.BlockPos.ZERO));
                cm.getSlot(1).set(new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                    net.minecraft.resources.Identifier.parse(t[0] == 0 ? "eln:iron_plate" : "eln:alloy_plate")), t[1]));
                cm.slotsChanged(cm.getSlot(1).container);
                results[q] = cm.getSlot(0).getItem().copy();
                cm.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, fp);
                cm.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
                menus[q] = cm;
            }
        }

        public void measure(TestContext c) {
            var coil = mods.eln.node.voxel.CableKitRecipes.coil(LV);
            var in = net.minecraft.world.item.crafting.CraftingInput.of(2, 2, List.of(coil.copy(), coil.copy(), coil.copy(), net.minecraft.world.item.ItemStack.EMPTY));
            var kit = mods.eln.node.voxel.CableKitRecipes.FromCoils.INSTANCE.assemble(in);
            c.check("cable kit: three coils make a 48-voxel kit", mods.eln.node.voxel.KitItem.length(kit) == 48 && kit.getItem() instanceof mods.eln.node.voxel.CableKitItem k && k.tier == LV,
                kit + " " + mods.eln.node.voxel.KitItem.length(kit));
            // in the recipe book and JEI (Baughn: they weren't): learnable, shown per tier, unlocked by holding cable
            // one recipe per tier (AdventurAgent: one for all tiers let the book fill the MV kit with LV cable)
            var rm = c.level().getServer().getRecipeManager();
            int learnable = 0;
            for (var h : rm.getLearnableRecipes())
                if (h.value() instanceof mods.eln.node.voxel.CableKitRecipes.FromCoils || h.value() instanceof mods.eln.node.voxel.CableKitRecipes.ToCoil) learnable++;
            var mvKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "cable_kit_from_coils_mv"));
            var mv = rm.byKey(mvKey).map(h -> h.value()).orElse(null);
            var adv = c.level().getServer().getAdvancements().get(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "recipes/misc/cable_kit_from_coils"));
            c.check("cable kit: in the recipe book, one recipe per tier each way, unlocked by cable",
                learnable == 12 && adv != null, learnable + " learnable, advancement " + (adv != null));
            var mvIngs = mv == null ? List.<net.minecraft.world.item.crafting.Ingredient>of() : mv.placementInfo().ingredients();
            var mvIn = net.minecraft.world.item.crafting.CraftingInput.of(1, 1, List.of(mods.eln.node.voxel.CableKitRecipes.coil(LV + 1)));
            c.check("cable kit: the MV recipe places MV cable only and takes no LV", mv instanceof mods.eln.node.voxel.CableKitRecipes.FromCoils
                && mvIngs.size() == 1 && mvIngs.get(0).test(mods.eln.node.voxel.CableKitRecipes.coil(LV + 1)) && !mvIngs.get(0).test(coil)
                && !((mods.eln.node.voxel.CableKitRecipes.FromCoils) mv).matches(net.minecraft.world.item.crafting.CraftingInput.of(1, 1, List.of(coil.copy())), c.level())
                && ((mods.eln.node.voxel.CableKitRecipes.FromCoils) mv).matches(mvIn, c.level()) && mv.display().size() == 1, String.valueOf(mv));
            c.check("cable kit: JEI examples per tier", mods.eln.node.voxel.CableKitRecipes.examples().size() >= 10,
                mods.eln.node.voxel.CableKitRecipes.examples().size() + " examples");
            // whole stacks (Baughn: a grid of cable stacks gave one cable's worth per slot, and shift-click a heap of kits)
            var four = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
            for (int q = 0; q < 4; q++) four.add(coil.copyWithCount(10));
            var k40 = mods.eln.node.voxel.CableKitRecipes.FromCoils.INSTANCE.assemble(net.minecraft.world.item.crafting.CraftingInput.of(2, 2, four));
            int[] use4 = mods.eln.node.voxel.CableKitRecipes.FromCoils.useForTests(four);
            c.check("cable kit: 2x2 stacks of 10 make one 40-cable kit and use them all", mods.eln.node.voxel.KitItem.length(k40) == 640
                && use4 != null && use4[0] + use4[1] + use4[2] + use4[3] == 40, mods.eln.node.voxel.KitItem.length(k40) + " " + java.util.Arrays.toString(use4));
            var nine = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
            for (int q = 0; q < 9; q++) nine.add(coil.copyWithCount(64));
            var kFull = mods.eln.node.voxel.CableKitRecipes.FromCoils.INSTANCE.assemble(net.minecraft.world.item.crafting.CraftingInput.of(3, 3, nine));
            int[] use9 = mods.eln.node.voxel.CableKitRecipes.FromCoils.useForTests(nine);
            int sum9 = 0, min9 = 99;
            for (int q = 0; use9 != null && q < 9; q++) { sum9 += use9[q]; min9 = Math.min(min9, use9[q]); }
            c.check("cable kit: 3x3 stacks of 64 make a full kit, at least one from each slot, the rest left", mods.eln.node.voxel.KitItem.length(kFull) == 1024
                && sum9 == 64 && min9 >= 1, mods.eln.node.voxel.KitItem.length(kFull) + " " + java.util.Arrays.toString(use9));
            var in2 = net.minecraft.world.item.crafting.CraftingInput.of(1, 1, List.of(kit.copy()));
            var back = mods.eln.node.voxel.CableKitRecipes.ToCoil.INSTANCE.assemble(in2);
            var rest = mods.eln.node.voxel.CableKitRecipes.ToCoil.INSTANCE.getRemainingItems(in2);
            c.check("cable kit: a kit gives a coil back and keeps 32", mods.eln.node.voxel.CableKitRecipes.coilTier(back) == LV
                && mods.eln.node.voxel.KitItem.length(rest.get(0)) == 32, back + " / " + rest);
            var mixed = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(coil.copy(), mods.eln.node.voxel.CableKitRecipes.coil(LV + 1)));
            c.check("cable kit: coils of two tiers don't mix", !mods.eln.node.voxel.CableKitRecipes.FromCoils.INSTANCE.matches(mixed, c.level()), "");
            var m = mods.eln.node.voxel.PipeKitRecipes.Merge.INSTANCE.assemble(net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
                List.of(mods.eln.node.voxel.CableKitItem.kit(LV, 100), mods.eln.node.voxel.CableKitItem.kit(LV, 28))));
            c.check("cable kit: two kits merge", mods.eln.node.voxel.KitItem.length(m) == 128 && m.getItem() instanceof mods.eln.node.voxel.CableKitItem, m.toString());
            // pipe kits as cable kits (Baughn: they didn't stack up or craft many at a time): whole stacks of plates
            var plate = new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.parse("eln:iron_plate")));
            var plates = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
            for (int q = 0; q < 4; q++) plates.add(plate.copyWithCount(10));
            var pk = mods.eln.node.voxel.PipeKitRecipes.FromPlates.INSTANCE.assemble(net.minecraft.world.item.crafting.CraftingInput.of(2, 2, plates));
            int[] pu = mods.eln.node.voxel.PipeKitRecipes.FromPlates.use(plates);
            c.check("pipe kit: 2x2 stacks of 10 iron plates make one 20-block kit and use them all", mods.eln.node.voxel.PipeKitItem.length(pk) == 320
                && pu != null && pu[0] + pu[1] + pu[2] + pu[3] == 40, mods.eln.node.voxel.PipeKitItem.length(pk) + " " + java.util.Arrays.toString(pu));
            var one = mods.eln.node.voxel.PipeKitRecipes.FromPlates.INSTANCE.assemble(net.minecraft.world.item.crafting.CraftingInput.of(1, 1, List.of(plate.copy())));
            c.check("pipe kit: a single plate makes nothing (two to a block)", one.isEmpty(), one.toString());
            // EA's casing (8 iron plates in a ring) still crafts: the kit recipe steps aside for another recipe
            var ring = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
            var ip = new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("eln:iron_plate")));
            for (int q = 0; q < 9; q++) ring.add(q == 4 ? net.minecraft.world.item.ItemStack.EMPTY : ip.copy());
            var ringIn = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, ring);
            var got = c.level().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, ringIn, c.level());
            c.check("casing: a ring of iron plates is still a casing", got.isPresent() && got.get().value().assemble(ringIn).getItem().toString().contains("casing"),
                got.map(h -> h.value().assemble(ringIn).toString()).orElse("none"));
            // the table crafts from build()
            for (int q = 0; q < TABLE.length; q++) {
                int[] t = TABLE[q];
                int per = t[0] == 0 ? 2 : 1, blocks = t[1] / per, left = menus[q].getSlot(1).getItem().getCount();
                c.check("pipe kit: a table craft of " + t[1] + (t[0] == 0 ? " iron" : " alloy") + " plates makes " + blocks + " blocks and leaves " + (t[1] - blocks * per),
                    mods.eln.node.voxel.PipeKitItem.length(results[q]) == 16 * blocks && left == t[1] - blocks * per,
                    "result " + mods.eln.node.voxel.PipeKitItem.length(results[q]) / 16 + " blocks, " + left + " left");
            }
            var pkit = mods.eln.node.voxel.PipeKitItem.kit(0, 16);
            var pbox = new net.minecraft.world.SimpleContainer(1);
            var pslot = new net.minecraft.world.inventory.Slot(pbox, 0, 0, 0);
            pbox.setItem(0, pkit);
            var pstack = plate.copyWithCount(7);
            pkit.getItem().overrideOtherStackedOnMe(pkit, pstack, pslot, net.minecraft.world.inventory.ClickAction.PRIMARY,
                net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level()),
                net.minecraft.world.entity.SlotAccess.of(() -> net.minecraft.world.item.ItemStack.EMPTY, x -> { }));
            c.check("pipe kit: 7 plates clicked on fill 3 blocks and leave 1", mods.eln.node.voxel.PipeKitItem.length(pkit) == 64 && pstack.getCount() == 1,
                mods.eln.node.voxel.PipeKitItem.length(pkit) + " / " + pstack.getCount());
            // Baughn: topping a kit up with cables like a bundle (a stack clicked onto the kit, or the kit onto a stack)
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());
            var box = new net.minecraft.world.SimpleContainer(1);
            var slot = new net.minecraft.world.inventory.Slot(box, 0, 0, 0);
            var k1 = mods.eln.node.voxel.CableKitItem.kit(LV, 144);
            box.setItem(0, k1);
            var stack = coil.copyWithCount(40);
            boolean took = k1.getItem().overrideOtherStackedOnMe(k1, stack, slot, net.minecraft.world.inventory.ClickAction.PRIMARY, player,
                net.minecraft.world.entity.SlotAccess.of(() -> net.minecraft.world.item.ItemStack.EMPTY, x -> { }));
            c.check("cable kit: 40 cables clicked onto a 9-cable kit go in (49 cables)", took && mods.eln.node.voxel.KitItem.length(k1) == 144 + 640 && stack.isEmpty(),
                mods.eln.node.voxel.KitItem.length(k1) + " voxels, " + stack.getCount() + " left");
            box.setItem(0, coil.copyWithCount(64));
            boolean took2 = k1.getItem().overrideStackedOnOther(k1, slot, net.minecraft.world.inventory.ClickAction.PRIMARY, player);
            c.check("cable kit: the kit clicked onto 64 cables takes what fits (to 1024 voxels)", took2 && mods.eln.node.voxel.KitItem.length(k1) == 1024
                && box.getItem(0).getCount() == 64 - 15, mods.eln.node.voxel.KitItem.length(k1) + " voxels, " + box.getItem(0).getCount() + " left");
            var other = mods.eln.node.voxel.CableKitItem.kit(LV, 16);
            box.setItem(0, mods.eln.node.voxel.CableKitRecipes.coil(LV + 1).copyWithCount(5));
            c.check("cable kit: another tier's cables don't go in", !other.getItem().overrideStackedOnOther(other, slot, net.minecraft.world.inventory.ClickAction.PRIMARY, player)
                && mods.eln.node.voxel.KitItem.length(other) == 16, "");
        }
    }

    /** A six-node device in a voxel block (phase 2): created there as a placement into a voxel block does. */
    static mods.eln.node.six.SixNodeElement host(TestContext c, int damage, BlockPos p, mods.eln.misc.Direction side) {
        var level = c.level();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var d = mods.eln.Eln.sixNodeItem.getDescriptor(damage);
        mods.eln.node.voxel.VoxelNode v;
        if (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, level)) instanceof mods.eln.node.voxel.VoxelNode old) v = old;
        else {
            v = new mods.eln.node.voxel.VoxelNode();
            v.onBlockPlacedBy(new mods.eln.misc.Coordonate(p, level), side, player, d.newItemStack());
            level.setBlock(p, mods.eln.registry.ElnRegistry.VOXEL_NODE_BLOCK.get().defaultBlockState(), 3);
        }
        if (!v.canPlaceElement(side.getInt()) || !v.createSubBlock(d.newItemStack(), side, player))
            throw new IllegalStateException("can't host " + d.name + " at " + p + " " + side);
        return v.getElement(side);
    }

    /**
     * Chips anywhere on a face (Baughn: 6 voxels across; pins that touch connect; a voxel of air otherwise):
     * two NOT chips on one floor, pin to pin: the first's output (high, its input open) drives the second (low). A NOT
     * placed at a click, its input pin touched by a floor cable from a Signal Source at 50 V next door: low. A chip with
     * no pin toward another is refused beside it; a chip over another is refused.
     */
    static final class ChipsPackedCase implements TestCase {
        mods.eln.node.six.SixNodeElement a, b, e;
        String placed = "", refused = "", four = "";
        boolean placedOk, refusedOk, fourOk;

        public String name() {
            return "chips anywhere on a face: pins connect, air between";
        }

        public int width() {
            return 9;
        }

        public int ticks() {
            return 40;
        }

        static mods.eln.node.voxel.VoxelNode block(TestContext c, BlockPos p) {
            var level = c.level();
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            var v = new mods.eln.node.voxel.VoxelNode();
            v.onBlockPlacedBy(new mods.eln.misc.Coordonate(p, level), mods.eln.misc.Direction.YN, player,
                mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.NOT).newItemStack());
            level.setBlock(p, mods.eln.registry.ElnRegistry.VOXEL_NODE_BLOCK.get().defaultBlockState(), 3);
            return v;
        }

        public void build(TestContext c) {
            var level = c.level();
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            var yn = mods.eln.misc.Direction.YN;
            var xp = mods.eln.misc.Direction.XP;
            for (int x = 0; x < 9; x++) c.floor(c.at(x, 0, 1));
            // NOT -> NOT, pin to pin
            var v = block(c, c.at(1, 1, 1));
            v.createSubBlockAt(mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.NOT).newItemStack(), 6, yn, -4, 0, 0, player);
            v.createSubBlockAt(mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.NOT).newItemStack(), 7, yn, 4, 0, 0, player);
            a = v.getElement(6);
            b = v.getElement(7);
            Wp9aCases.orient(a, l -> l, xp);
            Wp9aCases.orient(b, l -> l, xp);
            // over chip a: no room
            String over = v.placeAnywhere(mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.NOT).newItemStack(), yn, 0.3, 0, 0.5, player);
            // a NOT placed at a click; a Signal Source (50 V) in the next block west, a floor cable to the chip's input
            var w = block(c, c.at(4, 1, 1));
            String err = w.placeAnywhere(mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.NOT).newItemStack(), yn, 0.52, 0, 0.47, player);
            e = w.getElement(yn);
            if (e != null) Wp9aCases.orient(e, l -> l, xp);
            voxel(c, c.at(4, 1, 1), List.of(new CablePart(0, DOWN, new int[]{0, 0, 8, 4, 0, 8})));
            var src = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) host(c, Wp9aCases.SIGNAL_SOURCE, c.at(3, 1, 1), yn);
            src.networkUnserialize(c.stream(o -> {
                o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                o.writeFloat(50f);
            }));
            placed = "click: " + err + ", slot " + (e == null ? -1 : e.slot) + " at " + (e == null ? "-" : e.ox + "," + e.oy + "," + e.oz);
            placedOk = err == null && e != null && e.slot == yn.getInt();   // (the middle of a free face: its own slot)
            // beside a chip whose side toward it has no pin: too close
            var u = block(c, c.at(6, 1, 1));
            u.createSubBlockAt(mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.NOT).newItemStack(), 6, yn, -4, 0, 0, player);
            Wp9aCases.orient(u.getElement(6), l -> l, mods.eln.misc.Direction.ZP);   // (pins north and south)
            mods.eln.misc.LRDU eastward = null;
            for (var l : mods.eln.misc.LRDU.values()) if (yn.applyLRDU(l) == xp) eastward = l;
            // (pins east and west: its west pin a voxel from u's body, which has no pin there)
            String near = u.placeErrorAt(mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.NOT).newItemStack(), 7, yn, 3, 0, 0, eastward);
            refused = "over: " + over + "; beside: " + near;
            refusedOk = over != null && over.startsWith("No room") && near != null && near.startsWith("Too close");
            // four to a face (Baughn's mockup): centres (4, 4), (12, 4), (4, 12), (12, 12), pins east and west; the pairs
            // along x meet pin to pin, the pairs along z face each other's pinless sides two voxels apart
            var q = block(c, c.at(7, 1, 1));
            var not = mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.NOT);
            int[][] at = {{-4, -4}, {4, -4}, {-4, 4}};
            for (int k = 0; k < 3; k++) {
                q.createSubBlockAt(not.newItemStack(), 6 + k, yn, at[k][0], 0, at[k][1], player);
                Wp9aCases.orient(q.getElement(6 + k), l -> l, xp);
            }
            mods.eln.misc.LRDU east = null;
            for (var l : mods.eln.misc.LRDU.values()) if (yn.applyLRDU(l) == xp) east = l;
            four = String.valueOf(q.placeErrorAt(not.newItemStack(), 9, yn, 4, 0, 4, east));
            fourOk = four.equals("null");
        }

        public void measure(TestContext c) {
            c.check("chips: placed at a click in the middle of a free face: the face's slot", placedOk, placed);
            c.check("chips: refused over a chip, and beside one with no pin toward it", refusedOk, refused);
            c.check("chips: four fit a face (pins met along x, two voxels of air along z)", fourOk, four);
            c.checkValue("chips: the first NOT (input open) [V]", Wp9cCases.out(a), mods.eln.Eln.SVU);
            Wp9cCases.within(c, "chips: the second NOT, its input on the first's output pin [V]", Wp9cCases.out(b), 0, 0.5);
            Wp9cCases.within(c, "chips: a NOT whose input pin touches a cable from 50 V [V]", e == null ? -1 : Wp9cCases.out(e), 0, 0.5);
        }
    }

    /**
     * Other small devices go anywhere on a face too (Baughn), the redstone converters stay centred: a Signal Trimmer off
     * centre at 50 V, its output pin on a NOT chip's input pin: low.
     */
    static final class SmallDevicesAnywhereCase implements TestCase {
        mods.eln.node.six.SixNodeElement not;
        String redstone = "";

        public String name() {
            return "small devices anywhere on a face";
        }

        public int ticks() {
            return 40;
        }

        public void build(TestContext c) {
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());
            var yn = mods.eln.misc.Direction.YN;
            var xp = mods.eln.misc.Direction.XP;
            c.floor(c.at(1, 0, 1));
            var v = ChipsPackedCase.block(c, c.at(1, 1, 1));
            v.createSubBlockAt(mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.SIGNAL_TRIMMER).newItemStack(), 6, yn, -4, 0, 0, player);
            var t = (mods.eln.sixnode.electricalgatesource.ElectricalGateSourceElement) v.getElement(6);
            Wp9aCases.orient(t, l -> l, xp);
            t.networkUnserialize(c.stream(o -> {
                o.writeByte(mods.eln.sixnode.electricalgatesource.ElectricalGateSourceElement.setVoltagerId);
                o.writeFloat((float) mods.eln.Eln.SVU);
            }));
            v.createSubBlockAt(mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.NOT).newItemStack(), 7, yn, 4, 0, 0, player);
            not = v.getElement(7);
            Wp9aCases.orient(not, l -> l, xp);
            redstone = String.valueOf(v.placeAnywhere(mods.eln.Eln.sixNodeItem.getDescriptor(Wp9bCases.REDSTONE_IN).newItemStack(), yn, 0.5, 0, 0.9, player));
        }

        public void measure(TestContext c) {
            c.check("small devices: the trimmer goes anywhere, the redstone converter doesn't",
                mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.SIGNAL_TRIMMER).placeAnywhere()
                    && !mods.eln.Eln.sixNodeItem.getDescriptor(Wp9bCases.REDSTONE_IN).placeAnywhere() && redstone.startsWith("That can't"), redstone);
            Wp9cCases.within(c, "small devices: a NOT on an off-centre trimmer's pin at 50 V [V]", Wp9cCases.out(not), 0, 0.5);
        }
    }

    /**
     * Pins at a block's edge reach the next voxel block: NOT chips at the east edge of one block and the west edge of the
     * next, pin to pin across the boundary (the second low); and a NOT whose output pin at a block's edge touches a cable
     * in the next block that runs to another NOT's input (low too).
     */
    static final class PinsAcrossCase implements TestCase {
        mods.eln.node.six.SixNodeElement b, d;

        public String name() {
            return "chip pins across a block boundary";
        }

        public int ticks() {
            return 40;
        }

        public void build(TestContext c) {
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());
            var yn = mods.eln.misc.Direction.YN;
            var xp = mods.eln.misc.Direction.XP;
            var not = mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.NOT);
            for (int x = 0; x < 8; x++) c.floor(c.at(x, 0, 1));
            // pin to pin across
            var v1 = ChipsPackedCase.block(c, c.at(1, 1, 1));
            v1.createSubBlockAt(not.newItemStack(), 6, yn, 4, 0, 0, player);
            Wp9aCases.orient(v1.getElement(6), l -> l, xp);
            var v2 = ChipsPackedCase.block(c, c.at(2, 1, 1));
            v2.createSubBlockAt(not.newItemStack(), 6, yn, -4, 0, 0, player);
            b = v2.getElement(6);
            Wp9aCases.orient(b, l -> l, xp);
            // pin to a cable in the next block, the cable to a chip's input there
            var v4 = ChipsPackedCase.block(c, c.at(4, 1, 1));
            v4.createSubBlockAt(not.newItemStack(), 6, yn, 4, 0, 0, player);
            Wp9aCases.orient(v4.getElement(6), l -> l, xp);
            var v5 = ChipsPackedCase.block(c, c.at(5, 1, 1));
            v5.createSubBlockAt(not.newItemStack(), 6, yn, 4, 0, 0, player);
            d = v5.getElement(6);
            Wp9aCases.orient(d, l -> l, xp);
            voxel(c, c.at(5, 1, 1), List.of(new CablePart(0, DOWN, new int[]{0, 0, 8, 8, 0, 8})));
        }

        public void measure(TestContext c) {
            Wp9cCases.within(c, "across: a NOT whose input pin meets the output pin of a NOT in the next block [V]", Wp9cCases.out(b), 0, 0.5);
            Wp9cCases.within(c, "across: a NOT on a cable from the output pin of a NOT in the next block [V]", Wp9cCases.out(d), 0, 0.5);
        }
    }

    /** The cable kit from a Signal Source (50 V) to a NOT chip placed at a click: a click on the chip ends the route at its
     *  nearest pin (its input), and the laid cable drives it: low. */
    static final class CableToChipCase implements TestCase {
        mods.eln.node.six.SixNodeElement chip;
        String err = "";

        public String name() {
            return "cable kit to a chip's pin";
        }

        public int ticks() {
            return 40;
        }

        public void build(TestContext c) {
            var level = c.level();
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            var yn = mods.eln.misc.Direction.YN;
            for (int x = 0; x < 8; x++) c.floor(c.at(x, 0, 1));
            var src = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) host(c, Wp9aCases.SIGNAL_SOURCE, c.at(1, 1, 1), yn);
            src.networkUnserialize(c.stream(o -> {
                o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                o.writeFloat(50f);
            }));
            var v = ChipsPackedCase.block(c, c.at(5, 1, 1));
            err = String.valueOf(v.placeAnywhere(mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.NOT).newItemStack(), yn, 0.4, 0, 0.5, player));   // (two voxels off centre)
            chip = v.getElement(6);
            if (chip == null) return;
            Wp9aCases.orient(chip, l -> l, mods.eln.misc.Direction.XP);   // (input west, toward the source)
            BlockPos sp = c.at(1, 1, 1), cp = c.at(5, 1, 1);
            var a = mods.eln.node.voxel.CablePlacer.endAtForTests(level, sp, net.minecraft.core.Direction.EAST,
                net.minecraft.world.phys.Vec3.atLowerCornerOf(sp).add(0.65, 0.1, 0.5), true, new long[1][]);
            var b = mods.eln.node.voxel.CablePlacer.endAtForTests(level, cp, net.minecraft.core.Direction.UP,
                net.minecraft.world.phys.Vec3.atLowerCornerOf(cp).add(0.27, 0.1, 0.5), false, new long[1][]);
            err = "ends " + a + " / " + b + "; lay " + (a == null || b == null ? "-" : mods.eln.node.voxel.CablePlacer.routeAndLayForTests(level, 0, a, b));
        }

        public void measure(TestContext c) {
            c.line(err);
            Wp9cCases.within(c, "cable to chip: the NOT's input on the laid cable from 50 V [V]", chip == null ? -1 : Wp9cCases.out(chip), 0, 0.5);
        }
    }

    /**
     * Slots past the six faces (devices off centre on a face; placement by players comes later): two AND chips and a
     * Signal Trimmer on one floor, each in its own slot. Each is found where its model is, saved and loaded in place
     * (also the harness's round trip), its GUI opens by slot, and breaking takes that one only.
     */
    static final class SlotsCase implements TestCase {
        String found = "", loaded = "", gui = "", broke = "";
        boolean foundOk, loadedOk, guiOk, brokeOk;

        public String name() {
            return "voxel block slots: devices off centre on a face";
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext c) {
            var level = c.level();
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            BlockPos p = c.at(1, 1, 1);
            c.floor(c.at(1, 0, 1));
            var yn = mods.eln.misc.Direction.YN;
            var v = new mods.eln.node.voxel.VoxelNode();
            var and = mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.AND).newItemStack();
            v.onBlockPlacedBy(new mods.eln.misc.Coordonate(p, level), yn, player, and);
            level.setBlock(p, mods.eln.registry.ElnRegistry.VOXEL_NODE_BLOCK.get().defaultBlockState(), 3);
            boolean made = v.createSubBlockAt(and, 6, yn, -4, 0, -4, player)
                & v.createSubBlockAt(mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.AND).newItemStack(), 7, yn, 4, 0, -4, player)
                & v.createSubBlockAt(mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.SIGNAL_TRIMMER).newItemStack(), 9, yn, -4, 0, 4, player);
            // the model of slot 6 is round (4, 4) voxels, slot 7's round (12, 4), slot 9's round (4, 12)
            int a = v.elementAt(4 / 16.0, 1 / 16.0, 4 / 16.0, 0), b = v.elementAt(12 / 16.0, 1 / 16.0, 4 / 16.0, 0),
                t = v.elementAt(4 / 16.0, 1 / 16.0, 12 / 16.0, 0), none = v.elementAt(12 / 16.0, 1 / 16.0, 12 / 16.0, 0);
            found = "made " + made + "; at (4,4) " + a + ", (12,4) " + b + ", (4,12) " + t + ", (12,12) " + none + "; face slot " + v.getElement(yn);
            foundOk = made && a == 6 && b == 7 && t == 9 && none < 0 && v.getElement(yn) == null && v.getElement(9).side == yn;
            // saved and loaded: the same devices in the same slots and places
            var tag = new net.minecraft.nbt.CompoundTag();
            v.writeToNBT(tag);
            var w = new mods.eln.node.voxel.VoxelNode();
            w.readFromNBT(tag);
            StringBuilder lb = new StringBuilder();
            loadedOk = true;
            for (int s : new int[]{6, 7, 9}) {
                var x = v.getElement(s);
                var y = w.getElement(s);
                lb.append(s).append(": ").append(y == null ? "none" : y.sixNodeElementDescriptor.name + " " + y.side + " " + y.ox + "," + y.oy + "," + y.oz).append("; ");
                loadedOk &= y != null && y.slot == s && y.side == x.side && y.ox == x.ox && y.oy == x.oy && y.oz == x.oz
                    && y.sixNodeElementDescriptor == x.sixNodeElementDescriptor;
            }
            loadedOk &= !tag.contains("EID8") && tag.getShortOr("EID2", (short) -1) == 0;
            loaded = lb.toString();
            // the trimmer's GUI, by its slot
            var menu = mods.eln.node.NodeMenus.menu(1, player, v, 9);
            gui = menu == null ? "none" : "slot " + menu.slot + " id " + menu.descriptorId;
            guiOk = menu != null && menu.slot == 9 && menu.descriptorId == Wp9cCases.SIGNAL_TRIMMER;
            // breaking slot 7 takes that chip only
            boolean gone = v.deleteSlot(null, 7);
            broke = "deleted " + gone + "; left " + (v.getElement(6) != null) + "," + (v.getElement(7) != null) + "," + (v.getElement(9) != null);
            brokeOk = gone && v.getElement(6) != null && v.getElement(7) == null && v.getElement(9) != null;
        }

        public void measure(TestContext c) {
            c.check("slots: each device is where its model is", foundOk, found);
            c.check("slots: saved and loaded in place", loadedOk, loaded);
            c.check("slots: the trimmer's GUI opens by slot", guiOk, gui);
            c.check("slots: breaking one takes that one", brokeOk, broke);
        }
    }

    /**
     * A device hosted in a voxel block is clicked and outlined where its model is (Baughn: a Signal Trimmer's click area,
     * the whole face, didn't match its 6 x 6 px model): on the floor and on a wall. Logs every device's footprint.
     */
    static final class FootprintCase implements TestCase {
        String floor = "", wall = "", outline = "";
        boolean floorOk, wallOk, outlineOk;

        public String name() {
            return "a hosted device is clicked and outlined at its model";
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext c) {
            var tp = c.at(1, 1, 1);
            c.floor(c.at(1, 0, 1));
            host(c, Wp9cCases.SIGNAL_TRIMMER, tp, mods.eln.misc.Direction.YN);
            var v = (mods.eln.node.voxel.VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(tp, c.level()));
            int yn = mods.eln.misc.Direction.YN.getInt();
            int[] fp = mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.SIGNAL_TRIMMER).footprint();
            floor = "footprint " + fp[0] + "," + fp[1] + "; on the model " + v.elementAt(0.6, 0.08, 0.6, 0) + ", beside it " + v.elementAt(0.8, 0.08, 0.5, 0)
                + ", above it " + v.elementAt(0.5, 0.3, 0.5, 0);
            floorOk = fp[0] == 3 && fp[1] >= 2 && fp[1] <= 3 && v.elementAt(0.6, 0.08, 0.6, 0) == yn && v.elementAt(0.8, 0.08, 0.5, 0) < 0
                && v.elementAt(0.5, 0.3, 0.5, 0) < 0;
            if (c.level().getBlockEntity(tp) instanceof mods.eln.node.voxel.VoxelNodeEntity e) {
                var b = e.shapes().outline().bounds();
                outline = String.format("%.3f..%.3f, %.3f..%.3f, %.3f..%.3f", b.minX, b.maxX, b.minY, b.maxY, b.minZ, b.maxZ);
                outlineOk = Math.abs(b.minX - 5 / 16.0) < 1e-6 && Math.abs(b.maxX - 11 / 16.0) < 1e-6 && b.minY == 0 && b.maxY <= 3 / 16.0 + 1e-6
                    && Math.abs(b.minZ - 5 / 16.0) < 1e-6 && Math.abs(b.maxZ - 11 / 16.0) < 1e-6;
            }
            // on the wall to its west
            var wp = c.at(3, 1, 1);
            c.floor(c.at(2, 1, 1));
            host(c, Wp9cCases.SIGNAL_TRIMMER, wp, mods.eln.misc.Direction.XN);
            var w = (mods.eln.node.voxel.VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(wp, c.level()));
            int xn = mods.eln.misc.Direction.XN.getInt();
            wall = "on the model " + w.elementAt(0.08, 0.6, 0.4, 0) + ", beside it " + w.elementAt(0.08, 0.5, 0.8, 0) + " / " + w.elementAt(0.08, 0.2, 0.5, 0);
            wallOk = w.elementAt(0.08, 0.6, 0.4, 0) == xn && w.elementAt(0.08, 0.5, 0.8, 0) < 0 && w.elementAt(0.08, 0.2, 0.5, 0) < 0;
            StringBuilder all = new StringBuilder("footprints (half width, height; voxels):");
            for (var d : new java.util.TreeMap<>(mods.eln.Eln.sixNodeItem.all()).values()) {
                int[] f = d.footprint();
                all.append(' ').append(d.name).append('=').append(f[0]).append(',').append(f[1]).append(';');
            }
            c.line(all.toString());
            // Baughn 2026-10-29: a lamp supply's and a signal processor's open door made their box almost a full block
            StringBuilder doors = new StringBuilder();
            doorsOk = true;
            for (var d : mods.eln.Eln.sixNodeItem.all().values())
                if (d instanceof mods.eln.sixnode.lampsupply.LampSupplyDescriptor || d instanceof mods.eln.sixnode.electricalmath.ElectricalMathDescriptor
                    || d instanceof mods.eln.sixnode.modbusrtu.ModbusRtuDescriptor || d instanceof mods.eln.sixnode.EmergencyLampDescriptor) {
                    int[] f = d.footprint();
                    doors.append(d.name).append('=').append(f[0]).append(',').append(f[1]).append("; ");
                    doorsOk &= f[1] <= 8 && f[0] < 8;
                }
            this.doors = doors.toString();
        }

        boolean doorsOk;
        String doors;

        public void measure(TestContext c) {
            c.check("footprint: devices with doors are boxed closed, emergency lamps without their glow (at most half a block high, narrower than the face)", doorsOk && !doors.isEmpty(), doors);
            c.check("footprint: a floor trimmer is clicked on its 6 x 6 model only", floorOk, floor);
            c.check("footprint: its outline is the model's box", outlineOk, outline);
            c.check("footprint: a wall trimmer likewise", wallOk, wall);
        }
    }

    /**
     * A device hosted in a voxel block works as in a six-node block: source -> six-node cables -> a Power Resistor in a
     * voxel block (which also holds a pipe on its ceiling) -> ground; the current equals the all-six-node row's.
     */
    static final class HostedCase implements TestCase {
        ResistorElement six, vox;

        public String name() {
            return "six-node device hosted in a voxel block";
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
            for (int z : new int[]{1, 6}) {
                source(c, SOURCE, 0, z, 50);
                for (int x = 1; x <= 3; x++) c.placeSix(LV_CABLE, c.at(x, 1, z));
            }
            six = resistor(c, POWER_RESISTOR, 4, 1, CORE);
            c.placeSix(GROUND, c.at(5, 1, 1));
            // the voxel row: a pipe across the ceiling first, then the resistor on the floor
            String err = PipeKitItem.place(c.level(), c.at(4, 1, 6), new mods.eln.node.voxel.PipePart(0, new int[]{0, 12, 8, 16, 12, 8}), null);
            if (err != null) throw new IllegalStateException(err);
            vox = (ResistorElement) host(c, POWER_RESISTOR, c.at(4, 1, 6), mods.eln.misc.Direction.YN);
            Wp9aCases.orient(vox, mods.eln.misc.LRDU::right, mods.eln.misc.Direction.XN);
            vox.getInventory().setItem(0, Wp9aCases.coal(CORE));
            vox.getInventory().setChanged();
            c.placeSix(GROUND, c.at(5, 1, 6));
        }

        public void measure(TestContext c) {
            double a = current(six), b = current(vox);
            c.line(String.format("hosted: six-node %.5f A, in a voxel block %.5f A", a, b));
            c.check("hosted device: carries the same current as in a six-node block (0.1%)", Math.abs(a) > 1 && Math.abs(a - b) <= 1e-3 * Math.abs(a),
                String.format("six-node %.5f A, voxel %.5f A", a, b));
            var v = (mods.eln.node.voxel.VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(4, 1, 6), c.level()));
            c.check("hosted device: the block keeps its pipe", v.parts.size() == 1, v.parts.size() + " parts");
        }
    }

    /** Mining a device in a block that also holds a cable takes the device only (AdventurAgent's check 3). */
    static final class BreakMixedCase implements TestCase {
        String result;

        public String name() {
            return "break a device in a mixed voxel block";
        }

        public int width() {
            return 8;
        }

        public void build(TestContext c) {
            BlockPos p = c.at(1, 1, 1);
            c.floor(p.north());
            // a cable on the north wall, a switch on the floor
            voxel(c, p, CablePart.arms(LV, NORTH, WEST, EAST));
            host(c, Wp9aCases.SWITCH_LV, p, mods.eln.misc.Direction.YN);
            var level = c.level();
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            player.setPos(p.getX() + 0.5, p.getY() + 1.4 - player.getEyeHeight(), p.getZ() + 0.5);   // (eyes 1.4 above the floor, looking down)
            player.setXRot(90);
            player.setYRot(0);
            var pick = player.pick(player.blockInteractionRange() + 1, 1f, false);
            String hit = pick.getLocation().subtract(p.getX(), p.getY(), p.getZ()).toString();
            // (as a survival player's mining does: the whole server path)
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.gameMode.destroyBlock(p);
            // mining goes on at the same spot (now nothing there): the cable must stay
            player.gameMode.destroyBlock(p);
            boolean removed = level.getBlockState(p).isAir();
            var v = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, level));
            result = "block removed " + removed + ", node " + (v == null ? "gone" : v.getClass().getSimpleName())
                + (v instanceof mods.eln.node.voxel.VoxelNode vn ? ", parts " + vn.parts.size() + ", floor element " + (vn.getElement(mods.eln.misc.Direction.YN) != null) : "")
                + " (hit " + hit + ")";
            cableOverResistor(c);
        }

        public void measure(TestContext c) {
            c.check("mixed block: mining the switch takes the switch only", result.startsWith("block removed false, node VoxelNode, parts 1, floor element false"), result);
            c.check("mixed block: a wall cable inside a floor resistor's box is the cable's", resistorOk, resistor);
        }

        String resistor = "";
        boolean resistorOk;

        /** A Power Resistor's model box (16 x 16, 7 high) reaches over a cable down the north wall: a point on the
         *  cable is the cable's, not the resistor's (elementAt yields to a part's own box). */
        void cableOverResistor(TestContext c) {
            BlockPos p = c.at(5, 1, 1);
            c.floor(p.north());
            voxel(c, p, CablePart.arms(LV, NORTH, DOWN));
            host(c, POWER_RESISTOR, p, mods.eln.misc.Direction.YN);
            var vn = (mods.eln.node.voxel.VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, c.level()));
            int yn = mods.eln.misc.Direction.YN.getInt();
            double cy = 3 / 16.0, cz = 0.5 / 16;
            resistor = "on the cable: element " + vn.elementAt(0.5, cy, cz, 0) + " part " + vn.partAt(0.5, cy, cz)
                + "; beside it: element " + vn.elementAt(0.5, cy, 0.5, 0);
            resistorOk = vn.elementAt(0.5, cy, cz, 0) < 0 && vn.partAt(0.5, cy, cz) >= 0 && vn.elementAt(0.5, cy, 0.5, 0) == yn;
        }
    }

    /**
     * Thermal cables (the voxel thermal tier): a hot body (a big heat capacity at 500 C) on one end of six copper thermal
     * cables, over a bump halfway (outer corners: thermal knuckles); six-node and converted rows warm their far end
     * alike.
     */
    static final class ThermalCase implements TestCase {
        final List<mods.eln.sim.ThermalLoad> hot = new ArrayList<>();
        String err = "";

        public String name() {
            return "thermal cable converted";
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
            return 200;   // (steady: a corner's knuckle adds heat capacity, so the first second warms slower)
        }

        public void build(TestContext c) {
            int id = Ids.six("Copper Thermal Cable");
            for (int z : new int[]{1, 6}) {
                c.floor(c.at(4, 1, z));
                for (int x : new int[]{1, 2, 3, 5, 6}) c.placeSix(id, c.at(x, 1, z));
                c.placeSix(id, c.at(3, 1, z), mods.eln.misc.Direction.XP);
                c.placeSix(id, c.at(4, 2, z));
                c.placeSix(id, c.at(5, 1, z), mods.eln.misc.Direction.XN);
            }
            for (int x = 0; x < 10; x++)
                for (int y = 1; y < 4; y++) {
                    var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(x, y, 6), c.level()));
                    if (n instanceof mods.eln.node.six.SixNode s6 && !(n instanceof mods.eln.node.voxel.VoxelNode)) {
                        String why = mods.eln.node.voxel.CableConverter.convert(s6);
                        if (why != null) err += x + "," + y + ": " + why + "; ";
                    }
                }
            for (int z : new int[]{1, 6}) {
                var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(1, 1, z), c.level()));
                mods.eln.sim.ThermalLoad t = n instanceof mods.eln.node.voxel.VoxelNode v ? v.cableThermal(0)
                    : ((mods.eln.node.six.SixNode) n).getElement(mods.eln.misc.Direction.YN).getThermalLoad(mods.eln.misc.LRDU.Left);
                var body = new mods.eln.sim.ThermalLoad(500, 1e9, 1e-3, 1e9);
                mods.eln.Eln.simulator.addThermalLoad(body);
                mods.eln.Eln.simulator.addThermalConnection(new mods.eln.sim.ThermalConnection(body, t));
                hot.add(body);
            }
        }

        double far(TestContext c, int z) {
            var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(6, 1, z), c.level()));
            if (n instanceof mods.eln.node.voxel.VoxelNode v) return v.cableThermal(0).Tc;
            return ((mods.eln.node.six.SixNode) n).getElement(mods.eln.misc.Direction.YN).getThermalLoad(mods.eln.misc.LRDU.Left).Tc;
        }

        public void measure(TestContext c) {
            c.check("thermal: every block converted", err.isEmpty(), err);
            double a = far(c, 1), b = far(c, 6);
            c.line(String.format("thermal: far end %.3f C (six-node), %.3f C (converted)", a, b));
            c.check("thermal: heat crosses the corners alike (2%)", a > 1 && Math.abs(a - b) <= 0.02 * a, String.format("%.3f vs %.3f", a, b));
            for (var h : hot) mods.eln.Eln.simulator.removeThermalLoad(h);
        }
    }

    /**
     * Cables through decorative blocks (Mimic): a cable kit's route from the source to the resistor, along a corridor with
     * a fence and a top slab in it: under the slab, beside the fence post. Those blocks become voxel blocks imitating
     * them; the circuit conducts. Then: the slab block's cable taken out -> it's a slab again; the fence mined -> the
     * fence drops, the cable stays.
     */
    static final class MimicCase implements TestCase {
        ResistorElement r;
        String err, fenceHost, slabHost, afterPart, afterFence;

        public String name() {
            return "cable through a fence and under a slab";
        }

        public int width() {
            return 10;
        }

        public int ticks() {
            return 60;
        }

        public void build(TestContext c) {
            var level = c.level();
            source(c, SOURCE, 0, 1, 50);
            r = end(c, 7, 1);
            for (int x = 2; x <= 5; x++) {
                c.floor(c.at(x, 1, 0));
                c.floor(c.at(x, 1, 2));
            }
            level.setBlock(c.at(3, 1, 1), net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(c.at(4, 1, 1), net.minecraft.world.level.block.Blocks.STONE_SLAB.defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP), 3);
            var hitS = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(0, 1, 1)).add(1.0, 0.05, 0.5);
            var hitR = net.minecraft.world.phys.Vec3.atLowerCornerOf(c.at(7, 1, 1)).add(0.0, 0.05, 0.5);
            long[][] j = new long[1][];
            var a = mods.eln.node.voxel.CablePlacer.endAtForTests(level, c.at(0, 1, 1), net.minecraft.core.Direction.EAST, hitS, true, j);
            var b = mods.eln.node.voxel.CablePlacer.endAtForTests(level, c.at(7, 1, 1), net.minecraft.core.Direction.WEST, hitR, false, j);
            err = mods.eln.node.voxel.CablePlacer.routeAndLayForTests(level, LV, a, b);
            fenceHost = host(c, 3);
            slabHost = host(c, 4);
        }

        String host(TestContext c, int x) {
            var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(x, 1, 1), c.level()));
            return n instanceof mods.eln.node.voxel.VoxelNode v ? (v.host == null ? "none" : v.host.getBlock().getName().getString()) + " + " + v.parts.size() + " part(s)"
                : c.level().getBlockState(c.at(x, 1, 1)).getBlock().getName().getString();
        }

        public void measure(TestContext c) {
            c.check("mimic: laid through", err == null, String.valueOf(err));
            c.check("mimic: the fence's block imitates the fence and holds cable", fenceHost.startsWith("Oak Fence + "), fenceHost);
            c.check("mimic: the slab's block imitates the slab and holds cable", slabHost.startsWith("Stone Slab + "), slabHost);
            double i = current(r);
            c.check("mimic: the circuit conducts", Math.abs(i) > 1, String.format("%.4f A", i));
            // take the slab block's cable out: a slab again
            var sv = (mods.eln.node.voxel.VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(4, 1, 1), c.level()));
            while (!sv.parts.isEmpty()) sv.removePart(0, false);
            sv.removeEmptyBlock();
            afterPart = c.level().getBlockState(c.at(4, 1, 1)).getBlock().getName().getString();
            c.check("mimic: without its cable the block is a slab again", afterPart.equals("Stone Slab"), afterPart);
            var fv = (mods.eln.node.voxel.VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(3, 1, 1), c.level()));
            fv.clearHost(null);
            afterFence = host(c, 3);
            c.check("mimic: the fence mined, the cable stays", afterFence.startsWith("none + "), afterFence);
        }
    }

    /**
     * Down a staircase (Mimic, support inside blocks): a corridor with two stairs up to a platform; a cable kit's route
     * from the floor to the platform's top runs over the steps (on each step's tread and up its riser) through the stair
     * blocks, now voxel blocks imitating the stairs.
     */
    static final class StairsCase implements TestCase {
        String err, hosts = "";

        public String name() {
            return "cable up a staircase";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext c) {
            var level = c.level();
            for (int x = 1; x <= 7; x++) for (int y = 1; y <= 3; y++) {
                c.floor(c.at(x, y, 0));
                c.floor(c.at(x, y, 2));
            }
            var stairs = net.minecraft.world.level.block.Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.EAST);
            level.setBlock(c.at(3, 1, 1), stairs, 3);
            c.floor(c.at(4, 1, 1));
            level.setBlock(c.at(4, 2, 1), stairs, 3);
            for (int x = 5; x <= 7; x++) for (int y = 1; y <= 2; y++) c.floor(c.at(x, y, 1));
            BlockPos a = c.at(1, 1, 1), b = c.at(6, 3, 1);
            var start = new mods.eln.node.voxel.CableRouter.End(a.getX() * 16L + 8, a.getY() * 16L, a.getZ() * 16L + 8, 0, -1);
            var end = new mods.eln.node.voxel.CableRouter.End(b.getX() * 16L + 8, b.getY() * 16L, b.getZ() * 16L + 8, 0, -1);
            err = mods.eln.node.voxel.CablePlacer.routeAndLayForTests(level, LV, start, end);
            for (BlockPos p : new BlockPos[]{c.at(3, 1, 1), c.at(4, 2, 1)}) {
                var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, level));
                hosts += n instanceof mods.eln.node.voxel.VoxelNode v && v.host != null ? v.host.getBlock().getName().getString() + " + " + v.parts.size() + "; " : "none; ";
            }
        }

        public void measure(TestContext c) {
            c.check("stairs: a route over the steps is laid", err == null, String.valueOf(err));
            c.check("stairs: both stairs hold cable (imitated)", hosts.equals(hosts.replace("none", "")) && hosts.split(";").length >= 2, hosts);
        }
    }

    /**
     * The chisel (Baughn: carve a hole through a wall so a cable fits): a stone wall across a walled corridor; a 4-voxel
     * chisel carves a tunnel 8 wide and 4 high along the floor through it; a cable kit's route then goes through. Putting
     * material back, and a block carved and put back whole is plain stone again.
     */
    static final class ChiselCase implements TestCase {
        String err, wall, back, whole, refused;

        public String name() {
            return "chisel a cable hole through a wall";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext c) {
            var level = c.level();
            for (int x = 1; x <= 6; x++) for (int y = 1; y <= 2; y++) {
                c.floor(c.at(x, y, 0));
                c.floor(c.at(x, y, 2));
            }
            BlockPos w = c.at(3, 1, 1);
            c.floor(w);
            c.floor(w.above());
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(mods.eln.registry.ElnRegistry.CHISEL.get()));
            for (int k = 0; k < 4; k++)
                for (int zz : new int[]{4, 8}) {
                    var hit = new net.minecraft.world.phys.Vec3(w.getX() + k * 4 / 16.0, w.getY() + 2 / 16.0, w.getZ() + (zz + 2) / 16.0);
                    mods.eln.node.voxel.ChiselItem.carve(player, level, w, hit, net.minecraft.core.Direction.WEST, 4, true);
                }
            BlockPos a = c.at(1, 1, 1), b = c.at(6, 1, 1);
            var start = new mods.eln.node.voxel.CableRouter.End(a.getX() * 16L + 8, a.getY() * 16L, a.getZ() * 16L + 8, 0, -1);
            var end = new mods.eln.node.voxel.CableRouter.End(b.getX() * 16L + 8, b.getY() * 16L, b.getZ() * 16L + 8, 0, -1);
            err = mods.eln.node.voxel.CablePlacer.routeAndLayForTests(level, LV, start, end);
            var n = (mods.eln.node.voxel.VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(w, level));
            wall = (n.host == null ? "none" : n.host.getBlock().getName().getString()) + ", carved " + (n.carved == null ? 0 : n.carved.cardinality())
                + ", parts " + n.parts.size();
            // put back where no cable is: the upper half of the tunnel's far corner
            var hit = new net.minecraft.world.phys.Vec3(w.getX() + 14.5 / 16.0, w.getY() + 4 / 16.0, w.getZ() + 10.5 / 16.0);
            int before = n.carved.cardinality();
            mods.eln.node.voxel.ChiselItem.carve(player, level, w, hit, net.minecraft.core.Direction.DOWN, 1, false);
            back = before + " -> " + n.carved.cardinality();
            // a second block: carved a little, put back whole: stone again
            BlockPos w2 = w.above();
            var h2 = new net.minecraft.world.phys.Vec3(w2.getX() + 0, w2.getY() + 0.5, w2.getZ() + 0.5);
            mods.eln.node.voxel.ChiselItem.carve(player, level, w2, h2, net.minecraft.core.Direction.WEST, 2, true);
            boolean carvedNow = level.getBlockState(w2).getBlock() instanceof mods.eln.node.voxel.VoxelNodeBlock;
            mods.eln.node.voxel.ChiselItem.carve(player, level, w2.west(), new net.minecraft.world.phys.Vec3(w2.getX(), w2.getY() + 0.5, w2.getZ() + 0.5),
                net.minecraft.core.Direction.EAST, 2, false);
            whole = "carved " + carvedNow + ", after putting back: " + level.getBlockState(w2).getBlock().getName().getString();
            // a block it can't carve: it says why (#163)
            BlockPos chest = c.at(6, 1, 1);
            level.setBlock(chest, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
            refused = String.valueOf(mods.eln.node.voxel.ChiselItem.carve(player, level, chest,
                new net.minecraft.world.phys.Vec3(chest.getX(), chest.getY() + 0.5, chest.getZ() + 0.5), net.minecraft.core.Direction.WEST, 2, true));
        }

        public void measure(TestContext c) {
            c.check("chisel: a route through the carved hole is laid", err == null, String.valueOf(err));
            c.check("chisel: the wall block is carved stone holding cable", wall.startsWith("Stone, carved 512, parts "), wall);
            c.check("chisel: right-click puts material back", !back.startsWith(back.substring(back.indexOf("-> ") + 3) + " "), back);
            c.check("chisel: carved and put back whole: stone again", whole.equals("carved true, after putting back: Stone"), whole);
            c.check("chisel: on a chest it says why not", refused.startsWith("can't carve Chest: "), refused);
        }
    }

    /**
     * The chisel's brush shapes on stone (Baughn): a 4-voxel flat layer twice (16 voxels a layer), a 4-voxel tunnel
     * through a block (4 x 4 x 16) and a tunnel put back from the hole's floor (the 4 x 4 column across the hole);
     * sneak + right-click no longer changes the brush, it puts material back like a plain right-click.
     */
    static final class ChiselShapesCase implements TestCase {
        String flat, tunnel, sneak, packet, cache;

        public String name() {
            return "chisel brush shapes";
        }

        public int ticks() {
            return 5;
        }

        static int carved(net.minecraft.world.level.Level level, BlockPos p) {
            var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, level));
            return n instanceof mods.eln.node.voxel.VoxelNode v && v.carved != null ? v.carved.cardinality() : -1;
        }

        public void build(TestContext c) {
            var level = c.level();
            var west = net.minecraft.core.Direction.WEST;
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            var stack = new net.minecraft.world.item.ItemStack(mods.eln.registry.ElnRegistry.CHISEL.get());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            BlockPos f = c.at(2, 1, 1), t = c.at(4, 1, 1), s = c.at(6, 1, 1);
            c.floor(f);
            c.floor(t);
            c.floor(s);
            var flatShape = mods.eln.node.voxel.ChiselBrush.Shape.FLAT;
            var tunnelShape = mods.eln.node.voxel.ChiselBrush.Shape.TUNNEL;
            // flat: the west face, then one voxel deeper (the face the ray would hit next)
            mods.eln.node.voxel.ChiselItem.carve(player, level, f, new net.minecraft.world.phys.Vec3(f.getX(), f.getY() + 0.5, f.getZ() + 0.5), west, 4, flatShape, true);
            int one = carved(level, f);
            mods.eln.node.voxel.ChiselItem.carve(player, level, f, new net.minecraft.world.phys.Vec3(f.getX() + 1 / 16.0, f.getY() + 0.5, f.getZ() + 0.5), west, 4, flatShape, true);
            flat = one + ", " + carved(level, f);
            // tunnel through, then put back from the hole's floor (y = 8): the column x 4..8, z 8..12 across the hole
            mods.eln.node.voxel.ChiselItem.carve(player, level, t, new net.minecraft.world.phys.Vec3(t.getX(), t.getY() + 0.5, t.getZ() + 0.5), west, 4, tunnelShape, true);
            int through = carved(level, t);
            mods.eln.node.voxel.ChiselItem.carve(player, level, t, new net.minecraft.world.phys.Vec3(t.getX() + 5.5 / 16, t.getY() + 0.5, t.getZ() + 9.5 / 16),
                net.minecraft.core.Direction.UP, 4, tunnelShape, false);
            tunnel = through + ", " + carved(level, t);
            // sneak + right-click: a plain right-click (put back 1 voxel), the brush unchanged
            mods.eln.node.voxel.ChiselItem.setBrush(stack, 1, mods.eln.node.voxel.ChiselBrush.Shape.CUBE);
            player.setShiftKeyDown(true);
            int before = carved(level, t);
            var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(t.getX() + 1.5 / 16, t.getY() + 0.5, t.getZ() + 9.5 / 16),
                net.minecraft.core.Direction.UP, t, false);
            stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
            stack.use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
            var hitS = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(s.getX(), s.getY() + 0.5, s.getZ() + 0.5), west, s, false);
            stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hitS));
            player.setShiftKeyDown(false);
            sneak = "brush " + mods.eln.node.voxel.ChiselItem.size(stack) + " " + mods.eln.node.voxel.ChiselItem.shape(stack)
                + ", put back " + (before - carved(level, t)) + ", plain block " + level.getBlockState(s).getBlock().getName().getString()
                + ", name " + stack.getHoverName().getString();
            // Baughn: holding the chisel lagged everyone near. A second stroke in the same tick does nothing, and a
            // carved block's shapes are kept until it changes, and match the box-by-box union they replace
            var ve = (mods.eln.node.voxel.VoxelNodeEntity) level.getBlockEntity(t);
            var sh1 = ve.shapes();
            int c0 = carved(level, t);
            stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
            int c1 = carved(level, t);
            var sh2 = ve.shapes();
            mods.eln.node.voxel.ChiselItem.carve(player, level, t, new net.minecraft.world.phys.Vec3(t.getX(), t.getY() + 0.1, t.getZ() + 0.1), west, 1,
                mods.eln.node.voxel.ChiselBrush.Shape.CUBE, true);
            var sh3 = ve.shapes();
            var st = level.getBlockState(t);
            boolean query = st.getCollisionShape(level, t) == st.getCollisionShape(level, t) && st.getShape(level, t) == st.getShape(level, t);
            var old = net.minecraft.world.phys.shapes.Shapes.empty();
            for (int[] b : ve.hostBoxes())
                old = net.minecraft.world.phys.shapes.Shapes.or(old, net.minecraft.world.level.block.Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
            boolean same = !net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(old, sh3.carvedHost(), net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME);
            cache = "held " + (c0 - c1) + ", kept " + (sh1 == sh2) + ", renewed " + (sh3 != sh2) + ", carved " + (carved(level, t) - c1)
                + ", same shape " + same + ", queries cached " + query;
            // the radial menu's packet: applied to the held chisel; a bad size or shape, or no chisel in hand, ignored
            var handler = (java.util.function.Consumer<byte[]>) b -> mods.eln.network.ServerPacketHandler.handle(b, player);
            handler.accept(mods.eln.node.voxel.ChiselItem.brushPacket(8, tunnelShape));
            packet = mods.eln.node.voxel.ChiselItem.size(stack) + " " + mods.eln.node.voxel.ChiselItem.shape(stack);
            handler.accept(new byte[]{mods.eln.Eln.packetChiselBrush, 3, 1});
            handler.accept(new byte[]{mods.eln.Eln.packetChiselBrush, 4, 7});
            handler.accept(new byte[]{mods.eln.Eln.packetChiselBrush, 4});
            packet += ", bad: " + mods.eln.node.voxel.ChiselItem.size(stack) + " " + mods.eln.node.voxel.ChiselItem.shape(stack);
            var stone = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stone);
            handler.accept(mods.eln.node.voxel.ChiselItem.brushPacket(2, flatShape));
            packet += ", no chisel: " + stone.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA) + " " + mods.eln.node.voxel.ChiselItem.size(stack);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        }

        public void measure(TestContext c) {
            c.check("chisel: a 4-voxel flat layer is 16 voxels, the next layer 16 more", flat.equals("16, 32"), flat);
            c.check("chisel: a 4-voxel tunnel is 256 voxels; put back from the floor fills the 64 across the hole", tunnel.equals("256, 192"), tunnel);
            c.check("chisel: sneak + right-click puts back and keeps the brush", sneak.equals("brush 1 CUBE, put back 1, plain block Stone, name Chisel (1, cube)"), sneak);
            c.check("chisel: one stroke a tick at most; shapes cached until a carve, and right", cache.equals("held 0, kept true, renewed true, carved 1, same shape true, queries cached true"), cache);
            c.check("chisel: the brush packet sets the held chisel only, and only to a valid brush", packet.equals("8 TUNNEL, bad: 8 TUNNEL, no chisel: false 8"), packet);
        }
    }

    /** A cable through the middle of a fence line: the fence it passes becomes a voxel block that still joins its real
     *  neighbours both ways (the voxel block variant carries the fence tags). */
    static final class FenceJoinCase implements TestCase {
        String err, res;

        public String name() {
            return "fence joins an imitated fence";
        }

        public int width() {
            return 8;
        }

        public int ticks() {
            return 10;
        }

        public void build(TestContext c) {
            var level = c.level();
            var fence = net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState();
            for (int z = 0; z <= 2; z++) level.setBlock(c.at(3, 1, z), fence, 3);
            for (int z = 0; z <= 2; z++)
                level.setBlock(c.at(3, 1, z), net.minecraft.world.level.block.Block.updateFromNeighbourShapes(fence, level, c.at(3, 1, z)), 3);
            BlockPos a = c.at(1, 1, 1), b = c.at(6, 1, 1);
            var start = new mods.eln.node.voxel.CableRouter.End(a.getX() * 16L + 8, a.getY() * 16L, a.getZ() * 16L + 8, 0, -1);
            var end = new mods.eln.node.voxel.CableRouter.End(b.getX() * 16L + 8, b.getY() * 16L, b.getZ() * 16L + 8, 0, -1);
            err = mods.eln.node.voxel.CablePlacer.routeAndLayForTests(level, LV, start, end);
        }

        public void measure(TestContext c) {
            var level = c.level();
            // the fence the cable went through (a voxel block now) and a real fence next to it
            res = "no fence imitated";
            for (int z = 0; z <= 2; z++) {
                var v = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(3, 1, z), level));
                if (!(v instanceof mods.eln.node.voxel.VoxelNode vn) || vn.host == null) continue;
                int nz = z == 1 ? 0 : 1;
                var real = level.getBlockState(c.at(3, 1, nz));
                var toReal = nz < z ? net.minecraft.world.level.block.FenceBlock.NORTH : net.minecraft.world.level.block.FenceBlock.SOUTH;
                var toMe = nz < z ? net.minecraft.world.level.block.FenceBlock.SOUTH : net.minecraft.world.level.block.FenceBlock.NORTH;
                res = "imitated " + level.getBlockState(c.at(3, 1, z)).getBlock().getDescriptionId() + ", real fence joins it "
                    + real.getOptionalValue(toMe).orElse(false) + ", imitated joins back " + vn.host.getOptionalValue(toReal).orElse(false);
            }
            c.check("fence join: laid", err == null, String.valueOf(err));
            c.check("fence join: the real fence and the imitated one join both ways",
                res.equals("imitated block.eln.voxel_node_wooden_fence, real fence joins it true, imitated joins back true"), res);
        }
    }

    /**
     * Incognito's spawn lamp (2026-10-18: dark after a live conversion, lit again after a restart): a lamp socket on a
     * wall fed round two outer corners (wallCorners with the socket for the resistor); converted (and reverted), it is
     * still lit.
     */
    static final class LampCornerCase implements TestCase {
        /** 0: six-node (the control), 1: converted, 2: converted and reverted. */
        final int mode;
        final boolean revert;
        mods.eln.sixnode.lampsocket.LampSocketElement lamp;
        BlockPos at;
        int converted;

        LampCornerCase(int mode) {
            this.mode = mode;
            this.revert = mode == 2;
        }

        public String name() {
            return mode == 0 ? "six-node lamp round two corners" : revert ? "reverted lamp round two corners" : "converted lamp round two corners";
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
            return 40;
        }

        public void build(TestContext c) {
            int z = 6;
            for (int[] b : new int[][]{{6, 1, z + 1}, {5, 1, z + 1}, {4, 2, z + 1}}) c.floor(c.at(b[0], b[1], b[2]));
            mods.eln.misc.Direction wall = mods.eln.misc.Direction.ZP;
            var src = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) c.placeSix(SOURCE, c.at(6, 1, z), wall);
            src.networkUnserialize(c.stream(out -> {
                out.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                out.writeFloat(50);
            }));
            c.placeSix(LV_CABLE, c.at(5, 1, z), wall);
            c.placeSix(LV_CABLE, c.at(5, 2, z + 1), mods.eln.misc.Direction.YN);
            c.placeSix(LV_CABLE, c.at(5, 2, z + 1), mods.eln.misc.Direction.XN);
            at = c.at(4, 2, z);
            lamp = (mods.eln.sixnode.lampsocket.LampSocketElement) c.placeSix(0 + (LampLightCases.LAMP_ID << 6), at, wall);
            net.minecraft.world.item.ItemStack bulb = null;
            for (int i = 0; i < 64 * 128 && bulb == null; i++) {
                var d = mods.eln.Eln.sharedItem.getDescriptor(i);
                if (d != null && "Small 50V Economic Light Bulb".equals(d.name)) bulb = d.newItemStack();
            }
            lamp.getInventory().setItem(0, bulb);
            lamp.getInventory().setChanged();
            if ((Boolean) mods.eln.gametest.LabCommands.field(lamp, "poweredByLampSupply"))   // (by cable, as Incognito's)
                lamp.networkUnserialize(c.stream(out -> out.writeByte(3)));   // (tooglePowerSupplyType)
            if (mode == 0) return;
            for (BlockPos p : new BlockPos[]{c.at(4, 2, z), c.at(5, 1, z), c.at(5, 2, z + 1), c.at(6, 1, z)}) {
                var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, c.level()));
                if (n instanceof mods.eln.node.six.SixNode s && !(n instanceof mods.eln.node.voxel.VoxelNode)
                    && mods.eln.node.voxel.CableConverter.convert(s) == null) converted++;
            }
            if (revert)
                for (BlockPos p : new BlockPos[]{c.at(4, 2, z), c.at(5, 1, z), c.at(5, 2, z + 1), c.at(6, 1, z), c.at(5, 2, z)})
                    if (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, c.level())) instanceof mods.eln.node.voxel.VoxelNode v) {
                        if (v.legacy != null) mods.eln.node.voxel.CableConverter.revert(v);
                    }
        }

        public void measure(TestContext c) {
            var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(at, c.level()));
            var e = n instanceof mods.eln.node.six.SixNode s ? s.getElement(mods.eln.misc.Direction.ZP) : null;
            double u = e instanceof mods.eln.sixnode.lampsocket.LampSocketElement l ? l.positiveLoad.getU() : -1;
            c.check(name() + ": the lamp is powered (" + converted + " blocks converted)", u > 40, String.format("U %.2f V, node %s", u, n == null ? "-" : n.getClass().getSimpleName()));
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
