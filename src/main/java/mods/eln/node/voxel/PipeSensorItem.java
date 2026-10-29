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
        return java.util.List.of(sensing == PipeSensorPart.PRESSURE ? "Reads a pipe's pressure (absolute)"
                : sensing == PipeSensorPart.TEMPERATURE ? "Reads a pipe's temperature" : "Reads how wet a pipe's steam is (% of its water that is liquid)",
            "0-50 V out on a signal cable laid into its block", "(click the sensor with a cable kit: it lays to the floor under it,",
            "so the block needs a floor to lie on)", "Right-click it (empty hand) to step its full scale");
    }
}
