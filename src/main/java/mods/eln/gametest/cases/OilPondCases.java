package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.oil.Oil;
import mods.eln.oil.OilPonds;
import mods.eln.oil.OilWorldgen;
import mods.eln.reservoir.Reservoir;
import mods.eln.reservoir.ReservoirData;
import mods.eln.reservoir.Reservoirs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Oil ponds over half the oil lakes (Baughn 2026-10-28): on flat natural ground a pond is dug, rimmed with tar sand and
 * filled as its own small reservoir; a build in the way means no pond; the 50% roll is about half.
 */
public final class OilPondCases {
    private OilPondCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new PondCase(false));
        cases.add(new PondCase(true));
        cases.add(new TestCase() {
            public String id() {
                return "oil_pond_roll";
            }

            public void build(TestContext ctx) {
            }

            public void measure(TestContext ctx) {
                int n = 0;
                for (int i = 0; i < 1000; i++) if (OilPonds.rolls("oil:" + (i * 7919L << 20 ^ i))) n++;
                ctx.check("about half the lakes get a pond", n > 420 && n < 580, n + " of 1000");
            }
        });
    }

    static final class PondCase implements TestCase {
        final boolean build;
        OilPonds.Result result;
        String key;

        PondCase(boolean build) {
            this.build = build;
        }

        public String id() {
            return build ? "oil_pond_spares_builds" : "oil_pond_placed";
        }

        public int width() {
            return 24;
        }

        public int height() {
            return 8;
        }

        public int depth() {
            return 24;
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext ctx) {
            // a meadow: dirt, grass on top at y 5, a few flowers
            for (int x = 0; x < 24; x++)
                for (int z = 0; z < 24; z++) {
                    for (int y = 1; y < 5; y++) ctx.level().setBlock(ctx.at(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
                    ctx.level().setBlock(ctx.at(x, 5, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    if ((x * 31 + z * 17) % 11 == 0) ctx.level().setBlock(ctx.at(x, 6, z), Blocks.POPPY.defaultBlockState(), 2);
                }
            // (the test box's barrier lid would be the "ground" seen from above)
            for (int x = 0; x < 24; x++)
                for (int z = 0; z < 24; z++)
                    for (int y = 6; y < 16; y++) {
                        BlockPos q = ctx.at(x, y, z);
                        if (ctx.level().getBlockState(q).is(Blocks.BARRIER)) ctx.level().setBlock(q, Blocks.AIR.defaultBlockState(), 2);
                    }
            if (build) ctx.level().setBlock(ctx.at(12, 6, 12), Blocks.OAK_PLANKS.defaultBlockState(), 2);
            BlockPos c = ctx.at(12, 0, 12);
            key = "oil:test-pond-" + System.nanoTime();
            result = OilPonds.place(ctx.level(), key, c.getX(), c.getZ(), 0, 12345);
        }

        public void measure(TestContext ctx) {
            String pondKey = "oil-pond:" + key.substring(4);
            Reservoir r = ReservoirData.get(ctx.level()).byKey(pondKey);
            if (build) {
                ctx.check("a plank on the spot: no pond", result == OilPonds.Result.NO_SPOT && r == null, result + ", record " + r);
                ctx.check("the plank is still there", ctx.level().getBlockState(ctx.at(12, 6, 12)).is(Blocks.OAK_PLANKS), "");
                return;
            }
            ctx.check("a pond is placed", result == OilPonds.Result.PLACED && r != null, result + ", record " + r);
            if (r == null) return;
            BlockPos c = ctx.at(12, 0, 12);
            for (int cx = (c.getX() - 12) >> 4; cx <= (c.getX() + 12) >> 4; cx++)
                for (int cz = (c.getZ() - 12) >> 4; cz <= (c.getZ() + 12) >> 4; cz++) Reservoirs.sweep(ctx.level(), r, cx, cz);
            int oil = 0, tar = 0, open = 0, topOil = Integer.MIN_VALUE, holes = 0;
            for (int x = 0; x < 24; x++)
                for (int z = 0; z < 24; z++)
                    for (int y = 0; y < 8; y++) {
                        var s = ctx.level().getBlockState(ctx.at(x, y, z));
                        if (s.is(Oil.RESERVOIR_BLOCK.get())) {
                            oil++;
                            if (ctx.level().getBlockState(ctx.at(x, y + 1, z)).isAir()) open++;
                            topOil = Math.max(topOil, ctx.at(x, y, z).getY());
                        }
                        if (s.is(OilWorldgen.TAR_SAND.get())) tar++;
                    }
            // no pits around it: every column's ground-level block (y 5) is oil, tar sand or still ground
            for (int x = 0; x < 24; x++)
                for (int z = 0; z < 24; z++)
                    if (ctx.level().getBlockState(ctx.at(x, 5, z)).isAir()) holes++;
            long buckets = r.capacity() / 1000;
            String detail = buckets + " buckets, " + oil + " oil blocks (" + open + " open to the sky), " + tar + " tar sand, surface y "
                + r.surface() + ", top oil y " + topOil + " (ground top " + ctx.at(0, 5, 0).getY() + "), " + holes + " holes at ground level";
            ctx.line(detail);
            ctx.check("a small reservoir (20-400 buckets), full", buckets >= 20 && buckets <= 400 && r.amount() == r.capacity(), detail);
            ctx.check("its oil is in the world, open to the sky", oil == buckets && open > 10, detail);
            ctx.check("rimmed with tar sand", tar > 10, detail);
            ctx.check("level with the ground, like a vanilla lake, no pits around it", topOil == ctx.at(0, 5, 0).getY() && holes == 0, detail);
            ReservoirData.get(ctx.level()).remove(r);
        }
    }
}
