package mods.eln.node.voxel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** A pipe sensor (PipeSensorPart): right-click a straight stretch of pipe; its dial points out of the pipe's most open side. */
public class PipeSensorItem extends Item implements PipeTool {
    public final int sensing;

    public PipeSensorItem(Properties p, int sensing) {
        super(p);
        this.sensing = sensing;
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
            err = n.addSensorNear(hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ(), sensing);
        else err = "fit it on a pipe";
        if (err != null) {
            if (player != null) player.sendOverlayMessage(Component.literal("Can't fit a sensor: " + err));
            return InteractionResult.FAIL;
        }
        if (player == null || !player.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    /** Its tooltip lines (client ItemTooltips). */
    public java.util.List<String> tooltip() {
        return java.util.List.of(sensing == PipeSensorPart.PRESSURE ? "Reads a pipe's pressure"
                : sensing == PipeSensorPart.TEMPERATURE ? "Reads a pipe's temperature" : "Reads how wet a pipe's steam is",
            "0-50 V out on a signal cable laid into its block", "Right-click it with an empty hand to step its full scale");
    }
}
