package mods.eln.registry;

import mods.eln.misc.OreDictTags;
import mods.eln.misc.Recipe;
import mods.eln.misc.RecipesList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static mods.eln.Eln.*;

/**
 * EA's machine recipes (macerator, plate machine, compressor, magnetizer: the 1.12 ElnRecipes lists, P5 step 4), filled
 * at server start (stacks exist then; tags are loaded). As in 1.12, an input also matches the other items under its
 * ore dictionary names: here the tags those names became (EA items: their addToOre names; vanilla items: the c: tags of
 * Forge's 1.12 vanilla ore names, so deepslate ores macerate too). The AE2 entries need other mods' tags and are added
 * only when those have items.
 */
public final class ElnMachineRecipes {
    private ElnMachineRecipes() {
    }

    /** Forge 1.12's vanilla ore dictionary names, as 26.x tags. */
    static final Map<Item, String> VANILLA_ORE_NAMES = Map.ofEntries(
        Map.entry(Items.IRON_ORE, "c:ores/iron"), Map.entry(Items.GOLD_ORE, "c:ores/gold"), Map.entry(Items.COAL_ORE, "c:ores/coal"),
        Map.entry(Items.IRON_INGOT, "c:ingots/iron"), Map.entry(Items.GOLD_INGOT, "c:ingots/gold"),
        Map.entry(Items.COBBLESTONE, "c:cobblestones"), Map.entry(Items.SAND, "c:sands"), Map.entry(Items.OAK_LOG, "minecraft:logs"),
        Map.entry(Items.GRAVEL, "c:gravels"), Map.entry(Items.DIAMOND, "c:gems/diamond"),
        // 26.x raw ore chunks (no 1.12 name)
        Map.entry(Items.RAW_IRON, "c:raw_materials/iron"), Map.entry(Items.RAW_GOLD, "c:raw_materials/gold"),
        Map.entry(Items.RAW_COPPER, "c:raw_materials/copper"));

    public static int maceratorModOreAdded;

    /** Server start: refill the four lists. */
    public static void registerAll() {
        for (RecipesList l : new RecipesList[]{maceratorRecipes, plateMachineRecipes, compressorRecipes, magnetiserRecipes})
            l.getRecipes().clear();
        recipeMacerator();
        recipePlateMachine();
        recipeCompressor();
        recipeMagnetizer();
        int before = maceratorRecipes.getRecipes().size();
        recipeMaceratorModOres();
        maceratorModOreAdded = maceratorRecipes.getRecipes().size() - before;
        LOG.info("Electrical Age machine recipes: macerator {} ({} from mod ores), compressor {}, plate machine {}, magnetizer {}",
            maceratorRecipes.getRecipes().size(), maceratorModOreAdded, compressorRecipes.getRecipes().size(),
            plateMachineRecipes.getRecipes().size(), magnetiserRecipes.getRecipes().size());
    }

    static ItemStack findItemStack(String name, int count) {
        ItemStack s = mods.eln.Eln.findItemStack(name, count);
        if (s.isEmpty()) throw new IllegalStateException("machine recipes: no EA item named '" + name + "'");
        return s;
    }

    static ItemStack findItemStack(String name) {
        return findItemStack(name, 1);
    }

    /** The tags of the input's 1.12 ore names. */
    static List<TagKey<Item>> tagsOf(ItemStack input) {
        List<TagKey<Item>> tags = new ArrayList<>();
        Object d = mods.eln.misc.Utils.getItemObject(input);
        for (Map.Entry<String, List<Object>> e : OreDictTags.EA_ENTRIES.entrySet())
            if (e.getValue().contains(d)) tags.add(OreDictTags.tag(e.getKey()));
        String v = VANILLA_ORE_NAMES.get(input.getItem());
        if (v != null) tags.add(TagKey.create(Registries.ITEM, Identifier.parse(v)));
        return tags;
    }

    static void add(RecipesList list, Recipe r) {
        list.addRecipe(r.withTags(tagsOf(r.input)));
    }

