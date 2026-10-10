package mods.eln.selftest.cases;

import mods.eln.Eln;
import mods.eln.misc.Recipe;
import mods.eln.misc.Utils;
import mods.eln.registry.ElnRecipes;
import mods.eln.selftest.SelfTestCase;
import mods.eln.selftest.SelfTestContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Arrays;
import java.util.List;

import static mods.eln.registry.ElnDeviceRegistry.lowVoltageCableDescriptor;

/**
 * WP15 recipes (registry/ElnRecipes), no world needed. Expected output (default config, E36 ore dictionary):
 * <pre>
 * PASS recipes crafting count: eln:* registered = recipe calls = expected (293 + utility poles max(k,1) + X-ray 1 + converters 3)
 * PASS recipes smelting: 13 EA inputs smelt to their outputs (n added by EA; E36: 10, Iron/Gold Dust already smelt
 *      via another mod, Tree Resin's second recipe ignored since 1.12 keeps the first; E36's CraftTweaker scripts remove
 *      furnace recipes of every oreCopper/oreLead, listed as "removed by the pack" and counted ok)
 * PASS recipes machine lists: macerator 18 (+ AE2 mod ores), compressor 4, plate machine 6, magnetizer 2
 * PASS recipes LV cable: rubber/copper ingot/rubber rows -> 6 Low Voltage Cable
 * PASS recipes LV cable with a foreign ingotCopper (SKIP line if no other mod registers one)
 * PASS recipes Cost Oriented Battery registered (eln:cost_oriented_battery)
 * PASS recipes macerator EA copper ore -> 2 Copper Dust; same for a foreign oreCopper (SKIP line if none)
 * PASS recipes furnace Copper Dust -> Copper Ingot (any ore:ingotCopper)
 * INFO recipes: n 1.7.10 pattern quirks rewritten (unused keys dropped ...)
 * </pre>
 * k = how many of ingotAluminum, ingotAluminium, ingotSteel the ore dictionary has (one Utility Pole recipe each, else the
 * 1.7.10 fallback). 293 = 245 single recipes + 32 brushes + 8 vuMeters + 8 2x3 solar panels.
 */
public final class RecipeCases {
    private RecipeCases() {
    }

    public static void addTo(List<SelfTestCase> cases) {
        cases.add(new Recipes());
    }

    static final class Recipes implements SelfTestCase {
        @Override
        public String name() {
            return "recipes";
        }

        @Override
        public int width() {
            return 1;
        }

        @Override
        public void build(SelfTestContext ctx) {
        }

        static int expectedCrafting() {
            // pole variants as seen at registration (ore names queried later, e.g. by scripts, would inflate a fresh count)
            return 293 + Math.max(ElnRecipes.utilityPoleVariants, 1) + (Eln.xRayScannerCanBeCrafted ? 1 : 0) + (Eln.ElnToOtherEnergyConverterEnable ? 3 : 0);
        }

        static ItemStack foreign(String ore) {
            for (ItemStack s : OreDictionary.getOres(ore)) {
                ResourceLocation n = s.getItem().getRegistryName();
                if (n != null && !"eln".equals(n.getNamespace())) {
                    ItemStack c = s.copy();
                    if (c.getMetadata() == OreDictionary.WILDCARD_VALUE) c.setItemDamage(0);
                    c.setCount(1);
                    return c;
                }
            }
            return ItemStack.EMPTY;
        }

        static boolean same(ItemStack a, ItemStack b) {
            return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem() && a.getMetadata() == b.getMetadata() && a.getCount() == b.getCount();
        }

        /** Same item+meta, or both share an ore dictionary name (a unifier may swap outputs); count ignored. */
        static boolean sameOrOre(ItemStack got, ItemStack want) {
            if (got.isEmpty() || want.isEmpty()) return false;
            if (got.getItem() == want.getItem() && got.getMetadata() == want.getMetadata()) return true;
            for (int id : OreDictionary.getOreIDs(want))
                for (int id2 : OreDictionary.getOreIDs(got)) if (id == id2) return true;
            return false;
        }

