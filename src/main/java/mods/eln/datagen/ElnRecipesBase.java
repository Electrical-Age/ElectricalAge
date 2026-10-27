package mods.eln.datagen;

import mods.eln.Eln;
import mods.eln.generic.ItemNbt;
import mods.eln.misc.OreDictTags;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The 1.12 ElnRecipes framework on 26.x datagen (P5). The 1.7.10 calls stay as they were:
 * addRecipe(output, "row", ..., 'c', ingredient, ...), addShapelessRecipe(output, ingredient...), addSmelting(item,
 * damage, result). Ingredients: an ItemStack / Item / Block is that item; a String is a 1.12 ore name, which becomes its
 * tag (OreDictTags). Recipe ids: eln:&lt;snake_case output name&gt;[_n], as 1.12. 1.7.10 pattern quirks are rewritten as
 * 1.12 did (sanitizeShaped). Every miss is collected and the datagen run fails listing all of them, except outputs
 * known to be deferred (DEFERRED: ported later), which are skipped and listed.
 */
public abstract class ElnRecipesBase {
    static HolderGetter<Item> items;
    static RecipeOutput out;
    static Function<ItemLike, Criterion<?>> hasItem;
    static Function<TagKey<Item>, Criterion<?>> hasTag;

    static String section = "?";
    public static final List<String> problems = new ArrayList<>();
    public static final List<String> fixes = new ArrayList<>();
    public static final List<String> skipped = new ArrayList<>();
    static final Map<String, Integer> nameUse = new HashMap<>();
    public static int craftingAttempted, craftingRegistered, smeltingAdded;
    /** Smelting inputs already given a result (1.12 FurnaceRecipes kept the first: Tree Resin -> 1 Rubber). */
    static final HashSet<Item> smelted = new HashSet<>();

    /** Descriptor names of outputs that are deferred to a later phase (their recipes are skipped, not failed). */
    static final Map<String, String> DEFERRED = Map.of();

    /**
     * A recipe stack: item, count and EA tag. Recipes are generated while item components are not bound yet (datagen's
     * registry bootstrap), so the 1.12 recipe code works on these instead of ItemStacks.
     */
    public record RStack(Item item, int count, CompoundTag tag) {
        public static final RStack EMPTY = new RStack(Items.AIR, 0, null);

        public boolean isEmpty() {
            return item == null || item == Items.AIR || count <= 0;
        }

        public Item getItem() {
            return item;
        }

        public RStack copy() {
            return new RStack(item, count, tag == null ? null : tag.copy());
        }
    }

    public static RStack stack(ItemLike item) {
        return new RStack(item.asItem(), 1, null);
    }

    public static RStack stack(ItemLike item, int count) {
        return new RStack(item.asItem(), count, null);
    }

    /** descriptor.newItemStack(count), without the stack (and its default tag: made by the item on first use). */
    public static RStack stack(mods.eln.generic.GenericItemBlockUsingDamageDescriptor d, int count) {
        return new RStack(d.parentItem, count, null);
    }

    public static RStack stack(mods.eln.generic.GenericItemUsingDamageDescriptor d, int count) {
        return new RStack(d.parentItem, count, null);
    }

    /** 1.12 BrushDescriptor.setLife(stack, life): the brush's default tag with that life. */
    static RStack withLife(RStack s, int life) {
        CompoundTag t = defaultTag(s.item());
        if (t == null) t = new CompoundTag();
        t.putInt("life", life);
        return new RStack(s.item(), s.count(), t);
    }

    static CompoundTag defaultTag(Item item) {
        Object d = item instanceof mods.eln.generic.IDescriptorItem di ? di.getEaDescriptor() : null;
        if (d instanceof mods.eln.generic.GenericItemUsingDamageDescriptor g) return g.getDefaultNBT();
        if (d instanceof mods.eln.generic.GenericItemBlockUsingDamageDescriptor g) return g.getDefaultNBT();
        return null;
    }

    static void run(String name, Runnable recipes) {
        section = name;
        try {
            recipes.run();
        } catch (RuntimeException e) {
            problems.add(name + ": " + e);
        }
    }

    // ------------------------------------------------------------------ lookups

