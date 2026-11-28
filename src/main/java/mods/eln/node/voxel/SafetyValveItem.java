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
        var look = ValveItem.lookedAt(level, pos, player);
        if (look != null) {
            pos = look.getBlockPos();
            hit = look.getLocation();
        }
        String err;
        if (level.getBlockEntity(pos) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n)
            err = n.addSafetyValveNear(n.aimed(VoxelNode.eyeOf(player), hit).part(), hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ(), mode);
        else err = mods.eln.i18n.I18N.tr("must fit on a pipe");
        if (err != null) {
            if (player != null) player.sendOverlayMessage(Component.literal(switch (mode) {
                case SafetyValvePart.PURGE -> mods.eln.i18n.I18N.tr("Can't fit purge valve: %1$", err);
                case SafetyValvePart.BREATHER -> mods.eln.i18n.I18N.tr("Can't fit breather valve: %1$", err);
                default -> mods.eln.i18n.I18N.tr("Can't fit safety valve: %1$", err);
            }));
            return InteractionResult.FAIL;
        }
        if (player == null || !player.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    /** Its tooltip lines (client ItemTooltips). */
    public java.util.List<String> tooltip() {
        var list = new java.util.ArrayList<String>();
        if (mode == SafetyValvePart.BREATHER) {
            list.add(mods.eln.i18n.I18N.tr("Vents gas and admits air to hold atmospheric pressure (blocks liquid)."));
            mods.eln.client.ItemTooltips.detail(list, mods.eln.i18n.I18N.tr("Prevents a receiver tank from pressurising or pulling a vacuum."));
            return list;
        }
        if (mode == SafetyValvePart.PURGE) {
            list.add(mods.eln.i18n.I18N.tr("Vents gas above its set pressure (blocks liquid)."));
            mods.eln.client.ItemTooltips.detail(list, mods.eln.i18n.I18N.tr("Right-click with empty hand to set pressure."));
            return list;
        }
        list.add(mods.eln.i18n.I18N.tr("Relieves pipe pressure above its set point."));
        mods.eln.client.ItemTooltips.detail(list, mods.eln.i18n.I18N.tr("Right-click with empty hand to set pressure."));
        return list;
    }
}