        static String str(ItemStack s) {
            return s.isEmpty() ? "nothing" : s.getCount() + "x " + ElnRecipes.stackName(s);
        }

        /**
         * The ore* dictionary name of an input none of whose members smelts (a pack-wide furnace.remove for that ore), or
         * null. A lone missing EA recipe (the other members still smelt) stays a failure.
         */
        static String oreRemovedByPack(ItemStack in) {
            for (int id : OreDictionary.getOreIDs(in)) {
                String name = OreDictionary.getOreName(id);
                if (!name.startsWith("ore")) continue;
                boolean any = false;
                for (ItemStack o : OreDictionary.getOres(name)) {
                    ItemStack q = o.getMetadata() == OreDictionary.WILDCARD_VALUE ? new ItemStack(o.getItem(), 1, 0) : o;
                    if (!FurnaceRecipes.instance().getSmeltingResult(q).isEmpty()) {
                        any = true;
                        break;
                    }
                }
                if (!any) return name;
            }
            return null;
        }

        /** 3x3 grid, rows of the given stacks (null = empty) -> CraftingManager result. */
        static ItemStack craft(SelfTestContext ctx, ItemStack... grid) {
            InventoryCrafting inv = new InventoryCrafting(new Container() {
                @Override
                public boolean canInteractWith(EntityPlayer playerIn) {
                    return true;
                }
            }, 3, 3);
            for (int i = 0; i < 9; i++) inv.setInventorySlotContents(i, grid[i] == null ? ItemStack.EMPTY : grid[i].copy());
            IRecipe r = CraftingManager.findMatchingRecipe(inv, ctx.world());
            return r == null ? ItemStack.EMPTY : r.getCraftingResult(inv);
        }

        static ItemStack ea(String name, int n) {
            return mods.eln.Eln.findItemStack(name, n);
        }

