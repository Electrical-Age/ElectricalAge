package mods.eln.registry;

import mods.eln.Eln;
import mods.eln.compat.GameRegistryCompat;
import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.BrushDescriptor;
import mods.eln.misc.Recipe;
import mods.eln.misc.RecipesList;
import mods.eln.misc.Utils;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.launchwrapper.LogWrapper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;
import static mods.eln.registry.batch.Wp11Content.elnToOtherBlockHvu;
import static mods.eln.registry.batch.Wp11Content.elnToOtherBlockLvu;
import static mods.eln.registry.batch.Wp11Content.elnToOtherBlockMvu;

/**
 * Electrical Age recipes (WP15). The recipe methods below are the 1.7.10 Eln.java ones, unchanged except the
 * brush recipes (moved here from registerBrush) and smelting through the counting {@link #addSmelting}.
 * <ul>
 * <li>All of them run on RegistryEvent.Register&lt;IRecipe&gt; ({@link #EVENTS}, registered in ElnContentImpl.preInit),
 * which Forge fires after every mod's preInit and before init, in the 1.7.10 order. Crafting recipes become
 * ShapedOreRecipe / ShapelessOreRecipe named eln:&lt;snake_case output name&gt;[_n]; machine recipes go into EA's
 * RecipesList (macerator, compressor, plate machine, magnetizer); smelting into FurnaceRecipes. The AE2 macerator
 * recipes need other mods' ore dictionary and stay in postInit ({@link #recipeMaceratorModOres}).</li>
 * <li>EA items are looked up by descriptor name ({@link #findItemStack}: the TR_NAME key a descriptor registers with,
 * not a translated name). Every miss, null ingredient or rejected recipe is collected; after the last recipe the
 * whole list is thrown (startup fails, listing all of them).</li>
 * <li>1.12's parser rejects two things 1.7.10 accepted: keys that the pattern does not use (dropped) and pattern
 * symbols without a key (1.7.10: empty slot; here replaced by ' '). Each rewrite is logged and listed in {@link #fixes}.</li>
 * <li>ItemStack ingredients match regardless of NBT (Ingredient.fromStacks), as 1.7.10's OreDictionary.itemMatches did.
 * Machine recipes match any item sharing an ore dictionary name with the input (Utils.areSame), so other mods'
 * oreCopper / oreLead / ingotCopper work in EA's macerator and plate machine.</li>
 * <li>config debug.dumpRecipes=true: {@link #dumpIfEnabled} writes config/eln-recipes-dump.txt on server start.</li>
 * </ul>
 */
@SuppressWarnings({"SameParameterValue", "PointlessArithmeticExpression", "unused"})
public final class ElnRecipes {
    private ElnRecipes() {
    }

    /** Forge event bus listener (Register&lt;IRecipe&gt;). */
    public static final ElnRecipes EVENTS = new ElnRecipes();
    private static final Logger LOG = LogManager.getLogger("eln-recipes");

    /** Crafting recipe calls made / registered (eln:*); smelting entries added; macerator entries from mod ores. */
    public static int craftingAttempted, craftingRegistered, smeltingAdded, maceratorModOreAdded;
    /** Registry path -> the recipe as written (pattern + keys / ingredients), for the dump. */
    public static final Map<String, String> descriptions = new LinkedHashMap<String, String>();
    /** Smelting entries added by EA: "input -> output". */
    public static final List<String> smeltings = new ArrayList<String>();
    /** 1.7.10 pattern quirks rewritten for 1.12's parser. */
    public static final List<String> fixes = new ArrayList<String>();
    private static final List<String> problems = new ArrayList<String>();
    private static final Map<String, Integer> nameUse = new HashMap<String, Integer>();
    private static IForgeRegistry<IRecipe> registry;
    private static String section = "?";

    @SubscribeEvent
    public void onRegisterRecipes(RegistryEvent.Register<IRecipe> event) {
        registerAll(event.getRegistry());
    }

    /** The 1.7.10 Eln.load() recipe sequence (+ registerBrush's recipes first; recipeComputerProbe dropped, rule 8). */
    static void registerAll(IForgeRegistry<IRecipe> r) {
        registry = r;
        final HashSet<String> oreNames = new HashSet<String>(Arrays.asList(OreDictionary.getOreNames()));
        run("recipeBrush", ElnRecipes::recipeBrush);
        run("recipeEnergyConverter", ElnRecipes::recipeEnergyConverter);
        run("recipeArmor", ElnRecipes::recipeArmor);
        run("recipeTool", ElnRecipes::recipeTool);
        run("recipeGround", ElnRecipes::recipeGround);
        run("recipeElectricalSource", ElnRecipes::recipeElectricalSource);
        run("recipeElectricalCable", ElnRecipes::recipeElectricalCable);
        run("recipeThermalCable", ElnRecipes::recipeThermalCable);
        run("recipeLampSocket", ElnRecipes::recipeLampSocket);
        run("recipeLampSupply", ElnRecipes::recipeLampSupply);
        run("recipePowerSocket", ElnRecipes::recipePowerSocket);
        run("recipePassiveComponent", ElnRecipes::recipePassiveComponent);
        run("recipeSwitch", ElnRecipes::recipeSwitch);
        run("recipeWirelessSignal", ElnRecipes::recipeWirelessSignal);
        run("recipeElectricalRelay", ElnRecipes::recipeElectricalRelay);
        run("recipeElectricalDataLogger", ElnRecipes::recipeElectricalDataLogger);
        run("recipeElectricalGateSource", ElnRecipes::recipeElectricalGateSource);
        run("recipeElectricalBreaker", ElnRecipes::recipeElectricalBreaker);
        run("recipeFuses", ElnRecipes::recipeFuses);
        run("recipeElectricalVuMeter", ElnRecipes::recipeElectricalVuMeter);
        run("recipeElectricalEnvironmentalSensor", ElnRecipes::recipeElectricalEnvironmentalSensor);
        run("recipeElectricalRedstone", ElnRecipes::recipeElectricalRedstone);
        run("recipeElectricalGate", ElnRecipes::recipeElectricalGate);
        run("recipeElectricalAlarm", ElnRecipes::recipeElectricalAlarm);
        run("recipeSixNodeCache", ElnRecipes::recipeSixNodeCache);
        run("recipeElectricalSensor", ElnRecipes::recipeElectricalSensor);
        run("recipeThermalSensor", ElnRecipes::recipeThermalSensor);
        run("recipeSixNodeMisc", ElnRecipes::recipeSixNodeMisc);
        run("recipeTurret", ElnRecipes::recipeTurret);
        run("recipeMachine", ElnRecipes::recipeMachine);
        run("recipeChips", ElnRecipes::recipeChips);
        run("recipeTransformer", ElnRecipes::recipeTransformer);
        run("recipeHeatFurnace", ElnRecipes::recipeHeatFurnace);
        run("recipeTurbine", ElnRecipes::recipeTurbine);
        run("recipeBattery", ElnRecipes::recipeBattery);
        run("recipeElectricalFurnace", ElnRecipes::recipeElectricalFurnace);
        run("recipeAutoMiner", ElnRecipes::recipeAutoMiner);
        run("recipeSolarPanel", ElnRecipes::recipeSolarPanel);
        run("recipeThermalDissipatorPassiveAndActive", ElnRecipes::recipeThermalDissipatorPassiveAndActive);
        run("recipeElectricalAntenna", ElnRecipes::recipeElectricalAntenna);
        run("recipeEggIncubator", ElnRecipes::recipeEggIncubator);
        run("recipeBatteryCharger", ElnRecipes::recipeBatteryCharger);
        run("recipeTransporter", ElnRecipes::recipeTransporter);
        run("recipeWindTurbine", ElnRecipes::recipeWindTurbine);
        run("recipeFuelGenerator", ElnRecipes::recipeFuelGenerator);
        run("recipeGeneral", ElnRecipes::recipeGeneral);
        run("recipeHeatingCorp", ElnRecipes::recipeHeatingCorp);
        run("recipeRegulatorItem", ElnRecipes::recipeRegulatorItem);
        run("recipeLampItem", ElnRecipes::recipeLampItem);
        run("recipeProtection", ElnRecipes::recipeProtection);
        run("recipeCombustionChamber", ElnRecipes::recipeCombustionChamber);
        run("recipeFerromagneticCore", ElnRecipes::recipeFerromagneticCore);
        run("recipeIngot", ElnRecipes::recipeIngot);
        run("recipeDust", ElnRecipes::recipeDust);
        run("recipeElectricalMotor", ElnRecipes::recipeElectricalMotor);
        run("recipeSolarTracker", ElnRecipes::recipeSolarTracker);
        run("recipeDynamo", ElnRecipes::recipeDynamo);
        run("recipeWindRotor", ElnRecipes::recipeWindRotor);
        run("recipeMeter", ElnRecipes::recipeMeter);
        run("recipeElectricalDrill", ElnRecipes::recipeElectricalDrill);
        run("recipeOreScanner", ElnRecipes::recipeOreScanner);
        run("recipeMiningPipe", ElnRecipes::recipeMiningPipe);
        run("recipeTreeResinAndRubber", ElnRecipes::recipeTreeResinAndRubber);
        run("recipeRawCable", ElnRecipes::recipeRawCable);
        run("recipeMiscItem", ElnRecipes::recipeMiscItem);
        run("recipeBatteryItem", ElnRecipes::recipeBatteryItem);
        run("recipeElectricalTool", ElnRecipes::recipeElectricalTool);
        run("recipePortableCapacitor", ElnRecipes::recipePortableCapacitor);
        run("recipeFurnace", ElnRecipes::recipeFurnace);
        run("recipeMacerator", ElnRecipes::recipeMacerator);
        run("recipeCompressor", ElnRecipes::recipeCompressor);
        run("recipePlateMachine", ElnRecipes::recipePlateMachine);
        run("recipeMagnetizer", ElnRecipes::recipeMagnetizer);
        run("recipeFuelBurnerItem", ElnRecipes::recipeFuelBurnerItem);
        run("recipeECoal", ElnRecipes::recipeECoal);
        run("recipeGridDevices", () -> recipeGridDevices(oreNames));
        section = "?";
        registry = null;
        LOG.info("Electrical Age recipes: crafting " + craftingRegistered + "/" + craftingAttempted + ", smelting "
            + smeltingAdded + ", macerator " + maceratorRecipes.getRecipes().size() + ", compressor "
            + compressorRecipes.getRecipes().size() + ", plate machine " + plateMachineRecipes.getRecipes().size()
            + ", magnetizer " + magnetiserRecipes.getRecipes().size() + "; 1.7.10 pattern fixes " + fixes.size());
        if (!problems.isEmpty()) {
            StringBuilder b = new StringBuilder("Electrical Age: " + problems.size() + " recipe problem(s) (WP15):");
            for (String p : problems) b.append("\n  ").append(p);
            LOG.error(b.toString());
            throw new IllegalStateException(b.toString());
        }
    }