    /** 1.12 recipeMaceratorModOre: every item of the input ore name -> (count x) the first item of the output name. */
    public static void recipeMaceratorModOre(float f, String inputName, String outputName, int outputCount) {
        TagKey<Item> in = OreDictTags.tag(inputName), out = OreDictTags.tag(outputName);
        var outItems = BuiltInRegistries.ITEM.get(out).map(h -> h.stream().toList()).orElse(List.of());
        var inItems = BuiltInRegistries.ITEM.get(in).map(h -> h.stream().toList()).orElse(List.of());
        if (inItems.isEmpty() || outItems.isEmpty()) return;
        ItemStack output = new ItemStack(outItems.get(0), outputCount);
        maceratorRecipes.addRecipe(new Recipe(new ItemStack(inItems.get(0)), output, f).withTags(List.of(in)));
    }

    public static void recipeMacerator() {
        float f = 4000;
        // 26.x (Baughn, 2026-10-10): vanilla ore blocks macerate to raw chunks about like Fortune II (1-3, mean 1.75;
        // copper 2-5 base), and a chunk to one dust with a 20% chance of two. 1.12 gave 2 dust per ore block. EA's own
        // copper ore is gone (vanilla has copper).
        add(maceratorRecipes, new Recipe(new ItemStack(Blocks.IRON_ORE), new ItemStack(Items.RAW_IRON), 1.5 * f).withExtra(2, 1, 1));
        add(maceratorRecipes, new Recipe(new ItemStack(Blocks.GOLD_ORE), new ItemStack(Items.RAW_GOLD), 3.0 * f).withExtra(2, 1, 1));
        maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.COPPER_ORE), new ItemStack(Items.RAW_COPPER, 3), 1.0 * f)
            .withExtra(1, 1, 1, 1, 1, 1).withTags(List.of(TagKey.create(Registries.ITEM, Identifier.parse("c:ores/copper")))));
        add(maceratorRecipes, new Recipe(new ItemStack(Items.RAW_IRON), new ItemStack[]{findItemStack("Iron Dust", 1)}, 1.5 * f).withExtra(4, 1));
        add(maceratorRecipes, new Recipe(new ItemStack(Items.RAW_GOLD), new ItemStack[]{findItemStack("Gold Dust", 1)}, 3.0 * f).withExtra(4, 1));
        add(maceratorRecipes, new Recipe(new ItemStack(Items.RAW_COPPER), new ItemStack[]{findItemStack("Copper Dust", 1)}, 1.0 * f).withExtra(4, 1));
        add(maceratorRecipes, new Recipe(new ItemStack(Blocks.COAL_ORE, 1),
            new ItemStack(Items.COAL, 3), 1.0 * f));
        add(maceratorRecipes, new Recipe(findItemStack("Lead Ore"),
            new ItemStack[]{findItemStack("Lead Dust", 2)}, 2.0 * f));
        add(maceratorRecipes, new Recipe(findItemStack("Tungsten Ore"),
            new ItemStack[]{findItemStack("Tungsten Dust", 2)}, 2.0 * f));
        add(maceratorRecipes, new Recipe(new ItemStack(Items.COAL, 1),
            new ItemStack[]{findItemStack("Coal Dust", 2)}, 1.0 * f));
        add(maceratorRecipes, new Recipe(new ItemStack(Items.CHARCOAL),
            new ItemStack[]{findItemStack("Coal Dust", 2)}, 1.0 * f));
        add(maceratorRecipes, new Recipe(new ItemStack(Blocks.SAND, 1),
            new ItemStack[]{findItemStack("Silicon Dust", 1)}, 3.0 * f));
        add(maceratorRecipes, new Recipe(findItemStack("Cinnabar Ore"),
            new ItemStack[]{findItemStack("Cinnabar Dust", 2)}, 2.0 * f));

        add(maceratorRecipes, new Recipe(findItemStack("Copper Ingot"),
            new ItemStack[]{findItemStack("Copper Dust", 1)}, 0.5 * f));
        add(maceratorRecipes, new Recipe(new ItemStack(Items.IRON_INGOT),
            new ItemStack[]{findItemStack("Iron Dust", 1)}, 0.5 * f));
        add(maceratorRecipes, new Recipe(new ItemStack(Items.GOLD_INGOT),
            new ItemStack[]{findItemStack("Gold Dust", 1)}, 0.5 * f));
        add(maceratorRecipes, new Recipe(findItemStack("Lead Ingot"),
            new ItemStack[]{findItemStack("Lead Dust", 1)}, 0.5 * f));
        add(maceratorRecipes, new Recipe(findItemStack("Tungsten Ingot"),
            new ItemStack[]{findItemStack("Tungsten Dust", 1)}, 0.5 * f));

        add(maceratorRecipes, new Recipe(new ItemStack(Blocks.COBBLESTONE),
            new ItemStack[]{new ItemStack(Blocks.GRAVEL)}, 1.0 * f));
        add(maceratorRecipes, new Recipe(new ItemStack(Blocks.GRAVEL),
            new ItemStack[]{new ItemStack(Items.FLINT)}, 1.0 * f));

        add(maceratorRecipes, new Recipe(new ItemStack(Blocks.DIRT),
            new ItemStack[]{new ItemStack(Blocks.SAND)}, 1.0 * f));
    }

    public static void recipePlateMachine() {
        float f = 10000;
        add(plateMachineRecipes, new Recipe(
            findItemStack("Copper Ingot", plateConversionRatio),
            findItemStack("Copper Plate"), 1.0 * f));

        add(plateMachineRecipes, new Recipe(findItemStack("Lead Ingot", plateConversionRatio),
            findItemStack("Lead Plate"), 1.0 * f));

        add(plateMachineRecipes, new Recipe(
            findItemStack("Silicon Ingot", 4),
            findItemStack("Silicon Plate"), 1.0 * f));

        add(plateMachineRecipes, new Recipe(findItemStack("Alloy Ingot", plateConversionRatio),
            findItemStack("Alloy Plate"), 1.0 * f));

        add(plateMachineRecipes, new Recipe(new ItemStack(Items.IRON_INGOT, plateConversionRatio), findItemStack("Iron Plate"), 1.0 * f));

        add(plateMachineRecipes, new Recipe(new ItemStack(Items.GOLD_INGOT, plateConversionRatio), findItemStack("Gold Plate"), 1.0 * f));

    }

    public static void recipeCompressor() {
        add(compressorRecipes, new Recipe(findItemStack("Coal Plate", 4),
            new ItemStack[]{new ItemStack(Items.DIAMOND)}, 80000.0));
        // extractorRecipes.addRecipe(new
        // Recipe("dustCinnabar",new
        // ItemStack[]{findItemStack("Purified Cinnabar Dust",1)}, 1000.0));

        add(compressorRecipes, new Recipe(findItemStack("Coal Dust", 4),
            findItemStack("Coal Plate"), 4000.0));

        add(compressorRecipes, new Recipe(new ItemStack(Blocks.SAND),
            findItemStack("Dielectric"), 2000.0));

        add(compressorRecipes, new Recipe(new ItemStack(Blocks.OAK_LOG),
            findItemStack("Tree Resin"), 3000.0));

    }

    public static void recipeMagnetizer() {
        add(magnetiserRecipes, new Recipe(new ItemStack(Items.IRON_INGOT, 2),
            new ItemStack[]{findItemStack("Basic Magnet")}, 5000.0));
        add(magnetiserRecipes, new Recipe(findItemStack("Alloy Ingot", 2),
            new ItemStack[]{findItemStack("Advanced Magnet")}, 15000.0));
    }

    public static void recipeMaceratorModOres() {
        float f = 4000;

        // AE2:
        recipeMaceratorModOre(f * 3f, "oreCertusQuartz", "dustCertusQuartz", 3);
        recipeMaceratorModOre(f * 1.5f, "crystalCertusQuartz", "dustCertusQuartz", 1);
        recipeMaceratorModOre(f * 3f, "oreNetherQuartz", "dustNetherQuartz", 3);
        recipeMaceratorModOre(f * 1.5f, "crystalNetherQuartz", "dustNetherQuartz", 1);
        recipeMaceratorModOre(f * 1.5f, "crystalFluix", "dustFluix", 1);
    }
}
