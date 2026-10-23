package mods.eln.datagen;

import mods.eln.Eln;
import mods.eln.generic.IDescriptorItem;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.Map;

/**
 * en_us: every EA item and block. Descriptor items take their descriptor's name (the 1.12 TR_NAME key, which is the
 * English name); the plain items (E-Coal armour, spawn egg, tab icon) take their 1.12 names.
 */
public class ElnLang extends LanguageProvider {
    /** 1.12 TR_NAME of the items that have no descriptor. */
    static final Map<String, String> PLAIN = Map.ofEntries(
        Map.entry("e_coal_helmet", "E-Coal Helmet"), Map.entry("e_coal_chestplate", "E-Coal Chestplate"),
        Map.entry("e_coal_leggings", "E-Coal Leggings"), Map.entry("e_coal_boots", "E-Coal Boots"),
        Map.entry("replicator_spawn_egg", "Replicator Spawn Egg"),
        Map.entry("pipe_kit_iron", "Iron Pipe Kit"), Map.entry("pipe_kit_alloy", "Alloy Pipe Kit"), Map.entry("pipe_valve", "Pipe Valve"), Map.entry("pipe_adapter", "Pipe Adapter"), Map.entry("pipe_safety_valve", "Pipe Safety Valve"),
        Map.entry("cable_kit_signal", "Signal Cable Kit"), Map.entry("cable_kit_lv", "Low Voltage Cable Kit"), Map.entry("cable_kit_mv", "Medium Voltage Cable Kit"),
        Map.entry("cable_kit_hv", "High Voltage Cable Kit"), Map.entry("cable_kit_vhv", "Very High Voltage Cable Kit"), Map.entry("cable_kit_thermal", "Copper Thermal Cable Kit"), Map.entry("chisel", "Chisel"), Map.entry("raw_lead", "Raw Lead"), Map.entry("raw_tungsten", "Raw Tungsten"), Map.entry("charcoal_nugget", "Charcoal Nugget"),
        Map.entry("item_creative_tab", "Electrical Age"));

