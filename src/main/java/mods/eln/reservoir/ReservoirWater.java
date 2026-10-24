package mods.eln.reservoir;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Water reservoirs (Watershed's lakes): full cells are vanilla water (kelp, seagrass, bubble columns, waterlogging and
 * other mods expect it); only a partial surface cell (1-7 eighths) is reservoir water, which vanilla water treats as
 * the same fluid (mixin.WaterFluidMixin: AdventurAgent saw both draw faces where they met).
 */
public final class ReservoirWater {
    private ReservoirWater() {
    }

    public static final String KIND = "water";

    static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, "eln");
    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("eln");

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, ReservoirWaterFluid> FLUID =
        FLUIDS.register("reservoir_water", ReservoirWaterFluid::new);
    public static final DeferredBlock<ReservoirWaterBlock> BLOCK = BLOCKS.registerBlock("reservoir_water", ReservoirWaterBlock::new,
        p -> p.mapColor(MapColor.WATER).replaceable().noCollision().strength(100f).pushReaction(PushReaction.POPPED).noLootTable()
            .liquid().sound(net.minecraft.world.level.block.SoundType.EMPTY));

    static void register(IEventBus modBus) {
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
        Reservoirs.registerKind(KIND, new ReservoirKind() {
            @Override
            public BlockState cell(int level) {
                return level >= 8 ? Blocks.WATER.defaultBlockState()
                    : BLOCK.get().defaultBlockState().setValue(BlockStateProperties.LEVEL_FLOWING, Math.max(1, level));
            }

            @Override
            public boolean ours(BlockState s) {
                return s.is(Blocks.WATER) || s.is(BLOCK.get()) || plant(s);
            }

            @Override
            public boolean matches(BlockState s, int lvl) {
                if (lvl == 0) return s.isAir();
                if (lvl >= 8) return s.is(Blocks.WATER) && s.getFluidState().isSource() || plant(s);
                return s == cell(lvl);
            }

            @Override
            public boolean waterlogs() {
                return true;
            }
        });
    }

    /** Water plants and bubble columns: they belong in a full water cell. */
    static boolean plant(BlockState s) {
        return s.is(Blocks.SEAGRASS) || s.is(Blocks.TALL_SEAGRASS) || s.is(Blocks.KELP) || s.is(Blocks.KELP_PLANT) || s.is(Blocks.BUBBLE_COLUMN);
    }
}
