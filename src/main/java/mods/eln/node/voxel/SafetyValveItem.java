package mods.eln.node.voxel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** A safety valve: right-click a straight stretch of pipe; its outlet points out of the pipe's most open side. */
public class SafetyValveItem extends Item implements PipeTool {
    public SafetyValveItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var pos = ctx.getClickedPos();
        var player = ctx.getPlayer();
        var hit = ctx.getClickLocation();
        String err;
        if (level.getBlockEntity(pos) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n)
            err = n.addSafetyValveNear(hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ());
        else err = "fit it on a pipe";
        if (err != null) {
            if (player != null) player.sendOverlayMessage(Component.literal("Can't fit a safety valve: " + err));
            return InteractionResult.FAIL;
        }
        if (player == null || !player.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