    public ElnLang(PackOutput output) {
        super(output, Eln.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("key.eln.wrench", "Wrench (hold: a switch's screen on click; the chisel's brush menu)");
        add("item.eln.chisel.shape.cube", "cube");
        add("item.eln.chisel.shape.flat", "flat");
        add("item.eln.chisel.shape.tunnel", "tunnel");
        add("gui.eln.chisel.brush", "Chisel brush");
        // JEI (compat/jei/ResinCategory)
        add("jei.eln.tree_resin_collector", "Tree Resin Collector");
        add("jei.eln.machine.macerator", "Macerator");
        add("jei.eln.machine.compressor", "Compressor");
        add("jei.eln.machine.plate_machine", "Plate Machine");
        add("jei.eln.machine.magnetiser", "Magnetizer");
        add("jei.eln.machine.separator", "Separator");
        add("jei.eln.machine.extra", "(sometimes more)");
        add("jei.eln.tree_resin_collector.how", "On the side of a log under leaves. Right-click it to empty it.");
        // the guide advancements' hints (Baughn's words, 2026-10-20; data/eln/advancement/guide, tools/advancements/make_guide.py)
        add("advancements.eln.guide.root.title", "Electrical Age");
        add("advancements.eln.guide.root.description", "Every tree is a rubber tree.");
        add("advancements.eln.guide.copper_cable.title", "Live Wire");
        add("advancements.eln.guide.copper_cable.description", "It all starts with three copper in a row.");
        add("advancements.eln.guide.heat_furnace.title", "Fire Starter");
        add("advancements.eln.guide.heat_furnace.description", "Ouch - this one is hot.");
        add("advancements.eln.guide.boiler.title", "Boiling Point");
        add("advancements.eln.guide.boiler.description", "Pipe in your water.");
        add("advancements.eln.guide.safety_valve.title", "Safety First");
        add("advancements.eln.guide.safety_valve.description", "A safety valve beats a burst.");
        add("advancements.eln.guide.steam_turbine.title", "Full Steam Ahead");
        add("advancements.eln.guide.steam_turbine.description", "Set it under the boiler. Or don't.");
        add("advancements.eln.guide.big_iron.title", "Big Iron");
        add("advancements.eln.guide.big_iron.description", "Mind the rads.");
        add("advancements.eln.guide.machine_block.title", "Machine Shop");
        add("advancements.eln.guide.machine_block.description", "Most machines start as a block.");
        add("advancements.eln.guide.macerator.title", "Grinding Gears");
        add("advancements.eln.guide.macerator.description", "Better than cheap magic. (Worse than expensive magic.)");
        add("advancements.eln.guide.electrical_furnace.title", "Electric Oven");
        add("advancements.eln.guide.electrical_furnace.description", "An oven for your kitchen. Put your roast next to your ore!");
        add("advancements.eln.guide.auto_miner.title", "Hands Off");
        add("advancements.eln.guide.auto_miner.description", "Let machines do the mining, while you do the cooking.");
        add("advancements.eln.guide.cable_kit.title", "Bundled");
        add("advancements.eln.guide.cable_kit.description", "Sensible electricians bundle their cables.");
        add("advancements.eln.guide.lead.title", "Heavy Metal");
        add("advancements.eln.guide.lead.description", "Lead hides down below.");
        add("advancements.eln.guide.battery.title", "Bottled Lightning");
        add("advancements.eln.guide.battery.description", "Don't stress them.");
        add("advancements.eln.guide.solar.title", "Sunbather");
        add("advancements.eln.guide.solar.description", "Free power, then no power.");
        add("advancements.eln.guide.chisel.title", "Chip Off the Old Block");
        add("advancements.eln.guide.chisel.description", "It's not a wrench. Press C anyway.");
        add("advancements.eln.guide.multimeter.title", "Measure Twice");
        add("advancements.eln.guide.multimeter.description", "Can you imagine needing one of these?");
        add("advancements.eln.guide.converter.title", "Step Up");
        add("advancements.eln.guide.converter.description", "Place the cables, then connect the power.");
        add("advancements.eln.guide.fuse.title", "Fuse Box");
        add("advancements.eln.guide.fuse.description", "A fuse dies so your machines don't.");
        add("advancements.eln.guide.lamp.title", "Light Bulb Moment");
        add("advancements.eln.guide.lamp.description", "Bulbs wear out.");
        add("key.category.eln.keys", "Electrical Age");
        for (var h : ElnRegistry.ALL_ITEMS) {
            Item item = h.get();
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            String name;
            name = PLAIN.get(path);
            if (name == null && item instanceof IDescriptorItem d && d.getEaDescriptor() != null) name = descriptorName(d.getEaDescriptor());
            if (name == null) throw new IllegalStateException("ElnLang: no English name for eln:" + path);
            add(item, name);
            if (item instanceof BlockItem b && BuiltInRegistries.BLOCK.getKey(b.getBlock()).getNamespace().equals(Eln.MODID))
                add(b.getBlock(), name);
        }
        add(ElnRegistry.REPLICATOR.get(), "Replicator");
        add(ElnRegistry.SIX_NODE_BLOCK.get(), "Electrical Age Device");
        add(ElnRegistry.VOXEL_NODE_BLOCK.get(), "Pipes and Cables");
        add(ElnRegistry.VOXEL_NODE_WOODEN_FENCE.get(), "Pipes and Cables");
        add(ElnRegistry.VOXEL_NODE_FENCE.get(), "Pipes and Cables");
        add(ElnRegistry.VOXEL_NODE_WALL.get(), "Pipes and Cables");
        add(ElnRegistry.GHOST_BLOCK.get(), "Electrical Age Multiblock Part");
        add(ElnRegistry.LIGHT_BLOCK.get(), "Electrical Age Light");
        for (var b : ElnRegistry.TRANSPARENT_BLOCKS) {
            var d = b.get().descriptor;
            add(b.get(), d.name);
        }
        add("itemGroup.eln", "Electrical Age");
        add("death.attack.eln.scald", "%1$s was scalded by steam");
        add("death.attack.eln.scald.player", "%1$s was scalded by steam while fighting %2$s");
        add("block.eln.crude_oil", "Crude Oil");
        add("block.eln.reservoir_oil", "Crude Oil");
        add("item.eln.crude_oil_bucket", "Crude Oil Bucket");
        add("block.eln.tar_sand", "Tar Sand");
        add("block.eln.well_casing", "Well Casing");
        add("fluid.eln.steam", "Steam");
        add("fluid_type.eln.steam", "Steam");
        for (var c : mods.eln.oil.Oil.Cut.values()) {
            add("fluid.eln." + c.id, c.display);
            add("fluid_type.eln." + c.id, c.display);
            if (c.bucket) add("item.eln." + c.id + "_bucket", c.display + " Bucket");
        }
    }

    static String descriptorName(Object d) {
        if (d instanceof mods.eln.generic.GenericItemUsingDamageDescriptor g) return g.name;
        if (d instanceof mods.eln.generic.GenericItemBlockUsingDamageDescriptor g) return g.name;
        if (d instanceof mods.eln.misc.DescriptorBase b) return b.descriptorKey;
        return null;
    }
}
