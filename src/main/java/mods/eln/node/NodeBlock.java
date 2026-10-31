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

    /**
     * The light each node block emits, per level and position, for the light engine: it asks from its own thread, where
     * reading the chunk's block entities or the node list races the server thread (2026-10-18: a mass block change,
     * the cable converter, threw in the light worker, which then died and the server hung on save). Written on the
     * owning thread (Node.setLightValue, the client's publish), read here.
     */
    static final java.util.Map<Object, java.util.concurrent.ConcurrentHashMap<Long, Integer>> LIGHT =
        java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static void setLight(net.minecraft.world.level.Level level, BlockPos pos, int light) {
        if (level == null) return;
        var m = LIGHT.computeIfAbsent(level, k -> new java.util.concurrent.ConcurrentHashMap<>());
        if (light <= 0) m.remove(pos.asLong());
        else m.put(pos.asLong(), light);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        Object key = level instanceof net.minecraft.world.level.chunk.LevelChunk c ? c.getLevel() : level;
        var m = LIGHT.get(key);
        Integer v = m == null ? null : m.get(pos.asLong());
        return v == null ? 0 : v;
    }

    /** The light comes from the block entity (getLightEmission), not the state: chunk light scans must ask. */
    @Override
    public boolean hasDynamicLightEmission(BlockState state) {
        return true;
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
        // a pipe kit routes from and to devices' ports: the kit's use, not the device's (AdventurAgent)
        if (stack.getItem() instanceof mods.eln.node.voxel.PipeTool) return InteractionResult.PASS;
        return activate(level, pos, player, hit);
    }

    /** Middle-click (the server picks, 26.x): the device, cable or pipe under the crosshair, as its item (#161). */
    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        NodeBlockEntity e = tileAt(level, pos, NodeBlockEntity.class);
        if (e == null || player == null || !(e.getNode() instanceof Node n)) return ItemStack.EMPTY;
        var pick = player.pick(player.blockInteractionRange() + 1, 1f, false);
        return n.pickItem(pick instanceof BlockHitResult bh && bh.getBlockPos().equals(pos) ? bh : null, level);
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
