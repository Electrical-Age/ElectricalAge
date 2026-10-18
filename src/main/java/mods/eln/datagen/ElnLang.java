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
        Map.entry("pipe_kit_iron", "Iron Pipe Kit"), Map.entry("pipe_kit_alloy", "Alloy Pipe Kit"), Map.entry("pipe_valve", "Pipe Valve"), Map.entry("pipe_adapter", "Pipe Adapter"),
        Map.entry("item_creative_tab", "Electrical Age"),
        // 1.12 en_us.lang tile.eln.EnergyConverterElnToOther*Block.name
        Map.entry("energy_converter_eln_to_other_lvublock", "50V Electrical Age to other energy exporter"),
        Map.entry("energy_converter_eln_to_other_mvublock", "200V Electrical Age to other energy exporter"),
        Map.entry("energy_converter_eln_to_other_hvublock", "800V Electrical Age to other energy exporter"));

    public ElnLang(PackOutput output) {
        super(output, Eln.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("key.eln.wrench", "Wrench (hold to open a switch's screen)");
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
        add(ElnRegistry.VOXEL_NODE_BLOCK.get(), "Electrical Age Pipe");
        add(ElnRegistry.GHOST_BLOCK.get(), "Electrical Age Multiblock Part");
        add(ElnRegistry.LIGHT_BLOCK.get(), "Electrical Age Light");
        for (var b : ElnRegistry.TRANSPARENT_BLOCKS) {
            var d = b.get().descriptor;
            add(b.get(), d.name);
        }
        add("itemGroup.eln", "Electrical Age");
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
