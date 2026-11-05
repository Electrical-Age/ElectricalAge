package mods.eln.robotics;

import mods.eln.Eln;
import mods.eln.ElnContent;
import mods.eln.i18n.I18N;
import mods.eln.registry.ElnRegistry;
import mods.eln.robotics.computer.ChipItem;
import mods.eln.robotics.computer.ComputerDescriptor;
import mods.eln.robotics.computer.ComputerStore;
import mods.eln.robotics.computer.FloppyItem;
import mods.eln.robotics.computer.hw.ComputerPool;
import mods.eln.robotics.servo.ElevatorCar;
import mods.eln.robotics.servo.LinearServoDriveDescriptor;
import mods.eln.robotics.servo.ServoRailBlock;
import mods.eln.robotics.servo.StationMarkerBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Robotics: EA's servo layer (Baughn: the servo layer first; 200 V only). Kept in one package with its own registers,
 * so it can move to a subproject: the Linear Servo Drive (a transparent node, eln-ids.json id 5504), Servo Rails,
 * Station Markers, the elevator car, and the computer (id 5568) with its chips and floppies. Its payload is in network.ElnNet, its client side in robotics.client.
 */
public final class Robotics implements ElnContent {
    /** The drive's descriptor key (sub + (id << 6)): group 86, after the anvil's 85. */
    public static final int DRIVE_ID = 0 + (86 << 6);
    /** The computer's descriptor key: group 87. */
    public static final int COMPUTER_ID = 0 + (87 << 6);

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Eln.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Eln.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Eln.MODID);

    public static final DeferredBlock<ServoRailBlock> SERVO_RAIL = BLOCKS.registerBlock("servo_rail", ServoRailBlock::new,
        p -> p.mapColor(MapColor.METAL).strength(1.5f, 6.0f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<StationMarkerBlock> STATION_MARKER = BLOCKS.registerBlock("station_marker", StationMarkerBlock::new,
        p -> p.mapColor(MapColor.METAL).strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> SERVO_RAIL_ITEM = ITEMS.registerSimpleBlockItem(SERVO_RAIL);
    public static final DeferredItem<BlockItem> STATION_MARKER_ITEM = ITEMS.registerSimpleBlockItem(STATION_MARKER);
    /** The computer's chips and floppies. */
    public static final DeferredItem<ChipItem> RAM_CHIP = ITEMS.registerItem("ram_chip", p -> new ChipItem(ChipItem.Kind.RAM, p));
    public static final DeferredItem<ChipItem> JANET_ROM = ITEMS.registerItem("janet_rom_chip", p -> new ChipItem(ChipItem.Kind.JANET, p));
    public static final DeferredItem<FloppyItem> FLOPPY_DISK = ITEMS.registerItem("floppy_disk", FloppyItem::new);

    /** The car: a 3 x 3 x 1/16 plate (its cabin's walls are parts); no vanilla position sync (no update interval, no deltas), not saved, not summoned. */
    public static final DeferredHolder<EntityType<?>, EntityType<ElevatorCar>> ELEVATOR_CAR = ENTITY_TYPES.register("elevator_car",
        () -> EntityType.Builder.<ElevatorCar>of(ElevatorCar::new, MobCategory.MISC)
            .sized((float) ElevatorCar.CABIN, (float) ElevatorCar.THICK).noLootTable().noSave().noSummon().fireImmune()
            .clientTrackingRange(8).noUpdateInterval().dontTrackDeltas()
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Eln.MODID, "elevator_car"))));

    /** Its descriptors, before EA registers its device items. */
    @Override
    public void preInit() {
        Eln.transparentNodeItem.addDescriptor(DRIVE_ID, new LinearServoDriveDescriptor(I18N.TR_NAME(I18N.Type.NONE, "Linear Servo Drive"), "servodrive"));
        Eln.transparentNodeItem.addDescriptor(COMPUTER_ID, new ComputerDescriptor(I18N.TR_NAME(I18N.Type.NONE, "Computer"), "computer"));
    }

    /** Computers' threads stop and their files are written before the server is gone. */
    @Override
    public void serverStopped() {
        ComputerPool.shutdown();
        ComputerStore.serverStopped();
    }

    /** Its blocks, items and entity; its items join EA's tab. */
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        ElnRegistry.ALL_ITEMS.add(SERVO_RAIL_ITEM);
        ElnRegistry.ALL_ITEMS.add(STATION_MARKER_ITEM);
        ElnRegistry.ALL_ITEMS.add(RAM_CHIP);
        ElnRegistry.ALL_ITEMS.add(JANET_ROM);
        ElnRegistry.ALL_ITEMS.add(FLOPPY_DISK);
        ElnRegistry.RETIRED_ITEMS.add(STATION_MARKER_ITEM);   // (no longer used, Baughn 2026-11-05: out of the tab)
    }
}
