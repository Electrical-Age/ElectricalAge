package mods.eln.node.simple;

import mods.eln.misc.Coordonate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The item of a {@link SimpleNodeBlock} (1.12 SimpleNodeItem, an ItemBlock): placing makes the node first (front =
 * opposite of the player's view), then the block, as 1.12 placeBlockAt.
 */
public class SimpleNodeItem extends BlockItem {
    final SimpleNodeBlock block;

    public SimpleNodeItem(SimpleNodeBlock b, Properties properties) {
        super(b, properties);
        block = b;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos();
        if (!world.getBlockState(pos).canBeReplaced()) pos = pos.relative(ctx.getClickedFace());
        boolean ok = placeAt(ctx.getItemInHand(), ctx.getPlayer(), world, pos) != null;
        if (ok && ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
        return ok ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /** 1.12 placeBlockAt; returns the node, or null if it can't be placed. */
    public SimpleNode placeAt(ItemStack stack, Player player, Level world, BlockPos pos) {
        if (!world.getBlockState(pos).canBeReplaced()) return null;
        SimpleNode node = block.newNode();
        node.setDescriptorKey(block.descriptorKey);
        node.onBlockPlacedBy(new Coordonate(pos, world), block.getFrontForPlacement(player), player, stack);
        if (!world.setBlock(pos, block.defaultBlockState(), 3)) {
            node.onBreakBlock();
            return null;
        }
        return node;
    }
}