        @Override
        public void measure(SelfTestContext ctx) {
            int registered = 0;
            for (ResourceLocation k : ForgeRegistries.RECIPES.getKeys()) if ("eln".equals(k.getNamespace())) registered++;
            int expected = expectedCrafting();
            ctx.check("recipes crafting count (eln:* registered = recipe calls = expected " + expected + ")",
                registered == expected && ElnRecipes.craftingAttempted == expected && ElnRecipes.craftingRegistered == expected,
                "registry " + registered + ", registered " + ElnRecipes.craftingRegistered + ", calls " + ElnRecipes.craftingAttempted);

            // Count of EA's 13 smelting inputs that smelt to the wanted item (or an ore-dictionary equivalent; count
            // ignored: Tree Resin keeps recipeGeneral's 1 Rubber). "added" alone depends on the pack: in E36 another mod
            // already smelts EA's Iron Dust and Gold Dust (ore:dustIron/dustGold) to the vanilla ingots -> added 10.
            // Ores whose smelting the pack removed for every member of their ore* entry (E36: CraftTweaker
            // MineTweakerRecipeMaker.zs furnace.remove(<*>, <ore:oreCopper>) / <ore:oreLead>, an expert-mode rule for all
            // mods' ores) count as ok: EA registered them (smeltingCalls), the pack took them out afterwards.
            int smeltOk = 0;
            StringBuilder smeltBad = new StringBuilder();
            StringBuilder packRemoved = new StringBuilder();
            for (ItemStack[] c : ElnRecipes.smeltingCalls) {
                ItemStack res = FurnaceRecipes.instance().getSmeltingResult(c[0]);
                String ore = res.isEmpty() ? oreRemovedByPack(c[0]) : null;
                if (sameOrOre(res, c[1])) smeltOk++;
                else if (ore != null) {
                    smeltOk++;
                    packRemoved.append(packRemoved.length() == 0 ? "" : ", ").append(ElnRecipes.stackName(c[0])).append(" (ore:").append(ore).append(")");
                } else smeltBad.append("; ").append(ElnRecipes.stackName(c[0])).append(" -> ").append(str(res));
            }
            ctx.check("recipes smelting: 13 EA inputs smelt to their outputs (" + ElnRecipes.smeltingAdded + " added by EA, rest pre-existing"
                    + (packRemoved.length() == 0 ? "" : "; removed by the pack for every " + packRemoved + " ore") + ")",
                ElnRecipes.smeltingCalls.size() == 13 && smeltOk == 13,
                "calls " + ElnRecipes.smeltingCalls.size() + ", ok " + smeltOk + smeltBad);

            int mac = Eln.maceratorRecipes.getRecipes().size(), comp = Eln.compressorRecipes.getRecipes().size();
            int plate = Eln.plateMachineRecipes.getRecipes().size(), mag = Eln.magnetiserRecipes.getRecipes().size();
            ctx.check("recipes machine lists: macerator 18 (+" + ElnRecipes.maceratorModOreAdded + " AE2 mod ores), compressor 4, plate machine 6, magnetizer 2",
                mac == 18 + ElnRecipes.maceratorModOreAdded && comp == 4 && plate == 6 && mag == 2,
                "macerator " + mac + ", compressor " + comp + ", plate machine " + plate + ", magnetizer " + mag);

            ItemStack rubber = ea("Rubber", 1), copper = ea("Copper Ingot", 1);
            ItemStack lv = lowVoltageCableDescriptor.newItemStack(6);
            ItemStack got = craft(ctx, rubber, rubber, rubber, copper, copper, copper, rubber, rubber, rubber);
            ctx.check("recipes LV cable: rubber / 3 copper ingots / rubber -> 6 Low Voltage Cable", same(got, lv), str(got));
            ItemStack fc = foreign("ingotCopper");
            if (fc.isEmpty()) {
                ctx.line("SKIP recipes LV cable with a foreign ingotCopper: no other mod registers ingotCopper");
            } else {
                got = craft(ctx, rubber, rubber, rubber, fc, fc, fc, rubber, rubber, rubber);
                ctx.check("recipes LV cable with " + ElnRecipes.stackName(fc) + " (ore:ingotCopper)", same(got, lv), str(got));
            }

            IRecipe bat = ForgeRegistries.RECIPES.getValue(new ResourceLocation("eln", "cost_oriented_battery"));
            ItemStack batOut = bat == null ? ItemStack.EMPTY : bat.getRecipeOutput();
            ctx.check("recipes Cost Oriented Battery registered (eln:cost_oriented_battery)",
                same(batOut, ea("Cost Oriented Battery", 1)), str(batOut));

            ItemStack dust2 = ea("Copper Dust", 2);
            Recipe m = Eln.maceratorRecipes.getRecipe(ea("Copper Ore", 1));
            ctx.check("recipes macerator EA copper ore -> 2 Copper Dust",
                m != null && m.output.length == 1 && same(m.output[0], dust2), m == null ? "no recipe" : Arrays.toString(m.output));
            ItemStack fo = foreign("oreCopper");
            if (fo.isEmpty()) {
                ctx.line("SKIP recipes macerator foreign copper ore: no other mod registers oreCopper");
            } else {
                m = Eln.maceratorRecipes.getRecipe(fo);
                ctx.check("recipes macerator " + ElnRecipes.stackName(fo) + " (ore:oreCopper) -> 2 Copper Dust",
                    m != null && m.output.length == 1 && same(m.output[0], dust2), m == null ? "no recipe" : Arrays.toString(m.output));
            }

            ItemStack ingot = FurnaceRecipes.instance().getSmeltingResult(ea("Copper Dust", 1));
            // ore-tolerant: a unifier (UniDict in E36) may swap furnace outputs for its preferred mod's ingotCopper
            boolean isCopper = !ingot.isEmpty() && ingot.getCount() == 1 && OreDictionary.containsMatch(false, OreDictionary.getOres("ingotCopper"), ingot);
            ctx.check("recipes furnace Copper Dust -> Copper Ingot (ore:ingotCopper)", isCopper, str(ingot));

            ctx.line("INFO recipes: " + ElnRecipes.fixes.size() + " 1.7.10 quirk(s) rewritten for 1.12 (see config debug.dumpRecipes)");
        }
    }
}
