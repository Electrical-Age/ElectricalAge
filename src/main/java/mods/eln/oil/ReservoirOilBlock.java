package mods.eln.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * A cell of an underground oil lake (ReservoirOilFluid). A bucket takes 1000 mB from the lake's record (every cell's
 * level drops together on the next sweep); the block itself stays until the sweep says otherwise. Replaceable like
 * water: a block placed into the lake becomes a dam (mods.eln.reservoir.Reservoirs: it leaves the lake's area until the block goes).
 */
public class ReservoirOilBlock extends Block implements BucketPickup {
    public ReservoirOilBlock(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LEVEL_FLOWING, 8));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.LEVEL_FLOWING);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Oil.RESERVOIR_OIL.get().level(state.getValue(BlockStateProperties.LEVEL_FLOWING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public ItemStack pickupBlock(@Nullable LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state) {
        // the client can't see the record: predict a full bucket (the server decides)
        if (!(level instanceof ServerLevel sl)) return new ItemStack(Oil.CRUDE_BUCKET.get());
        mods.eln.reservoir.Reservoir r = mods.eln.reservoir.ReservoirData.get(sl).at(sl, pos);
        if (r == null || mods.eln.reservoir.Reservoirs.take(sl, r, 1000, true) < 1000) return ItemStack.EMPTY;
        mods.eln.reservoir.Reservoirs.take(sl, r, 1000, false);
        return new ItemStack(Oil.CRUDE_BUCKET.get());
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
    }
}