    private static void run(String name, Runnable recipes) {
        section = name;
        try {
            recipes.run();
        } catch (RuntimeException e) {
            problems.add(name + ": " + e);
        }
    }

    // ------------------------------------------------------------------ lookups

    /** EA stack by descriptor name (or EA ore dictionary name); a miss is recorded (startup fails after all recipes). */
    public static ItemStack findItemStack(String name, int stackSize) {
        ItemStack stack = Eln.findItemStack(name, stackSize);
        if (Utils.isEmpty(stack)) {
            problems.add(section + ": no EA item named '" + name + "'");
            return ItemStack.EMPTY;
        }
        return stack;
    }

    public static ItemStack findItemStack(String name) {
        return findItemStack(name, 1);
    }

    /** Descriptor name of an EA stack, else the item's registry path (+ @meta when not 0). */
    public static String stackName(ItemStack s) {
        if (Utils.isEmpty(s)) return "EMPTY";
        Item it = s.getItem();
        if (it instanceof GenericItemUsingDamage) {
            Object d = ((GenericItemUsingDamage<?>) it).getDescriptor(s);
            if (d != null) return ((GenericItemUsingDamageDescriptor) d).name;
        }
        if (it instanceof GenericItemBlockUsingDamage) {
            Object d = ((GenericItemBlockUsingDamage<?>) it).getDescriptor(s);
            if (d != null) return ((GenericItemBlockUsingDamageDescriptor) d).name;
        }
        String n = it.getRegistryName() == null ? String.valueOf(it) : it.getRegistryName().toString();
        return s.getMetadata() == 0 ? n : n + "@" + s.getMetadata();
    }

    private static String describe(Object o) {
        if (o instanceof String) return "ore:" + o;
        if (o instanceof ItemStack) {
            ItemStack s = (ItemStack) o;
            return (s.getCount() != 1 ? s.getCount() + "x " : "") + stackName(s);
        }
        return String.valueOf(o);
    }

    // ------------------------------------------------------------------ registration

    private static String nextName(ItemStack output) {
        String base = GameRegistryCompat.registryName(stackName(output).replaceFirst("^[a-z0-9_]+:", ""));
        if (base.isEmpty()) base = "recipe";
        int n = nameUse.containsKey(base) ? nameUse.get(base) + 1 : 1;
        nameUse.put(base, n);
        return n == 1 ? base : base + "_" + n;
    }

    /** @return false (and records the problem) when the output or an ingredient is missing. */
    private static boolean complete(String name, ItemStack output, Object[] params) {
        if (Utils.isEmpty(output)) return false; // the lookup miss is already listed
        for (Object o : params) {
            if (o == null) {
                problems.add(section + ": " + name + ": null ingredient");
                return false;
            }
            if (o instanceof ItemStack && ((ItemStack) o).isEmpty()) return false; // listed by the lookup
        }
        return true;
    }

    private static void register(String name, IRecipe recipe, String description) {
        recipe.setRegistryName(new ResourceLocation(MODID, name));
        registry.register(recipe);
        descriptions.put(name, description);
        craftingRegistered++;
    }

    /** Shaped ore recipe (1.7.10 GameRegistry.addRecipe(new ShapedOreRecipe(output, params))). */
    public static void addRecipe(ItemStack output, Object... params) {
        craftingAttempted++;
        String name = nextName(output);
        try {
            if (!complete(name, output, params)) return;
            Object[] p = sanitizeShaped(name, params);
            StringBuilder d = new StringBuilder("shaped    ");
            int i = 0;
            for (; i < p.length && p[i] instanceof String; i++) d.append(i == 0 ? "" : "/").append('[').append(p[i]).append(']');
            for (; i + 1 < p.length; i += 2) d.append(' ').append(p[i]).append('=').append(describe(p[i + 1]));
            register(name, new ShapedOreRecipe(null, output, p), d.toString());
        } catch (RuntimeException e) {
            problems.add(section + ": " + name + ": " + e);
        }
    }

    /** Shapeless ore recipe (1.7.10 GameRegistry.addRecipe(new ShapelessOreRecipe(output, params))). */
    public static void addShapelessRecipe(ItemStack output, Object... params) {
        craftingAttempted++;
        String name = nextName(output);
        try {
            if (!complete(name, output, params)) return;
            StringBuilder d = new StringBuilder("shapeless");
            for (Object o : params) d.append(' ').append(describe(o)).append(',');
            d.setLength(d.length() - 1);
            register(name, new ShapelessOreRecipe(null, output, params), d.toString());
        } catch (RuntimeException e) {
            problems.add(section + ": " + name + ": " + e);
        }
    }

    /**
     * 1.7.10's ShapedOreRecipe ignored keys the pattern does not use and left symbols without a key empty; 1.12's
     * CraftingHelper.parseShaped throws for both. Same recipe, expressed so 1.12 accepts it.
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
                fix(name + ": key '" + k + "' is not in the pattern " + rows + ": dropped");
            }
        }
        for (Character c : used) {
            if (!keys.containsKey(c)) {
                for (int r = 0; r < rows.size(); r++) rows.set(r, rows.get(r).replace(c.charValue(), ' '));
                fix(name + ": pattern symbol '" + c + "' has no key (1.7.10: empty slot): now ' '");
            }
        }
        List<Object> out = new ArrayList<Object>(rows);
        for (Map.Entry<Character, Object> e : keys.entrySet()) {
            out.add(e.getKey());
            out.add(e.getValue());
        }
        return out.toArray();
    }

    private static void fix(String s) {
        fixes.add(s);
        LOG.info("1.7.10 recipe rewritten for 1.12: " + s);
    }

    /** Utils.addSmelting, counted; a missing input/output is already listed by the lookup. */
    public static void addSmelting(Item parentItem, int parentItemDamage, ItemStack result) {
        addSmelting(parentItem, parentItemDamage, result, 0.3f);
    }

    /**
     * 1.12 FurnaceRecipes keeps the first recipe for an input and ignores later ones (1.7.10 kept both in a HashMap and
     * used whichever its iteration met first): Tree Resin is smelted by recipeGeneral (1 Rubber) and recipeFurnace (2).
     */
    public static void addSmelting(Item parentItem, int parentItemDamage, ItemStack result, float xp) {
        if (parentItem == null || parentItem == Items.AIR || Utils.isEmpty(result)) return;
        ItemStack in = new ItemStack(parentItem, 1, parentItemDamage);
        ItemStack old = FurnaceRecipes.instance().getSmeltingResult(in);
        if (!old.isEmpty()) {
            fix("smelting " + stackName(in) + " -> " + describe(result) + ": input already smelts to " + describe(old) + " (kept)");
            return;
        }
        Utils.addSmelting(parentItem, parentItemDamage, result, xp);
        smeltingAdded++;
        smeltings.add(stackName(in) + " -> " + describe(result));
    }

    // ------------------------------------------------------------------ dump (config debug.dumpRecipes)

    /** Server start: write every EA recipe to config/eln-recipes-dump.txt when debug.dumpRecipes is true. */
    public static void dumpIfEnabled() {
        if (!dumpRecipes) return;
        File f = new File(Loader.instance().getConfigDir(), "eln-recipes-dump.txt");
        try (PrintWriter w = new PrintWriter(f, "UTF-8")) {
            dump(w);
            LOG.info("Electrical Age recipes dumped to " + f);
        } catch (IOException e) {
            LOG.error("cannot write " + f, e);
        }
    }

    static void dump(PrintWriter w) {
        w.println("# Electrical Age recipes (config debug.dumpRecipes). Crafting: registry name, output, recipe as written.");
        w.println("# '!! no items' marks an ingredient nothing in this pack matches (e.g. an ore name no mod registers).");
        TreeMap<String, IRecipe> crafting = new TreeMap<String, IRecipe>();
        for (Map.Entry<ResourceLocation, IRecipe> e : ForgeRegistries.RECIPES.getEntries())
            if (MODID.equals(e.getKey().getNamespace())) crafting.put(e.getKey().getPath(), e.getValue());
        w.println();
        w.println("## crafting: " + crafting.size() + " registered (" + craftingAttempted + " recipe calls)");
        for (Map.Entry<String, IRecipe> e : crafting.entrySet()) {
            IRecipe r = e.getValue();
            StringBuilder dead = new StringBuilder();
            int slot = 0;
            for (Ingredient ing : r.getIngredients()) {
                if (ing != Ingredient.EMPTY && ing.getMatchingStacks().length == 0) dead.append(" ").append(slot);
                slot++;
            }
            String d = descriptions.containsKey(e.getKey()) ? descriptions.get(e.getKey()) : r.getClass().getSimpleName();
            w.println("eln:" + e.getKey() + " -> " + describe(r.getRecipeOutput()) + " | " + d
                + (dead.length() > 0 ? " | !! no items for slot(s)" + dead : ""));
        }
        w.println();
        w.println("## 1.7.10 pattern quirks rewritten for 1.12: " + fixes.size());
        for (String s : fixes) w.println(s);
        w.println();
        w.println("## smelting added by EA: " + smeltings.size());
        for (String s : smeltings) w.println(s);
        w.println();
        w.println("## furnace recipes producing EA items or smelting EA items (all mods)");
        for (Map.Entry<ItemStack, ItemStack> e : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
            if (isEln(e.getKey()) || isEln(e.getValue()))
                w.println(describe(e.getKey()) + " -> " + describe(e.getValue()));
        }
        dumpList(w, "macerator", maceratorRecipes);
        dumpList(w, "compressor", compressorRecipes);
        dumpList(w, "plate machine", plateMachineRecipes);
        dumpList(w, "magnetizer", magnetiserRecipes);
        w.println();
        w.println("## machine inputs match any item sharing an ore dictionary name with the listed input (Utils.areSame)");
    }

    private static boolean isEln(ItemStack s) {
        return !Utils.isEmpty(s) && s.getItem().getRegistryName() != null && MODID.equals(s.getItem().getRegistryName().getNamespace());
    }

    private static void dumpList(PrintWriter w, String name, RecipesList list) {
        w.println();
        w.println("## " + name + ": " + list.getRecipes().size());
        for (Recipe r : list.getRecipes()) {
            StringBuilder b = new StringBuilder(describe(r.input)).append(" ->");
            for (ItemStack o : r.output) b.append(' ').append(describe(o));
            int[] ids = Utils.isEmpty(r.input) ? new int[0] : OreDictionary.getOreIDs(r.input);
            if (ids.length > 0) {
                b.append("  (also:");
                for (int id : ids) b.append(" ore:").append(OreDictionary.getOreName(id));
                b.append(')');
            }
            w.println(b.append("  ").append(Math.round(r.energy)).append(" J"));
        }
    }

    // ------------------------------------------------------------------ recipes (1.7.10)

