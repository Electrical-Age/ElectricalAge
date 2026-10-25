package mods.eln.node.transparent;

import mods.eln.node.NodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The block of one transparent-node device (1.12: one shared TransparentNodeBlock; 26.x: one block per device, all of
 * this class, for per-device models, drops and names). The device's node is a TransparentNode; ghosts make up the
 * rest of a multiblock.
 */
public class TransparentNodeBlock extends NodeBlock {
    public final TransparentNodeDescriptor descriptor;

    public TransparentNodeBlock(Properties properties, TransparentNodeDescriptor descriptor) {
        super(properties);
        this.descriptor = descriptor;
    }

    /** The state placed for a device facing FRONT (TransparentNodeItem); one without a front state: the default. */
    public BlockState stateForFront(mods.eln.misc.Direction front) {
        return defaultBlockState();
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

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                                   net.minecraft.world.phys.shapes.CollisionContext context) {
        return shape();
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                                            net.minecraft.world.phys.shapes.CollisionContext context) {
        return shape();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransparentNodeEntity(pos, state);
    }
}
