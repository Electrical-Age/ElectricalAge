package mods.eln.registry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mods.eln.Eln;
import mods.eln.ghost.GhostBlock;
import mods.eln.node.six.SixNodeBlock;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.node.six.SixNodeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * EA's 26.x registry entries. Device items come from the descriptor registries (Eln.sixNodeItem / transparentNodeItem,
 * filled by ElnDeviceRegistry with 1.12 ids); each gets its registry name from eln-ids.json ("eln:six_node@<id>").
 */
public final class ElnRegistry {
    private ElnRegistry() {
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Eln.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Eln.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Eln.MODID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Eln.MODID);
    /** Every EA device GUI (P4): ElnNodeMenu, rebuilt on the client from its open data. */
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<mods.eln.gui.ElnNodeMenu>> NODE_MENU =
        MENUS.register("node", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(mods.eln.gui.ElnNodeMenu::fromNetwork));
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<mods.eln.item.xray.XRayScannerMenu>> XRAY_MENU =
        MENUS.register("xray_scanner", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(mods.eln.item.xray.XRayScannerMenu::fromNetwork));
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Eln.MODID);

    /** Every EA item, in 1.12 id order (creative tab, tests). */
    public static final List<DeferredItem<? extends Item>> ALL_ITEMS = new ArrayList<>();

    public static final DeferredBlock<SixNodeBlock> SIX_NODE_BLOCK = BLOCKS.registerBlock("six_node", SixNodeBlock::new,
        p -> p.mapColor(MapColor.METAL).strength(1.0f, 1.0f).sound(SoundType.METAL).noOcclusion().dynamicShape());
    public static final DeferredBlock<GhostBlock> GHOST_BLOCK = BLOCKS.registerBlock("ghost_block", GhostBlock::new,
        p -> p.mapColor(MapColor.METAL).strength(0.5f).noOcclusion().noLootTable());

    /** Lamp/turret/teleporter light (1.12 LightBlock): level in META, no shape, no drops, replaceable. */
    public static final DeferredBlock<mods.eln.sixnode.lampsocket.LightBlock> LIGHT_BLOCK = BLOCKS.registerBlock("light_block",
        mods.eln.sixnode.lampsocket.LightBlock::new,
        p -> p.noCollision().replaceable().noLootTable().noOcclusion()
            .lightLevel(s -> s.getValue(mods.eln.compat.BlockMeta.META)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mods.eln.sixnode.lampsocket.LightBlockEntity>> LIGHT_ENTITY =
        BLOCK_ENTITIES.register("light_block", () -> new BlockEntityType<>(mods.eln.sixnode.lampsocket.LightBlockEntity::new, LIGHT_BLOCK.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SixNodeEntity>> SIX_NODE_ENTITY =
        BLOCK_ENTITIES.register("six_node", () -> new BlockEntityType<>(SixNodeEntity::new, SIX_NODE_BLOCK.get()));

    /** Voxel nodes (pipes phase 3, notes/pipes/DESIGN.md section 2): parts in a block's voxels; the pipe kits. */
    public static final DeferredBlock<mods.eln.node.voxel.VoxelNodeBlock> VOXEL_NODE_BLOCK = BLOCKS.registerBlock("voxel_node",
        mods.eln.node.voxel.VoxelNodeBlock::new,
        p -> p.mapColor(MapColor.METAL).strength(1.0f, 1.0f).sound(SoundType.METAL).noOcclusion().dynamicShape());
    /** Voxel blocks imitating fences and walls carry their tags (data/minecraft/tags), so neighbours join them (Mimic). */
    public static final DeferredBlock<mods.eln.node.voxel.VoxelNodeBlock> VOXEL_NODE_WOODEN_FENCE = BLOCKS.registerBlock("voxel_node_wooden_fence",
        mods.eln.node.voxel.VoxelNodeBlock::new,
        p -> p.mapColor(MapColor.WOOD).strength(1.0f, 1.0f).sound(SoundType.WOOD).noOcclusion().dynamicShape());
    public static final DeferredBlock<mods.eln.node.voxel.VoxelNodeBlock> VOXEL_NODE_FENCE = BLOCKS.registerBlock("voxel_node_fence",
        mods.eln.node.voxel.VoxelNodeBlock::new,
        p -> p.mapColor(MapColor.NETHER).strength(1.0f, 1.0f).sound(SoundType.NETHER_BRICKS).noOcclusion().dynamicShape());
    public static final DeferredBlock<mods.eln.node.voxel.VoxelNodeBlock> VOXEL_NODE_WALL = BLOCKS.registerBlock("voxel_node_wall",
        mods.eln.node.voxel.VoxelNodeBlock::new,
        p -> p.mapColor(MapColor.STONE).strength(1.0f, 1.0f).sound(SoundType.STONE).noOcclusion().dynamicShape());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mods.eln.node.voxel.VoxelNodeEntity>> VOXEL_NODE_ENTITY =
        BLOCK_ENTITIES.register("voxel_node", () -> new BlockEntityType<>(mods.eln.node.voxel.VoxelNodeEntity::new, VOXEL_NODE_BLOCK.get(),
            VOXEL_NODE_WOODEN_FENCE.get(), VOXEL_NODE_FENCE.get(), VOXEL_NODE_WALL.get()));
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, Eln.MODID);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.RecipeSerializer<mods.eln.node.voxel.PipeKitRecipes.FromPlates>> PIPE_KIT_FROM_PLATES =
        RECIPE_SERIALIZERS.register("pipe_kit_from_plates", () -> mods.eln.node.voxel.PipeKitRecipes.FromPlates.SERIALIZER);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.RecipeSerializer<mods.eln.node.voxel.PipeKitRecipes.Merge>> PIPE_KIT_MERGE =
        RECIPE_SERIALIZERS.register("pipe_kit_merge", () -> mods.eln.node.voxel.PipeKitRecipes.Merge.SERIALIZER);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.RecipeSerializer<mods.eln.node.voxel.PipeKitRecipes.Split>> PIPE_KIT_SPLIT =
        RECIPE_SERIALIZERS.register("pipe_kit_split", () -> mods.eln.node.voxel.PipeKitRecipes.Split.SERIALIZER);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.RecipeSerializer<mods.eln.node.voxel.CableKitRecipes.FromCoils>> CABLE_KIT_FROM_COILS =
        RECIPE_SERIALIZERS.register("cable_kit_from_coils", () -> mods.eln.node.voxel.CableKitRecipes.FromCoils.SERIALIZER);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.RecipeSerializer<mods.eln.node.voxel.CableKitRecipes.ToCoil>> CABLE_KIT_TO_COIL =
        RECIPE_SERIALIZERS.register("cable_kit_to_coil", () -> mods.eln.node.voxel.CableKitRecipes.ToCoil.SERIALIZER);
    /** Cable kits, one per CablePart tier: signal, low, medium, high, very high voltage. */
    public static final java.util.List<net.neoforged.neoforge.registries.DeferredItem<mods.eln.node.voxel.CableKitItem>> CABLE_KITS = java.util.List.of(
        ITEMS.registerItem("cable_kit_signal", p -> new mods.eln.node.voxel.CableKitItem(0, p)),
        ITEMS.registerItem("cable_kit_lv", p -> new mods.eln.node.voxel.CableKitItem(1, p)),
        ITEMS.registerItem("cable_kit_mv", p -> new mods.eln.node.voxel.CableKitItem(2, p)),
        ITEMS.registerItem("cable_kit_hv", p -> new mods.eln.node.voxel.CableKitItem(3, p)),
        ITEMS.registerItem("cable_kit_vhv", p -> new mods.eln.node.voxel.CableKitItem(4, p)),
        ITEMS.registerItem("cable_kit_thermal", p -> new mods.eln.node.voxel.CableKitItem(5, p)));
    /** Raw lead and tungsten: what their ores drop, like raw iron (Baughn 2026-10-19: they dropped the ore block, as in
     *  1.7); smelted or blasted to ingots, macerated to dust. */
    public static final DeferredItem<Item> RAW_LEAD = ITEMS.registerSimpleItem("raw_lead");
    public static final DeferredItem<Item> RAW_TUNGSTEN = ITEMS.registerSimpleItem("raw_tungsten");
    /** What food turns into in an electrical oven above 300 °C (Baughn); four make a charcoal, and it burns a quarter as long. */
    public static final DeferredItem<Item> CHARCOAL_NUGGET = ITEMS.registerSimpleItem("charcoal_nugget",
        p -> p.component(net.minecraft.core.component.DataComponents.COOKING_FUEL, new net.minecraft.world.item.component.CookingFuel(
            new net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Constant(400),
            new net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat.Reference(
                net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER))));
    /** The chisel (carving blocks voxel by voxel: a hole through a wall for a cable; art). */
    public static final net.neoforged.neoforge.registries.DeferredItem<mods.eln.node.voxel.ChiselItem> CHISEL =
        ITEMS.registerItem("chisel", mods.eln.node.voxel.ChiselItem::new);
    public static final net.neoforged.neoforge.registries.DeferredItem<mods.eln.node.voxel.PipeKitItem> PIPE_KIT_IRON =
        ITEMS.registerItem("pipe_kit_iron", p -> new mods.eln.node.voxel.PipeKitItem(0, p));
    public static final net.neoforged.neoforge.registries.DeferredItem<mods.eln.node.voxel.PipeKitItem> PIPE_KIT_ALLOY =
        ITEMS.registerItem("pipe_kit_alloy", p -> new mods.eln.node.voxel.PipeKitItem(1, p));
    public static final net.neoforged.neoforge.registries.DeferredItem<mods.eln.node.voxel.ValveItem> PIPE_VALVE =
        ITEMS.registerItem("pipe_valve", mods.eln.node.voxel.ValveItem::new);
    public static final net.neoforged.neoforge.registries.DeferredItem<mods.eln.node.voxel.SafetyValveItem> PIPE_SAFETY_VALVE =
        ITEMS.registerItem("pipe_safety_valve", mods.eln.node.voxel.SafetyValveItem::new);
    public static final net.neoforged.neoforge.registries.DeferredItem<mods.eln.node.voxel.AdapterItem> PIPE_ADAPTER =
        ITEMS.registerItem("pipe_adapter", mods.eln.node.voxel.AdapterItem::new);

    /** One block per transparent-node device; the shared block-entity type covers them all. */
    public static final List<DeferredBlock<mods.eln.node.transparent.TransparentNodeBlock>> TRANSPARENT_BLOCKS = new ArrayList<>();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mods.eln.node.transparent.TransparentNodeEntity>> TRANSPARENT_NODE_ENTITY =
        BLOCK_ENTITIES.register("transparent_node", () -> new BlockEntityType<>(mods.eln.node.transparent.TransparentNodeEntity::new,
            TRANSPARENT_BLOCKS.stream().map(DeferredBlock::get).collect(java.util.stream.Collectors.toSet())));

    static {
        // the FE exporter's own block-entity type, from when it was a simple node: saved chunks name it
        BLOCK_ENTITIES.addAlias(net.minecraft.resources.Identifier.fromNamespaceAndPath(Eln.MODID, "energy_converter_eln_to_other_entity"),
            net.minecraft.resources.Identifier.fromNamespaceAndPath(Eln.MODID, "transparent_node"));
    }

    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, Eln.MODID);

    /** The replicator (1.12 eln:replicator, size 0.3 x 0.7, tracked 80 blocks every 3 ticks). */
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<mods.eln.entity.ReplicatorEntity>> REPLICATOR =
        ENTITY_TYPES.register("replicator", () -> net.minecraft.world.entity.EntityType.Builder
            .<mods.eln.entity.ReplicatorEntity>of(mods.eln.entity.ReplicatorEntity::new, net.minecraft.world.entity.MobCategory.MONSTER)
            .sized(0.3f, 0.7f).clientTrackingRange(5).updateInterval(3)
            .build(net.minecraft.resources.ResourceKey.create(Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath(Eln.MODID, "replicator"))));

    /** 1.12 dropped a vanilla spawn egg tagged eln:replicator; 26.x eggs are per entity type. */
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> REPLICATOR_SPAWN_EGG =
        registerListed("replicator_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(REPLICATOR.get()));

    private static <I extends Item> DeferredItem<I> registerListed(String name, java.util.function.Function<Item.Properties, I> f,
                                                                   java.util.function.UnaryOperator<Item.Properties> props) {
        DeferredItem<I> i = ITEMS.registerItem(name, f, props);
        ALL_ITEMS.add(i);
        return i;
    }


    static JsonObject ids;

    /** The 26.x registry name of a 1.12 item ("eln:six_node", damage), from eln-ids.json. */
    public static String nameOf(String legacyItem, int damage) {
        if (ids == null) {
            try (Reader r = new InputStreamReader(ElnRegistry.class.getResourceAsStream("/eln-ids.json"), StandardCharsets.UTF_8)) {
                ids = JsonParser.parseReader(r).getAsJsonObject();
            } catch (Exception e) {
                throw new IllegalStateException("eln-ids.json", e);
            }
        }
        JsonObject o = ids.getAsJsonObject(legacyItem + "@" + damage);
        if (o == null) throw new IllegalStateException("no registry name for " + legacyItem + "@" + damage + " in eln-ids.json");
        return o.get("name").getAsString();
    }

    private static boolean parentsSet;

    /** Server about to start: the descriptors' setParent hooks (1.12 ran them at addDescriptor), once. */
    public static void afterRegistration() {
        if (parentsSet) return;
        parentsSet = true;
        for (Map.Entry<Integer, SixNodeDescriptor> e : Eln.sixNodeItem.all().entrySet())
            e.getValue().setParent(e.getValue().parentItem, e.getKey());
        for (Map.Entry<Integer, mods.eln.node.transparent.TransparentNodeDescriptor> e : Eln.transparentNodeItem.all().entrySet())
            e.getValue().setParent(e.getValue().parentItem, e.getKey());
        for (var reg : List.of(Eln.sharedItem, Eln.sharedItemStackOne))
            for (Map.Entry<Integer, mods.eln.generic.GenericItemUsingDamageDescriptor> e : reg.all().entrySet())
                e.getValue().setParent(e.getValue().parentItem, e.getKey());
    }

    static void registerShared(mods.eln.node.DescriptorRegistry<mods.eln.generic.GenericItemUsingDamageDescriptor> reg, String legacy, int stack) {
        for (Map.Entry<Integer, mods.eln.generic.GenericItemUsingDamageDescriptor> e : reg.all().entrySet()) {
            int id = e.getKey();
            mods.eln.generic.GenericItemUsingDamageDescriptor d = e.getValue();
            ALL_ITEMS.add(ITEMS.registerItem(nameOf(legacy, id), p -> {
                mods.eln.generic.ElnItem item = new mods.eln.generic.ElnItem(p, d);
                d.parentItem = item;
                d.parentItemDamage = id;
                Eln.itemsByName.putIfAbsent(d.name, item); // first wins: "Signal Switch" is 6081 (gate source) as in 1.12
                return item;
            }, p -> p.stacksTo(stack)));
        }
    }

    public static void register(IEventBus modBus) {
        for (Map.Entry<Integer, SixNodeDescriptor> e : Eln.sixNodeItem.all().entrySet()) {
            int id = e.getKey();
            SixNodeDescriptor d = e.getValue();
            ALL_ITEMS.add(ITEMS.registerItem(nameOf("eln:six_node", id), p -> {
                SixNodeItem item = new SixNodeItem(p, d);
                // only the fields here: setParent overrides make ItemStacks (wiki entries...), which can't exist
                // during registration; they run in afterRegistration()
                d.parentItem = item;
                d.parentItemDamage = id;
                Eln.itemsByName.putIfAbsent(d.name, item); // first wins: "Signal Switch" is 6081 (gate source) as in 1.12
                return item;
            }));
        }
        for (Map.Entry<Integer, mods.eln.node.transparent.TransparentNodeDescriptor> e : Eln.transparentNodeItem.all().entrySet()) {
            int id = e.getKey();
            mods.eln.node.transparent.TransparentNodeDescriptor d = e.getValue();
            String name = nameOf("eln:transparent_node", id);
            // (a device may bring its own block class and properties: the FE exporter's baked model)
            DeferredBlock<mods.eln.node.transparent.TransparentNodeBlock> block = BLOCKS.registerBlock(name, d::newBlock, d::blockProperties);
            TRANSPARENT_BLOCKS.add(block);
            ALL_ITEMS.add(ITEMS.registerItem(name, p -> {
                mods.eln.node.transparent.TransparentNodeItem item = new mods.eln.node.transparent.TransparentNodeItem(p, d, block::get);
                d.parentItem = item;
                d.parentItemDamage = id;
                Eln.itemsByName.putIfAbsent(d.name, item); // first wins: "Signal Switch" is 6081 (gate source) as in 1.12
                return item;
            }));
        }
        for (Map.Entry<Integer, mods.eln.ore.OreDescriptor> e : mods.eln.registry.batch.Wp12cContent.ores.all().entrySet()) {
            int id = e.getKey();
            mods.eln.ore.OreDescriptor d = e.getValue();
            String name = nameOf("eln:ore", id);
            // 1.12 OreBlock: Material.ROCK, hardness 3, resistance 5 (pickaxe, any level: data tag mineable/pickaxe)
            DeferredBlock<mods.eln.ore.OreBlock> block = BLOCKS.registerBlock(name, p -> {
                mods.eln.ore.OreBlock b = new mods.eln.ore.OreBlock(p, d);
                d.block = b;
                return b;
            }, name.startsWith("deepslate_")   // (as vanilla's deepslate ores: darker, harder, deepslate sounds)
                ? p -> p.mapColor(MapColor.DEEPSLATE).strength(4.5f, 5.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)
                : p -> p.mapColor(MapColor.STONE).strength(3.0f, 5.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));
            ALL_ITEMS.add(ITEMS.registerItem(name, p -> {
                mods.eln.ore.OreItem item = new mods.eln.ore.OreItem(block.get(), p);
                d.parentItem = item;
                d.parentItemDamage = id;
                Eln.itemsByName.putIfAbsent(d.name, item); // first wins: "Signal Switch" is 6081 (gate source) as in 1.12
                return item;
            }));
        }
        registerShared(Eln.sharedItem, "eln:shared_item", 64);
        registerShared(Eln.sharedItemStackOne, "eln:shared_item_stack_one", 1);
        // 1.12 itemCreativeTab: an item only to be the tab's icon (texture elncreativetab), not listed in the tab
        DeferredItem<Item> tabIcon = ITEMS.registerSimpleItem("item_creative_tab");
        ALL_ITEMS.add(tabIcon);
        ALL_ITEMS.add(PIPE_KIT_IRON);   // (pipes, 26.x)
        ALL_ITEMS.add(PIPE_KIT_ALLOY);
        ALL_ITEMS.add(PIPE_VALVE);
        ALL_ITEMS.add(PIPE_ADAPTER);
        ALL_ITEMS.add(PIPE_SAFETY_VALVE);
        ALL_ITEMS.addAll(CABLE_KITS);   // (voxel cables, 26.x)
        ALL_ITEMS.add(CHISEL);
        ALL_ITEMS.add(RAW_LEAD);
        ALL_ITEMS.add(RAW_TUNGSTEN);
        ALL_ITEMS.add(CHARCOAL_NUGGET);
        TABS.register("eln", () -> CreativeModeTab.builder()
            .title(Component.literal("Electrical Age"))
            .icon(() -> new ItemStack(tabIcon.get()))
            .displayItems((params, out) -> ALL_ITEMS.forEach(i -> {
                if (i != tabIcon && !mods.eln.node.DescriptorRegistry.isHiddenItem(i.get())) out.accept(i.get()); // 1.12 addWithoutRegistry
            }))
            .build());
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        ENTITY_TYPES.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);
    }
}
