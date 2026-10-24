package mods.eln.reservoir;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * A water reservoir's partial surface cell (ReservoirWater): still, level 1-7 eighths (drawn level/9 high), never flows;
 * the reservoir's record sets it, so the whole surface moves together. Water's fluid type and the minecraft:water tag:
 * swimming, drowning, boats and fishing as in water.
 */
public class ReservoirWaterFluid extends Fluid {
    public ReservoirWaterFluid() {
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
        return Items.WATER_BUCKET;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
        return false;
    }

    @Override
    protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
        return Vec3.ZERO;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 5;
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
        return ReservoirWater.BLOCK.get().defaultBlockState().setValue(BlockStateProperties.LEVEL_FLOWING, getAmount(state));
    }

    @Override
    public boolean isSource(FluidState state) {
        return true;
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
        return net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value();
    }

    /** Water counts as the same fluid (a lake's surface over its sources is one body of water). */
    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == this || fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
    }
}
