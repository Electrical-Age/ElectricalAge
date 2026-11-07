package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * No two crafting recipes, one of them EA's, take the same grid for different items (Baughn 2026-11-07: the old cable
 * tiers became one Copper Cable, which made tier pairs like the 48 V and 230 V bulbs alike). Shaped recipes clash when
 * they are the same size and every cell (as laid, or mirrored, as the game matches) has an item both take, or both are
 * empty; shapeless ones when their ingredients pair up that way; a shapeless recipe and a shaped one when the shaped
 * one's ingredients pair up with the shapeless one's.
 */
public final class RecipeClashCases {
    private RecipeClashCases() {
    }

    /** A crafting recipe as items per cell: shaped (w x h, an empty set an empty cell) or shapeless (w = -1). */
    record Grid(String id, Item out, int w, int h, List<Set<Item>> cells) {
        boolean eln() {
            return id.startsWith("eln:");
        }

        List<Set<Item>> filled() {
            List<Set<Item>> l = new ArrayList<>();
            for (Set<Item> c : cells) if (!c.isEmpty()) l.add(c);
            return l;
        }
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new TestCase() {
            public String id() {
                return "recipes_no_two_alike";
            }

            public int ticks() {
                return 1;
            }

            public void build(TestContext ctx) {
            }

            public void measure(TestContext ctx) {
                List<Grid> grids = grids(ctx);
                List<String> clashes = new ArrayList<>();
                for (int i = 0; i < grids.size(); i++)
                    for (int j = i + 1; j < grids.size(); j++) {
                        Grid a = grids.get(i), b = grids.get(j);
                        if (!a.eln() && !b.eln() || a.out() == b.out()) continue;
                        if (clash(a, b)) clashes.add(a.id() + " / " + b.id());
                    }
                for (String c : clashes) ctx.line("INFO clash: " + c);
                ctx.check("no two crafting recipes take the same grid for different items", clashes.isEmpty(),
                    clashes.size() + " clashes among " + grids.size() + " recipes");
            }
        });
    }

    static List<Grid> grids(TestContext ctx) {
        var c = SlotDisplayContext.fromLevel(ctx.level());
        List<Grid> l = new ArrayList<>();
        for (RecipeHolder<?> h : ctx.level().recipeAccess().getRecipes()) {
            if (!(h.value() instanceof ShapedRecipe) && !(h.value() instanceof ShapelessRecipe)) continue;
            for (RecipeDisplay d : h.value().display()) {
                List<ItemStack> res = d.result().resolveForStacks(c);
                if (res.isEmpty()) continue;
                List<Set<Item>> cells = new ArrayList<>();
                if (d instanceof ShapedCraftingRecipeDisplay s) {
                    for (SlotDisplay x : s.ingredients()) cells.add(items(x, c));
                    l.add(new Grid(h.id().identifier().toString(), res.get(0).getItem(), s.width(), s.height(), cells));
                } else if (d instanceof ShapelessCraftingRecipeDisplay s) {
                    for (SlotDisplay x : s.ingredients()) cells.add(items(x, c));
                    l.add(new Grid(h.id().identifier().toString(), res.get(0).getItem(), -1, 0, cells));
                }
            }
        }
        return l;
    }

    static Set<Item> items(SlotDisplay d, net.minecraft.util.context.ContextMap c) {
        Set<Item> s = new LinkedHashSet<>();
        for (ItemStack st : d.resolveForStacks(c)) if (!st.isEmpty()) s.add(st.getItem());
        return s;
    }

    static boolean overlap(Set<Item> a, Set<Item> b) {
        if (a.isEmpty() || b.isEmpty()) return a.isEmpty() && b.isEmpty();
        for (Item i : a) if (b.contains(i)) return true;
        return false;
    }

    static boolean clash(Grid a, Grid b) {
        if (a.w() >= 0 && b.w() >= 0) {
            if (a.w() != b.w() || a.h() != b.h()) return false;
            return cellsMatch(a, b, false) || cellsMatch(a, b, true);
        }
        return pairUp(a.filled(), b.filled());
    }

    static boolean cellsMatch(Grid a, Grid b, boolean mirror) {
        for (int y = 0; y < a.h(); y++)
            for (int x = 0; x < a.w(); x++) {
                int bx = mirror ? a.w() - 1 - x : x;
                if (!overlap(a.cells().get(y * a.w() + x), b.cells().get(y * b.w() + bx))) return false;
            }
        return true;
    }

    /** Can each of a's ingredients be given a different one of b's that shares an item (augmenting paths)? */
    static boolean pairUp(List<Set<Item>> a, List<Set<Item>> b) {
        if (a.size() != b.size()) return false;
        int[] owner = new int[b.size()];
        java.util.Arrays.fill(owner, -1);
        for (int i = 0; i < a.size(); i++) if (!augment(i, a, b, owner, new boolean[b.size()])) return false;
        return true;
    }

    static boolean augment(int i, List<Set<Item>> a, List<Set<Item>> b, int[] owner, boolean[] seen) {
        for (int j = 0; j < b.size(); j++) {
            if (seen[j] || !overlap(a.get(i), b.get(j))) continue;
            seen[j] = true;
            if (owner[j] < 0 || augment(owner[j], a, b, owner, seen)) {
                owner[j] = i;
                return true;
            }
        }
        return false;
    }
}
