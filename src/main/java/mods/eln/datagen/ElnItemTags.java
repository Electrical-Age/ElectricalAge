package mods.eln.datagen;

import mods.eln.Eln;
import mods.eln.misc.OreDictTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Item tags: EA's items in the tags of their 1.12 ore dictionary names (OreDictTags.EA_ENTRIES, recorded by the
 * addToOre calls of Wp12a/Wp12c), and each c:group/x tag in its c:group parent (convention tags).
 */
public class ElnItemTags extends ItemTagsProvider {
    public ElnItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, Eln.MODID);
    }

    static Item itemOf(Object descriptor) {
        if (descriptor instanceof mods.eln.generic.GenericItemUsingDamageDescriptor d) return d.parentItem;
        if (descriptor instanceof mods.eln.generic.GenericItemBlockUsingDamageDescriptor d) return d.parentItem;
        throw new IllegalStateException("ElnItemTags: no item for " + descriptor);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        for (Map.Entry<String, List<Object>> e : OreDictTags.EA_ENTRIES.entrySet()) {
            TagKey<Item> tag = OreDictTags.tag(e.getKey());
            var appender = tag(tag);
            for (Object d : e.getValue()) {
                Item item = itemOf(d);
                appender.add(ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(item)));
            }
            Identifier id = tag.location();
            int slash = id.getPath().indexOf('/');
            if (id.getNamespace().equals("c") && slash > 0) {
                TagKey<Item> parent = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", id.getPath().substring(0, slash)));
                tag(parent).addTag(tag);
            }
        }
        // what EA's recipes take for a plate (OreDictTags.recipeTag): its own plates; a modpack may add others
        for (Map.Entry<String, List<Object>> e : OreDictTags.EA_ENTRIES.entrySet()) {
            String id = OreDictTags.tagId(e.getKey());
            if (!id.startsWith("c:plates/")) continue;
            TagKey<Item> hq = OreDictTags.highQualityPlates(id.substring("c:plates/".length()));
            for (Object d : e.getValue()) tag(hq).add(ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(itemOf(d))));
            tag(itemTag("eln", "high_quality_plates")).addTag(hq);
        }
        // tungsten ore is also a convention ore (Baughn 2026-10-24), so other mods (and the X-ray scanner) see it; its
        // dust and ingot stay EA-private, as 1.12's dictionary.tungsten=false
        tag(itemTag("c", "ores/tungsten")).add(key(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("eln", "tungsten_ore")).asItem()));
        tag(itemTag("c", "ores")).addTag(itemTag("c", "ores/tungsten"));
        // the X-ray scanner's filters (XRayFilter): coal is what coal ore drops, but has no convention tag of its own
        tag(itemTag("eln", "ore_drops/coal")).add(key(net.minecraft.world.item.Items.COAL));
    }

    static TagKey<Item> itemTag(String ns, String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ns, path));
    }

    static ResourceKey<Item> key(Item item) {
        return ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(item));
    }
}
