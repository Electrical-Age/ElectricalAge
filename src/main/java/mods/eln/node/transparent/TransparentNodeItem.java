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

    /** Whether the device may take pos: in the build height and the world border, and the player may build there. */
    static boolean mayBuild(Player player, Level world, BlockPos pos, Direction direction, ItemStack stack) {
        if (world.isOutsideBuildHeight(pos) || !world.getWorldBorder().isWithinBounds(pos)) return false;
        return player == null || world.mayInteract(player, pos) && player.mayUseItemAt(pos, direction.toEnumFacing(), stack);
    }

    /** 1.12 placeBlockAt; returns the node, or null if it can't be placed. */
    public TransparentNode placeAt(ItemStack stack, Player player, Level world, BlockPos pos, Direction direction) {
        Direction front = descriptor.getFrontFromPlace(direction, player);
        int[] v = new int[]{descriptor.getSpawnDeltaX(), descriptor.getSpawnDeltaY(), descriptor.getSpawnDeltaZ()};
        front.rotateFromXN(v);
        BlockPos at = pos.offset(v[0], v[1], v[2]);
        if (!world.getBlockState(at).canBeReplaced()) return null;
        // (where the block can't go, refused before the node exists: past the build height a node was made, holding what
        // the item carried, with no block, and the item was used up)
        if (world.isOutsideBuildHeight(at)) {
            if (player != null) player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("build.tooHigh", world.getMaxY()));
            return null;
        }
        if (!mayBuild(player, world, at, direction, stack)) return null;
        Coordonate coord = new Coordonate(at, world);
        // (and its ghosts' cells: checkCanPlace sees their room and the build height, not the player's rights)
        GhostGroup ghostgroup = descriptor.getGhostGroup(front);
        if (ghostgroup != null) for (BlockPos g : ghostgroup.positions(coord)) if (!mayBuild(player, world, g, direction, stack)) return null;

        String error;
        if ((error = descriptor.checkCanPlace(coord, front)) != null) {
            if (player != null) player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(error));
            return null;
        }

        if (ghostgroup != null) ghostgroup.plot(coord, coord, descriptor.getGhostGroupUuid());

        TransparentNode node = new TransparentNode();
        node.onBlockPlacedBy(coord, front, player, stack);
        Block b = block.get();
        if (!world.setBlock(at, b instanceof TransparentNodeBlock tb ? tb.placedState(front, world, at) : b.defaultBlockState(), 3)) {
            // (the block didn't go in after all: the node goes without drops, and the player gets back its item as it would
            // drop now, with what it holds (a tank that joined a box takes its share back out), so nothing is lost or made,
            // with block drops off too)
            ItemStack back = node.element == null ? ItemStack.EMPTY : node.element.getDropItemStack();
            if (ghostgroup != null) ghostgroup.erase(coord);
            node.removeUnplaced();
            if (player == null || !player.isCreative()) {
                stack.shrink(1);
                if (player != null) {
                    if (!back.isEmpty() && !player.getInventory().add(back)) net.minecraft.world.Containers.dropItemStack(world, player.getX(), player.getY(), player.getZ(), back);
                }
                else if (!back.isEmpty()) world.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(world, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, back));
            }
            return null;
        }
        node.checkCanStay(true);
        return node;
    }
}
