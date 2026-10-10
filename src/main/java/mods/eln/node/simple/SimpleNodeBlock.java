package mods.eln.node.simple;

import mods.eln.misc.DescriptorBase;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A full-block device with its own node (1.12 SimpleNodeBlock, e.g. the energy converters): the node is a
 * {@link SimpleNode}, made by the {@link SimpleNodeItem} when placed; the block entity forwards world events to it.
 */
public abstract class SimpleNodeBlock extends Block implements EntityBlock {

    protected SimpleNodeBlock(Properties properties) {
        super(properties);
    }

    String descriptorKey;

    public SimpleNodeBlock setDescriptorKey(String descriptorKey) {
        this.descriptorKey = descriptorKey;
        return this;
    }

    public SimpleNodeBlock setDescriptor(DescriptorBase descriptor) {
        this.descriptorKey = descriptor.descriptorKey;
        return this;
    }

    Direction getFrontForPlacement(LivingEntity e) {
        return Utils.entityLivingViewDirection(e).getInverse();
    }

    protected abstract SimpleNode newNode();

    /** 1.7.10 rendered these as normal blocks (1.12 TODO: models). */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (l, p, s, be) -> {
            if (be instanceof SimpleNodeEntity e) e.update();
        };
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            SimpleNodeEntity e = NodeBlock.tileAt(level, pos, SimpleNodeEntity.class);
            SimpleNode node = e == null ? null : e.getNode();
            if (node != null && player instanceof net.minecraft.server.level.ServerPlayer sp) node.removedByPlayer = sp;
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) {
            SimpleNodeEntity e = NodeBlock.tileAt(level, pos, SimpleNodeEntity.class);
            if (e != null) e.onNeighborBlockChange();
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player, hit);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 1.7.10 had one hand; EA code reads the main hand, so only react once (main hand).
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        return activate(level, pos, player, hit);
    }

    protected InteractionResult activate(Level level, BlockPos pos, Player player, BlockHitResult hit) {
        SimpleNodeEntity e = NodeBlock.tileAt(level, pos, SimpleNodeEntity.class);
        if (e == null) return InteractionResult.PASS;
        Direction side = Direction.fromIntMinecraftSide(hit.getDirection().get3DDataValue());
        float vx = (float) (hit.getLocation().x - pos.getX()), vy = (float) (hit.getLocation().y - pos.getY()),
            vz = (float) (hit.getLocation().z - pos.getZ());
        return e.onBlockActivated(player, side, vx, vy, vz) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
