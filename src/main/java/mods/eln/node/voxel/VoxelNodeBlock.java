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

    /** A pipe or cable kit in hand lays against a pipe as against any block (no sneaking: AdventurAgent); a six-node
     *  item goes onto a free face of the block (SixNodeItem). */
    @Override
    protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (stack.getItem() instanceof PipeTool || stack.getItem() instanceof mods.eln.node.six.SixNodeItem) return net.minecraft.world.InteractionResult.PASS;
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
        // six-node elements hosted here: their plates (grown like the parts' outline), for aiming; they don't collide
        int mask = e.getSideMask();
        for (int f = 0; f < 6; f++)
            if ((mask & (1 << f)) != 0) {
                int[] b = VoxelNode.plate(f);
                s = Shapes.or(s, box(Math.max(0, b[0] - 2), Math.max(0, b[1] - 1), Math.max(0, b[2] - 2), Math.min(16, b[3] + 2), Math.min(16, b[4] + 1), Math.min(16, b[5] + 2)));
            }
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
        if (level.isClientSide())
            return e.parts().size() + Integer.bitCount(e.getSideMask()) <= 1 && super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        if (!(e.getNode() instanceof VoxelNode node)) return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        int index = -1, el = -1;
        var pick = player.pick(player.blockInteractionRange() + 1, 1f, false);
        if (pick instanceof net.minecraft.world.phys.BlockHitResult bh && bh.getBlockPos().equals(pos)) {
            var h = bh.getLocation();
            double x = h.x - pos.getX(), y = h.y - pos.getY(), z = h.z - pos.getZ();
            el = node.elementAt(x, y, z, 0);
            if (el < 0) index = node.partAt(x, y, z);
            if (el < 0 && index < 0) el = node.elementAt(x, y, z, 2);
        }
        if (el < 0 && index < 0) index = node.parts.size() - 1;
        if (el < 0 && index < 0)   // (no part: an element)
            for (int f = 0; f < 6 && el < 0; f++) if (node.sideElementList[f] != null) el = f;
        if (el < 0 && index < 0) return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        node.breakingByPlayer = true;
        try {
            if (el >= 0) {
                if (!(player instanceof net.minecraft.server.level.ServerPlayer sp) || !node.playerAskToBreakSubBlock(sp, mods.eln.misc.Direction.fromInt(el))) return false;
            } else if (!node.playerAskToBreakPart(player, index)) return false;
        } finally {
            node.breakingByPlayer = false;
        }
        if (!node.isEmptyNode()) return false;   // (others remain)
        return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
    }
}
