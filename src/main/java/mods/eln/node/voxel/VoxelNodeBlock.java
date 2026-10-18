package mods.eln.node.voxel;

import mods.eln.node.NodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * The voxel node's block (notes/pipes/DESIGN.md section 2): its shape is its parts' boxes (pipes collide); breaking
 * takes the part under the crosshair, and the block goes with its last part.
 */
public class VoxelNodeBlock extends NodeBlock {
    public VoxelNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VoxelNodeEntity(pos, state);
    }

    /** A pipe kit in hand lays pipe against a pipe as against any block (no sneaking: AdventurAgent). */
    @Override
    protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (stack.getItem() instanceof PipeTool) return net.minecraft.world.InteractionResult.PASS;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    static VoxelShape shapeOf(java.util.List<VoxelPart> parts) {
        return shapeOf(parts, 0);
    }

    /** The parts' boxes, grown by grow voxels across (not past the block): thin pipes are hard to aim at
     *  (AdventurAgent: a 5 degree band at 3 blocks), so the outline is fatter than what collides. */
    static VoxelShape shapeOf(java.util.List<VoxelPart> parts, int grow) {
        VoxelShape s = Shapes.empty();
        for (VoxelPart p : parts)
            for (int[] b : p.boxes())
                s = Shapes.or(s, box(Math.max(0, b[0] - grow), Math.max(0, b[1] - grow), Math.max(0, b[2] - grow),
                    Math.min(16, b[3] + grow), Math.min(16, b[4] + grow), Math.min(16, b[5] + grow)));
        return s;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelNodeEntity e = tileAt(level, pos, VoxelNodeEntity.class);
        if (e == null) return Shapes.block();
        VoxelShape s = shapeOf(e.parts(), 2);
        return s.isEmpty() ? Shapes.block() : s;
    }

    /** Drawn by its block model (client/render/VoxelModel) in the chunk mesh. */
    @Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelNodeEntity e = tileAt(level, pos, VoxelNodeEntity.class);
        return e == null ? Shapes.empty() : shapeOf(e.parts());
    }

    /** Breaking takes the part under the crosshair; the block stays while others remain. */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
                                       net.minecraft.world.item.ItemStack tool, boolean willHarvest, net.minecraft.world.level.material.FluidState fluid) {
        VoxelNodeEntity e = tileAt(level, pos, VoxelNodeEntity.class);
        if (e == null || player == null) return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        if (level.isClientSide()) return e.parts().size() <= 1 && super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        if (!(e.getNode() instanceof VoxelNode node)) return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        int index = -1;
        var pick = player.pick(player.blockInteractionRange() + 1, 1f, false);
        if (pick instanceof net.minecraft.world.phys.BlockHitResult bh && bh.getBlockPos().equals(pos)) {
            var h = bh.getLocation();
            index = node.partAt(h.x - pos.getX(), h.y - pos.getY(), h.z - pos.getZ());
        }
        if (index < 0) index = node.parts.size() - 1;
        if (index < 0) return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        if (!node.playerAskToBreakPart(player, index)) return false;   // (others remain)
        return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
    }
}
