package mods.eln.node.voxel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** A pipe valve: right-click a pipe running straight through a block's middle to fit it there. */
public class ValveItem extends Item implements PipeTool {
    public ValveItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var pos = ctx.getClickedPos();
        var player = ctx.getPlayer();
        // (thin pipes are easy to miss: if the click went past, the pipe the player looks at)
        if (!(level.getBlockEntity(pos) instanceof VoxelNodeEntity) && player != null
            && player.pick(player.blockInteractionRange(), 1f, false) instanceof net.minecraft.world.phys.BlockHitResult bh
            && level.getBlockEntity(bh.getBlockPos()) instanceof VoxelNodeEntity) pos = bh.getBlockPos();
        String err;
        if (level.getBlockEntity(pos) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n) err = n.addValve();
        else {
            err = "fit it on a pipe";
            mods.eln.misc.Utils.println("Valve: no pipe at " + pos.toShortString() + ": " + level.getBlockState(pos));
        }
        if (err != null) {
            if (player != null) player.sendOverlayMessage(Component.literal("Can't fit a valve: " + err));
            return InteractionResult.FAIL;
        }
        if (player == null || !player.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
