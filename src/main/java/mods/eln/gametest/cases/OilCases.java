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
