package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.RecipesList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * JEI coverage (Baughn 2026-11-06): every EA item can be made by clicking its recipe in JEI, then its ingredients'
 * recipes, down to vanilla. The test collects what JEI shows (crafting and furnace recipes from their display(),
 * the machines' recipe lists with the machine as a requirement, and the plugin's own pages from JeiPages), then grows
 * the set of obtainable items from vanilla until it stops: an EA item JEI lists that is left out has no page, or only
 * pages needing itself. Each one is named in the log.
 */
public final class JeiAuditCases {
    private JeiAuditCases() {
    }

    /** One way to make something, as JEI shows it: each slot takes any of its items; the station is needed too. */
    record Way(String source, List<Set<Item>> slots, Set<Item> out) {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new TestCase() {
            public String id() {
                return "jei_every_item_reachable";
            }

            public int ticks() {
                return 1;
            }

            public void build(TestContext ctx) {
            }

            public void measure(TestContext ctx) {
                List<Way> ways = ways(ctx);
                Set<Item> hidden = mods.eln.compat.JeiPages.hidden();
                ctx.check("the hidden items are all found", hidden.size() == 12, hidden.size() + " items");
                Set<Item> have = new LinkedHashSet<>();
                for (Item i : BuiltInRegistries.ITEM) if (!isEln(i)) have.add(i);
                net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(ctx.level().enabledFeatures(), true, ctx.level().registryAccess());
                Set<Item> listed = new LinkedHashSet<>();
                for (ItemStack st : net.minecraft.world.item.CreativeModeTabs.searchTab().getDisplayItems()) listed.add(st.getItem());
                boolean grew = true;
                while (grew) {
                    grew = false;
                    for (Way w : ways) {
                        if (have.containsAll(w.out())) continue;
                        boolean ok = true;
                        for (Set<Item> s : w.slots()) if (s.stream().noneMatch(have::contains)) { ok = false; break; }
                        if (ok) grew |= have.addAll(w.out());
                    }
                }
                TreeMap<String, String> missing = new TreeMap<>();
                for (Item i : BuiltInRegistries.ITEM) {
                    // JEI lists what the creative tabs list; spawn eggs come from creative, as vanilla's
                    if (!isEln(i) || have.contains(i) || hidden.contains(i) || !listed.contains(i) || i instanceof net.minecraft.world.item.SpawnEggItem) continue;
                    List<String> how = new ArrayList<>();
                    for (Way w : ways) if (w.out().contains(i)) {
                        List<String> lack = new ArrayList<>();
                        for (Set<Item> s : w.slots()) if (s.stream().noneMatch(have::contains))
                            lack.add(s.isEmpty() ? "(empty slot)" : itemName(s.iterator().next()) + (s.size() > 1 ? "|+" + (s.size() - 1) : ""));
                        how.add(w.source() + " needs " + lack);
                    }
                    missing.put(itemName(i), how.isEmpty() ? "no page" : String.join("; ", how));
                }
                for (var e : missing.entrySet()) ctx.line("INFO jei: " + e.getKey() + ": " + e.getValue());
                ctx.check("every EA item in JEI has a way to make it from vanilla", missing.isEmpty(),
                    missing.size() + " unreachable of " + listed.stream().filter(JeiAuditCases::isEln).count() + " listed, " + ways.size() + " ways");
            }
        });
    }

    static boolean isEln(Item i) {
        return BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("eln");
    }

    static String itemName(Item i) {
        return BuiltInRegistries.ITEM.getKey(i).getPath();
    }

    static Set<Item> items(SlotDisplay d, net.minecraft.util.context.ContextMap c) {
        Set<Item> s = new LinkedHashSet<>();
        for (ItemStack st : d.resolveForStacks(c)) if (!st.isEmpty()) s.add(st.getItem());
        return s;
    }

    static Set<Item> one(ItemStack s) {
        Set<Item> r = new LinkedHashSet<>();
        if (s != null && !s.isEmpty()) r.add(s.getItem());
        return r;
    }

    static List<Way> ways(TestContext ctx) {
        List<Way> ways = new ArrayList<>();
        var c = SlotDisplayContext.fromLevel(ctx.level());
        for (RecipeHolder<?> h : ctx.level().recipeAccess().getRecipes()) {
            if (h.value().isSpecial()) continue;
            for (RecipeDisplay d : h.value().display()) {
                List<Set<Item>> slots = new ArrayList<>();
                if (d instanceof ShapedCraftingRecipeDisplay s) s.ingredients().forEach(x -> { var i = items(x, c); if (!i.isEmpty()) slots.add(i); });
                else if (d instanceof ShapelessCraftingRecipeDisplay s) s.ingredients().forEach(x -> slots.add(items(x, c)));
                else if (d instanceof FurnaceRecipeDisplay f) slots.add(items(f.ingredient(), c));
                else continue;   // (smithing, stonecutting: none of EA's)
                ways.add(new Way(h.id().identifier().getPath(), slots, items(d.result(), c)));
            }
        }
        // the cable- and pipe-kit examples the plugin adds to crafting
        for (var h : mods.eln.node.voxel.CableKitRecipes.examples()) addCrafting(ways, "cable kit example", h.value(), c);
        for (var h : mods.eln.node.voxel.PipeKitRecipes.examples()) addCrafting(ways, "pipe kit example", h.value(), c);
        // the machines' lists, needing a machine that makes them
        for (var e : mods.eln.compat.JeiPages.MACHINES.entrySet()) {
            RecipesList list = e.getValue().get();
            Set<Item> machines = new LinkedHashSet<>();
            for (ItemStack m : list.getMachines()) machines.addAll(one(m));
            for (mods.eln.misc.Recipe r : list.getRecipes()) {
                Set<Item> out = new LinkedHashSet<>();
                for (ItemStack o : r.output) out.addAll(one(o));
                ways.add(new Way(e.getKey(), List.of(one(r.input), machines), out));
            }
        }
        // the plugin's own pages: resin, annealing, wire drawing, information
        for (var r : mods.eln.compat.JeiPages.resins()) ways.add(new Way("resin collector", List.of(one(r.collector())), one(r.resin())));
        for (var a : mods.eln.compat.JeiPages.anneals()) ways.add(new Way("anneal", List.of(one(a.in()), one(mods.eln.compat.JeiPages.furnace())), one(a.out())));
        Set<Item> drawers = new LinkedHashSet<>();
        for (ItemStack m : mods.eln.compat.JeiPages.drawers()) drawers.addAll(one(m));
        for (var d : mods.eln.compat.JeiPages.draws())
            ways.add(new Way("wire drawing", d.rubber().isEmpty() ? List.of(one(d.copper()), drawers) : List.of(one(d.copper()), one(d.rubber()), drawers), one(d.out())));
        for (var i : mods.eln.compat.JeiPages.infos()) {
            Set<Item> out = new LinkedHashSet<>();
            for (ItemStack st : i.items()) out.addAll(one(st));
            ways.add(new Way(i.key(), List.of(), out));
        }
        return ways;
    }

    static void addCrafting(List<Way> ways, String src, net.minecraft.world.item.crafting.Recipe<?> r, net.minecraft.util.context.ContextMap c) {
        for (RecipeDisplay d : r.display()) {
            List<Set<Item>> slots = new ArrayList<>();
            if (d instanceof ShapedCraftingRecipeDisplay s) s.ingredients().forEach(x -> { var i = items(x, c); if (!i.isEmpty()) slots.add(i); });
            else if (d instanceof ShapelessCraftingRecipeDisplay s) s.ingredients().forEach(x -> slots.add(items(x, c)));
            ways.add(new Way(src, slots, items(d.result(), c)));
        }
    }
}
