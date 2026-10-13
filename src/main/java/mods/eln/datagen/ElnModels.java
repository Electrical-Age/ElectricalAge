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
    static final Set<String> HANDHELD = Set.of();

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
        // the redrawn icon set: textures/item/<registry id>.png (the schematic set overrides the same paths in a pack)
        if (textureExists("item/" + name)) return "item/" + name;
        if (item instanceof IDescriptorItem di) {
            Object d = di.getEaDescriptor();
            if (d instanceof GenericItemUsingDamageDescriptor g) return iconTexture(g.getIconName(), false);
            if (d instanceof GenericItemBlockUsingDamageDescriptor b) return iconTexture(b.getIconName(), true);
        }
        String t = iconTexture(name, false);
        if (t == null) t = iconTexture(name.replace("e_coal_", "ecoal_"), false);
        if (t == null && name.equals("item_creative_tab")) t = iconTexture("elncreativetab", false);
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
            } else if (b instanceof mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock) {
                // the synchronous-condenser models (AdventurAgent, tools/models/converter.py; OBJ in src/main/resources),
                // turned so their EA face is the node's FRONT
                JsonObject variants = new JsonObject();
                for (int f = 0; f <= 6; f++) {
                    JsonObject v = obj("model", converterModel(name));
                    int[] r = CONVERTER_ROT[f];
                    if (r[0] != 0) v.addProperty("x", r[0]);
                    if (r[1] != 0) v.addProperty("y", r[1]);
                    variants.add("front=" + f, v);
                }
                out.add(DataProvider.saveStable(cache, obj("variants", variants), assets.resolve("blockstates/" + name + ".json")));
                continue;
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
                model = converterModel(name);
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
                // the data-log print had an empty icon (1.12 drew the graph over it): vanilla paper in the GUI
                String layer = name.equals("data_logger_print") ? "minecraft:item/paper" : "eln:" + tex;
                out.add(DataProvider.saveStable(cache, obj("parent", HANDHELD.contains(name) ? "minecraft:item/handheld" : "minecraft:item/generated",
                    "textures", obj("layer0", layer)), assets.resolve("models/item/" + name + ".json")));
            }
            JsonObject flat = obj("type", "minecraft:model", "model", model);
            Object d = item instanceof mods.eln.generic.IDescriptorItem di ? di.getEaDescriptor() : null;
            if (d instanceof mods.eln.generic.GenericItemBlockUsingDamageDescriptor gd && gd.hasItemDraw()) {
                // R5: the flat icon in the GUI, the device's 3D draw everywhere else (eln:device, ElnItemRenderer);
                // the pose per context is the base model's display block (device_six / device_transparent)
                String base = "eln:item/" + name + "_3d";
                String parent = d instanceof mods.eln.node.six.SixNodeDescriptor ? "eln:item/device_six" : "eln:item/device_transparent";
                String particle = "eln:" + (itemTexture(name, item) == null ? "items/missing" : itemTexture(name, item));
                out.add(DataProvider.saveStable(cache, obj("parent", parent, "textures", obj("particle", particle)),
                    assets.resolve("models/item/" + name + "_3d.json")));
                JsonArray cases = new JsonArray();
                cases.add(obj("when", "gui", "model", flat));
                JsonArray entity = new JsonArray();
                for (String c : new String[]{"ground", "fixed", "head"}) entity.add(c);
                cases.add(obj("when", entity, "model", obj("type", "minecraft:special", "base", base,
                    "model", obj("type", "eln:device", "context", "entity"))));
                out.add(DataProvider.saveStable(cache, obj("model", obj("type", "minecraft:select", "property", "minecraft:display_context",
                    "cases", cases, "fallback", obj("type", "minecraft:special", "base", base,
                        "model", obj("type", "eln:device", "context", "hand")))), assets.resolve("items/" + name + ".json")));
                continue;
            }
            if (d instanceof mods.eln.generic.GenericItemUsingDamageDescriptor gi && gi.hasItemDraw()) {
                // R5: item descriptors with a live look (data-log print): drawn in the flat model's pose, GUI = icon;
                // model items (X-ray scanner) get a base model with their own pose
                String base = "eln:item/" + name;
                if (gi.itemDrawParent() != null) {
                    base = "eln:item/" + name + "_3d";
                    out.add(DataProvider.saveStable(cache, obj("parent", gi.itemDrawParent(), "textures",
                        obj("particle", "eln:" + (itemTexture(name, item) == null ? "items/missing" : itemTexture(name, item)))),
                        assets.resolve("models/item/" + name + "_3d.json")));
                }
                JsonArray cases = new JsonArray();
                cases.add(obj("when", "gui", "model", flat));
                JsonArray entity = new JsonArray();
                for (String c : new String[]{"ground", "fixed", "head"}) entity.add(c);
                cases.add(obj("when", entity, "model", obj("type", "minecraft:special", "base", base,
                    "model", obj("type", "eln:device", "context", "entity"))));
                out.add(DataProvider.saveStable(cache, obj("model", obj("type", "minecraft:select", "property", "minecraft:display_context",
                    "cases", cases, "fallback", obj("type", "minecraft:special", "base", base,
                        "model", obj("type", "eln:device", "context", "hand")))), assets.resolve("items/" + name + ".json")));
                continue;
            }
            if (d instanceof mods.eln.item.electricalitem.ElectricalLampItem lamp) {
                // R5: flashlights show their "on" icon while lit (client condition eln:lit)
                String onTex = textureExists("item/" + name + "_on") ? "item/" + name + "_on"
                    : iconTexture(lamp.getIconName().replaceAll("off$", "on"), false);
                if (onTex != null) {
                    out.add(DataProvider.saveStable(cache, obj("parent", "minecraft:item/generated", "textures", obj("layer0", "eln:" + onTex)),
                        assets.resolve("models/item/" + name + "_on.json")));
                    out.add(DataProvider.saveStable(cache, obj("model", obj("type", "minecraft:condition", "property", "eln:lit",
                        "on_true", obj("type", "minecraft:model", "model", "eln:item/" + name + "_on"), "on_false", flat)),
                        assets.resolve("items/" + name + ".json")));
                    continue;
                }
                missing.add("lit icon " + name);
            }
            out.add(DataProvider.saveStable(cache, obj("model", flat), assets.resolve("items/" + name + ".json")));
        }
        if (!missing.isEmpty()) Eln.LOG.warn("ElnModels: no texture for {} entries: {}", missing.size(), missing);
        return CompletableFuture.allOf(out.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "EA models";
    }

    /** FRONT (vanilla direction index, 6 = unknown) -> {x, y} rotation of the north-facing converter model. */
    static final int[][] CONVERTER_ROT = {{90, 0}, {270, 0}, {0, 0}, {0, 180}, {0, 270}, {0, 90}, {0, 0}};

    /** eln:energy_converter_eln_to_other_lvublock -> eln:block/energy_converter_lvu. */
    static String converterModel(String name) {
        return "eln:block/energy_converter_" + name.replace("energy_converter_eln_to_other_", "").replace("block", "");
    }
}
