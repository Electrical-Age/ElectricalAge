package mods.eln.compat;

import mods.eln.Eln;
import mods.eln.misc.RecipesList;
import mods.eln.node.voxel.CableKitItem;
import mods.eln.node.voxel.CableSpec;
import mods.eln.node.voxel.WindingItem;
import mods.eln.transparentnode.wiredrawing.WireDrawingDescriptor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * What EA's JEI plugin shows beyond the game's own recipes, without JEI's classes: the plugin (compat.jei) draws it,
 * and the GameTest jei_every_item_reachable checks from it that every EA item can be made by clicking through JEI
 * down to vanilla (Baughn 2026-11-06).
 */
public final class JeiPages {
    private JeiPages() {
    }

    /** The machines' recipe lists, by key (one category each). */
    public static final Map<String, Supplier<RecipesList>> MACHINES = new LinkedHashMap<>();

    static {
        MACHINES.put("macerator", () -> Eln.maceratorRecipes);
        MACHINES.put("compressor", () -> Eln.compressorRecipes);
        MACHINES.put("plate_machine", () -> Eln.plateMachineRecipes);
        MACHINES.put("magnetiser", () -> Eln.magnetiserRecipes);
        MACHINES.put("separator", () -> Eln.separatorRecipes);
    }

    /** The Tree Resin Collector on a log: tree resin. */
    public record Resin(ItemStack collector, ItemStack resin) {
    }

    /** Annealing in the Electrical Furnace. */
    public record Anneal(ItemStack in, ItemStack out, double minC, double maxC, double kJ) {
    }

    /** A wire-drawing example: what it takes (whole items), what comes out, and which machine makes it. */
    public record Draw(ItemStack copper, ItemStack rubber, ItemStack out, double kJ, boolean small) {
    }

    /** An information page: the items it is on, and its text's lang key. */
    public record Info(List<ItemStack> items, String key) {
    }

    public static ItemStack collector() {
        return Eln.findItemStack("Tree Resin Collector", 1);
    }

    public static ItemStack furnace() {
        return Eln.findItemStack("Electrical Furnace", 1);
    }

    /** The wire-drawing machines, small first. */
    public static List<ItemStack> drawers() {
        List<ItemStack> l = new ArrayList<>();
        for (String n : new String[]{"48V Wire Drawing Machine", "230V Wire Drawing Machine"}) {
            ItemStack m = Eln.findItemStack(n, 1);
            if (!m.isEmpty()) l.add(m);
        }
        return l;
    }

    public static List<Resin> resins() {
        ItemStack c = collector(), r = Eln.findItemStack("Tree Resin", 1);
        return c.isEmpty() || r.isEmpty() ? List.of() : List.of(new Resin(c, r));
    }

    /** The Electrical Furnace's heat treatments (annealing, vulcanising). */
    public static List<Anneal> anneals() {
        if (furnace().isEmpty()) return List.of();
        List<Anneal> l = new ArrayList<>();
        for (var h : mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess.TREATMENTS) {
            ItemStack in = h.in().get(), out = h.out().get();
            if (!in.isEmpty() && !out.isEmpty()) l.add(new Anneal(in, out, h.minC(), h.maxC(), h.energy() / 1000));
        }
        return l;
    }

    /** An example of blocks of wire of a spec: a kit (turns 0) or a winding of turns. */
    static Draw draw(int tier, CableSpec spec, int blocks, int turns) {
        double len = turns > 0 ? turns * WindingItem.BLOCKS_PER_TURN : blocks;
        int cu = (int) Math.ceil(len * WireDrawingDescriptor.copperPerBlock(spec.mm2()) - 1e-9);
        int ru = (int) Math.ceil(len * WireDrawingDescriptor.rubberPerBlock(tier, spec.mm2()) - 1e-9);
        ItemStack out = turns > 0 ? WindingItem.make(tier, spec, turns) : CableKitItem.kit(tier, spec, blocks * 16);
        return new Draw(new ItemStack(Items.COPPER_INGOT, Math.max(1, cu)), new ItemStack(Eln.itemsByName.get("Rubber"), Math.max(1, ru)), out,
            len * WireDrawingDescriptor.energyPerBlock(spec.mm2()) / 1000, tier <= 2 && spec.mm2() <= 6);
    }