    /** 1.7.10 registerBrush: empty white brush from wool + iron, coloured brushes from dye + empty white brush. */
    public static void recipeBrush() {
        String[] subNames = {"Black Brush", "Red Brush", "Green Brush", "Brown Brush", "Blue Brush", "Purple Brush",
            "Cyan Brush", "Silver Brush", "Gray Brush", "Pink Brush", "Lime Brush", "Yellow Brush", "Light Blue Brush",
            "Magenta Brush", "Orange Brush", "White Brush"};
        ItemStack emptyStack = findItemStack("White Brush");
        if (!emptyStack.isEmpty()) ((BrushDescriptor) sharedItem.getDescriptor(emptyStack)).setLife(emptyStack, 0);

        for (int idx = 0; idx < 16; idx++) {
            addShapelessRecipe(emptyStack.copy(),
                new ItemStack(Blocks.WOOL, 1, idx),
                new ItemStack(Items.IRON_INGOT));
        }

        for (int idx = 0; idx < 16; idx++) {
            addShapelessRecipe(findItemStack(subNames[idx], 1),
                new ItemStack(Items.DYE, 1, idx),
                emptyStack.copy());
        }
    }

    /** postInit (1.7.10 modsLoaded): AE2 macerator recipes from other mods' ore dictionary entries. */
    public static void registerModOres() {
        int before = maceratorRecipes.getRecipes().size();
        recipeMaceratorModOres();
        maceratorModOreAdded = maceratorRecipes.getRecipes().size() - before;
    }

    public static void recipeGround() {
        addRecipe(findItemStack("Ground Cable"),
            " C ",
            " C ",
            "CCC",
            'C', findItemStack("Copper Cable"));
    }

    public static void recipeElectricalSource() {
        // Trololol
    }

    public static void recipeElectricalCable() {
        addRecipe(signalCableDescriptor.newItemStack(1),
            "R",
            "C",
            'C', findItemStack("Iron Cable"),
            'R', "itemRubber");

        addRecipe(lowVoltageCableDescriptor.newItemStack(1),
            "R",
            "C",
            'C', findItemStack("Copper Cable"),
            'R', "itemRubber");

        addRecipe(meduimVoltageCableDescriptor.newItemStack(1),
            "R",
            "C",
            'C', lowVoltageCableDescriptor.newItemStack(1),
            'R', "itemRubber");

        addRecipe(highVoltageCableDescriptor.newItemStack(1),
            "R",
            "C",
            'C', meduimVoltageCableDescriptor.newItemStack(1),
            'R', "itemRubber");

        addRecipe(signalCableDescriptor.newItemStack(6),
            "RRR",
            "CCC",
            "RRR",
            'C', new ItemStack(Items.IRON_INGOT),
            'R', "itemRubber");

        addRecipe(lowVoltageCableDescriptor.newItemStack(6),
            "RRR",
            "CCC",
            "RRR",
            'C', "ingotCopper",
            'R', "itemRubber");


        addRecipe(veryHighVoltageCableDescriptor.newItemStack(6),
            "RRR",
            "CCC",
            "RRR",
            'C', "ingotAlloy",
            'R', "itemRubber");

    }

    public static void recipeThermalCable() {
        addRecipe(findItemStack("Copper Thermal Cable", 6),
            "SSS",
            "CCC",
            "SSS",
            'S', new ItemStack(Blocks.COBBLESTONE),
            'C', "ingotCopper");

        addRecipe(findItemStack("Copper Thermal Cable", 1),
            "S",
            "C",
            'S', new ItemStack(Blocks.COBBLESTONE),
            'C', findItemStack("Copper Cable"));
    }

    public static void recipeLampSocket() {
        addRecipe(findItemStack("Lamp Socket A", 3),
            "G ",
            "IG",
            "G ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Lamp Socket B Projector", 3),
            " I",
            "IG",
            " I",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Street Light", 1),
            "G",
            "I",
            "I",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Robust Lamp Socket", 3),
            "GIG",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'I', new ItemStack(Items.IRON_INGOT));
        addRecipe(findItemStack("Flat Lamp Socket", 3),
            "IGI",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'I', new ItemStack(Items.IRON_INGOT));
        addRecipe(findItemStack("Simple Lamp Socket", 3),
            " I ",
            "GGG",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Fluorescent Lamp Socket", 3),
            " I ",
            "I I",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'I', new ItemStack(Items.IRON_INGOT));


        addRecipe(findItemStack("Suspended Lamp Socket", 2),
            "I",
            "G",
            'G', findItemStack("Robust Lamp Socket"),
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Long Suspended Lamp Socket", 2),
            "I",
            "I",
            "G",
            'G', findItemStack("Robust Lamp Socket"),
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Sconce Lamp Socket", 2),
            "GCG",
            "GIG",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'C', "dustCoal",
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("50V Emergency Lamp"),
            "cbc",
            " l ",
            " g ",
            'c', findItemStack("Low Voltage Cable"),
            'b', findItemStack("Portable Battery Pack"),
            'l', findItemStack("50V LED Bulb"),
            'g', new ItemStack(Blocks.GLASS_PANE));

