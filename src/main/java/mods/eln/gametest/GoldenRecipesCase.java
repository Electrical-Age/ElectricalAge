package mods.eln.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * EA's 26.x recipes (datagen JSON, loaded by the server) against golden/1.12/recipes.json (P5, port rule T4).
 * Golden ingredients are the stacks the 1.12 pack (E36) matched; only minecraft: and eln: ones are checked (mapped to
 * 26.x: eln-ids.json names, flattened vanilla meta items), other mods' items are not in the test server. A golden
 * crafting recipe matches a 26.x recipe with the same result item and count and: shaped, the same size and empty
 * slots, each slot accepting all the golden slot's (mapped) items; shapeless, a one-to-one pairing of ingredients the
 * same way. A golden slot holding only other mods' items means a recipe 26.x leaves out without those mods (load
 * condition): counted, not failed. Smelting: each 1.12 call's input smelts to its output (the first call wins, as in
 * 1.12: Tree Resin -> 1 Rubber).
 */
final class GoldenRecipesCase implements TestCase {
    public String id() {
        return "golden_recipes";
    }

    public int width() {
        return 3;
    }

    public int ticks() {
        return 1;
    }

    public void build(TestContext ctx) {
    }

    /** Golden recipes whose output is deferred to a later phase (notes/port26/deferred.md). */
    static final Set<String> DEFERRED = Set.of("eln:x_ray_scanner");
    /** Recipes changed on purpose (Baughn 2026-10-13): copper cable 1 per ingot, windings take the new Copper Coil.
     *  Their output must still have a 26.x recipe. */
    static final Set<String> CHANGED = Set.of("eln:copper_cable", "eln:power_resistor", "eln:dc_dc_converter", "eln:rheostat",
        "eln:thermistor", "eln:power_inductor", "eln:small_50_v_copper_heating_corp", "eln:50_v_copper_heating_corp");

    /**
     * 1.12 outputs dropped on purpose: copper tools and armour, vanilla since 1.21.9 (Baughn, 2026-10-11); the heat
     * turbines, replaced by the 50V/200V steam turbines (Baughn, 2026-10-14).
     */
    static final Set<String> REMOVED = Set.of("eln:copper_sword", "eln:copper_hoe", "eln:copper_shovel", "eln:copper_pickaxe",
        "eln:copper_axe", "eln:copper_helmet", "eln:copper_chestplate", "eln:copper_leggings", "eln:copper_boots",
        "eln:50_v_turbine", "eln:200_v_turbine");

    /** Macerator inputs 26.x changes on purpose: vanilla ores give raw chunks (Baughn, 2026-10-10; golden/README.md). */
    static final Set<String> ORES_TO_CHUNKS = Set.of("Iron Ore", "Gold Ore", "Copper Ore");

    static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
    static final Set<String> EA_DAMAGE_ITEMS = Set.of("eln:six_node", "eln:transparent_node", "eln:shared_item",
        "eln:shared_item_stack_one", "eln:ore");

    /** A 1.12 golden stack as a 26.x item; null for other mods' items (or an unknown name, reported). */
    /** EA items dropped for vanilla's (Baughn 2026-10-11): 1.12 name -> the 26.x item that stands in. */
    static final Map<String, Item> NOW_VANILLA = Map.of("Copper Ingot", Items.COPPER_INGOT);