    /** Each machine's presets, a 48 -> 230 V converter's winding, an 11 kV one, bare wire. */
    public static List<Draw> draws() {
        if (drawers().isEmpty()) return List.of();
        List<Draw> l = new ArrayList<>();
        for (int tier = 1; tier <= 4; tier++) l.add(draw(tier, CableSpec.preset(tier), 16, 0));
        l.add(draw(1, new CableSpec(6, CableSpec.BROWN), 16, 0));
        l.add(draw(2, CableSpec.preset(2), 0, 58));    // a 48 -> 230 V converter's secondary on a cheap core
        l.add(draw(4, CableSpec.preset(4), 0, 688));   // 400 V -> 11 kV on an optimal core
        // bare wire: 2 ingots, 3 Copper Cable (no rubber)
        l.add(new Draw(new ItemStack(Items.COPPER_INGOT, 2), ItemStack.EMPTY, Eln.findItemStack("Copper Cable", 3),
            3 * WireDrawingDescriptor.JOULES_PER_BLOCK / 1000, true));
        return l;
    }

    static ItemStack eln(String path) {
        Item i = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("eln", path));
        return i == null || i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i);
    }

    static void info(List<Info> l, String key, String... paths) {
        List<ItemStack> items = new ArrayList<>();
        for (String p : paths) {
            ItemStack s = eln(p);
            if (!s.isEmpty()) items.add(s);
        }
        if (!items.isEmpty()) l.add(new Info(items, "jei.eln.info." + key));
    }

    /**
     * What no recipe makes: got from the world (ores, oil), from a device at work (fuel cuts, a blown fuse, a print),
     * or only in creative.
     */
    public static List<Info> infos() {
        List<Info> l = new ArrayList<>();
        info(l, "lead_ore", "lead_ore", "deepslate_lead_ore");
        info(l, "tungsten_ore", "tungsten_ore");
        info(l, "tar_sand", "tar_sand");
        info(l, "sulfur_dust", "sulfur_dust");
        info(l, "soul_dust", "soul_dust");
        info(l, "rubber", "rubber", "raw_rubber", "rubber_compound");
        info(l, "crude_oil", "crude_oil_bucket");
        info(l, "fraction", "gasoline_bucket", "diesel_bucket", "heavyoil_bucket", "residue_bucket");
        info(l, "blown_fuse", "blown_lead_fuse");
        info(l, "data_logger_print", "data_logger_print");
        info(l, "creative", "electrical_source", "signal_source", "tutorial_sign");
        return l;
    }

    /**
     * Kept out of JEI's list: the pipe sensor devices' own items (the pipe sensor items place them, so each name shows
     * once); the fuses for the other tiers (one Lead Fuse is rated in its holder, Baughn 2026-11-06); the 50 V and 200 V
     * turbines (replaced by the steam turbines, Baughn 2026-10-14); and items nothing makes or uses: Ferrite Ingot (no
     * recipe in 1.12 either), and cinnabar ore, its dust and mercury (cinnabar doesn't generate, as in 1.12).
     */
    public static Set<Item> hidden() {
        Set<Item> h = new LinkedHashSet<>();
        for (int s = 0; s < 3; s++) {
            var d = mods.eln.node.voxel.PipeSensorItem.device(s);
            if (d != null && d.parentItem != null) h.add(d.parentItem);
        }
        for (String p : new String[]{"lead_fuse_for_medium_voltage_cables", "lead_fuse_for_high_voltage_cables", "lead_fuse_for_very_high_voltage_cables",
            "50v_turbine", "200v_turbine", "ferrite_ingot", "cinnabar_ore", "cinnabar_dust", "mercury"}) {
            ItemStack f = eln(p);
            if (!f.isEmpty()) h.add(f.getItem());
        }
        return h;
    }
}
