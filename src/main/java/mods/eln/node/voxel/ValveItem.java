package mods.eln.node.voxel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** A pipe valve: right-click a pipe running straight through a block's middle to fit it there. */
public class ValveItem extends Item {
    public ValveItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var pos = ctx.getClickedPos();
        String err;
        if (level.getBlockEntity(pos) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n) err = n.addValve();
        else err = "fit it on a pipe";
        var player = ctx.getPlayer();
        if (err != null) {
            if (player != null) player.sendOverlayMessage(Component.literal("Can't fit a valve: " + err));
            return InteractionResult.FAIL;
        }
        if (player == null || !player.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