    static Item map(JsonObject o, List<String> unknown) {
        String id = o.get("item").getAsString();
        int damage = o.has("damage") ? o.get("damage").getAsInt() : 0;
        if (id.startsWith("eln:") && o.has("name") && NOW_VANILLA.containsKey(o.get("name").getAsString()))
            return NOW_VANILLA.get(o.get("name").getAsString());
        String path;
        if (EA_DAMAGE_ITEMS.contains(id)) {
            try {
                path = "eln:" + ElnRegistry.nameOf(id, damage);
            } catch (IllegalStateException e) {
                unknown.add(id + "@" + damage);
                return null;
            }
        } else if (id.startsWith("eln:")) {
            path = id;
        } else if (id.startsWith("minecraft:")) {
            String p = id.substring(10);
            path = switch (p) {
                case "wool" -> "minecraft:" + COLORS[damage] + "_wool";
                case "dye" -> "minecraft:" + COLORS[15 - damage] + "_dye";
                case "planks" -> "minecraft:" + WOODS[damage] + "_planks";
                case "log" -> "minecraft:" + WOODS[damage] + "_log";
                case "log2" -> "minecraft:" + WOODS[4 + damage] + "_log";
                case "coal" -> damage == 1 ? "minecraft:charcoal" : "minecraft:coal";
                case "noteblock" -> "minecraft:note_block";
                default -> id;
            };
        } else {
            return null; // another mod
        }
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(path));
        if (item == Items.AIR) {
            unknown.add(id + "@" + damage + " -> " + path);
            return null;
        }
        return item;
    }

    record Slot(boolean empty, boolean foreignOnly, List<Item> items) {
    }

    static Slot slot(JsonArray alts, List<String> unknown) {
        if (alts == null || alts.isEmpty()) return new Slot(true, false, List.of());
        List<Item> items = new ArrayList<>();
        for (JsonElement a : alts) {
            Item i = map(a.getAsJsonObject(), unknown);
            if (i != null) items.add(i);
        }
        return new Slot(false, items.isEmpty(), items);
    }

    static boolean accepts(Ingredient ing, List<Item> items) {
        for (Item i : items) if (!ing.acceptsItem(BuiltInRegistries.ITEM.wrapAsHolder(i))) return false;
        return true;
    }

    /**
     * 26.x shaped patterns drop empty outer rows and columns (1.12 kept them, which only pinned the recipe to a grid
     * position): the golden grid is trimmed the same way before comparing.
     */
    static boolean matchShaped(ShapedRecipe r, int w0, int h0, List<Slot> grid) {
        int x0 = w0, x1 = -1, y0 = h0, y1 = -1;
        for (int y = 0; y < h0; y++)
            for (int x = 0; x < w0; x++)
                if (!grid.get(y * w0 + x).empty()) {
                    x0 = Math.min(x0, x);
                    x1 = Math.max(x1, x);
                    y0 = Math.min(y0, y);
                    y1 = Math.max(y1, y);
                }
        int w = x1 - x0 + 1, h = y1 - y0 + 1;
        List<Slot> slots = new ArrayList<>();
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) slots.add(grid.get(y * w0 + x));
        if (r.getWidth() != w || r.getHeight() != h) return false;
        List<Optional<Ingredient>> ings = r.getIngredients();
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            Optional<Ingredient> ing = ings.get(i);
            if (s.empty() != ing.isEmpty()) return false;
            if (!s.empty() && !accepts(ing.get(), s.items())) return false;
        }
        return true;
    }

    static boolean matchShapeless(ShapelessRecipe r, List<Slot> slots) {
        List<Ingredient> ings = new ArrayList<>(r.placementInfo().ingredients());
        if (ings.size() != slots.size()) return false;
        // most specific golden slots first, greedy pairing
        List<Slot> order = new ArrayList<>(slots);
        order.sort((a, b) -> a.items().size() - b.items().size());
        for (Slot s : order) {
            int found = -1;
            for (int i = 0; i < ings.size(); i++) if (accepts(ings.get(i), s.items())) {
                found = i;
                break;
            }
            if (found < 0) return false;
            ings.remove(found);
        }
        return true;
    }

    public void measure(TestContext ctx) {
        String dir = System.getProperty("eln.golden");
        if (dir == null) {
            ctx.line("INFO golden: eln.golden not set, skipped");
            return;
        }
        JsonObject golden;
        try (Reader rd = Files.newBufferedReader(Paths.get(dir, "recipes.json"))) {
            golden = JsonParser.parseReader(rd).getAsJsonObject();
        } catch (Exception e) {
            ctx.check("golden recipes: read recipes.json", false, e.toString());
            return;
        }
        List<RecipeHolder<?>> mine = new ArrayList<>();
        for (RecipeHolder<?> h : ctx.world().recipeAccess().getRecipes())
            if (h.id().identifier().getNamespace().equals("eln")) mine.add(h);
        List<String> unknown = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        Set<Identifier> used = new HashSet<>();
        int matched = 0, conditional = 0;
        List<String> conditionalNames = new ArrayList<>(), deferred = new ArrayList<>();
        for (JsonElement e : golden.getAsJsonArray("crafting")) {
            JsonObject g = e.getAsJsonObject();
            JsonObject outJ = g.getAsJsonObject("output");
            Item out = map(outJ, unknown);
            int count = outJ.has("count") ? outJ.get("count").getAsInt() : 1;
            if (out == null && DEFERRED.contains(g.get("id").getAsString())) {
                deferred.add(g.get("id").getAsString());
                continue;
            }
            if (REMOVED.contains(g.get("id").getAsString())) continue;   // gone, or kept but no longer craftable
            if (out == null) {
                failed.add(g.get("id").getAsString() + ": output not on 26.x");
                continue;
            }
            if (CHANGED.contains(g.get("id").getAsString())) {
                boolean has = false;
                for (RecipeHolder<?> h : mine) {
                    Recipe<?> r = h.value();
                    ItemStack res = r instanceof ShapedRecipe sr ? sr.assemble(CraftingInput.EMPTY)
                        : r instanceof ShapelessRecipe sl ? sl.assemble(CraftingInput.EMPTY) : ItemStack.EMPTY;
                    if (res.getItem() == out) { has = true; used.add(h.id().identifier()); }
                }
                if (!has) failed.add(g.get("id").getAsString() + ": changed on purpose, but no 26.x recipe makes it");
                else matched++;
                continue;
            }
            List<Slot> slots = new ArrayList<>();
            for (JsonElement s : g.getAsJsonArray("ingredients")) slots.add(slot(s.isJsonNull() ? null : s.getAsJsonArray(), unknown));
            if (slots.stream().anyMatch(Slot::foreignOnly)) {
                conditional++;
                conditionalNames.add(g.get("id").getAsString());
                continue;
            }
            boolean shaped = g.get("class").getAsString().contains("Shaped");
            RecipeHolder<?> hit = null;
            for (RecipeHolder<?> h : mine) {
                Recipe<?> r = h.value();
                ItemStack res;
                if (shaped && r instanceof ShapedRecipe sr) res = sr.assemble(CraftingInput.EMPTY);
                else if (!shaped && r instanceof ShapelessRecipe sl) res = sl.assemble(CraftingInput.EMPTY);
                else continue;
                if (res.getItem() != out || res.getCount() != count) continue;
                boolean ok = shaped ? matchShaped((ShapedRecipe) r, g.get("width").getAsInt(), g.get("height").getAsInt(), slots)
                    : matchShapeless((ShapelessRecipe) r, slots);
                if (ok) {
                    hit = h;
                    if (!used.contains(h.id().identifier())) break; // prefer a recipe not matched yet
                }
            }
            if (hit == null) failed.add(g.get("id").getAsString() + " (" + outJ.get("name").getAsString() + ")");
            else {
                matched++;
                used.add(hit.id().identifier());
            }
        }
        ctx.line(String.format("golden recipes: %d crafting matched, %d need other mods' items (left out by load conditions), "
            + "%d failed; %d of %d 26.x eln recipes matched", matched, conditional, failed.size(), used.size(), mine.size()));
        for (String f : failed) ctx.line("  unmatched 1.12 recipe: " + f);
        ctx.line("  needing other mods: " + conditionalNames);
        ctx.line("  deferred outputs (ported later): " + deferred);
        List<String> extra = new ArrayList<>();
        for (RecipeHolder<?> h : mine)
            if (!used.contains(h.id().identifier()) && !(h.value() instanceof SmeltingRecipe)) extra.add(h.id().identifier().getPath());
        ctx.line("  26.x recipes without a 1.12 twin (load-condition variants): " + extra);
        for (String u : new java.util.TreeSet<>(unknown))
            if (!u.endsWith("eln:x_ray_scanner")) ctx.line("  unknown golden item: " + u);
        ctx.check("golden recipes: every 1.12 crafting recipe has its 26.x recipe", failed.isEmpty(), failed.size() + " unmatched");

        // smelting: the first call per input wins
        Set<Item> seenInputs = new HashSet<>();
        List<String> smeltFail = new ArrayList<>();
        int smelts = 0;
        for (JsonElement e : golden.getAsJsonArray("smelting")) {
            JsonObject g = e.getAsJsonObject();
            Item in = map(g.getAsJsonObject("input"), unknown), out = map(g.getAsJsonObject("output"), unknown);
            if (in == null || !seenInputs.add(in)) continue;
            int count = g.getAsJsonObject("output").has("count") ? g.getAsJsonObject("output").get("count").getAsInt() : 1;
            boolean ok = false;
            for (RecipeHolder<?> h : mine) {
                if (h.value() instanceof SmeltingRecipe sr && accepts(sr.input(), List.of(in))) {
                    ItemStack res = sr.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(in)));
                    ok = res.getItem() == out && res.getCount() == count;
                    break;
                }
            }
            if (ok) smelts++;
            else smeltFail.add(g.getAsJsonObject("input").get("name").getAsString());
        }
        ctx.check("golden recipes: smelting as 1.12 (first call per input)", smeltFail.isEmpty(),
            smelts + " ok" + (smeltFail.isEmpty() ? "" : ", failed: " + smeltFail));

        // machine lists (runtime, ElnMachineRecipes)
        Map<String, mods.eln.misc.RecipesList> lists = Map.of("macerator", mods.eln.Eln.maceratorRecipes,
            "compressor", mods.eln.Eln.compressorRecipes, "plateMachine", mods.eln.Eln.plateMachineRecipes,
            "magnetizer", mods.eln.Eln.magnetiserRecipes);
        for (Map.Entry<String, mods.eln.misc.RecipesList> l : lists.entrySet()) {
            List<String> fail = new ArrayList<>();
            int ok = 0, modOres = 0, changed = 0;
            for (JsonElement e : golden.getAsJsonArray(l.getKey())) {
                JsonObject g = e.getAsJsonObject();
                JsonObject inJ = g.getAsJsonObject("input");
                if (l.getKey().equals("macerator") && ORES_TO_CHUNKS.contains(inJ.get("name").getAsString())) {
                    changed++;
                    continue;
                }
                Item in = map(inJ, unknown);
                if (in == null) {
                    modOres++;
                    continue;
                }
                int inCount = inJ.has("count") ? inJ.get("count").getAsInt() : 1;
                mods.eln.misc.Recipe r = l.getValue().getRecipe(new ItemStack(in, 64));
                boolean good = r != null && r.input.getCount() == inCount && Math.abs(r.energy - g.get("energy").getAsDouble()) < 1e-6;
                if (good) {
                    JsonArray outs = g.getAsJsonArray("output");
                    good = outs.size() == r.output.length;
                    for (int k = 0; good && k < outs.size(); k++) {
                        JsonObject o = outs.get(k).getAsJsonObject();
                        Item oi = map(o, unknown);
                        int oc = o.has("count") ? o.get("count").getAsInt() : 1;
                        good = r.output[k].getItem() == oi && r.output[k].getCount() == oc;
                    }
                }
                if (good) ok++;
                else fail.add(inJ.get("name").getAsString() + (r == null ? " (no recipe)" : ""));
            }
            ctx.check("golden recipes: " + l.getKey() + " list as 1.12", fail.isEmpty(),
                ok + " ok, " + modOres + " from other mods' ores (not in the test server), " + changed
                    + " changed on purpose (golden/README.md)" + (fail.isEmpty() ? "" : ", failed: " + fail));
        }
    }
}
