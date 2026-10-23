package mods.eln.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * The oil of an underground lake: still, level 1-8 (8 = full: drawn 8/9 high like water), never flows. Its level is
 * set by the lake's Reservoir record (the sweep in Reservoirs), so the whole surface moves together; buckets and pumps
 * take from the record (ReservoirOilBlock), not from the block.
 */
public class ReservoirOilFluid extends Fluid {
    public ReservoirOilFluid() {
        registerDefaultState(getStateDefinition().any().setValue(BlockStateProperties.LEVEL_FLOWING, 8));
    }

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
        super.createFluidStateDefinition(builder);
        builder.add(BlockStateProperties.LEVEL_FLOWING);
    }

    public FluidState level(int level) {
        return defaultFluidState().setValue(BlockStateProperties.LEVEL_FLOWING, Math.max(1, Math.min(8, level)));
    }

    @Override
    public Item getBucket() {
        return Oil.CRUDE_BUCKET.get();
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
        return false;
    }

    @Override
    protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
        return Vec3.ZERO;   // a still lake: no current toward the shore
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 20;
    }

    @Override
    protected float getExplosionResistance() {
        return 100f;
    }

    @Override
    public float getHeight(FluidState state, BlockGetter level, BlockPos pos) {
        return level.getFluidState(pos.above()).getType().isSame(this) ? 1f : getOwnHeight(state);
    }

    @Override
    public float getOwnHeight(FluidState state) {
        return getAmount(state) / 9f;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return Oil.RESERVOIR_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.LEVEL_FLOWING, getAmount(state));
    }

    @Override
    public boolean isSource(FluidState state) {
        return true;   // buckets look for sources (and take from the lake's record, not the block)
    }

    @Override
    public int getAmount(FluidState state) {
        return state.getValue(BlockStateProperties.LEVEL_FLOWING);
    }

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        return Shapes.box(0, 0, 0, 1, getHeight(state, level, pos), 1);
    }

    @Override
    public FluidType getFluidType() {
        return Oil.CRUDE_TYPE.get();
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == this;
    }
}
