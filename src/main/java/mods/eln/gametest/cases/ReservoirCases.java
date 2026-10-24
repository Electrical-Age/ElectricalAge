package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.reservoir.ArrayBasin;
import mods.eln.reservoir.Basin;
import mods.eln.reservoir.Reservoir;
import mods.eln.reservoir.ReservoirData;
import mods.eln.reservoir.ReservoirWater;
import mods.eln.reservoir.Reservoirs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

/**
 * Reservoirs (mods.eln.reservoir; RESERVOIR-PLAN-2.md): water lakes (vanilla full cells, a partial reservoir-water
 * surface, waterlogging, buckets from the record, dams that refill), calibration from a basin's estimate to the truth,
 * and the surface lookup at Watershed's scale. A 5 x 3 lake 3 deep from y 1 in the test box.
 */
public final class ReservoirCases {
    private ReservoirCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Water());
        cases.add(new Calibrate());
        cases.add(new Scale());
    }

    static final int SX = 5, SZ = 3;

    static int[] col(int v) {
        int[] a = new int[SX * SZ];
        java.util.Arrays.fill(a, v);
        return a;
    }

    static void sweep(TestContext ctx, Reservoir r) {
        BlockPos a = ctx.at(1, 1, 0), b = ctx.at(SX, 1, SZ - 1);
        for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++)
            for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++)
                Reservoirs.sweep(ctx.level(), r, cx, cz);
    }

    /** Water: the surface half way up the middle layer, then full; waterlogging, a bucket, a dam. */
    static final class Water implements TestCase {
        Reservoir r;

        public String id() {
            return "reservoir_water";
        }

        public int width() {
            return 7;
        }

        public void build(TestContext ctx) {
            BlockPos o = ctx.at(1, 1, 0);
            int y1 = o.getY();
            int[][] a = ArrayBasin.area(col(y1), col(3));
            r = ReservoirData.get(ctx.level()).create(ctx.level(), null, ReservoirWater.KIND, ArrayBasin.TYPE,
                ArrayBasin.spec(o.getX(), o.getZ(), SX, SZ, col(y1), col(3)), a[0][0], a[1], 0);
            // a slab in the middle layer, stairs in the top one: waterloggable blocks are part of the lake
            ctx.level().setBlock(ctx.at(2, 2, 1), Blocks.OAK_SLAB.defaultBlockState(), 3);
            ctx.level().setBlock(ctx.at(4, 3, 1), Blocks.OAK_STAIRS.defaultBlockState(), 3);
        }

        public void measure(TestContext ctx) {
            var level = ctx.level();
            int y1 = ctx.at(0, 1, 0).getY();
            ctx.check("water: capacity 5 x 3 x 3 cells", r.capacity() == 45_000, "" + r.capacity());
            Reservoirs.setAmount(level, r, r.volumeAt((y1 + 1) * 8 + 4));
            sweep(ctx, r);
            BlockState low = level.getBlockState(ctx.at(1, 1, 0)), mid = level.getBlockState(ctx.at(1, 2, 0)), top = level.getBlockState(ctx.at(1, 3, 0));
            ctx.check("water: full cells are vanilla water sources", low.is(Blocks.WATER) && low.getFluidState().isSource(), low.toString());
            ctx.check("water: the surface cell is reservoir water at 4/8", mid.is(ReservoirWater.BLOCK.get())
                && mid.getValue(BlockStateProperties.LEVEL_FLOWING) == 4, mid.toString());
            ctx.check("water: above the surface, air", top.isAir(), top.toString());
            ctx.check("water: the slab at the surface is waterlogged", level.getBlockState(ctx.at(2, 2, 1)).getValue(BlockStateProperties.WATERLOGGED), "");
            ctx.check("water: the stairs above it aren't", !level.getBlockState(ctx.at(4, 3, 1)).getValue(BlockStateProperties.WATERLOGGED), "");
            ctx.check("water: vanilla water and reservoir water are one fluid (mixin)", Fluids.WATER.isSame(ReservoirWater.FLUID.get()), "");
            ctx.check("water: reservoir water is water (tag)", mid.getFluidState().is(net.minecraft.tags.FluidTags.WATER), "");

            Reservoirs.setAmount(level, r, r.capacity());
            sweep(ctx, r);
            ctx.check("water: full, the top layer is vanilla water", level.getBlockState(ctx.at(1, 3, 0)).is(Blocks.WATER), "");
            ctx.check("water: full, the stairs are waterlogged", level.getBlockState(ctx.at(4, 3, 1)).getValue(BlockStateProperties.WATERLOGGED), "");

            // a bucket from a vanilla source cell takes from the record (mixin on LiquidBlock)
            BlockPos cell = ctx.at(3, 3, 2);
            long before = r.amount();
            ItemStack got = ((LiquidBlock) Blocks.WATER).pickupBlock(null, level, cell, level.getBlockState(cell));
            ctx.check("water: a bucket of water", got.is(Items.WATER_BUCKET), got.toString());
            ctx.check("water: the record lost 1000 mB", r.amount() == before - 1000, "" + r.amount());
            Reservoirs.setAmount(level, r, 500);
            sweep(ctx, r);
            BlockPos under = ctx.at(3, 1, 2);
            BlockState us = level.getBlockState(under);
            ItemStack none = us.getBlock() instanceof LiquidBlock lb ? lb.pickupBlock(null, level, under, us)
                : us.getBlock() instanceof net.minecraft.world.level.block.BucketPickup bp ? bp.pickupBlock(null, level, under, us) : ItemStack.EMPTY;
            ctx.check("water: from a nearly empty lake, no bucket", none.isEmpty() && r.amount() == 500, none + ", " + r.amount() + " mB, " + us);

            // poured back: onto the partial surface (the block takes it), or as a source above it (the sweep takes it)
            Reservoirs.setAmount(level, r, r.volumeAt((y1 + 1) * 8 + 4));
            sweep(ctx, r);
            long half = r.amount();
            BlockPos surf = ctx.at(1, 2, 0);
            ((net.minecraft.world.level.block.LiquidBlockContainer) ReservoirWater.BLOCK.get()).placeLiquid(level, surf, level.getBlockState(surf),
                Fluids.WATER.getSource(false));
            ctx.check("water: a bucket poured on the surface goes into the record", r.amount() == half + 1000, "" + r.amount());
            level.setBlock(ctx.at(3, 3, 0), Blocks.WATER.defaultBlockState(), 3);
            sweep(ctx, r);
            ctx.check("water: a source poured above the surface joins the record", r.amount() == half + 2000, "" + r.amount());

            // a dam: the cell leaves the area, the amount stays; gone, it fills again
            Reservoirs.setAmount(level, r, r.capacity());
            sweep(ctx, r);
            long cap = r.capacity();
            BlockPos d = ctx.at(5, 2, 0);
            level.setBlock(d, Blocks.COBBLESTONE.defaultBlockState(), 3);
            sweep(ctx, r);
            ctx.check("water: a dam takes its cell out of the area", r.capacity() == cap - 1000, r.capacity() + " vs " + cap);
            level.setBlock(d, Blocks.AIR.defaultBlockState(), 3);
            sweep(ctx, r);
            ctx.check("water: the dam gone, the cell is water again", r.capacity() == cap && level.getBlockState(d).is(Blocks.WATER),
                r.capacity() + ", " + level.getBlockState(d));
            ReservoirData.get(level).remove(r);
        }
    }

    /** A basin whose estimate is off (one layer where there are two): the first sweep corrects the area table. */
    static final class Calibrate implements TestCase {
        Reservoir r;
        static final String TYPE = "eln:test_off_estimate";

        public String id() {
            return "reservoir_calibrate";
        }

        public int width() {
            return 7;
        }

        public void build(TestContext ctx) {
            Reservoirs.registerBasin(TYPE, (l, spec) -> {
                ArrayBasin truth = (ArrayBasin) Reservoirs.basinOf(l, ArrayBasin.TYPE, spec);
                return new Basin() {
                    public long[] chunks() {
                        return truth.chunks();
                    }

                    public void cells(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk, short[] b, short[] t) {
                        truth.cells(level, chunk, b, t);
                    }

                    public int[] estimate(int cx, int cz, int minY, int h) {
                        int[] e = truth.estimate(cx, cz, minY, h);
                        if (e.length > 1) e[1] = 0;   // (the estimate missed the second layer)
                        return e;
                    }
                };
            });
            BlockPos o = ctx.at(1, 1, 0);
            int y1 = o.getY();
            int[] area = {SX * SZ};   // the (wrong) estimate: one layer
            r = ReservoirData.get(ctx.level()).create(ctx.level(), null, ReservoirWater.KIND, TYPE,
                ArrayBasin.spec(o.getX(), o.getZ(), SX, SZ, col(y1), col(2)), y1, area, Long.MAX_VALUE);
        }

        public void measure(TestContext ctx) {
            ctx.check("calibrate: before any sweep, the estimate (15 cells)", r.capacity() == 15_000, "" + r.capacity());
            sweep(ctx, r);
            ctx.check("calibrate: after the sweep, the truth (30 cells)", r.capacity() == 30_000, "" + r.capacity());
            sweep(ctx, r);
            ctx.check("calibrate: once only", r.capacity() == 30_000, "" + r.capacity());
            ReservoirData.get(ctx.level()).remove(r);
        }
    }

    /** Watershed's scale: a lake of 2M cells over 64 levels; the surface is a quick lookup. */
    static final class Scale implements TestCase {
        public String id() {
            return "reservoir_scale";
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            int[] area = new int[64];
            for (int k = 0; k < 64; k++) area[k] = 10_000 + 500 * k;   // (a bowl: wider higher up)
            Reservoir r = ReservoirData.get(ctx.level()).create(ctx.level(), null, ReservoirWater.KIND, ArrayBasin.TYPE,
                ArrayBasin.spec(0, 0, 0, 0, new int[0], new int[0]), 60, area, 0);
            long cap = r.capacity();
            long t0 = System.nanoTime();
            int s = 0;
            for (int i = 0; i < 10_000; i++) {
                Reservoirs.setAmountQuiet(r, cap * (i % 97) / 97);
                s += r.surface();
            }
            double us = (System.nanoTime() - t0) / 1000.0 / 10_000;
            ctx.check("scale: 10k surface lookups on a 1.6M-cell lake under 20 us each", us < 20, String.format("%.2f us (%d)", us, s));
            Reservoirs.setAmountQuiet(r, r.volumeAt(80 * 8 + 3));
            ctx.check("scale: volume and surface agree", r.surface() == 80 * 8 + 3, "" + r.surface());
            ReservoirData.get(ctx.level()).remove(r);
        }
    }
}
