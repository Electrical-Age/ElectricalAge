package mods.eln.node.voxel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A safety valve, purge valve or breather (SafetyValvePart's modes): right-click a straight stretch of pipe; its outlet
 * points out of the pipe's most open side (up if it can).
 */
public class SafetyValveItem extends Item implements PipeTool {
    /** SafetyValvePart.SAFETY, PURGE or BREATHER. */
    final int mode;

    public SafetyValveItem(Properties p) {
        this(p, SafetyValvePart.SAFETY);
    }

    public SafetyValveItem(Properties p, int mode) {
        super(p);
        this.mode = mode;
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
            err = n.addSafetyValveNear(hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ(), mode);
        else err = "fit it on a pipe";
        if (err != null) {
            if (player != null) player.sendOverlayMessage(Component.literal("Can't fit a " + switch (mode) {
                case SafetyValvePart.PURGE -> "purge valve";
                case SafetyValvePart.BREATHER -> "breather valve";
                default -> "safety valve";
            } + ": " + err));
            return InteractionResult.FAIL;
        }
        if (player == null || !player.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    /** Its tooltip lines (client ItemTooltips). */
    public java.util.List<String> tooltip() {
        if (mode == SafetyValvePart.BREATHER)
            // (Neon's wording; Baughn 2026-11-24: "also a good way to lose natural gas", a flare or gas tank the fix)
            return java.util.List.of("Lets gas out and air in to hold outside pressure, never liquid.",
                "Vents a receiver tank so it neither pressurizes nor pulls a vacuum.",
                "Natural gas escapes too; pipe it to a flare or a gas tank instead.",
                "Fits on top of a pipe or tank stub; on a pipe's side, liquid shuts it.");
        return mode == SafetyValvePart.PURGE
            ? java.util.List.of("Lets gas out of a pipe above its set pressure, never liquid.",
                "Bleeds a crude tank's gas or the air of a filling run (set to 1.1 bar at first).",
                "Fits on a straight stretch of pipe; right-click it with an empty hand to set it.")
            : java.util.List.of("Lets out whatever is in a pipe above its set pressure.",
                "Fits on a straight stretch of pipe; right-click it with an empty hand to set it.");
    }
}