    /** EA stack by descriptor name, or by an EA 1.12 ore name (first item registered under it). */
    public static RStack findItemStack(String name, int stackSize) {
        if (DEFERRED.containsKey(name)) {
            skipped.add(section + ": " + name + " (" + DEFERRED.get(name) + ")");
            return RStack.EMPTY;
        }
        Item it = Eln.itemsByName.get(name);
        if (it == null) {
            // plain items (copper tools/armour, E-Coal armour): their 1.12 names are the lang names
            for (Map.Entry<String, String> e : ElnLang.PLAIN.entrySet())
                if (e.getValue().equals(name)) it = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Eln.MODID, e.getKey()));
        }
        if (it == null || it == Items.AIR) {
            it = null;
            List<Object> ds = OreDictTags.EA_ENTRIES.get(name);
            if (ds != null && !ds.isEmpty()) it = ElnItemTags.itemOf(ds.get(0));
        }
        if (it == null) {
            problems.add(section + ": no EA item named '" + name + "'");
            return RStack.EMPTY;
        }
        return new RStack(it, stackSize, null);
    }

    public static RStack findItemStack(String name) {
        return findItemStack(name, 1);
    }

    // 1.12 meta stacks of flattened vanilla items
    static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};

    static RStack vanilla(String path) {
        Item i = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
        if (i == Items.AIR) throw new IllegalStateException("no minecraft:" + path);
        return stack(i);
    }

    /** 1.12 wool@meta (meta = EnumDyeColor wool order: 0 white .. 15 black). */
    public static RStack wool(int meta) {
        return vanilla(COLORS[meta] + "_wool");
    }

    /** 1.12 dye@meta (dye order = 15 - wool order; 0 ink sac -> black dye, 15 bone meal -> white dye, 4 lapis -> blue dye). */
    public static RStack dye(int meta) {
        return vanilla(COLORS[15 - meta] + "_dye");
    }

    /** 1.12 planks@meta (0 oak .. 5 dark oak). */
    public static RStack planks(int meta) {
        return vanilla(WOODS[meta] + "_planks");
    }

    public static String stackName(RStack s) {
        if (s.isEmpty()) return "EMPTY";
        Object d = s.item() instanceof mods.eln.generic.IDescriptorItem di ? di.getEaDescriptor() : null;
        if (d instanceof mods.eln.generic.GenericItemUsingDamageDescriptor g) return g.name;
        if (d instanceof mods.eln.generic.GenericItemBlockUsingDamageDescriptor g) return g.name;
        return BuiltInRegistries.ITEM.getKey(s.item()).getPath();
    }

    // ------------------------------------------------------------------ registration

    static String nextName(RStack output) {
        String base = stackName(output).replaceFirst("^[a-z0-9_]+:", "").toLowerCase(java.util.Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (base.isEmpty()) base = "recipe";
        int n = nameUse.containsKey(base) ? nameUse.get(base) + 1 : 1;
        nameUse.put(base, n);
        return n == 1 ? base : base + "_" + n;
    }

    static ResourceKey<Recipe<?>> key(String name) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Eln.MODID, name));
    }

    static Ingredient ingredient(Object o) {
        if (o instanceof String ore) return Ingredient.of(items.getOrThrow(OreDictTags.recipeTag(ore)));
        if (o instanceof TagKey<?> t) return Ingredient.of(items.getOrThrow(itemTag(t)));
        if (o instanceof RStack s) return Ingredient.of(s.item());
        if (o instanceof ItemLike l) return Ingredient.of(l);
        throw new IllegalArgumentException("ingredient " + o);
    }

    /** A vanilla item tag as an ingredient (a 26.x choice, not 1.12's: e.g. any wool). */
    @SuppressWarnings("unchecked")
    static TagKey<Item> itemTag(TagKey<?> t) {
        return (TagKey<Item>) t;
    }

    static Criterion<?> criterion(Object o) {
        if (o instanceof String ore) return hasTag.apply(OreDictTags.recipeTag(ore));
        if (o instanceof TagKey<?> t) return hasTag.apply(itemTag(t));
        if (o instanceof RStack s) return hasItem.apply(s.item());
        return hasItem.apply((ItemLike) o);
    }

    /**
     * The recipe result: the item, count and components, but without CUSTOM_DATA when it is the descriptor's default
     * tag (ignoring the random "rand"): the item makes that tag itself when first used, and one fixed "rand" in a
     * recipe would be shared by every crafted item. A changed tag (the empty brush: life 0) is kept.
     */
    static ItemStackTemplate result(RStack output) {
        DataComponentPatch patch = DataComponentPatch.EMPTY;
        if (output.tag() != null) {
            CompoundTag def = defaultTag(output.item());
            CompoundTag a = output.tag().copy(), b = def == null ? new CompoundTag() : def.copy();
            a.remove("rand");
            b.remove("rand");
            if (!a.equals(b))
                patch = DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(output.tag())).build();
        }
        return new ItemStackTemplate(output.item(), output.count(), patch);
    }

    /**
     * Load conditions for the tag ingredients EA does not fill itself: 1.12 kept a recipe whose ore name nothing had
     * (an uncraftable recipe); a 26.x recipe naming a tag that does not exist fails to load, so it is left out instead
     * (NeoForge not(tag_empty)). Plus the given conditions.
     */
    static ICondition[] conditions(ICondition[] given, Object[] params) {
        List<ICondition> c = new ArrayList<>();
        if (given != null) c.addAll(java.util.Arrays.asList(given));
        HashSet<String> seen = new HashSet<>();
        for (Object o : params) {
            if (!(o instanceof String ore) || o instanceof String && isPatternRow(ore, params)) continue;
            if (OreDictTags.EA_ENTRIES.containsKey(ore) || !seen.add(ore)) continue;
            c.add(new net.neoforged.neoforge.common.conditions.NotCondition(
                new net.neoforged.neoforge.common.conditions.TagEmptyCondition<>(OreDictTags.recipeTag(ore))));
        }
        return c.isEmpty() ? null : c.toArray(new ICondition[0]);
    }

    /** The leading strings of a shaped recipe are its pattern rows, not ore names. */
    static boolean isPatternRow(String s, Object[] params) {
        for (Object o : params) {
            if (!(o instanceof String)) return false;
            if (o == s) return true;
        }
        return false;
    }

    static boolean complete(String name, RStack output, Object[] params) {
        if (output.isEmpty()) return false; // the lookup miss is already listed
        for (Object o : params) {
            if (o == null) {
                problems.add(section + ": " + name + ": null ingredient");
                return false;
            }
            if (o instanceof RStack s && s.isEmpty()) return false; // listed by the lookup
        }
        return true;
    }

    /** 1.12 Utils.firstExistingOre(a, b): ore name a if it existed at startup, else b. */
    record FirstOf(String first, String fallback) {
    }

    public static Object firstExistingOre(String first, String fallback) {
        return new FirstOf(first, fallback);
    }

    /**
     * Shaped ore recipe (1.7.10 GameRegistry.addRecipe(new ShapedOreRecipe(output, params))). firstExistingOre
     * ingredients become one recipe per choice, each with load conditions (tag empty / not empty), as 1.12 decided at
     * startup.
     */
    public static void addRecipe(RStack output, Object... params) {
        List<Integer> choices = new ArrayList<>();
        for (int i = 0; i < params.length; i++) if (params[i] instanceof FirstOf) choices.add(i);
        if (choices.isEmpty()) {
            addRecipeIf(output, null, params);
            return;
        }
        for (int mask = 0; mask < (1 << choices.size()); mask++) {
            Object[] p = params.clone();
            List<ICondition> cond = new ArrayList<>();
            for (int c = 0; c < choices.size(); c++) {
                FirstOf f = (FirstOf) params[choices.get(c)];
                var tag = new net.neoforged.neoforge.common.conditions.TagEmptyCondition<>(OreDictTags.recipeTag(f.first()));
                if ((mask & (1 << c)) == 0) {
                    p[choices.get(c)] = f.first();
                    cond.add(new net.neoforged.neoforge.common.conditions.NotCondition(tag));
                } else {
                    p[choices.get(c)] = f.fallback();
                    cond.add(tag);
                }
            }
            addRecipeIf(output.copy(), cond.toArray(new ICondition[0]), p);
        }
    }

    /** As addRecipe, saved with NeoForge load conditions (1.12 decided by the ore names present at startup). */
    static void addRecipeIf(RStack output, ICondition[] conditions, Object... params) {
        craftingAttempted++;
        if (output.isEmpty()) return;
        String name = nextName(output);
        try {
            if (!complete(name, output, params)) return;
            Object[] p = sanitizeShaped(name, params);
            ShapedRecipeBuilder b = ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, result(output));
            int i = 0;
            for (; i < p.length && p[i] instanceof String; i++) b.pattern((String) p[i]);
            Object first = null;
            for (; i + 1 < p.length; i += 2) {
                b.define((Character) p[i], ingredient(p[i + 1]));
                if (first == null) first = p[i + 1];
            }
            b.unlockedBy("has_ingredient", criterion(first));
            ICondition[] all = conditions(conditions, p);
            b.save(all == null ? out : out.withConditions(all), key(name));
            craftingRegistered++;
        } catch (RuntimeException e) {
            problems.add(section + ": " + name + ": " + e);
        }
    }

    /** Shapeless ore recipe (1.7.10 GameRegistry.addRecipe(new ShapelessOreRecipe(output, params))). */
    public static void addShapelessRecipe(RStack output, Object... params) {
        craftingAttempted++;
        if (output.isEmpty()) return;
        String name = nextName(output);
        try {
            if (!complete(name, output, params)) return;
            ShapelessRecipeBuilder b = ShapelessRecipeBuilder.shapeless(items, RecipeCategory.MISC, result(output));
            for (Object o : params) b.requires(ingredient(o));
            b.unlockedBy("has_ingredient", criterion(params[0]));
            ICondition[] all = conditions(null, params);
            b.save(all == null ? out : out.withConditions(all), key(name));
            craftingRegistered++;
        } catch (RuntimeException e) {
            problems.add(section + ": " + name + ": " + e);
        }
    }

    /**
     * 1.7.10's ShapedOreRecipe ignored keys the pattern does not use and left symbols without a key empty; 1.12's (and
     * 26.x's) parser rejects both. Same recipe, expressed so it is accepted.
     */
    static Object[] sanitizeShaped(String name, Object[] params) {
        List<String> rows = new ArrayList<String>();
        int i = 0;
        while (i < params.length && params[i] instanceof String) rows.add((String) params[i++]);
        LinkedHashMap<Character, Object> keys = new LinkedHashMap<Character, Object>();
        for (; i < params.length; i += 2) {
            if (!(params[i] instanceof Character) || i + 1 >= params.length)
                throw new IllegalArgumentException("malformed shaped recipe at argument " + i + ": " + params[i]);
            keys.put((Character) params[i], params[i + 1]);
        }
        HashSet<Character> used = new HashSet<Character>();
        for (String r : rows) for (char c : r.toCharArray()) if (c != ' ') used.add(c);
        for (Character k : new ArrayList<Character>(keys.keySet())) {
            if (!used.contains(k)) {
                keys.remove(k);
                fixes.add(name + ": key '" + k + "' is not in the pattern " + rows + ": dropped");
            }
        }
        for (Character c : used) {
            if (!keys.containsKey(c)) {
                for (int r = 0; r < rows.size(); r++) rows.set(r, rows.get(r).replace(c.charValue(), ' '));
                fixes.add(name + ": pattern symbol '" + c + "' has no key (1.7.10: empty slot): now ' '");
            }
        }
        List<Object> o = new ArrayList<Object>(rows);
        for (Map.Entry<Character, Object> e : keys.entrySet()) {
            o.add(e.getKey());
            o.add(e.getValue());
        }
        return o.toArray();
    }

    public static void addSmelting(Item parentItem, int parentItemDamage, RStack result) {
        addSmelting(parentItem, parentItemDamage, result, 0.3f);
    }

    /** Furnace recipe; the first one for an input wins, as 1.12's FurnaceRecipes (Tree Resin: 1 Rubber, not 2). */
    public static void addSmelting(Item parentItem, int parentItemDamage, RStack result, float xp) {
        if (parentItem == null || parentItem == Items.AIR || result.isEmpty()) return;
        if (!smelted.add(parentItem)) {
            fixes.add("smelting " + BuiltInRegistries.ITEM.getKey(parentItem) + " -> " + stackName(result) + ": input already smelts (kept the first)");
            return;
        }
        String name = "smelting_" + nextName(stack(parentItem));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(parentItem), RecipeCategory.MISC, CookingBookCategory.MISC,
                result(result), xp, 200)
            .unlockedBy("has_ingredient", hasItem.apply(parentItem))
            .save(out, key(name));
        smeltingAdded++;
    }

    /** Blast furnace recipe (raw ores, as vanilla's: half the time). */
    public static void addBlasting(Item parentItem, RStack result, float xp) {
        if (parentItem == null || parentItem == Items.AIR || result.isEmpty()) return;
        String name = "blasting_" + nextName(stack(parentItem));
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(parentItem), RecipeCategory.MISC, CookingBookCategory.MISC,
                result(result), xp, 100)
            .unlockedBy("has_ingredient", hasItem.apply(parentItem))
            .save(out, key(name));
    }
}
