package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** EA's lead and tungsten ores drop raw lead / raw tungsten as iron ore drops raw iron (Baughn 2026-10-19); silk touch
 *  keeps the ore; the raw pieces smelt and blast to ingots and macerate to dust. */
public final class OreDropCases {
    private OreDropCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new DropCase());
    }

    static final class DropCase implements TestCase {
        public String id() {
            return "ore_drops_raw";
        }

        public int width() {
            return 3;
        }

        public void build(TestContext c) {
        }

        public void measure(TestContext c) {
            var level = c.level();
            var silk = new ItemStack(Items.IRON_PICKAXE);
            silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
            for (String m : new String[]{"lead", "tungsten"}) {
                Block ore = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("eln", m + "_ore"));
                var pos = c.at(1, 1, 1);
                level.setBlock(pos, ore.defaultBlockState(), 3);
                List<ItemStack> plain = Block.getDrops(ore.defaultBlockState(), level, pos, null, null, new ItemStack(Items.IRON_PICKAXE));
                List<ItemStack> silky = Block.getDrops(ore.defaultBlockState(), level, pos, null, null, silk);
                var raw = m.equals("lead") ? ElnRegistry.RAW_LEAD.get() : ElnRegistry.RAW_TUNGSTEN.get();
                c.check(m + " ore drops raw " + m, plain.size() == 1 && plain.get(0).is(raw), plain.toString());
                c.check(m + " ore with silk touch drops the ore", silky.size() == 1 && silky.get(0).is(ore.asItem()), silky.toString());
                var input = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(raw));
                var sm = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, level);
                var bl = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.BLASTING, input, level);
                String ingot = "eln:" + m + "_ingot";
                c.check("raw " + m + " smelts to an ingot", sm.isPresent() && BuiltInRegistries.ITEM.getKey(sm.get().value().assemble(input).getItem()).toString().equals(ingot),
                    sm.map(r -> r.value().assemble(input).toString()).orElse("none"));
                c.check("raw " + m + " blasts to an ingot", bl.isPresent(), bl.map(r -> r.value().assemble(input).toString()).orElse("none"));
                var mac = mods.eln.Eln.maceratorRecipes.getRecipe(new ItemStack(raw));
                c.check("raw " + m + " macerates to dust", mac != null, String.valueOf(mac));
            }
        }
    }
}
