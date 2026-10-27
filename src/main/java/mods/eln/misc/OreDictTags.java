package mods.eln.misc;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.7.10/1.12 ore dictionary names -> 26.x item tags (P5). Recipes that named an ore name take the tag; EA's own items
 * go into the tags their 1.12 addToOre / OreDictionary.registerOre calls named ({@link #addToOre}, written by datagen).
 * Rules: ingotX -> c:ingots/x, dustX -> c:dusts/x, plateX -> c:plates/x, oreX -> c:ores/x, blockX ->
 * c:storage_blocks/x, plus the explicit table. EA-private names (1.12 config dictionary.tungsten=false: oreElnTungsten
 * ...) become eln: tags. An unknown name is an error (1.12 failed startup on a recipe miss too).
 */
public final class OreDictTags {
    private OreDictTags() {
    }

    static final Map<String, String> EXPLICIT = Map.ofEntries(
        Map.entry("plankWood", "minecraft:planks"),
        Map.entry("logWood", "minecraft:logs"),
        Map.entry("treeWood", "minecraft:logs"),
        Map.entry("treeLeaves", "minecraft:leaves"),
        Map.entry("itemRubber", "c:rubbers"),
        Map.entry("materialResin", "c:resins"),
        Map.entry("sheetPlastic", "c:plastics"),
        Map.entry("quicksilver", "c:quicksilver"),
        Map.entry("circuitBasic", "c:circuits/basic"),
        Map.entry("circuitAdvanced", "c:circuits/advanced"),
        Map.entry("circuitElnBasic", "eln:circuits/basic"),
        Map.entry("circuitElnAdvanced", "eln:circuits/advanced"),
        Map.entry("oreElnTungsten", "eln:ores/tungsten"),
        Map.entry("dustElnTungsten", "eln:dusts/tungsten"),
        Map.entry("ingotElnTungsten", "eln:ingots/tungsten"),
        Map.entry("casingMachine", "c:casings/machine"),
        Map.entry("casingMachineAdvanced", "c:casings/machine_advanced"),
        Map.entry("ingotAluminium", "c:ingots/aluminum"), // 1.12 had both spellings
        Map.entry("crystalCertusQuartz", "c:gems/certus_quartz"),
        Map.entry("crystalNetherQuartz", "c:gems/quartz"),
        Map.entry("crystalFluix", "c:gems/fluix"),
        Map.entry("oreNetherQuartz", "c:ores/quartz"),
        Map.entry("dustNetherQuartz", "c:dusts/quartz"));

    static final String[][] RULES = {
        {"ingot", "c:ingots/"}, {"dust", "c:dusts/"}, {"plate", "c:plates/"}, {"ore", "c:ores/"},
        {"block", "c:storage_blocks/"}, {"gem", "c:gems/"}, {"nugget", "c:nuggets/"}};

    /** The tag id for a 1.12 ore name; throws for a name no rule covers. */
    public static String tagId(String oreName) {
        String e = EXPLICIT.get(oreName);
        if (e != null) return e;
        for (String[] r : RULES) {
            if (oreName.startsWith(r[0]) && oreName.length() > r[0].length() && Character.isUpperCase(oreName.charAt(r[0].length())))
                return r[1] + snake(oreName.substring(r[0].length()));
        }
        throw new IllegalArgumentException("no 26.x tag for the 1.12 ore name '" + oreName + "' (OreDictTags)");
    }

    public static TagKey<Item> tag(String oreName) {
        return TagKey.create(Registries.ITEM, Identifier.parse(tagId(oreName)));
    }

    /**
     * The tag EA's recipes take for an ore name: a plate is eln:high_quality_plates/&lt;metal&gt; (Baughn 2026-10-27: EA's
     * plates hold their tolerances, a hammered one doesn't; a tag of its own so a modpack can add other plates), the rest
     * as {@link #tag}.
     */
    public static TagKey<Item> recipeTag(String oreName) {
        String id = tagId(oreName);
        return id.startsWith("c:plates/") ? highQualityPlates(id.substring("c:plates/".length())) : tag(oreName);
    }

    public static TagKey<Item> highQualityPlates(String metal) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("eln", "high_quality_plates/" + metal));
    }

    /** "CertusQuartz" -> "certus_quartz". */
    static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }

    /** EA items by 1.12 ore name, in registration order (each entry a descriptor; its item is resolved by datagen). */
    public static final Map<String, List<Object>> EA_ENTRIES = new LinkedHashMap<>();

    /** 1.12 addToOre(name, descriptor.newItemStack()) / OreDictionary.registerOre: recorded for the item tags. */
    public static void addToOre(String oreName, Object descriptor) {
        tagId(oreName); // fail early on unknown names
        EA_ENTRIES.computeIfAbsent(oreName, k -> new ArrayList<>()).add(descriptor);
    }
}
