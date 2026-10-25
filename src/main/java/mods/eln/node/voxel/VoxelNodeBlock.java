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
public class VoxelNodeBlock extends NodeBlock implements net.minecraft.world.level.block.SimpleWaterloggedBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty WATERLOGGED =
        net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

    public VoxelNodeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    // waterloggable, like the six-node block (cables and devices under water; a converted underwater block stays wet)
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
        // an imitated fence or wall keeps its connections (Mimic)
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode v && v.host != null) {
            BlockState h = v.host.updateShape(level, ticks, pos, dir, neighbourPos, neighbour, random);
            if (h != v.host && !h.isAir()) {
                v.host = h;
                v.elementsChanged();
            }
        }
        return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbour, random);
    }

    /** The voxel block to imitate host with: the variant carrying its fence or wall tags (neighbours join it), else the
     *  plain one; waterlogged as the host was. */
    public static BlockState forHost(BlockState host) {
        net.minecraft.world.level.block.Block b = mods.eln.registry.ElnRegistry.VOXEL_NODE_BLOCK.get();
        if (host != null) {
            if (host.is(net.minecraft.tags.BlockTags.WOODEN_FENCES)) b = mods.eln.registry.ElnRegistry.VOXEL_NODE_WOODEN_FENCE.get();
            else if (host.is(net.minecraft.tags.BlockTags.FENCES)) b = mods.eln.registry.ElnRegistry.VOXEL_NODE_FENCE.get();
            else if (host.is(net.minecraft.tags.BlockTags.WALLS) || host.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock)
                b = mods.eln.registry.ElnRegistry.VOXEL_NODE_WALL.get();
        }
        boolean wet = host != null && host.getOptionalValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED).orElse(false);
        return b.defaultBlockState().setValue(WATERLOGGED, wet);
    }

    /** The state to place at pos: waterlogged if a water source is there. */
    public BlockState stateFor(Level world, BlockPos pos) {
        var f = world.getFluidState(pos);
        return defaultBlockState().setValue(WATERLOGGED, f.is(net.minecraft.world.level.material.Fluids.WATER) && f.isSource());
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
        java.util.List<int[]> boxes = new java.util.ArrayList<>();
        for (VoxelPart p : parts) boxes.addAll(p.boxes());
        return union(boxes, grow);
    }

    /** The union of boxes (voxels, grown by grow, not past the block), optimised once at the end. */
    static VoxelShape union(java.util.List<int[]> boxes, int grow) {
        VoxelShape s = Shapes.empty();
        for (int[] b : boxes)
            s = Shapes.joinUnoptimized(s, box(Math.max(0, b[0] - grow), Math.max(0, b[1] - grow), Math.max(0, b[2] - grow),
                Math.min(16, b[3] + grow), Math.min(16, b[4] + grow), Math.min(16, b[5] + grow)), net.minecraft.world.phys.shapes.BooleanOp.OR);
        return s.optimize();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelNodeEntity e = tileAt(level, pos, VoxelNodeEntity.class);
        if (e == null) return Shapes.block();
        var c = e.shapes();
        BlockState host = e.host();
        VoxelShape s;
        if (host == null) s = c.outline();
        else if (e.isCarved()) s = c.outlineCarved();
        else s = Shapes.or(c.outline(), host.getShape(level, pos, context));   // (the imitated block: Mimic)
        return s.isEmpty() ? Shapes.block() : s;
    }

    /** The parts' outline (grown) and the model boxes of six-node elements hosted here (as they are clicked; they don't
     *  collide). deviceBoxes: per face (EA Direction int), null where there is none. */
    static VoxelShape outlineOf(java.util.List<VoxelPart> parts, int[][] deviceBoxes) {
        java.util.List<int[]> boxes = new java.util.ArrayList<>();
        for (VoxelPart p : parts)
            for (int[] b : p.boxes())
                boxes.add(new int[]{Math.max(0, b[0] - 2), Math.max(0, b[1] - 2), Math.max(0, b[2] - 2), Math.min(16, b[3] + 2), Math.min(16, b[4] + 2), Math.min(16, b[5] + 2)});
        for (int[] b : deviceBoxes) if (b != null) boxes.add(b);
        return union(boxes, 0);
    }

    /** Drawn by its block model (client/render/VoxelModel) in the chunk mesh. */
    @Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelNodeEntity e = tileAt(level, pos, VoxelNodeEntity.class);
        if (e == null) return Shapes.empty();
        BlockState host = e.host();
        var c = e.shapes();
        if (host == null) return c.collision();
        if (e.isCarved()) return c.collisionCarved();
        return Shapes.or(c.collision(), host.getCollisionShape(level, pos, context));
    }

    // ---- an imitated block (Mimic): its sound, its tool and speed when aimed at, its connections kept up to date

    @Override
    public net.minecraft.world.level.block.SoundType getSoundType(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos,
                                                                 @Nullable net.minecraft.world.entity.Entity entity) {
        VoxelNodeEntity e = tileAt(level, pos, VoxelNodeEntity.class);
        BlockState host = e == null ? null : e.host();
        return host != null ? host.getSoundType(level, pos, entity) : super.getSoundType(state, level, pos, entity);
    }

    /** What the player aims at in this block: 0 nothing, 1 a part or device, 2 the imitated block. */
    static int aimed(VoxelNodeEntity e, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        var pick = player.pick(player.blockInteractionRange() + 1, 1f, false);
        if (!(pick instanceof net.minecraft.world.phys.BlockHitResult bh) || !bh.getBlockPos().equals(pos)) return 0;
        var h = bh.getLocation();
        double x = h.x - pos.getX(), y = h.y - pos.getY(), z = h.z - pos.getZ();
        for (VoxelPart p : e.parts()) for (int[] b : p.boxes()) if (VoxelPart.contains(b, x, y, z, 0.05)) return 1;
        for (int[] b : e.deviceBoxes()) if (b != null && VoxelPart.contains(b, x, y, z, 0.05)) return 1;
        BlockState host = e.host();
        if (host != null) for (int[] b : e.hostBoxes()) if (VoxelPart.contains(b, x, y, z, 0.05)) return 2;
        return 1;
    }

    @Override
    protected float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player, BlockGetter level, BlockPos pos) {
        VoxelNodeEntity e = tileAt(level, pos, VoxelNodeEntity.class);
        BlockState host = e == null ? null : e.host();
        if (host != null && aimed(e, pos, player) == 2) return host.getDestroyProgress(player, level, pos);
        return super.getDestroyProgress(state, player, level, pos);
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
        // nothing of this block under the crosshair (mining went on after what was aimed at went): nothing goes
        // (AdventurAgent: a fallback took the cable left in the block, and with it the block)
        if (el < 0 && index < 0) {
            // the imitated block itself: it drops; the parts stay
            if (node.host != null) {
                if (aimed(e, pos, player) == 2) node.clearHost(player);
                return false;
            }
            // only faces this version could not load are left (no part, plate or host to aim at): the block goes, as a
            // six-node block's does; onBreakBlock logs each discarded face. Else the block could never be broken.
            if (node.parts.isEmpty() && e.getSideMask() == 0)
                return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
            return false;
        }
        node.breakingByPlayer = true;
        try {
            if (el >= 0) {
                if (!(player instanceof net.minecraft.server.level.ServerPlayer sp) || !node.playerAskToBreakSubBlock(sp, mods.eln.misc.Direction.fromInt(el))) return false;
            } else if (!node.playerAskToBreakPart(player, index)) return false;
        } finally {
            node.breakingByPlayer = false;
        }
        if (!node.isEmptyNode()) return false;   // (others remain)
        if (node.host != null) {   // (the last part went: the block is the stair again)
            node.removeEmptyBlock();
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
    }
}
