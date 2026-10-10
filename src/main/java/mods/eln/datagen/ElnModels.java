package mods.eln.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.IDescriptorItem;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Client item definitions, item models, block states and block models (P3 R1). Items get the flat icon 1.12 showed
 * (the descriptor's 1.7.10 icon name, in textures/items or textures/blocks; both directories are stitched into the item
 * and block atlases by assets/minecraft/atlases/*.json, which extend the vanilla atlases). Node, ghost and light blocks are drawn by block-entity renderers or not at all:
 * a particle-only model. Ores are cubes; energy converters are the 1.12 cube (front fixed for now).
 */
public class ElnModels implements DataProvider {
    static final Set<String> HANDHELD = Set.of("copper_sword", "copper_hoe", "copper_shovel", "copper_pickaxe", "copper_axe");

    final PackOutput output;
    final List<String> missing = new ArrayList<>();

    public ElnModels(PackOutput output) {
        this.output = output;
    }

    static boolean textureExists(String tex) {
        return Eln.class.getClassLoader().getResource("assets/eln/textures/" + tex + ".png") != null;
    }

    /** "items/x" or "blocks/x" for an icon name (with or without "eln:"), items first; null if neither exists. */
    static String iconTexture(String icon, boolean blockFirst) {
        if (icon == null) return null;
        String n = icon.startsWith("eln:") ? icon.substring(4) : icon;
        String[] order = blockFirst ? new String[]{"blocks/", "items/"} : new String[]{"items/", "blocks/"};
        for (String dir : order) if (textureExists(dir + n)) return dir + n;
        return null;
    }

    String itemTexture(String name, Item item) {
        if (item instanceof IDescriptorItem di) {
            Object d = di.getEaDescriptor();
            if (d instanceof GenericItemUsingDamageDescriptor g) return iconTexture(g.getIconName(), false);
            if (d instanceof GenericItemBlockUsingDamageDescriptor b) return iconTexture(b.getIconName(), true);
        }
        String t = iconTexture(name, false);
        if (t == null) t = iconTexture(name.replace("e_coal_", "ecoal_"), false);
        return t;
    }

    static JsonObject obj(Object... kv) {
        JsonObject o = new JsonObject();
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof String s) o.addProperty((String) kv[i], s);
            else o.add((String) kv[i], (com.google.gson.JsonElement) v);
        }
        return o;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(Eln.MODID);
        List<CompletableFuture<?>> out = new ArrayList<>();
        // blocks
        out.add(DataProvider.saveStable(cache, obj("textures", obj("particle", "minecraft:block/iron_block")),
            assets.resolve("models/block/particle_iron.json")));
        out.add(DataProvider.saveStable(cache, obj("textures", obj("particle", "minecraft:block/glass")),
            assets.resolve("models/block/particle_none.json")));
        // 1.12 energy_converter_front2: EA side north, IC2 side south
        out.add(DataProvider.saveStable(cache, obj("parent", "minecraft:block/cube", "textures", obj(
                "down", "eln:blocks/elntoic2lvu_side", "up", "eln:blocks/elntoic2lvu_side",
                "north", "eln:blocks/elntoic2lvu_eln2", "south", "eln:blocks/elntoic2lvu_ic2",
                "west", "eln:blocks/elntoic2lvu_side", "east", "eln:blocks/elntoic2lvu_side",
                "particle", "eln:blocks/elntoic2lvu_side")),
            assets.resolve("models/block/energy_converter.json")));
        for (Block b : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(b);
            if (!id.getNamespace().equals(Eln.MODID)) continue;
            String name = id.getPath();
            String model;
            if (b instanceof mods.eln.ore.OreBlock ore) {
                String tex = iconTexture(ore.descriptor.getIconName(), true);
                if (tex == null) missing.add("block " + name);
                model = "eln:block/" + name;
                out.add(DataProvider.saveStable(cache, obj("parent", "minecraft:block/cube_all", "textures",
                    obj("all", "eln:" + (tex == null ? "blocks/ore" : tex))), assets.resolve("models/block/" + name + ".json")));
            } else if (b instanceof mods.eln.node.simple.SimpleNodeBlock) {
                model = "eln:block/energy_converter";
            } else if (b instanceof mods.eln.sixnode.lampsocket.LightBlock) {
                model = "eln:block/particle_none";
            } else {
                model = "eln:block/particle_iron";
            }
            JsonArray parts = new JsonArray();
            parts.add(obj("apply", obj("model", model)));
            out.add(DataProvider.saveStable(cache, obj("multipart", parts), assets.resolve("blockstates/" + name + ".json")));
        }
        // items
        for (var h : ElnRegistry.ALL_ITEMS) {
            Item item = h.get();
            String name = BuiltInRegistries.ITEM.getKey(item).getPath();
            String model = "eln:item/" + name;
            if (item instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof mods.eln.node.simple.SimpleNodeBlock) {
                model = "eln:block/energy_converter";
            } else if (name.equals("replicator_spawn_egg")) {
                out.add(DataProvider.saveStable(cache, obj("parent", "minecraft:item/generated", "textures",
                    obj("layer0", "minecraft:item/egg")), assets.resolve("models/item/" + name + ".json")));
            } else if (item instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof mods.eln.ore.OreBlock) {
                model = "eln:block/" + name;
            } else {
                String tex = itemTexture(name, item);
                if (tex == null) {
                    missing.add("item " + name);
                    tex = "items/missing";
                }
                out.add(DataProvider.saveStable(cache, obj("parent", HANDHELD.contains(name) ? "minecraft:item/handheld" : "minecraft:item/generated",
                    "textures", obj("layer0", "eln:" + tex)), assets.resolve("models/item/" + name + ".json")));
            }
            out.add(DataProvider.saveStable(cache, obj("model", obj("type", "minecraft:model", "model", model)),
                assets.resolve("items/" + name + ".json")));
        }
        if (!missing.isEmpty()) Eln.LOG.warn("ElnModels: no texture for {} entries: {}", missing.size(), missing);
        return CompletableFuture.allOf(out.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "EA models";
    }
}
