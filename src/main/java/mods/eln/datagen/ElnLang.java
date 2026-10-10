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
 * English name); the plain items (copper tools and armour, E-Coal armour, spawn egg) take their 1.12 names.
 */
public class ElnLang extends LanguageProvider {
    /** 1.12 TR_NAME of the items that have no descriptor. */
    static final Map<String, String> PLAIN = Map.ofEntries(
        Map.entry("copper_sword", "Copper Sword"), Map.entry("copper_hoe", "Copper Hoe"),
        Map.entry("copper_shovel", "Copper Shovel"), Map.entry("copper_pickaxe", "Copper Pickaxe"),
        Map.entry("copper_axe", "Copper Axe"),
        Map.entry("copper_helmet", "Copper Helmet"), Map.entry("copper_chestplate", "Copper Chestplate"),
        Map.entry("copper_leggings", "Copper Leggings"), Map.entry("copper_boots", "Copper Boots"),
        Map.entry("e_coal_helmet", "E-Coal Helmet"), Map.entry("e_coal_chestplate", "E-Coal Chestplate"),
        Map.entry("e_coal_leggings", "E-Coal Leggings"), Map.entry("e_coal_boots", "E-Coal Boots"),
        Map.entry("replicator_spawn_egg", "Replicator Spawn Egg"),
        // 1.12 en_us.lang tile.eln.EnergyConverterElnToOther*Block.name
        Map.entry("energy_converter_eln_to_other_lvublock", "50V Electrical Age to other energy exporter"),
        Map.entry("energy_converter_eln_to_other_mvublock", "200V Electrical Age to other energy exporter"),
        Map.entry("energy_converter_eln_to_other_hvublock", "800V Electrical Age to other energy exporter"));

    public ElnLang(PackOutput output) {
        super(output, Eln.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
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
        add(ElnRegistry.GHOST_BLOCK.get(), "Electrical Age Multiblock Part");
        add(ElnRegistry.LIGHT_BLOCK.get(), "Electrical Age Light");
        for (var b : ElnRegistry.TRANSPARENT_BLOCKS) {
            var d = b.get().descriptor;
            add(b.get(), d.name);
        }
        add("itemGroup.eln", "Electrical Age");
    }

    static String descriptorName(Object d) {
        if (d instanceof mods.eln.generic.GenericItemUsingDamageDescriptor g) return g.name;
        if (d instanceof mods.eln.generic.GenericItemBlockUsingDamageDescriptor g) return g.name;
        if (d instanceof mods.eln.misc.DescriptorBase b) return b.descriptorKey;
        return null;
    }
}
