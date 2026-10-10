package mods.eln.node.transparent;

import mods.eln.generic.IDescriptorItem;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * The item of one transparent-node device (1.12: a sub-item of TransparentNodeItem). Places the device's block at the
 * clicked face + the descriptor's spawn delta (rotated by the front), with its ghosts, as in 1.12 placeBlockAt.
 */
public class TransparentNodeItem extends Item implements IDescriptorItem {
    public final TransparentNodeDescriptor descriptor;
    private final Supplier<Block> block;

    public TransparentNodeItem(Properties properties, TransparentNodeDescriptor descriptor, Supplier<Block> block) {
        super(properties);
        this.descriptor = descriptor;
        this.block = block;
    }

    @Override
    public Object getEaDescriptor() {
        return descriptor;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Direction direction = Direction.fromIntMinecraftSide(ctx.getClickedFace().get3DDataValue()).getInverse();
        boolean ok = placeAt(ctx.getItemInHand(), ctx.getPlayer(), world, pos, direction) != null;
        if (ok && ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
        return ok ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /** 1.12 placeBlockAt; returns the node, or null if it can't be placed. */
    public TransparentNode placeAt(ItemStack stack, Player player, Level world, BlockPos pos, Direction direction) {
        Direction front = descriptor.getFrontFromPlace(direction, player);
        int[] v = new int[]{descriptor.getSpawnDeltaX(), descriptor.getSpawnDeltaY(), descriptor.getSpawnDeltaZ()};
        front.rotateFromXN(v);
        BlockPos at = pos.offset(v[0], v[1], v[2]);
        if (!world.getBlockState(at).canBeReplaced()) return null;
        Coordonate coord = new Coordonate(at, world);

        String error;
        if ((error = descriptor.checkCanPlace(coord, front)) != null) {
            if (player != null) Utils.sendMessage(player, error);
            return null;
        }

        GhostGroup ghostgroup = descriptor.getGhostGroup(front);
        if (ghostgroup != null) ghostgroup.plot(coord, coord, descriptor.getGhostGroupUuid());

        TransparentNode node = new TransparentNode();
        node.onBlockPlacedBy(coord, front, player, stack);
        world.setBlock(at, block.get().defaultBlockState(), 3);
        node.checkCanStay(true);
        return node;
    }
}