        addRecipe(findItemStack("200V Emergency Lamp"),
            "cbc",
            " l ",
            " g ",
            'c', findItemStack("Medium Voltage Cable"),
            'b', findItemStack("Portable Battery Pack"),
            'l', findItemStack("200V LED Bulb"),
            'g', new ItemStack(Blocks.GLASS_PANE));
    }

    public static void recipeLampSupply() {
        addRecipe(findItemStack("Lamp Supply", 1),
            " I ",
            "ICI",
            " I ",
            'C', "ingotCopper",
            'I', new ItemStack(Items.IRON_INGOT));

    }

    public static void recipePowerSocket() {
        addRecipe(findItemStack("50V Power Socket", 16),
            "RUR",
            "ACA",
            'R', "itemRubber",
            'U', findItemStack("Copper Plate"),
            'A', findItemStack("Alloy Plate"),
            'C', findItemStack("Low Voltage Cable"));
        addRecipe(findItemStack("200V Power Socket", 16),
            "RUR",
            "ACA",
            'R', "itemRubber",
            'U', findItemStack("Copper Plate"),
            'A', findItemStack("Alloy Plate"),
            'C', findItemStack("Medium Voltage Cable"));
    }

    public static void recipePassiveComponent() {
        addRecipe(findItemStack("Signal Diode", 4),
            " RB",
            "IIR",
            " RB",
            'R', new ItemStack(Items.REDSTONE),
            'I', findItemStack("Iron Cable"),
            'B', "itemRubber");

        addRecipe(findItemStack("10A Diode", 3),
            " RB",
            "IIR",
            " RB",
            'R', new ItemStack(Items.REDSTONE),
            'I', new ItemStack(Items.IRON_INGOT),
            'B', "itemRubber");

        addRecipe(findItemStack("25A Diode"),
            "D",
            "D",
            "D",
            'D', findItemStack("10A Diode"));


        addRecipe(findItemStack("Power Capacitor"),
            "cPc",
            "III",
            'I', new ItemStack(Items.IRON_INGOT),
            'c', findItemStack("Iron Cable"),
            'P', "plateIron");

        addRecipe(findItemStack("Power Inductor"),
            " P ",
            "cIc",
            "IPI",
            'I', new ItemStack(Items.IRON_INGOT),
            'c', findItemStack("Copper Cable"),
            'P', "plateIron");

        addRecipe(findItemStack("Power Resistor"),
            " P ",
            "c c",
            "IPI",
            'I', new ItemStack(Items.IRON_INGOT),
            'c', findItemStack("Copper Cable"),
            'P', "plateCopper");

        addRecipe(findItemStack("Rheostat"),
            " R ",
            " MS",
            "cmc",
            'R', findItemStack("Power Resistor"),
            'c', findItemStack("Copper Cable"),
            'm', findItemStack("Machine Block"),
            'M', findItemStack("Electrical Motor"),
            'S', findItemStack("Signal Cable")
        );

        addRecipe(findItemStack("Thermistor"),
            " P ",
            "csc",
            "IPI",
            's', "dustSilicon",
            'I', new ItemStack(Items.IRON_INGOT),
            'c', findItemStack("Copper Cable"),
            'P', "plateCopper");

        addRecipe(findItemStack("Large Rheostat"),
            "   ",
            " D ",
            "CRC",
            'R', findItemStack("Rheostat"),
            'C', findItemStack("Copper Thermal Cable"),
            'D', findItemStack("Small Passive Thermal Dissipator")
        );
    }

    public static void recipeSwitch() {
		/*
		 * addRecipe(findItemStack("Signal Switch"), "  I", " I ", "CAC", 'R', new ItemStack(Items.REDSTONE), 'A', "itemRubber", 'I', findItemStack("Copper Cable"), 'C', findItemStack("Signal Cable"));
		 *
		 * addRecipe(findItemStack("Signal Switch with LED"), " RI", " I ", "CAC", 'R', new ItemStack(Items.REDSTONE), 'A', "itemRubber", 'I', findItemStack("Copper Cable"), 'C', findItemStack("Signal Cable"));
		 */

        addRecipe(findItemStack("Low Voltage Switch"),
            "  I",
            " I ",
            "CAC",
            'R', new ItemStack(Items.REDSTONE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("Medium Voltage Switch"),
            "  I",
            "AIA",
            "CAC",
            'R', new ItemStack(Items.REDSTONE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Medium Voltage Cable"));

        addRecipe(findItemStack("High Voltage Switch"),
            "AAI",
            "AIA",
            "CAC",
            'R', new ItemStack(Items.REDSTONE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("High Voltage Cable"));

        addRecipe(findItemStack("Very High Voltage Switch"),
            "AAI",
            "AIA",
            "CAC",
            'R', new ItemStack(Items.REDSTONE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Very High Voltage Cable"));

    }

    public static void recipeElectricalRelay() {
        addRecipe(findItemStack("Low Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.REDSTONE),
            'O', new ItemStack(Items.IRON_INGOT),
            'G', new ItemStack(Blocks.GLASS_PANE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("Medium Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.REDSTONE),
            'O', new ItemStack(Items.IRON_INGOT),
            'G', new ItemStack(Blocks.GLASS_PANE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Medium Voltage Cable"));

        addRecipe(findItemStack("High Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.REDSTONE),
            'O', new ItemStack(Items.IRON_INGOT),
            'G', new ItemStack(Blocks.GLASS_PANE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("High Voltage Cable"));

        addRecipe(findItemStack("Very High Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.REDSTONE),
            'O', new ItemStack(Items.IRON_INGOT),
            'G', new ItemStack(Blocks.GLASS_PANE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Very High Voltage Cable"));

        addRecipe(findItemStack("Signal Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.REDSTONE),
            'O', new ItemStack(Items.IRON_INGOT),
            'G', new ItemStack(Blocks.GLASS_PANE),
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Signal Cable"));
    }

    public static void recipeWirelessSignal() {
        addRecipe(findItemStack("Wireless Signal Transmitter"),
            " S ",
            " R ",
            "ICI",
            'R', new ItemStack(Items.REDSTONE),
            'I', new ItemStack(Items.IRON_INGOT),
            'C', dictCheapChip,
            'S', findItemStack("Signal Antenna"));

        addRecipe(findItemStack("Wireless Signal Repeater"),
            "S S",
            "R R",
            "ICI",
            'R', new ItemStack(Items.REDSTONE),
            'I', new ItemStack(Items.IRON_INGOT),
            'C', dictCheapChip,
            'S', findItemStack("Signal Antenna"));

        addRecipe(findItemStack("Wireless Signal Receiver"),
            " S ",
            "ICI",
            'R', new ItemStack(Items.REDSTONE),
            'I', new ItemStack(Items.IRON_INGOT),
            'C', dictCheapChip,
            'S', findItemStack("Signal Antenna"));
    }

    public static void recipeChips() {
        addRecipe(findItemStack("NOT Chip"),
            "   ",
            "cCr",
            "   ",
            'C', dictCheapChip,
            'r', new ItemStack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("AND Chip"),
            " c ",
            "cCc",
            " c ",
            'C', dictCheapChip,
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("NAND Chip"),
            " c ",
            "cCr",
            " c ",
            'C', dictCheapChip,
            'r', new ItemStack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("OR Chip"),
            " r ",
            "rCr",
            " r ",
            'C', dictCheapChip,
            'r', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("NOR Chip"),
            " r ",
            "rCc",
            " r ",
            'C', dictCheapChip,
            'r', new ItemStack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("XOR Chip"),
            " rr",
            "rCr",
            " rr",
            'C', dictCheapChip,
            'r', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("XNOR Chip"),
            " rr",
            "rCc",
            " rr",
            'C', dictCheapChip,
            'r', new ItemStack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("PAL Chip"),
            "rcr",
            "cCc",
            "rcr",
            'C', dictAdvancedChip,
            'r', new ItemStack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Schmitt Trigger Chip"),
            "   ",
            "cCc",
            "   ",
            'C', dictAdvancedChip,
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("D Flip Flop Chip"),
            "   ",
            "cCc",
            " p ",
            'C', dictAdvancedChip,
            'p', findItemStack("Copper Plate"),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Oscillator Chip"),
            "pdp",
            "cCc",
            "   ",
            'C', dictAdvancedChip,
            'p', findItemStack("Copper Plate"),
            'c', findItemStack("Copper Cable"),
            'd', findItemStack("Dielectric"));

        addRecipe(findItemStack("JK Flip Flop Chip"),
            " p ",
            "cCc",
            " p ",
            'C', dictAdvancedChip,
            'p', findItemStack("Copper Plate"),
            'c', findItemStack("Copper Cable"));


        addRecipe(findItemStack("Amplifier"),
            "  r",
            "cCc",
            "   ",
            'r', new ItemStack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("OpAmp"),
            "  r",
            "cCc",
            " c ",
            'r', new ItemStack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("Configurable summing unit"),
            " cr",
            "cCc",
            " c ",
            'r', new ItemStack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("Sample and hold"),
            " rr",
            "cCc",
            " c ",
            'r', new ItemStack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("Voltage controlled sine oscillator"),
            "rrr",
            "cCc",
            "   ",
            'r', new ItemStack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("Voltage controlled sawtooth oscillator"),
            "   ",
            "cCc",
            "rrr",
            'r', new ItemStack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("PID Regulator"),
            "rrr",
            "cCc",
            "rcr",
            'r', new ItemStack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("Lowpass filter"),
            "CdC",
            "cDc",
            " s ",
            'd', findItemStack("Dielectric"),
            'c', findItemStack("Copper Cable"),
            'C', findItemStack("Copper Plate"),
            'D', findItemStack("Coal Dust"),
            's', dictCheapChip);
    }

    public static void recipeTransformer() {
        addRecipe(findItemStack("DC-DC Converter"),
            "C C",
            "III",
            'C', findItemStack("Copper Cable"),
            'I', new ItemStack(Items.IRON_INGOT));
    }

    public static void recipeHeatFurnace() {
        addRecipe(findItemStack("Stone Heat Furnace"),
            "BBB",
            "BIB",
            "BiB",
            'B', new ItemStack(Blocks.STONE),
            'i', findItemStack("Copper Thermal Cable"),
            'I', findItemStack("Combustion Chamber"));

        addRecipe(findItemStack("Fuel Heat Furnace"),
            "IcI",
            "mCI",
            "IiI",
            'c', findItemStack("Cheap Chip"),
            'm', findItemStack("Electrical Motor"),
            'C', new ItemStack(Items.CAULDRON),
            'I', new ItemStack(Items.IRON_INGOT),
            'i', findItemStack("Copper Thermal Cable"));
    }

    public static void recipeTurbine() {
        addRecipe(findItemStack("50V Turbine"),
            " m ",
            "HMH",
            " E ",
            'M', findItemStack("Machine Block"),
            'E', findItemStack("Low Voltage Cable"),
            'H', findItemStack("Copper Thermal Cable"),
            'm', findItemStack("Electrical Motor")

        );
        addRecipe(findItemStack("200V Turbine"),
            "ImI",
            "HMH",
            "IEI",
            'I', "itemRubber",
            'M', findItemStack("Advanced Machine Block"),
            'E', findItemStack("Medium Voltage Cable"),
            'H', findItemStack("Copper Thermal Cable"),
            'm', findItemStack("Advanced Electrical Motor"));
        addRecipe(findItemStack("Generator"),
            "mmm",
            "ama",
            " ME",
            'm', findItemStack("Advanced Electrical Motor"),
            'M', findItemStack("Advanced Machine Block"),
            'a', firstExistingOre("ingotAluminum", "ingotIron"),
            'E', findItemStack("High Voltage Cable")
        );
        addRecipe(findItemStack("Steam Turbine"),
            " a ",
            "aAa",
            " M ",
            'a', firstExistingOre("ingotAluminum", "ingotIron"),
            'A', firstExistingOre("blockAluminum", "blockIron"),
            'M', findItemStack("Advanced Machine Block")
        );
        addRecipe(findItemStack("Gas Turbine"),
            "msH",
            "sSs",
            " M ",
            'm', findItemStack("Advanced Electrical Motor"),
            'H', findItemStack("Copper Thermal Cable"),
            's', firstExistingOre("ingotSteel", "ingotIron"),
            'S', firstExistingOre("blockSteel", "blockIron"),
            'M', findItemStack("Advanced Machine Block")
        );

        addRecipe(findItemStack("Joint"),
            "   ",
            "iii",
            " m ",
            'i', "ingotIron",
            'm', findItemStack("Machine Block")
        );

        addRecipe(findItemStack("Joint hub"),
            " i ",
            "iii",
            " m ",
            'i', "ingotIron",
            'm', findItemStack("Machine Block")
        );

        addRecipe(findItemStack("Flywheel"),
            "iIi",
            "ImI",
            "iIi",
            'i', "ingotIron",
            'I', "blockIron",
            'm', findItemStack("Machine Block")
        );

        addRecipe(findItemStack("Tachometer"),
            "p  ",
            "iii",
            "cm ",
            'i', "ingotIron",
            'm', findItemStack("Machine Block"),
            'p', findItemStack("Electrical Probe Chip"),
            'c', findItemStack("Signal Cable")
        );
    }

    public static void recipeBattery() {
        addRecipe(findItemStack("Cost Oriented Battery"),
            "C C",
            "PPP",
            "PPP",
            'C', findItemStack("Low Voltage Cable"),
            'P', "ingotLead",
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Capacity Oriented Battery"),
            "PPP",
            "PBP",
            "PPP",
            'B', findItemStack("Cost Oriented Battery"),
            'P', "ingotLead");

        addRecipe(findItemStack("Voltage Oriented Battery"),
            "PPP",
            "PBP",
            "PPP",
            'B', findItemStack("Cost Oriented Battery"),
            'P', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Current Oriented Battery"),
            "PPP",
            "PBP",
            "PPP",
            'B', findItemStack("Cost Oriented Battery"),
            'P', "ingotCopper");

        addRecipe(findItemStack("Life Oriented Battery"),
            "P P",
            " B ",
            "P P",
            'B', findItemStack("Cost Oriented Battery"),
            'P', new ItemStack(Items.GOLD_INGOT));

        addRecipe(findItemStack("Single-use Battery"),
            "ppp",
            "III",
            "ppp",
            'C', findItemStack("Low Voltage Cable"),
            'p', new ItemStack(Items.COAL, 1, 0),
            'I', "ingotCopper");

        addRecipe(findItemStack("Single-use Battery"),
            "ppp",
            "III",
            "ppp",
            'C', findItemStack("Low Voltage Cable"),
            'p', new ItemStack(Items.COAL, 1, 1),
            'I', "ingotCopper");
    }

    public static void recipeGridDevices(HashSet<String> oreNames) {
        int poleRecipes = 0;
        for (String oreName : new String[]{
            "ingotAluminum",
            "ingotAluminium",
            "ingotSteel",
        }) {
            if (oreNames.contains(oreName)) {
                addRecipe(findItemStack("Utility Pole"),
                    "WWW",
                    "IWI",
                    " W ",
                    'W', "logWood",
                    'I', oreName
                );
                poleRecipes++;
            }
        }
        if (poleRecipes == 0) {
            // Really?
            addRecipe(findItemStack("Utility Pole"),
                "WWW",
                "IWI",
                " W ",
                'I', "ingotIron"
            );
        }
        addRecipe(findItemStack("Utility Pole w/DC-DC Converter"),
            "HHH",
            " TC",
            " PH",
            'P', findItemStack("Utility Pole"),
            'H', findItemStack("High Voltage Cable"),
            'C', findItemStack("Optimal Ferromagnetic Core"),
            'T', findItemStack("DC-DC Converter")
        );
//		if (oreNames.contains("sheetPlastic")) {
//			addRecipe(findItemStack("Downlink"),
//					"H H",
//					"PMP",
//					"PPP",
//					'P', "sheetPlastic",
//					'M', findItemStack("Machine Block"),
//					'H', findItemStack("High Voltage Cable")
//			);
//		} else {
//			addRecipe(findItemStack("Downlink"),
//					"H H",
//					"PMP",
//					"PPP",
//					'P', "itemRubber",
//					'M', findItemStack("Machine Block"),
//					'H', findItemStack("High Voltage Cable")
//			);
//		}
    }


    public static void recipeElectricalFurnace() {
        addRecipe(findItemStack("Electrical Furnace"),
            "III",
            "IFI",
            "ICI",
            'C', findItemStack("Low Voltage Cable"),
            'F', new ItemStack(Blocks.FURNACE),
            'I', new ItemStack(Items.IRON_INGOT));
    }

    public static void recipeSixNodeMisc() {
        addRecipe(findItemStack("Analog Watch"),
            "crc",
            "III",
            'c', findItemStack("Iron Cable"),
            'r', new ItemStack(Items.REDSTONE),
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Digital Watch"),
            "rcr",
            "III",
            'c', findItemStack("Iron Cable"),
            'r', new ItemStack(Items.REDSTONE),
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Hub"),
            "I I",
            " c ",
            "I I",
            'c', findItemStack("Copper Cable"),
            'I', new ItemStack(Items.IRON_INGOT));


        addRecipe(findItemStack("Energy Meter"),
            "IcI",
            "IRI",
            "IcI",
            'c', findItemStack("Copper Cable"),
            'R', dictCheapChip,
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Advanced Energy Meter"),
            " c ",
            "PRP",
            " c ",
            'c', findItemStack("Copper Cable"),
            'R', dictAdvancedChip,
            'P', findItemStack("Iron Plate"));

    }

    public static void recipeAutoMiner() {
        addRecipe(findItemStack("Auto Miner"),
            "MCM",
            "BOB",
            " P ",
            'C', dictAdvancedChip,
            'O', findItemStack("Ore Scanner"),
            'B', findItemStack("Advanced Machine Block"),
            'M', findItemStack("Advanced Electrical Motor"),
            'P', findItemStack("Mining Pipe"));
    }

    public static void recipeWindTurbine() {
        addRecipe(findItemStack("Wind Turbine"),
            " I ",
            "IMI",
            " B ",
            'B', findItemStack("Machine Block"),
            'I', "plateIron",
            'M', findItemStack("Electrical Motor"));

        addRecipe(findItemStack("Water Turbine"),
            "  I",
            "BMI",
            "  I",
            'I', "plateIron",
            'B', findItemStack("Machine Block"),
            'M', findItemStack("Electrical Motor"));

    }

    public static void recipeFuelGenerator() {
        addRecipe(findItemStack("50V Fuel Generator"),
            "III",
            " BA",
            "CMC",
            'I', "plateIron",
            'B', findItemStack("Machine Block"),
            'A', findItemStack("Analogic Regulator"),
            'C', findItemStack("Low Voltage Cable"),
            'M', findItemStack("Electrical Motor"));

        addRecipe(findItemStack("200V Fuel Generator"),
            "III",
            " BA",
            "CMC",
            'I', "plateIron",
            'B', findItemStack("Advanced Machine Block"),
            'A', findItemStack("Analogic Regulator"),
            'C', findItemStack("Medium Voltage Cable"),
            'M', findItemStack("Advanced Electrical Motor"));
    }

    public static void recipeSolarPanel() {
        addRecipe(findItemStack("Small Solar Panel"),
            "III",
            "CSC",
            "III",
            'S', "plateSilicon",
            'I', new ItemStack(Items.IRON_INGOT),
            'C', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("Small Rotating Solar Panel"),
            "ISI",
            "I I",
            'S', findItemStack("Small Solar Panel"),
            'M', findItemStack("Electrical Motor"),
            'I', new ItemStack(Items.IRON_INGOT));

        for (String metal : new String[] { "blockSteel", "blockAluminum", "blockAluminium", "casingMachineAdvanced" }) {
            for (String panel : new String[] {"Small Solar Panel", "Small Rotating Solar Panel"}) {
                addRecipe(findItemStack("2x3 Solar Panel"),
                    "PPP",
                    "PPP",
                    "I I",
                    'P', findItemStack(panel),
                    'I', metal);
            }
        }
        addRecipe(findItemStack("2x3 Rotating Solar Panel"),
            "ISI",
            "IMI",
            "I I",
            'S', findItemStack("2x3 Solar Panel"),
            'M', findItemStack("Electrical Motor"),
            'I', new ItemStack(Items.IRON_INGOT));
    }

    public static void recipeThermalDissipatorPassiveAndActive() {
        addRecipe(
            findItemStack("Small Passive Thermal Dissipator"),
            "I I",
            "III",
            "CIC",
            'I', "ingotCopper",
            'C', findItemStack("Copper Thermal Cable"));

	/*	addRecipe(
				findItemStack("Small Active Thermal Dissipator"),
				"RMR",
				"I I",
				"III",
				'I', "ingotCopper",
				'M', findItemStack("Electrical Motor"),
				'R', "itemRubber",
				'C', findItemStack("Copper Thermal Cable"));*/

        addRecipe(
            findItemStack("Small Active Thermal Dissipator"),
            "RMR",
            " D ",
            'D', findItemStack("Small Passive Thermal Dissipator"),
            'M', findItemStack("Electrical Motor"),
            'R', "itemRubber");

	/*	addRecipe(
				findItemStack("200V Active Thermal Dissipator"),
				"RMR",
				"I I",
				"III",
				'I', "ingotCopper",
				'M', findItemStack("Advanced Electrical Motor"),
				'R', "itemRubber",
				'C', findItemStack("Copper Thermal Cable"));*/

        addRecipe(
            findItemStack("200V Active Thermal Dissipator"),
            "RMR",
            " D ",
            'D', findItemStack("Small Passive Thermal Dissipator"),
            'M', findItemStack("Advanced Electrical Motor"),
            'R', "itemRubber");

    }

    public static void recipeGeneral() {
        addSmelting(treeResin.parentItem,
            treeResin.parentItemDamage, findItemStack("Rubber", 1), 0f);

    }

    public static void recipeHeatingCorp() {
        addRecipe(findItemStack("Small 50V Copper Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', findItemStack("Copper Cable"));

        addRecipe(findItemStack("50V Copper Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', "ingotCopper");

        addRecipe(findItemStack("Small 200V Copper Heating Corp"),
            "CC",
            'C', findItemStack("50V Copper Heating Corp"));

        addRecipe(findItemStack("200V Copper Heating Corp"),
            "CC",
            'C', findItemStack("Small 200V Copper Heating Corp"));

        addRecipe(findItemStack("Small 50V Iron Heating Corp"),
            "C C",
            "CCC",
            "C C", 'C', findItemStack("Iron Cable"));

        addRecipe(findItemStack("50V Iron Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Small 200V Iron Heating Corp"),
            "CC",
            'C', findItemStack("50V Iron Heating Corp"));

        addRecipe(findItemStack("200V Iron Heating Corp"),
            "CC",
            'C', findItemStack("Small 200V Iron Heating Corp"));

        addRecipe(findItemStack("Small 50V Tungsten Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', findItemStack("Tungsten Cable"));

        addRecipe(findItemStack("50V Tungsten Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', findItemStack("Tungsten Ingot"));

        addRecipe(findItemStack("Small 200V Tungsten Heating Corp"),
            "CC",
            'C', findItemStack("50V Tungsten Heating Corp"));
        addRecipe(findItemStack("200V Tungsten Heating Corp"),
            "CC",
            'C', findItemStack("Small 200V Tungsten Heating Corp"));
    }

    public static void recipeRegulatorItem() {
        addRecipe(findItemStack("On/OFF Regulator 10 Percent", 1),
            "R R",
            " R ",
            " I ",
            'R', new ItemStack(Items.REDSTONE),
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("On/OFF Regulator 1 Percent", 1),
            "RRR",
            " I ",
            'R', new ItemStack(Items.REDSTONE),
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Analogic Regulator", 1),
            "R R",
            " C ",
            " I ",
            'R', new ItemStack(Items.REDSTONE),
            'I', new ItemStack(Items.IRON_INGOT),
            'C', dictCheapChip);
    }

    public static void recipeLampItem() {
        // Tungsten
        addRecipe(
            findItemStack("Small 50V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("50V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'S', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("200V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'S', findItemStack("Medium Voltage Cable"));

        // CARBON
        addRecipe(findItemStack("Small 50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'F', new ItemStack(Items.COAL),
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Small 50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'F', new ItemStack(Items.COAL, 1, 1),
            'S', findItemStack("Copper Cable"));

        addRecipe(
            findItemStack("50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'F', new ItemStack(Items.COAL),
            'S', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'F', new ItemStack(Items.COAL, 1, 1),
            'S', findItemStack("Low Voltage Cable"));

        addRecipe(
            findItemStack("Small 50V Economic Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'F', new ItemStack(Items.GLOWSTONE_DUST),
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("50V Economic Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'F', new ItemStack(Items.GLOWSTONE_DUST),
            'S', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("200V Economic Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'F', new ItemStack(Items.GLOWSTONE_DUST),
            'S', findItemStack("Medium Voltage Cable"));

        addRecipe(findItemStack("50V Farming Lamp", 2),
            "GGG",
            "FFF",
            "GSG",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'S', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("200V Farming Lamp", 2),
            "GGG",
            "FFF",
            "GSG",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'S', findItemStack("Medium Voltage Cable"));

        addRecipe(findItemStack("50V LED Bulb", 2),
            "GGG",
            "SSS",
            " C ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'S', findItemStack("Silicon Ingot"),
            'C', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("200V LED Bulb", 2),
            "GGG",
            "SSS",
            " C ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'S', findItemStack("Silicon Ingot"),
            'C', findItemStack("Medium Voltage Cable"));

    }

    public static void recipeProtection() {
        addRecipe(findItemStack("Overvoltage Protection", 4),
            "SCD",
            'S', findItemStack("Electrical Probe Chip"),
            'C', dictCheapChip,
            'D', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("Overheating Protection", 4),
            "SCD",
            'S', findItemStack("Thermal Probe Chip"),
            'C', dictCheapChip,
            'D', new ItemStack(Items.REDSTONE));

    }

    public static void recipeCombustionChamber() {
        addRecipe(findItemStack("Combustion Chamber"),
            " L ",
            "L L",
            " L ",
            'L', new ItemStack(Blocks.STONE));
        addRecipe(findItemStack("Thermal Insulation", 4),
            "WSW",
            "SWS",
            "WSW",
            'S', new ItemStack(Blocks.STONE),
            'W', new ItemStack(Blocks.WOOL));
    }

    public static void recipeFerromagneticCore() {
        addRecipe(findItemStack("Cheap Ferromagnetic Core"),
            "LLL",
            "L  ",
            "LLL",
            'L', Items.IRON_INGOT);

        addRecipe(findItemStack("Average Ferromagnetic Core"),
            "PCP",
            'C', findItemStack("Cheap Ferromagnetic Core"),
            'P', "plateIron");

        addRecipe(findItemStack("Optimal Ferromagnetic Core"),
            "P",
            "C",
            "P",
            'C', findItemStack("Average Ferromagnetic Core"),
            'P', "plateIron");
    }

    public static void recipeIngot() {
        // Done
    }

    public static void recipeDust() {
        addShapelessRecipe(findItemStack("Alloy Dust", 2),
            "dustIron",
            "dustIron",
            "dustCoal",
            dictTungstenDust);

    }

    public static void recipeElectricalMotor() {
        addRecipe(findItemStack("Electrical Motor"),
            " C ",
            "III",
            "C C",
            'I', new ItemStack(Items.IRON_INGOT),
            'C', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("Advanced Electrical Motor"),
            "RCR",
            "MIM",
            "CRC",
            'M', findItemStack("Advanced Magnet"),
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE),
            'C', findItemStack("Medium Voltage Cable"));

        // TODO

    }

    public static void recipeSolarTracker() {
        addRecipe(findItemStack("Solar Tracker", 4),
            "VVV",
            "RQR",
            "III",
            'Q', new ItemStack(Items.QUARTZ),
            'V', new ItemStack(Blocks.GLASS_PANE),
            'R', new ItemStack(Items.REDSTONE),
            'G', new ItemStack(Items.GOLD_INGOT),
            'I', new ItemStack(Items.IRON_INGOT));

    }

    public static void recipeDynamo() {

    }

    public static void recipeWindRotor() {

    }

    public static void recipeMeter() {
        addRecipe(findItemStack("MultiMeter"),
            "RGR",
            "RER",
            "RCR",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'C', findItemStack("Electrical Probe Chip"),
            'E', new ItemStack(Items.REDSTONE),
            'R', "itemRubber");

        addRecipe(findItemStack("Thermometer"),
            "RGR",
            "RER",
            "RCR",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'C', findItemStack("Thermal Probe Chip"),
            'E', new ItemStack(Items.REDSTONE),
            'R', "itemRubber");

        addShapelessRecipe(findItemStack("AllMeter"),
            findItemStack("MultiMeter"),
            findItemStack("Thermometer"));

        addRecipe(findItemStack("Wireless Analyser"),
            " S ",
            "RGR",
            "RER",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'S', findItemStack("Signal Antenna"),
            'E', new ItemStack(Items.REDSTONE),
            'R', "itemRubber");

    }

    public static void recipeElectricalDrill() {
        addRecipe(findItemStack("Cheap Electrical Drill"),
            "CMC",
            " T ",
            " P ",
            'T', findItemStack("Mining Pipe"),
            'C', dictCheapChip,
            'M', findItemStack("Electrical Motor"),
            'P', new ItemStack(Items.IRON_PICKAXE));

        addRecipe(findItemStack("Average Electrical Drill"),
            "RCR",
            " D ",
            " d ", 'R', Items.REDSTONE,
            'C', dictCheapChip,
            'D', findItemStack("Cheap Electrical Drill"),
            'd', new ItemStack(Items.DIAMOND));

        addRecipe(findItemStack("Fast Electrical Drill"),
            "MCM",
            " T ",
            " P ",
            'T', findItemStack("Mining Pipe"),
            'C', dictAdvancedChip,
            'M', findItemStack("Advanced Electrical Motor"),
            'P', new ItemStack(Items.DIAMOND_PICKAXE));

    }

    public static void recipeOreScanner() {
        addRecipe(findItemStack("Ore Scanner"),
            "IGI",
            "RCR",
            "IGI",
            'C', dictCheapChip,
            'R', new ItemStack(Items.REDSTONE),
            'I', new ItemStack(Items.IRON_INGOT),
            'G', new ItemStack(Items.GOLD_INGOT));

    }

    public static void recipeMiningPipe() {
        addRecipe(findItemStack("Mining Pipe", 4),
            "A",
            "A",
            "A",
            'A', "ingotAlloy");
    }

    public static void recipeTreeResinAndRubber() {
        addRecipe(findItemStack("Tree Resin Collector"),
            "W W",
            "WW ", 'W', "plankWood");

        addRecipe(findItemStack("Tree Resin Collector"),
            "W W",
            " WW", 'W', "plankWood");

    }

    public static void recipeRawCable() {
        addRecipe(findItemStack("Copper Cable", 6),
            "III",
            'I', "ingotCopper");

        addRecipe(findItemStack("Iron Cable", 6),
            "III",
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Tungsten Cable", 6),
            "III",
            'I', dictTungstenIngot);

    }

    public static void recipeBatteryItem() {
        addRecipe(findItemStack("Portable Battery"),
            " I ",
            "IPI",
            "IPI",
            'P', "ingotLead",
            'I', new ItemStack(Items.IRON_INGOT));
        addShapelessRecipe(
            findItemStack("Portable Battery Pack"),
            findItemStack("Portable Battery"), findItemStack("Portable Battery"), findItemStack("Portable Battery"));
    }

    public static void recipeElectricalTool() {
        addRecipe(findItemStack("Small Flashlight"),
            "GLG",
            "IBI",
            " I ",
            'L', findItemStack("50V Incandescent Light Bulb"),
            'B', findItemStack("Portable Battery"),
            'G', new ItemStack(Blocks.GLASS_PANE),
            'I', new ItemStack(Items.IRON_INGOT));
        addRecipe(findItemStack("Improved Flashlight"),
            "GLG",
            "IBI",
            " I ",
            'L', findItemStack("50V LED Bulb"),
            'B', findItemStack("Portable Battery Pack"),
            'G', new ItemStack(Blocks.GLASS_PANE),
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Portable Electrical Mining Drill"),
            " T ",
            "IBI",
            " I ",
            'T', findItemStack("Average Electrical Drill"),
            'B', findItemStack("Portable Battery"),
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Portable Electrical Axe"),
            " T ",
            "IMI",
            "IBI",
            'T', new ItemStack(Items.IRON_AXE),
            'B', findItemStack("Portable Battery"),
            'M', findItemStack("Electrical Motor"),
            'I', new ItemStack(Items.IRON_INGOT));

        if (xRayScannerCanBeCrafted) {
            addRecipe(findItemStack("X-Ray Scanner"),
                "PGP",
                "PCP",
                "PBP",
                'C', dictAdvancedChip,
                'B', findItemStack("Portable Battery"),
                'P', new ItemStack(Items.IRON_INGOT),
                'G', findItemStack("Ore Scanner"));
        }

    }

    public static void recipeECoal() {
        addRecipe(findItemStack("E-Coal Helmet"),
            "PPP",
            "PCP",
            'P', "plateCoal",
            'C', dictAdvancedChip);
        addRecipe(findItemStack("E-Coal Boots"),
            " C ",
            "P P",
            "P P",
            'P', "plateCoal",
            'C', dictAdvancedChip);

        addRecipe(findItemStack("E-Coal Chestplate"),
            "P P",
            "PCP",
            "PPP",
            'P', "plateCoal",
            'C', dictAdvancedChip);

        addRecipe(findItemStack("E-Coal Leggings"),
            "PPP",
            "PCP",
            "P P",
            'P', "plateCoal",
            'C', dictAdvancedChip);

    }

    public static void recipePortableCapacitor() {
        addRecipe(findItemStack("Portable Condensator"),
            "RcR",
            "wCw",
            "RcR",
            'C', new ItemStack(Items.REDSTONE),
            'R', "itemRubber",
            'w', findItemStack("Copper Cable"),
            'c', "plateCopper");

        addShapelessRecipe(findItemStack("Portable Condensator Pack"),
            findItemStack("Portable Condensator"),
            findItemStack("Portable Condensator"),
            findItemStack("Portable Condensator"));
    }

    public static void recipeMiscItem() {
        addRecipe(findItemStack("Cheap Chip"),
            " R ",
            "RSR",
            " R ",
            'S', "ingotSilicon",
            'R', new ItemStack(Items.REDSTONE));
        addRecipe(findItemStack("Advanced Chip"),
            "LRL",
            "RCR",
            "LRL",
            'C', dictCheapChip,
            'L', "ingotSilicon",
            'R', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("Machine Block"),
            "LLL",
            "LcL",
            "LLL",
            'L', new ItemStack(Items.IRON_INGOT),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Advanced Machine Block"),
            " C ",
            "CcC",
            " C ",
            'C', "plateAlloy",
            'L', "ingotAlloy",
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Electrical Probe Chip"),
            " R ",
            "RCR",
            " R ",
            'C', findItemStack("High Voltage Cable"),
            'R', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("Thermal Probe Chip"),
            " C ",
            "RIR",
            " C ",
            'G', new ItemStack(Items.GOLD_INGOT),
            'I', new ItemStack(Items.IRON_INGOT),
            'C', "ingotCopper",
            'R', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("Signal Antenna"),
            "c",
            "c",
            'c', findItemStack("Iron Cable"));

        addRecipe(findItemStack("Machine Booster"),
            "m",
            "c",
            "m",
            'm', findItemStack("Electrical Motor"),
            'c', dictAdvancedChip);

        addRecipe(findItemStack("Wrench"),
            " c ",
            "cc ",
            "  c",
            'c', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Player Filter"),
            " g",
            "gc",
            " g",
            'g', new ItemStack(Blocks.GLASS_PANE),
            'c', new ItemStack(Items.DYE, 1, 2));

        addRecipe(findItemStack("Monster Filter"),
            " g",
            "gc",
            " g",
            'g', new ItemStack(Blocks.GLASS_PANE),
            'c', new ItemStack(Items.DYE, 1, 1));

        addRecipe(findItemStack("Casing", 8),
            "ppp",
            "p p",
            "ppp",
            'p', findItemStack("Iron Plate"));

    }

    public static void recipeMacerator() {
        float f = 4000;
	maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.COAL_ORE, 1),
	    new ItemStack(Items.COAL, 3, 0), 1.0 * f));
        maceratorRecipes.addRecipe(new Recipe(findItemStack("Copper Ore"),
            new ItemStack[]{findItemStack("Copper Dust", 2)}, 1.0 * f));
        maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.IRON_ORE),
            new ItemStack[]{findItemStack("Iron Dust", 2)}, 1.5 * f));
        maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.GOLD_ORE),
            new ItemStack[]{findItemStack("Gold Dust", 2)}, 3.0 * f));
        maceratorRecipes.addRecipe(new Recipe(findItemStack("Lead Ore"),
            new ItemStack[]{findItemStack("Lead Dust", 2)}, 2.0 * f));
        maceratorRecipes.addRecipe(new Recipe(findItemStack("Tungsten Ore"),
            new ItemStack[]{findItemStack("Tungsten Dust", 2)}, 2.0 * f));
        maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.COAL, 1, 0),
            new ItemStack[]{findItemStack("Coal Dust", 2)}, 1.0 * f));
        maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.COAL, 1, 1),
            new ItemStack[]{findItemStack("Coal Dust", 2)}, 1.0 * f));
        maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.SAND, 1),
            new ItemStack[]{findItemStack("Silicon Dust", 1)}, 3.0 * f));
        maceratorRecipes.addRecipe(new Recipe(findItemStack("Cinnabar Ore"),
            new ItemStack[]{findItemStack("Cinnabar Dust", 2)}, 2.0 * f));

        maceratorRecipes.addRecipe(new Recipe(findItemStack("Copper Ingot"),
            new ItemStack[]{findItemStack("Copper Dust", 1)}, 0.5 * f));
        maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.IRON_INGOT),
            new ItemStack[]{findItemStack("Iron Dust", 1)}, 0.5 * f));
        maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.GOLD_INGOT),
            new ItemStack[]{findItemStack("Gold Dust", 1)}, 0.5 * f));
        maceratorRecipes.addRecipe(new Recipe(findItemStack("Lead Ingot"),
            new ItemStack[]{findItemStack("Lead Dust", 1)}, 0.5 * f));
        maceratorRecipes.addRecipe(new Recipe(findItemStack("Tungsten Ingot"),
            new ItemStack[]{findItemStack("Tungsten Dust", 1)}, 0.5 * f));

        maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.COBBLESTONE),
            new ItemStack[]{new ItemStack(Blocks.GRAVEL)}, 1.0 * f));
        maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.GRAVEL),
            new ItemStack[]{new ItemStack(Items.FLINT)}, 1.0 * f));

        maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.DIRT),
            new ItemStack[]{new ItemStack(Blocks.SAND)}, 1.0 * f));
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

    public static void recipeMaceratorModOre(float f, String inputName, String outputName, int outputCount) {
        if (!OreDictionary.doesOreNameExist(inputName)) {
            LogWrapper.info("No entries for oredict: " + inputName);
            return;
        }
        if (!OreDictionary.doesOreNameExist(outputName)) {
            LogWrapper.info("No entries for oredict: " + outputName);
            return;
        }
        List<ItemStack> inOres = OreDictionary.getOres(inputName);
        List<ItemStack> outOres = OreDictionary.getOres(outputName);
        if (inOres.size() == 0) {
            LogWrapper.info("No ores in oredict entry: " + inputName);
        }
        if (outOres.size() == 0) {
            LogWrapper.info("No ores in oredict entry: " + outputName);
            return;
        }
        ItemStack output = outOres.get(0).copy();
        output.setCount(outputCount);
        LogWrapper.info("Adding mod recipe from " + inputName + " to " + outputName);
        for (ItemStack input : inOres) {
            maceratorRecipes.addRecipe(new Recipe(input, output, f));
        }
    }

    public static void recipePlateMachine() {
        float f = 10000;
        plateMachineRecipes.addRecipe(new Recipe(
            findItemStack("Copper Ingot", plateConversionRatio),
            findItemStack("Copper Plate"), 1.0 * f));

        plateMachineRecipes.addRecipe(new Recipe(findItemStack("Lead Ingot", plateConversionRatio),
            findItemStack("Lead Plate"), 1.0 * f));

        plateMachineRecipes.addRecipe(new Recipe(
            findItemStack("Silicon Ingot", 4),
            findItemStack("Silicon Plate"), 1.0 * f));

        plateMachineRecipes.addRecipe(new Recipe(findItemStack("Alloy Ingot", plateConversionRatio),
            findItemStack("Alloy Plate"), 1.0 * f));

        plateMachineRecipes.addRecipe(new Recipe(new ItemStack(Items.IRON_INGOT, plateConversionRatio,
            0), findItemStack("Iron Plate"), 1.0 * f));

        plateMachineRecipes.addRecipe(new Recipe(new ItemStack(Items.GOLD_INGOT, plateConversionRatio,
            0), findItemStack("Gold Plate"), 1.0 * f));

    }

    public static void recipeCompressor() {
        compressorRecipes.addRecipe(new Recipe(findItemStack("Coal Plate", 4),
            new ItemStack[]{new ItemStack(Items.DIAMOND)}, 80000.0));
        // extractorRecipes.addRecipe(new
        // Recipe("dustCinnabar",new
        // ItemStack[]{findItemStack("Purified Cinnabar Dust",1)}, 1000.0));

        compressorRecipes.addRecipe(new Recipe(findItemStack("Coal Dust", 4),
            findItemStack("Coal Plate"), 4000.0));

        compressorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.SAND),
            findItemStack("Dielectric"), 2000.0));

        compressorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.LOG),
            findItemStack("Tree Resin"), 3000.0));

    }

    public static void recipeMagnetizer() {
        magnetiserRecipes.addRecipe(new Recipe(new ItemStack(Items.IRON_INGOT, 2),
            new ItemStack[]{findItemStack("Basic Magnet")}, 5000.0));
        magnetiserRecipes.addRecipe(new Recipe(findItemStack("Alloy Ingot", 2),
            new ItemStack[]{findItemStack("Advanced Magnet")}, 15000.0));
    }

    public static void recipeFuelBurnerItem() {
        addRecipe(findItemStack("Small Fuel Burner"),
            "   ",
            " Cc",
            "   ",
            'C', findItemStack("Combustion Chamber"),
            'c', findItemStack("Copper Thermal Cable"));

        addRecipe(findItemStack("Medium Fuel Burner"),
            "   ",
            " Cc",
            " C ",
            'C', findItemStack("Combustion Chamber"),
            'c', findItemStack("Copper Thermal Cable"));

        addRecipe(findItemStack("Big Fuel Burner"),
            "   ",
            "CCc",
            "CC ",
            'C', findItemStack("Combustion Chamber"),
            'c', findItemStack("Copper Thermal Cable"));
    }

    public static void recipeFurnace() {
        ItemStack in;

        in = findItemStack("Copper Ore");
        addSmelting(in.getItem(), in.getMetadata(),
            findItemStack("Copper Ingot"));
        in = findItemStack("dustCopper");
        addSmelting(in.getItem(), in.getMetadata(),
            findItemStack("Copper Ingot"));
        in = findItemStack("Lead Ore");
        addSmelting(in.getItem(), in.getMetadata(),
            findItemStack("ingotLead"));
        in = findItemStack("dustLead");
        addSmelting(in.getItem(), in.getMetadata(),
            findItemStack("ingotLead"));
        in = findItemStack("Tungsten Ore");
        addSmelting(in.getItem(), in.getMetadata(),
            findItemStack("Tungsten Ingot"));
        in = findItemStack("Tungsten Dust");
        addSmelting(in.getItem(), in.getMetadata(),
            findItemStack("Tungsten Ingot"));
        in = findItemStack("ingotAlloy");
        // addSmelting(in.getItem().itemID, in.getMetadata(),
        // findItemStack("Ferrite Ingot"));
        in = findItemStack("dustIron");
        addSmelting(in.getItem(), in.getMetadata(),
            new ItemStack(Items.IRON_INGOT));

        in = findItemStack("dustGold");
        addSmelting(in.getItem(), in.getMetadata(),
            new ItemStack(Items.GOLD_INGOT));

        in = findItemStack("Tree Resin");
        addSmelting(in.getItem(), in.getMetadata(),
            findItemStack("Rubber", 2));

        in = findItemStack("Alloy Dust");
        addSmelting(in.getItem(), in.getMetadata(),
            findItemStack("Alloy Ingot"));

        in = findItemStack("Silicon Dust");
        addSmelting(in.getItem(), in.getMetadata(),
            findItemStack("Silicon Ingot"));

        // in = findItemStack("Purified Cinnabar Dust");
        in = findItemStack("dustCinnabar");
        addSmelting(in.getItem(), in.getMetadata(),
            findItemStack("Mercury"));

    }

    public static void recipeElectricalSensor() {
        addRecipe(findItemStack("Voltage Probe", 1),
            "SC",
            'S', findItemStack("Electrical Probe Chip"),
            'C', findItemStack("Signal Cable"));

        addRecipe(findItemStack("Electrical Probe", 1),
            "SCS",
            'S', findItemStack("Electrical Probe Chip"),
            'C', findItemStack("Signal Cable"));

    }

    public static void recipeThermalSensor() {
        addRecipe(findItemStack("Thermal Probe", 1),
            "SCS",
            'S', findItemStack("Thermal Probe Chip"),
            'C', findItemStack("Signal Cable"));

        addRecipe(findItemStack("Temperature Probe", 1),
            "SC",
            'S', findItemStack("Thermal Probe Chip"),
            'C', findItemStack("Signal Cable"));

    }

    public static void recipeTransporter() {
        addRecipe(findItemStack("Experimental Transporter", 1),
            "RMR",
            "RMR",
            " R ",
            'M', findItemStack("Advanced Machine Block"),
            'C', findItemStack("High Voltage Cable"),
            'R', dictAdvancedChip);
    }


    public static void recipeTurret() {
        addRecipe(findItemStack("800V Defence Turret", 1),
            " R ",
            "CMC",
            " c ",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', highVoltageCableDescriptor.newItemStack(),
            'R', new ItemStack(Blocks.REDSTONE_BLOCK));

    }

    public static void recipeMachine() {
        addRecipe(findItemStack("50V Macerator", 1),
            "IRI",
            "FMF",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'F', new ItemStack(Items.FLINT),
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE));
        addRecipe(findItemStack("200V Macerator", 1),
            "ICI",
            "DMD",
            "IcI",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'D', new ItemStack(Items.DIAMOND),
            'I', "ingotAlloy");

        addRecipe(findItemStack("50V Compressor", 1),
            "IRI",
            "FMF",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'F', "plateIron",
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE));
        addRecipe(findItemStack("200V Compressor", 1),
            "ICI",
            "DMD",
            "IcI",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'D', "plateAlloy",
            'I', "ingotAlloy");

        addRecipe(findItemStack("50V Plate Machine", 1),
            "IRI",
            "IMI",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("200V Plate Machine", 1),
            "DCD",
            "DMD",
            "DcD",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'D', "plateAlloy",
            'I', "ingotAlloy");

        addRecipe(findItemStack("50V Magnetizer", 1),
            "IRI",
            "cMc",
            "III",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("200V Magnetizer", 1),
            "ICI",
            "cMc",
            "III",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'I', "ingotAlloy");

    }

    public static void recipeElectricalGate() {
        addShapelessRecipe(findItemStack("Electrical Timer"),
            new ItemStack(Items.REPEATER),
            dictCheapChip);

        addRecipe(findItemStack("Signal Processor", 1),
            "IcI",
            "cCc",
            "IcI",
            'I', new ItemStack(Items.IRON_INGOT),
            'c', findItemStack("Signal Cable"),
            'C', dictCheapChip);
    }

    public static void recipeElectricalRedstone() {
        addRecipe(findItemStack("Redstone-to-Voltage Converter", 1),
            "TCS",
            'S', findItemStack("Signal Cable"),
            'C', dictCheapChip,
            'T', new ItemStack(Blocks.REDSTONE_TORCH));

        addRecipe(findItemStack("Voltage-to-Redstone Converter", 1),
            "CTR",
            'R', new ItemStack(Items.REDSTONE),
            'C', dictCheapChip,
            'T', new ItemStack(Blocks.REDSTONE_TORCH));

    }

    public static void recipeElectricalEnvironmentalSensor() {
        addShapelessRecipe(findItemStack("Electrical Daylight Sensor"),
            new ItemStack(Blocks.DAYLIGHT_DETECTOR),
            findItemStack("Redstone-to-Voltage Converter"));

        addShapelessRecipe(findItemStack("Electrical Light Sensor"),
            new ItemStack(Blocks.DAYLIGHT_DETECTOR),
            new ItemStack(Items.QUARTZ),
            findItemStack("Redstone-to-Voltage Converter"));

        addRecipe(findItemStack("Electrical Weather Sensor"),
            " r ",
            "rRr",
            " r ",
            'R', new ItemStack(Items.REDSTONE),
            'r', "itemRubber");

        addRecipe(findItemStack("Electrical Anemometer Sensor"),
            " I ",
            " R ",
            "I I",
            'R', new ItemStack(Items.REDSTONE),
            'I', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Electrical Entity Sensor"),
            " G ",
            "GRG",
            " G ",
            'G', new ItemStack(Blocks.GLASS_PANE),
            'R', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("Electrical Fire Detector"),
            "cbr",
            "p p",
            "r r",
            'c', findItemStack("Signal Cable"),
            'b', dictCheapChip,
            'r', "itemRubber",
            'p', "plateCopper");

        addRecipe(findItemStack("Electrical Fire Buzzer"),
            "rar",
            "p p",
            "r r",
            'a', dictAdvancedChip,
            'r', "itemRubber",
            'p', "plateCopper");

        addShapelessRecipe(findItemStack("Scanner"),
            new ItemStack(Items.COMPARATOR),
            dictAdvancedChip);

    }

    public static void recipeElectricalVuMeter() {
        for (int idx = 0; idx < 4; idx++) {
            addRecipe(findItemStack("Analog vuMeter", 1),
                "WWW",
                "RIr",
                "WSW",
                'W', new ItemStack(Blocks.PLANKS, 1, idx),
                'R', new ItemStack(Items.REDSTONE),
                'I', new ItemStack(Items.IRON_INGOT),
                'r', new ItemStack(Items.DYE, 1, 1),
                'S', findItemStack("Signal Cable"));
        }
        for (int idx = 0; idx < 4; idx++) {
            addRecipe(findItemStack("LED vuMeter", 1),
                " W ",
                "WTW",
                " S ",
                'W', new ItemStack(Blocks.PLANKS, 1, idx),
                'T', new ItemStack(Blocks.REDSTONE_TORCH),
                'S', findItemStack("Signal Cable"));
        }
    }

    public static void recipeElectricalBreaker() {

        addRecipe(findItemStack("Electrical Breaker", 1),
            "crC",
            'c', findItemStack("Overvoltage Protection"),
            'C', findItemStack("Overheating Protection"),
            'r', findItemStack("High Voltage Relay"));

    }

    public static void recipeFuses() {

        addRecipe(findItemStack("Electrical Fuse Holder", 1),
            "i",
            " ",
            "i",
            'i', new ItemStack(Items.IRON_INGOT));

        addRecipe(findItemStack("Lead Fuse for low voltage cables", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Low Voltage Cable"));

        addRecipe(findItemStack("Lead Fuse for medium voltage cables", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Medium Voltage Cable"));

        addRecipe(findItemStack("Lead Fuse for high voltage cables", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("High Voltage Cable"));

        addRecipe(findItemStack("Lead Fuse for very high voltage cables", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Very High Voltage Cable"));

    }

    public static void recipeElectricalGateSource() {
        addRecipe(findItemStack("Signal Trimmer", 1),
            "RsR",
            "rRr",
            " c ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'r', "itemRubber",
            's', new ItemStack(Items.STICK),
            'R', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("Signal Switch", 3),
            " r ",
            "rRr",
            " c ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'r', "itemRubber",
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("Signal Button", 3),
            " R ",
            "rRr",
            " c ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'r', "itemRubber",
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("Wireless Switch", 3),
            " a ",
            "rCr",
            " r ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'C', dictCheapChip,
            'a', findItemStack("Signal Antenna"),
            'r', "itemRubber",
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("Wireless Button", 3),
            " a ",
            "rCr",
            " R ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'C', dictCheapChip,
            'a', findItemStack("Signal Antenna"),
            'r', "itemRubber",
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE));

        // Wireless Switch
        // Wireless Button
    }

    public static void recipeElectricalDataLogger() {
        addRecipe(findItemStack("Data Logger", 1),
            "RRR",
            "RGR",
            "RCR",
            'R', "itemRubber",
            'C', dictCheapChip,
            'G', new ItemStack(Blocks.GLASS_PANE));

        addRecipe(findItemStack("Modern Data Logger", 1),
            "RRR",
            "RGR",
            "RCR",
            'R', "itemRubber",
            'C', dictAdvancedChip,
            'G', new ItemStack(Blocks.GLASS_PANE));

        addRecipe(findItemStack("Industrial Data Logger", 1),
            "RRR",
            "GGG",
            "RCR",
            'R', "itemRubber",
            'C', dictAdvancedChip,
            'G', new ItemStack(Blocks.GLASS_PANE));
    }

    public static void recipeSixNodeCache() {

    }

    public static void recipeElectricalAlarm() {
        addRecipe(findItemStack("Nuclear Alarm", 1),
            "ITI",
            "IMI",
            "IcI",
            'c', findItemStack("Signal Cable"),
            'T', new ItemStack(Blocks.REDSTONE_TORCH),
            'I', new ItemStack(Items.IRON_INGOT),
            'M', new ItemStack(Blocks.NOTEBLOCK));
        addRecipe(findItemStack("Standard Alarm", 1),
            "MTM",
            "IcI",
            "III",
            'c', findItemStack("Signal Cable"),
            'T', new ItemStack(Blocks.REDSTONE_TORCH),
            'I', new ItemStack(Items.IRON_INGOT),
            'M', new ItemStack(Blocks.NOTEBLOCK));

    }

    public static void recipeElectricalAntenna() {
        addRecipe(findItemStack("Low Power Transmitter Antenna", 1),
            "R i",
            "CI ",
            "R i",
            'C', dictCheapChip,
            'i', new ItemStack(Items.IRON_INGOT),
            'I', "plateIron",
            'R', new ItemStack(Items.REDSTONE));
        addRecipe(findItemStack("Low Power Receiver Antenna", 1),
            "i  ",
            " IC",
            "i  ",
            'C', dictCheapChip,
            'I', "plateIron",
            'i', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE));
        addRecipe(findItemStack("Medium Power Transmitter Antenna", 1),
            "c I",
            "CI ",
            "c I",
            'C', dictAdvancedChip,
            'c', dictCheapChip,
            'I', "plateIron",
            'R', new ItemStack(Items.REDSTONE));
        addRecipe(findItemStack("Medium Power Receiver Antenna", 1),
            "I  ",
            " IC",
            "I  ",
            'C', dictAdvancedChip,
            'I', "plateIron",
            'R', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("High Power Transmitter Antenna", 1),
            "C I",
            "CI ",
            "C I",
            'C', dictAdvancedChip,
            'c', dictCheapChip,
            'I', "plateIron",
            'R', new ItemStack(Items.REDSTONE));
        addRecipe(findItemStack("High Power Receiver Antenna", 1),
            "I D",
            " IC",
            "I D",
            'C', dictAdvancedChip,
            'I', "plateIron",
            'R', new ItemStack(Items.REDSTONE),
            'D', new ItemStack(Items.DIAMOND));

    }

    public static void recipeBatteryCharger() {
        addRecipe(findItemStack("Weak 50V Battery Charger", 1),
            "RIR",
            "III",
            "RcR",
            'c', findItemStack("Low Voltage Cable"),
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE));
        addRecipe(findItemStack("50V Battery Charger", 1),
            "RIR",
            "ICI",
            "RcR",
            'C', dictCheapChip,
            'c', findItemStack("Low Voltage Cable"),
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE));

        addRecipe(findItemStack("200V Battery Charger", 1),
            "RIR",
            "ICI",
            "RcR",
            'C', dictAdvancedChip,
            'c', findItemStack("Medium Voltage Cable"),
            'I', new ItemStack(Items.IRON_INGOT),
            'R', new ItemStack(Items.REDSTONE));

    }

    public static void recipeEggIncubator() {
        addRecipe(findItemStack("50V Egg Incubator", 1),
            "IGG",
            "E G",
            "CII",
            'C', dictCheapChip,
            'E', findItemStack("Small 50V Tungsten Heating Corp"),
            'I', new ItemStack(Items.IRON_INGOT),
            'G', new ItemStack(Blocks.GLASS_PANE));

    }

    public static void recipeEnergyConverter() {
        if (ElnToOtherEnergyConverterEnable) {
            addRecipe(new ItemStack(elnToOtherBlockLvu),
                "III",
                "cCR",
                "III",
                'C', dictCheapChip,
                'c', findItemStack("Low Voltage Cable"),
                'I', new ItemStack(Items.IRON_INGOT),
                'R', "ingotCopper");

            addRecipe(new ItemStack(elnToOtherBlockMvu),
                "III",
                "cCR",
                "III",
                'C', dictCheapChip,
                'c', findItemStack("Medium Voltage Cable"),
                'I', new ItemStack(Items.IRON_INGOT),
                'R', dictTungstenIngot);

            addRecipe(new ItemStack(elnToOtherBlockHvu),
                "III",
                "cCR",
                "III",
                'C', dictAdvancedChip,
                'c', findItemStack("High Voltage Cable"),
                'I', new ItemStack(Items.IRON_INGOT),
                'R', new ItemStack(Items.GOLD_INGOT));

        }
    }

    // recipeComputerProbe(): OpenComputers/ComputerCraft dropped (rule 8)


    public static void recipeArmor() {
        addRecipe(new ItemStack(helmetCopper),
            "CCC",
            "C C",
            'C', "ingotCopper");

        addRecipe(new ItemStack(plateCopper),
            "C C",
            "CCC",
            "CCC",
            'C', "ingotCopper");

        addRecipe(new ItemStack(legsCopper),
            "CCC",
            "C C",
            "C C",
            'C', "ingotCopper");

        addRecipe(new ItemStack(bootsCopper),
            "C C",
            "C C",
            'C', "ingotCopper");
    }

    public static void recipeTool() {
        addRecipe(new ItemStack(shovelCopper),
            "i",
            "s",
            "s",
            'i', "ingotCopper",
            's', new ItemStack(Items.STICK));
        addRecipe(new ItemStack(axeCopper),
            "ii",
            "is",
            " s",
            'i', "ingotCopper",
            's', new ItemStack(Items.STICK));
        addRecipe(new ItemStack(hoeCopper),
            "ii",
            " s",
            " s",
            'i', "ingotCopper",
            's', new ItemStack(Items.STICK));
        addRecipe(new ItemStack(pickaxeCopper),
            "iii",
            " s ",
            " s ",
            'i', "ingotCopper",
            's', new ItemStack(Items.STICK));
        addRecipe(new ItemStack(swordCopper),
            "i",
            "i",
            "s",
            'i', "ingotCopper",
            's', new ItemStack(Items.STICK));

    }
}
