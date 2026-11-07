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
