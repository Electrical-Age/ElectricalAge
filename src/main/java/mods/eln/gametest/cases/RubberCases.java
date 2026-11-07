package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/** The rubber chain (Baughn 2026-11-07): vanilla sulfur macerates to Sulfur Dust (twice an ore's dust), soul sand to
 *  Soul Dust; resin smelts to Raw Rubber, which smelts to 1 Rubber; four Raw Rubber, a Sulfur Dust and a Soul Dust make
 *  a Rubber Compound that vulcanises in the Electrical Furnace to 8 Rubber. */
public final class RubberCases {
    private RubberCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new ChainCase());
        cases.add(new TreeCase());
    }

    /** Rubber trees (Baughn 2026-11-07): the sapling grows a rubber fig (Ficus elastica); a collector gets 1.5x the old rate on its trunk and
     *  half on other trees; the compressor takes two logs a resin (two rubber logs: three); the grove is in the worldgen
     *  registries; a sapling is crafted from jungle, oak, birch and acacia saplings. */
    static final class TreeCase implements TestCase {
        String grown = "";

        public String id() {
            return "rubber_tree";
        }

        public int width() {
            return 21;
        }

        public int depth() {
            return 21;
        }

        public int height() {
            return 20;   // (a rubber fig: a trunk up to 13, its crown, and 8 blocks of reach each way)
        }

        public void build(TestContext c) {
            var level = c.level();
            var ground = c.at(10, 1, 10);
            for (var g : net.minecraft.core.BlockPos.betweenClosed(c.at(1, 1, 1), c.at(19, 1, 19)))
                level.setBlock(g, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            var pos = ground.above();
            var sap = mods.eln.rubber.RubberTrees.SAPLING.get().defaultBlockState();
            level.setBlock(pos, sap, 3);
            boolean ok = mods.eln.rubber.RubberTrees.GROWER.growTree(level, level.getChunkSource().getGenerator(), pos, sap, level.getRandom());
            int logs = 0;
            for (int y = 0; y < 16; y++) if (level.getBlockState(pos.above(y)).is(mods.eln.rubber.RubberTrees.LOG.get())) logs++;
            int leaves = 0, roots = 0, wide = 0, decaying = 0;
            for (var p : net.minecraft.core.BlockPos.betweenClosed(pos.offset(-8, -1, -8), pos.offset(8, 17, 8))) {
                var st = level.getBlockState(p);
                if (st.is(mods.eln.rubber.RubberTrees.LEAVES.get())) {
                    leaves++;
                    if (Math.max(Math.abs(p.getX() - pos.getX()), Math.abs(p.getZ() - pos.getZ())) >= 5) wide++;
                    if (st.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE) >= 7) decaying++;
                }
                if (st.is(mods.eln.rubber.RubberTrees.AERIAL_ROOT.get())) roots++;
                if (st.is(mods.eln.rubber.RubberTrees.LOG.get()) && p.getY() == pos.getY() && !p.equals(pos)) wide += 0;
            }
            if (!ok) c.line("rubber fig refused: " + mods.eln.rubber.RubberFigFeature.lastRefusal);
            grown = ok + " " + logs + " logs " + leaves + " leaves " + wide + " wide " + roots + " roots " + decaying + " decaying";
        }

        public void measure(TestContext c) {
            String[] g = grown.split(" ");
            c.check("a rubber sapling grows a rubber fig: a trunk of 10+ logs, a broad crown (leaves 5+ out), none decaying",
                g[0].equals("true") && Integer.parseInt(g[1]) >= 10 && Integer.parseInt(g[3]) > 50 && Integer.parseInt(g[5]) > 5
                    && g[9].equals("0"), grown);
            c.check("rubber logs are logs, its leaves leaves (the collector's checks)",
                mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor.isWood(mods.eln.rubber.RubberTrees.LOG.get())
                    && mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor.isLeaf(mods.eln.rubber.RubberTrees.LEAVES.get()), "");
            double oak = mods.eln.sixnode.TreeResinCollector.TreeResinCollectorElement.treeFactor(Blocks.OAK_LOG),
                rub = mods.eln.sixnode.TreeResinCollector.TreeResinCollectorElement.treeFactor(mods.eln.rubber.RubberTrees.LOG.get());
            c.check("a collector gets half the old rate on an oak, 1.5x on a rubber tree", oak == 0.5 && rub == 1.5, oak + " " + rub);

            var oneOak = mods.eln.Eln.compressorRecipes.getRecipe(new ItemStack(Items.OAK_LOG, 1));
            var twoOak = mods.eln.Eln.compressorRecipes.getRecipe(new ItemStack(Items.SPRUCE_LOG, 2));
            var twoRubber = mods.eln.Eln.compressorRecipes.getRecipe(new ItemStack(mods.eln.rubber.RubberTrees.LOG_ITEM.get(), 2));
            c.check("compressor: one log isn't enough, two logs make a resin", oneOak == null && twoOak != null
                && twoOak.getOutputCopy()[0].getCount() == 1, oneOak + " / " + (twoOak == null ? "none" : twoOak.getOutputCopy()[0].toString()));
            c.check("compressor: two rubber logs make three resin", twoRubber != null && twoRubber.getOutputCopy()[0].getCount() == 3,
                twoRubber == null ? "none" : twoRubber.getOutputCopy()[0].toString());

            var reg = c.level().registryAccess();
            boolean feature = reg.lookupOrThrow(net.minecraft.core.registries.Registries.FEATURE).get(mods.eln.rubber.RubberTrees.TREE).isPresent();
            boolean grove = reg.lookupOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE).get(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.PLACED_FEATURE, net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "rubber_grove"))).isPresent();
            c.check("worldgen: the rubber tree and its grove are registered", feature && grove, feature + " " + grove);

            List<ItemStack> grid = new ArrayList<>(List.of(new ItemStack(Items.JUNGLE_SAPLING), new ItemStack(Items.OAK_SAPLING),
                new ItemStack(Items.BIRCH_SAPLING), new ItemStack(Items.ACACIA_SAPLING)));
            var input = CraftingInput.of(2, 2, grid);
            var r = c.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, c.level());
            c.check("a rubber sapling from jungle, oak, birch and acacia saplings",
                r.isPresent() && r.get().value().assemble(input).is(mods.eln.rubber.RubberTrees.SAPLING_ITEM.get()), r.map(rh -> rh.value().assemble(input).toString()).orElse("none"));
        }
    }

    static final class SlotSpecName {
        static String of(mods.eln.gui.SlotSpec sl) {
            return sl.kind() + "#" + sl.index();
        }
    }

    static final class ChainCase implements TestCase {
        public String id() {
            return "rubber_chain";
        }

        public int width() {
            return 3;
        }

        public void build(TestContext c) {
        }

        static String smelt(TestContext c, Item in) {
            var input = new SingleRecipeInput(new ItemStack(in));
            var r = c.level().recipeAccess().getRecipeFor(RecipeType.SMELTING, input, c.level());
            if (r.isEmpty()) return "none";
            ItemStack out = r.get().value().assemble(input);
            return out.getCount() + " " + BuiltInRegistries.ITEM.getKey(out.getItem());
        }

        static String macerate(Item in) {
            var r = mods.eln.Eln.maceratorRecipes.getRecipe(new ItemStack(in, 64));
            if (r == null) return "none";
            ItemStack[] max = r.maxOutput();
            return r.getOutputCopy()[0].getCount() + ".." + max[0].getCount() + " " + BuiltInRegistries.ITEM.getKey(max[0].getItem());
        }

        public void measure(TestContext c) {
            c.check("sulfur macerates to 4-5 sulfur dust", macerate(Items.SULFUR).equals("4..5 eln:sulfur_dust"), macerate(Items.SULFUR));
            c.check("sulfur bricks macerate like sulfur", macerate(Items.SULFUR_BRICKS).equals("4..5 eln:sulfur_dust"), macerate(Items.SULFUR_BRICKS));
            c.check("a sulfur spike macerates to 1 dust", macerate(Items.SULFUR_SPIKE).equals("1..1 eln:sulfur_dust"), macerate(Items.SULFUR_SPIKE));
            c.check("potent sulfur macerates to 36 dust", macerate(Items.POTENT_SULFUR).equals("36..36 eln:sulfur_dust"), macerate(Items.POTENT_SULFUR));
            c.check("sulfur slabs don't macerate (no dupes)", macerate(Items.SULFUR_SLAB).equals("none"), macerate(Items.SULFUR_SLAB));
            c.check("soul sand macerates to 2 soul dust", macerate(Items.SOUL_SAND).equals("2..2 eln:soul_dust"), macerate(Items.SOUL_SAND));
            c.check("soul soil macerates to 2 soul dust", macerate(Items.SOUL_SOIL).equals("2..2 eln:soul_dust"), macerate(Items.SOUL_SOIL));

            Item resin = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "tree_resin"));
            c.check("resin smelts to raw rubber", smelt(c, resin).equals("1 eln:raw_rubber"), smelt(c, resin));
            c.check("raw rubber smelts to 1 rubber", smelt(c, ElnRegistry.RAW_RUBBER.get()).equals("1 eln:rubber"), smelt(c, ElnRegistry.RAW_RUBBER.get()));
            c.check("rubber compound doesn't smelt in a plain furnace", smelt(c, ElnRegistry.RUBBER_COMPOUND.get()).equals("none"), smelt(c, ElnRegistry.RUBBER_COMPOUND.get()));
            var h = mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess.treatmentFor(new ItemStack(ElnRegistry.RUBBER_COMPOUND.get()));
            var furnace = mods.eln.Eln.transparentNodeItem.getDescriptor(0 + (32 << 6));
            var inSlot = mods.eln.gui.MenuSpecs.of(furnace).slots().stream()
                .filter(sl -> sl.index() == mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceElement.inSlotId).findFirst();
            c.check("the Electrical Furnace's input slot takes Rubber Compound (AdventurAgent: it refused it)",
                inSlot.isPresent() && inSlot.get().filter().test(new ItemStack(ElnRegistry.RUBBER_COMPOUND.get())), String.valueOf(inSlot.map(SlotSpecName::of)));
            c.check("rubber compound vulcanises to 8 rubber in the Electrical Furnace", h != null && h.out().get().getCount() == 8
                && h.out().get().is(mods.eln.Eln.findItemStack("Rubber").getItem()), String.valueOf(h));

            List<ItemStack> grid = new ArrayList<>();
            for (int i = 0; i < 4; i++) grid.add(new ItemStack(ElnRegistry.RAW_RUBBER.get()));
            grid.add(new ItemStack(ElnRegistry.SULFUR_DUST.get()));
            grid.add(new ItemStack(ElnRegistry.SOUL_DUST.get()));
            for (int i = 0; i < 3; i++) grid.add(ItemStack.EMPTY);
            var input = CraftingInput.of(3, 3, grid);
            var r = c.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, c.level());
            String got = r.map(rh -> rh.value().assemble(input).toString()).orElse("none");
            c.check("4 raw rubber + sulfur dust + soul dust make a rubber compound",
                r.isPresent() && r.get().value().assemble(input).is(ElnRegistry.RUBBER_COMPOUND.get()), got);
            c.check("the macerator gives off souls for soul sand, not for sulfur",
                mods.eln.transparentnode.electricalmachine.MaceratorDescriptor.isSoulBlock(new ItemStack(Blocks.SOUL_SAND))
                    && !mods.eln.transparentnode.electricalmachine.MaceratorDescriptor.isSoulBlock(new ItemStack(Blocks.SULFUR)), "");
        }
    }
}
