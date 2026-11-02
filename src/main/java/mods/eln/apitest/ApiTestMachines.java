package mods.eln.apitest;

import mods.eln.api.ElnApi;
import mods.eln.api.MachineSpec;
import mods.eln.api.RelativeFace;
import mods.eln.api.Tier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A test fixture written as another mod would write it, on the Machine API alone (this package may use no EA type
 * outside mods.eln.api; ApiTestFixtureImportsTest checks): the hidden eln:api_test_machine, a furnace-like block with
 * FACING and LIT whose block entity draws power through its back and front and turns it into work. No item, so it is
 * in no creative tab or recipe viewer; GameTests place it.
 */
public final class ApiTestMachines {
    private ApiTestMachines() {
    }

    static final String MODID = "eln";

    /** 50 V, 100 W (no idle draw): power at the front and back; "enable" on the left reads 1 unwired; the bus on the right. */
    public static final MachineSpec SPEC = MachineSpec.consumer(Tier.V50).ratedWatts(100)
        .power(RelativeFace.FRONT, RelativeFace.BACK)
        .signalIn("enable", RelativeFace.LEFT, 1).bus(RelativeFace.RIGHT).build();

    /** The bus channel it writes 1 on while it works (0, white). */
    public static final int BUSY_CHANNEL = 0;

    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredBlock<ApiTestMachineBlock> BLOCK = BLOCKS.registerBlock("api_test_machine", ApiTestMachineBlock::new,
        p -> p.mapColor(MapColor.METAL).strength(1.5f, 6.0f).sound(SoundType.METAL).noLootTable());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ApiTestMachineEntity>> TYPE =
        BLOCK_ENTITIES.register("api_test_machine", () -> new BlockEntityType<>(ApiTestMachineEntity::new, BLOCK.get()));

    /** The mod's constructor: its registers, and the type registered as a machine at common setup. */
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(FMLCommonSetupEvent.class, e -> ElnApi.machines().register(TYPE.get(), be -> SPEC));
    }
}
