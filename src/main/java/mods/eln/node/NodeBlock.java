package mods.eln.node;

import mods.eln.misc.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * Base of EA's node blocks (1.12 NodeBlock): the block forwards world events to its NodeBlockEntity, which forwards
 * them to the node. Light comes from the BE (NeoForge position-aware light emission). Rendering: no block model in
 * P1/P2 (INVISIBLE, as in 1.12); models come in P3.
 */
public abstract class NodeBlock extends Block implements EntityBlock {

    protected NodeBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    public static <T> T tileAt(BlockGetter world, BlockPos pos, Class<T> type) {
        if (world == null || pos == null) return null;
        BlockEntity te = world.getBlockEntity(pos);
        return type.isInstance(te) ? type.cast(te) : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        NodeBlockEntity e = tileAt(level, pos, NodeBlockEntity.class);
        return e == null ? 0 : e.getLightValue();
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction side) {
        NodeBlockEntity e = tileAt(level, pos, NodeBlockEntity.class);
        if (e == null) return 0;
        // 1.12 getWeakPower(side) took the side as seen from the neighbour, like vanilla
        return e.isProvidingWeakPower(Direction.fromIntMinecraftSide(side.get3DDataValue()));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) {
            NodeBlockEntity e = tileAt(level, pos, NodeBlockEntity.class);
            if (e != null) e.onNeighborBlockChange();
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player, hit);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        return activate(level, pos, player, hit);
    }

    protected InteractionResult activate(Level level, BlockPos pos, Player player, BlockHitResult hit) {
        NodeBlockEntity e = tileAt(level, pos, NodeBlockEntity.class);
        if (e == null) return InteractionResult.PASS;
        Direction side = Direction.fromIntMinecraftSide(hit.getDirection().get3DDataValue());
        float vx = (float) (hit.getLocation().x - pos.getX()), vy = (float) (hit.getLocation().y - pos.getY()),
            vz = (float) (hit.getLocation().z - pos.getZ());
        return e.onBlockActivated(player, side, vx, vy, vz) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
