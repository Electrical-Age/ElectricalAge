package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.oil.Oil;
import mods.eln.oil.Reservoir;
import mods.eln.oil.ReservoirData;
import mods.eln.oil.Reservoirs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

/**
 * Oil lakes (design v2 §2): the pooled volume and the lockstep surface. A small lake in the test box: 7 x 3 columns,
 * odd x two cells deep from y 1, even x one cell from y 2 (stone below), so the floor is uneven.
 */
public final class OilCases {
    private OilCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Lockstep());
        cases.add(new Buckets());
        cases.add(new Dam());
        cases.add(new CatchUp());
        cases.add(new LakeSizes());
        cases.add(new Separator());
        cases.add(new Pump(false));
        cases.add(new Pump(true));
    }

    /** Build the lake (full) and sweep it in. */
    static Reservoir lake(TestContext ctx) {
        int x0 = ctx.at(1, 0, 0).getX(), z0 = ctx.at(0, 0, 0).getZ(), y1 = ctx.at(0, 1, 0).getY();
        int[] floor = new int[7 * 3], mask = new int[7 * 3];
        for (int dx = 0; dx < 7; dx++)
            for (int dz = 0; dz < 3; dz++) {
                boolean deep = (dx + 1) % 2 == 1;
                floor[dx * 3 + dz] = deep ? y1 : y1 + 1;
                mask[dx * 3 + dz] = deep ? 0b11 : 0b1;
                if (!deep) ctx.level().setBlock(new BlockPos(x0 + dx, y1, z0 + dz), Blocks.STONE.defaultBlockState(), 3);
            }
        Reservoir r = ReservoirData.get(ctx.level()).create(x0, z0, 7, 3, floor, mask, Long.MAX_VALUE);
        sweepAll(ctx, r);
        return r;
    }

    static void sweepAll(TestContext ctx, Reservoir r) {
        BlockPos a = ctx.at(1, 0, 0), b = ctx.at(7, 0, 2);
        for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++)
            for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++)
                Reservoirs.sweep(ctx.level(), r, cx, cz);
    }

    static int level(TestContext ctx, int x, int y, int z) {
        BlockState s = ctx.level().getBlockState(ctx.at(x, y, z));
        return s.is(Oil.RESERVOIR_BLOCK.get()) ? s.getValue(BlockStateProperties.LEVEL_FLOWING) : 0;
    }

    static void cleanup(TestContext ctx, Reservoir r) {
        ReservoirData.get(ctx.level()).remove(r);
    }

    abstract static class Base implements TestCase {
        Reservoir r;

        public int width() {
            return 9;
        }

        public int ticks() {
            return 2;
        }
    }

    /** Half way up the second cell: every column shows the same surface (level 4 at y 2; level 8 at y 1 where deep). */
    static final class Lockstep extends Base {
        public String id() {
            return "oil_lake_lockstep";
        }

        public void build(TestContext ctx) {
            r = lake(ctx);
        }

        public void measure(TestContext ctx) {
            long cap = r.capacity();
            ctx.check("oil: full capacity is 4 x 3 deep columns x 2 cells + 3 x 3 shallow x 1 = 33 cells", cap == 33_000, "cap " + cap);
            int y1 = ctx.at(0, 1, 0).getY();
            long half = r.volumeAt((y1 + 1) * 8 + 4);
            ctx.check("oil: the volume half way up the second cell", half == 12 * 12 * 125 + 9 * 4 * 125, "" + half);
            Reservoirs.take(ctx.level(), r, r.amount() - half, false);
            ctx.check("oil: surface at y2 + 4/8", r.surface() == (y1 + 1) * 8 + 4, r.surface() + " vs " + ((y1 + 1) * 8 + 4));
            sweepAll(ctx, r);
            boolean same = true;
            StringBuilder sb = new StringBuilder();
            for (int x = 1; x <= 7; x++)
                for (int z = 0; z <= 2; z++) {
                    int top = level(ctx, x, 2, z), low = level(ctx, x, 1, z);
                    boolean deep = x % 2 == 1;
                    if (top != 4 || (deep && low != 8)) {
                        same = false;
                        sb.append(x).append(',').append(z).append(':').append(low).append('/').append(top).append(' ');
                    }
                }
            ctx.check("oil: every column shows the same surface", same, sb.toString());
            cleanup(ctx, r);
        }
    }

    /** A bucket takes 1000 mB from the record; crude poured back in rejoins it; poured elsewhere it's plain crude. */
    static final class Buckets extends Base {
        public String id() {
            return "oil_lake_buckets";
        }

        public void build(TestContext ctx) {
            r = lake(ctx);
        }

        public void measure(TestContext ctx) {
            long full = r.amount();
            BlockPos cell = ctx.at(3, 2, 1);
            ItemStack got = ((mods.eln.oil.ReservoirOilBlock) Oil.RESERVOIR_BLOCK.get()).pickupBlock(null, ctx.level(), cell, ctx.level().getBlockState(cell));
            ctx.check("oil: a bucket of crude out", got.is(Oil.CRUDE_BUCKET.get()), got.toString());
            ctx.check("oil: the record lost 1000 mB", r.amount() == full - 1000, "" + r.amount());
            ctx.check("oil: the cell is still oil (the surface moves on the next sweep)", level(ctx, 3, 2, 1) > 0, "");
            // pour it back into the lake: a crude source in a lake cell
            ctx.level().setBlock(cell, Oil.CRUDE_BLOCK.get().defaultBlockState(), 3);
            sweepAll(ctx, r);
            ctx.check("oil: poured back, the record is full again", r.amount() == full, "" + r.amount());
            ctx.check("oil: and the cell is lake oil again", level(ctx, 3, 2, 1) > 0, "");
            // poured elsewhere: plain crude, nothing to do with the lake
            BlockPos out = ctx.at(8, 1, 1);
            ctx.level().setBlock(out, Oil.CRUDE_BLOCK.get().defaultBlockState(), 3);
            sweepAll(ctx, r);
            ctx.check("oil: crude poured outside stays crude", ctx.level().getBlockState(out).is(Oil.CRUDE_BLOCK.get()), "");
            ctx.check("oil: and the lake doesn't change", r.amount() == full, "" + r.amount());
            ctx.level().setBlock(out, Blocks.AIR.defaultBlockState(), 3);
            cleanup(ctx, r);
        }
    }

    /** A block placed into the lake is a dam: the cell leaves the lake; the oil stays in the record. */
    static final class Dam extends Base {
        public String id() {
            return "oil_lake_dam";
        }

        public void build(TestContext ctx) {
            r = lake(ctx);
        }

        public void measure(TestContext ctx) {
            long cap = r.capacity();
            Reservoirs.take(ctx.level(), r, 5000, false);
            long amount = r.amount();
            BlockPos cell = ctx.at(5, 1, 0);
            ctx.level().setBlock(cell, Blocks.COBBLESTONE.defaultBlockState(), 3);
            sweepAll(ctx, r);
            ctx.check("oil: the dammed cell left the lake", !r.contains(cell.getX(), cell.getY(), cell.getZ()), "");
            ctx.check("oil: capacity one cell less", r.capacity() == cap - 1000, r.capacity() + " vs " + (cap - 1000));
            ctx.check("oil: the oil it held stays in the record", r.amount() == amount, "" + r.amount());
            ctx.check("oil: the dam block stays", ctx.level().getBlockState(cell).is(Blocks.COBBLESTONE), "");
            cleanup(ctx, r);
        }
    }

    /** The tar sand separator: tar sand -> sand + 100 mB crude in its tank, drainable through the fluid capability. */
    static final class Separator implements TestCase {
        mods.eln.transparentnode.electricalmachine.OilSeparatorElement sep;

        public String id() {
            return "oil_separator";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 600;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            sep = (mods.eln.transparentnode.electricalmachine.OilSeparatorElement) l.device(0 + (80 << 6), 2, 1);
            l.feed(ctx.at(2, 1, 1), sep.front.left());
            sep.getInventory().setItem(sep.inSlotId, new ItemStack(mods.eln.oil.OilWorldgen.TAR_SAND_ITEM.get(), 1));
            sep.getInventory().setChanged();
            // 20 kJ a block at 200 W is 100 s: run it 4x faster for the test (800 W, within the LV source and cable)
            mods.eln.sim.ElectricalStackMachineProcess proc = Wp10aCases.field(sep, "slowRefreshProcess");
            proc.setSpeedUp(4);
        }

        public void measure(TestContext ctx) {
            ItemStack in = sep.getInventory().getItem(sep.inSlotId), out = sep.getInventory().getItem(sep.outSlotId);
            var tank = (mods.eln.fluid.ElementFluidHandler) sep.getFluidHandler();
            mods.eln.sim.ElectricalStackMachineProcess proc = Wp10aCases.field(sep, "slowRefreshProcess");
            ctx.check("oil: separator used the tar sand and made sand", in.isEmpty() && out.is(net.minecraft.world.item.Items.SAND) && out.getCount() == 1,
                "in " + in + ", out " + out + ", recipes " + mods.eln.Eln.separatorRecipes.getRecipes().size() + ", can " + proc.smeltCan()
                    + ", extra " + proc.extraCondition.getAsBoolean() + ", P " + proc.getPower() + ", state " + proc.processState());
            ctx.check("oil: 100 mB crude in its tank", tank.getFluidAmount() == 100, tank.getFluidAmount() + " mB");
            var cap = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, sep.node.coordonate.pos(), null);
            ctx.check("oil: the tank is the block's fluid capability (pipes can drain it)", cap == tank, "" + cap);
        }
    }

    /**
     * The pump on well casing: from a 3 x 3 lake under the test floor (or water) at its 1 kW: crude flow = power / (5% of
     * crude's heat value + a tiny lift), ~7.8 mB/s; the lake's record loses what the tank gains. Water at 20 J/mB.
     */
    static final class Pump implements TestCase {
        final boolean water;
        mods.eln.oil.OilPumpElement pump;
        Reservoir r;
        long before;

        Pump(boolean water) {
            this.water = water;
        }

        public String id() {
            return water ? "oil_pump_water" : "oil_pump_lake";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 100;
        }

        public void build(TestContext ctx) {
            BlockPos casing = ctx.at(2, 0, 1), under = casing.below();
            ctx.level().setBlock(casing, mods.eln.oil.OilWorldgen.WELL_CASING.get().defaultBlockState(), 3);
            if (water) {
                ctx.level().setBlock(under, Blocks.WATER.defaultBlockState(), 3);
            } else {
                // (below the test floor is the structure's barrier layer: clear room for the lake, or it'd be a dam)
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) ctx.level().setBlock(under.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 3);
                int[] floor = new int[9], mask = new int[9];
                java.util.Arrays.fill(floor, under.getY());
                java.util.Arrays.fill(mask, 1);
                r = ReservoirData.get(ctx.level()).create(under.getX() - 1, under.getZ() - 1, 3, 3, floor, mask, Long.MAX_VALUE);
                Reservoirs.sweep(ctx.level(), r, under.getX() >> 4, under.getZ() >> 4);
                before = r.amount();
            }
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            pump = (mods.eln.oil.OilPumpElement) l.device(0 + (81 << 6), 2, 1);
            l.feed(ctx.at(2, 1, 1), pump.front.left());
        }

        public void measure(TestContext ctx) {
            var tank = (mods.eln.fluid.ElementFluidHandler) pump.getFluidHandler();
            int got = tank.getFluidAmount();
            var fluid = tank.getFluid().getFluid();
            BlockPos under = ctx.at(2, 0, 1).below();
            ctx.line("oil: pump " + (water ? "water" : "lake") + ": " + got + " mB in 5 s, " + pump.getWaila() + ", at(under) "
                + ReservoirData.get(ctx.level()).at(under) + ", block under " + ctx.level().getBlockState(under) + ", casing " + ctx.level().getBlockState(ctx.at(2, 0, 1)));
            if (water) {
                // 1000 W / (20 J/mB + lift) ~ 50 mB/s
                ctx.check("oil: the pump draws water", fluid.isSame(net.minecraft.world.level.material.Fluids.WATER) && got > 100, got + " mB of " + fluid);
            } else {
                ctx.check("oil: the pump draws crude", fluid.isSame(Oil.CRUDE.get()) && got > 15 && got < 60, got + " mB of " + fluid);
                ctx.check("oil: the lake lost what the tank gained", before - r.amount() == got, (before - r.amount()) + " vs " + got);
                ReservoirData.get(ctx.level()).remove(r);
            }
        }
    }

    /** Generated lakes (LakeShape): mostly 5-20k buckets (design: a tower-fortnight or a few), every one a real lake. */
    static final class LakeSizes extends Base {
        public String id() {
            return "oil_lake_sizes";
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            var rnd = new java.util.Random(12345);
            int inRange = 0, n = 12;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                int r0 = 28 + rnd.nextInt(29);
                float aspect = 0.7f + 0.6f * rnd.nextFloat();
                var shape = new mods.eln.oil.LakeShape(i, 0, 0, 0, Math.round(r0 * aspect), Math.round(r0 / aspect), rnd.nextInt());
                int[][] cols = shape.columns();
                long cells = 0;
                for (int k = 0; k < cols[1].length; k++) cells += Integer.bitCount(cols[1][k]);
                sb.append(cells).append(' ');
                if (cells >= 4000 && cells <= 25000) inRange++;
                ctx.check("oil: lake " + i + " has oil", cells > 500, "" + cells);
            }
            ctx.line("oil: lake sizes (buckets): " + sb);
            ctx.check("oil: most lakes hold 4-25k buckets", inRange >= n * 2 / 3, inRange + " of " + n + ": " + sb);
            // a lake below y 0 (negative heights): full means the surface at its top
            var deep = ReservoirData.get(ctx.level()).create(0, 0, 2, 1, new int[]{-21, -22}, new int[]{0b111, 0b1111}, Long.MAX_VALUE);
            ctx.check("oil: below y 0, a full lake's surface is its top (y -18)", deep.surface() == -18 * 8, "" + deep.surface());
            Reservoirs.take(ctx.level(), deep, 2000, false);
            ctx.check("oil: below y 0, draining 2 of 7 cells lowers it to y -19", deep.surface() == -19 * 8, "" + deep.surface());
            ReservoirData.get(ctx.level()).remove(deep);
            ctx.check("oil: the lake structure is registered",
                ctx.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                    .get(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "oil_lake")).isPresent(), "");
        }
    }

    /** A chunk swept after the lake moved (as when it loads) shows the current level. */
    static final class CatchUp extends Base {
        public String id() {
            return "oil_lake_catch_up";
        }

        public void build(TestContext ctx) {
            r = lake(ctx);
        }

        public void measure(TestContext ctx) {
            int y1 = ctx.at(0, 1, 0).getY();
            Reservoirs.take(ctx.level(), r, r.amount() - r.volumeAt((y1 + 1) * 8 + 2), false);
            // stale blocks: still full (as if the chunk was unloaded while the lake drained)
            ctx.check("oil: before the sweep, stale", level(ctx, 1, 2, 0) == 8, "");
            sweepAll(ctx, r);
            ctx.check("oil: after the sweep, at the record's level", level(ctx, 1, 2, 0) == 2 && level(ctx, 7, 2, 2) == 2,
                level(ctx, 1, 2, 0) + " / " + level(ctx, 7, 2, 2));
            cleanup(ctx, r);
        }
    }
}
