package mods.eln.node.transparent;

import mods.eln.node.NodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The block of one transparent-node device (1.12: one shared TransparentNodeBlock; 26.x: one block per device, all of
 * this class, for per-device models, drops and names). The device's node is a TransparentNode; ghosts make up the
 * rest of a multiblock. Waterloggable, as six nodes are: a device placed in a water source keeps the water around it
 * (Baughn: the submersible pump must be waterlogged).
 */
public class TransparentNodeBlock extends NodeBlock implements net.minecraft.world.level.block.SimpleWaterloggedBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty WATERLOGGED =
        net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;
    public final TransparentNodeDescriptor descriptor;

    public TransparentNodeBlock(Properties properties, TransparentNodeDescriptor descriptor) {
        super(properties);
        this.descriptor = descriptor;
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    protected net.minecraft.world.level.material.FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? net.minecraft.world.level.material.Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks,
            BlockPos pos, net.minecraft.core.Direction dir, BlockPos neighbourPos, BlockState neighbour, net.minecraft.util.RandomSource random) {
        if (state.getValue(WATERLOGGED))
            ticks.scheduleTick(pos, net.minecraft.world.level.material.Fluids.WATER, net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbour, random);
    }

    /** The state placed for a device facing FRONT (TransparentNodeItem); one without a front state: the default. */
    public BlockState stateForFront(mods.eln.misc.Direction front) {
        return defaultBlockState();
    }

    /** The state placed at pos for a device facing FRONT: waterlogged if a water source is there. */
    public BlockState placedState(mods.eln.misc.Direction front, net.minecraft.world.level.Level world, BlockPos pos) {
        var f = world.getFluidState(pos);
        return stateForFront(front).setValue(WATERLOGGED, f.is(net.minecraft.world.level.material.Fluids.WATER) && f.isSource());
    }

    /** The device's collision and outline: its descriptor's 1.7.10 collision boxes (addCollisionBoxesToList, a block at
     *  the origin), so a flat solar panel or the egg incubator is half a block high (Baughn: on a flat solar panel you
     *  stood on thin air). Built once. */
    private net.minecraft.world.phys.shapes.VoxelShape shape;

    net.minecraft.world.phys.shapes.VoxelShape shape() {
        if (shape == null) {
            java.util.List<net.minecraft.world.phys.AABB> boxes = new java.util.ArrayList<>();
            descriptor.addCollisionBoxesToList(new net.minecraft.world.phys.AABB(0, 0, 0, 1, 1, 1), boxes, null, 0, 0, 0);
            net.minecraft.world.phys.shapes.VoxelShape s = net.minecraft.world.phys.shapes.Shapes.empty();
            for (var b : boxes) s = net.minecraft.world.phys.shapes.Shapes.or(s, net.minecraft.world.phys.shapes.Shapes.create(b));
            shape = boxes.isEmpty() ? net.minecraft.world.phys.shapes.Shapes.block() : s.optimize();
        }
        return shape;
    }

    /** Its model's hitbox turned to the device's front (read from the block entity, as SixNodeBlock reads its faces),
     *  if it has one and the front is known; else shape(). */
    net.minecraft.world.phys.shapes.VoxelShape shape(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        ModelHitbox h = descriptor.modelHitbox();
        if (h != null && level.getBlockEntity(pos) instanceof TransparentNodeEntity e) {
            mods.eln.misc.Direction front = e.front();
            if (front != null) return h.shape(front);
        }
        return shape();
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                                   net.minecraft.world.phys.shapes.CollisionContext context) {
        return shape(level, pos);
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                                            net.minecraft.world.phys.shapes.CollisionContext context) {
        return shape(level, pos);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransparentNodeEntity(pos, state);
    }
}
