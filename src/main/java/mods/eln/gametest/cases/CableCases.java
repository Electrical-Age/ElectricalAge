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
        cases.add(new HostedCase());
        cases.add(new BreakMixedCase());
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

        final boolean revert;
        int voxelLeft = -1;

        ConvertCase(String name, Layout layout, boolean revert) {
            this.name = name;
            this.layout = layout;
            this.revert = revert;
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

    /** Cable kits from coils and back, merging; breaking a laid piece gives its length back. */
    static final class KitCase implements TestCase {
        public String name() {
            return "cable kits";
        }

        public void build(TestContext c) {
        }

        public void measure(TestContext c) {
            var coil = mods.eln.node.voxel.CableKitRecipes.coil(LV);
            var in = net.minecraft.world.item.crafting.CraftingInput.of(2, 2, List.of(coil.copy(), coil.copy(), coil.copy(), net.minecraft.world.item.ItemStack.EMPTY));
            var kit = mods.eln.node.voxel.CableKitRecipes.FromCoils.INSTANCE.assemble(in);
            c.check("cable kit: three coils make a 48-voxel kit", mods.eln.node.voxel.KitItem.length(kit) == 48 && kit.getItem() instanceof mods.eln.node.voxel.CableKitItem k && k.tier == LV,
                kit + " " + mods.eln.node.voxel.KitItem.length(kit));
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
            return 3;
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
        }

        public void measure(TestContext c) {
            c.check("mixed block: mining the switch takes the switch only", result.startsWith("block removed false, node VoxelNode, parts 1, floor element false"), result);
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
