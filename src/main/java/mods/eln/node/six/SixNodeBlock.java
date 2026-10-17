package mods.eln.node.six;

import mods.eln.misc.Direction;
import mods.eln.node.NodeBlock;
import mods.eln.registry.ElnRegistry;
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
 * The block holding up to six wall-mounted devices (1.12 SixNodeBlock), one per face. Shape: a thin plate on each
 * occupied face (1.12 used per-element boxes; cables don't collide). Placement is done by SixNodeItem.
 * Waterloggable (26.x, Baughn): cables and the other wall devices can sit in water. Electrically nothing changes yet
 * (shorting is planned).
 */
public class SixNodeBlock extends NodeBlock implements net.minecraft.world.level.block.SimpleWaterloggedBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty WATERLOGGED =
        net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;
    private static final VoxelShape[] PLATES = new VoxelShape[6];

    static {
        double t = 3; // 3/16 thick
        PLATES[Direction.XN.getInt()] = box(0, 0, 0, t, 16, 16);
        PLATES[Direction.XP.getInt()] = box(16 - t, 0, 0, 16, 16, 16);
        PLATES[Direction.YN.getInt()] = box(0, 0, 0, 16, t, 16);
        PLATES[Direction.YP.getInt()] = box(0, 16 - t, 0, 16, 16, 16);
        PLATES[Direction.ZN.getInt()] = box(0, 0, 0, 16, 16, t);
        PLATES[Direction.ZP.getInt()] = box(0, 0, 16 - t, 16, 16, 16);
    }

    public SixNodeBlock(Properties properties) {
        super(properties);
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

    /** The state to place at pos: waterlogged if a water source is there. */
    public BlockState stateFor(Level world, BlockPos pos) {
        var f = world.getFluidState(pos);
        return defaultBlockState().setValue(WATERLOGGED, f.is(net.minecraft.world.level.material.Fluids.WATER) && f.isSource());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SixNodeEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        SixNodeEntity e = tileAt(level, pos, SixNodeEntity.class);
        if (e == null) return Shapes.block();
        VoxelShape s = Shapes.empty();
        int mask = e.getSideMask();
        for (int i = 0; i < 6; i++) if ((mask & (1 << i)) != 0) s = Shapes.or(s, PLATES[i]);
        return s.isEmpty() ? Shapes.block() : s;
    }

    /**
     * The element clicked is the one whose plate holds the hit point (1.12's collisionRayTrace reported the element's
     * side as the hit side; 26.x reports the face of the shape that was hit: the top of a floor plate says UP).
     */
    @Override
    protected net.minecraft.world.InteractionResult activate(net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        SixNodeEntity e = tileAt(level, pos, SixNodeEntity.class);
        if (e == null) return net.minecraft.world.InteractionResult.PASS;
        Direction side = sideAt(e, pos, hit);
        if (side == null) return net.minecraft.world.InteractionResult.PASS;
        double x = hit.getLocation().x - pos.getX(), y = hit.getLocation().y - pos.getY(), z = hit.getLocation().z - pos.getZ();
        return e.onBlockActivated(player, side, (float) x, (float) y, (float) z)
            ? net.minecraft.world.InteractionResult.SUCCESS : net.minecraft.world.InteractionResult.PASS;
    }

    /** The element whose plate holds the hit point, or null. */
    static @Nullable Direction sideAt(SixNodeEntity e, BlockPos pos, net.minecraft.world.phys.BlockHitResult hit) {
        double x = hit.getLocation().x - pos.getX(), y = hit.getLocation().y - pos.getY(), z = hit.getLocation().z - pos.getZ();
        int mask = e.getSideMask();
        Direction side = null;
        Direction facing = Direction.fromIntMinecraftSide(hit.getDirection().getOpposite().get3DDataValue());
        if ((mask & (1 << facing.getInt())) != 0 && PLATES[facing.getInt()].bounds().inflate(0.002).contains(x, y, z)) side = facing;
        for (int i = 0; side == null && i < 6; i++)
            if ((mask & (1 << i)) != 0 && PLATES[i].bounds().inflate(0.002).contains(x, y, z)) side = Direction.fromInt(i);
        return side;
    }

    /**
     * Breaking takes only the element the player is looking at (1.12 removedByPlayer: the 26.x port lost this, so a
     * pickaxe removed every face of the block and nothing dropped). Its item and inventory drop (SixNodeElement.destroy);
     * the block stays while other faces remain.
     */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
                                       net.minecraft.world.item.ItemStack tool, boolean willHarvest, net.minecraft.world.level.material.FluidState fluid) {
        SixNodeEntity e = tileAt(level, pos, SixNodeEntity.class);
        if (e == null || player == null) return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        if (level.isClientSide())   // (no prediction while other faces stay: the server's update follows)
            return Integer.bitCount(e.getSideMask()) <= 1 && super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        if (!(e.getNode() instanceof SixNode node) || !(player instanceof net.minecraft.server.level.ServerPlayer sp))
            return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        Direction side = null;
        var pick = player.pick(player.blockInteractionRange() + 1, 1f, false);
        if (pick instanceof net.minecraft.world.phys.BlockHitResult bh && bh.getBlockPos().equals(pos)) side = sideAt(e, pos, bh);
        if (side == null) {   // (no element under the crosshair: take the first one)
            for (int i = 0; i < 6 && side == null; i++) if ((e.getSideMask() & (1 << i)) != 0) side = Direction.fromInt(i);
            if (side == null) return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        }
        if (!node.playerAskToBreakSubBlock(sp, side)) return false;
        if (node.getIfSideRemain()) return false;
        return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** The neighbour on that side can hold a wall device (1.12: opaque cube). */
    public boolean getIfOtherBlockIsSolid(Level world, BlockPos pos, Direction direction) {
        BlockPos o = pos.relative(direction.toEnumFacing());
        BlockState s = world.getBlockState(o);
        return !s.isAir() && s.isSolidRender();
    }

    public static SixNodeBlock get() {
        return ElnRegistry.SIX_NODE_BLOCK.get();
    }
}
