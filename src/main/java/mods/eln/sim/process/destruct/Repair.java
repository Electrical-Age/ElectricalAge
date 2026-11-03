package mods.eln.sim.process.destruct;

import mods.eln.Eln;
import mods.eln.misc.Utils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;

/** Repairing burnt devices: right-click with the repair part. */
public final class Repair {
    private Repair() {
    }

    /**
     * The repair part: the descriptor's repairPart if it names one, else by kind of device. Cables, HV gear and
     * batteries are replaced (one of the same item); everything else gets the part that failed, cheaper than the
     * device (Baughn: a stone heat furnace took a Machine Block, which cost more than the furnace). An overheated
     * machine gets a Copper Thermal Cable, which every heat device is built with.
     */
    public static ItemStack defaultPart(IBurnable b) {
        Failure.Kind kind = b.getBurntKind();
        if (kind == null) return ItemStack.EMPTY;
        String named = descriptorPart(b);
        if (named != null) {
            ItemStack s = Eln.findItemStack(named);
            if (!Utils.isEmpty(s)) return s;
        }
        switch (kind) {
            case ELECTRONICS:
                return Eln.findItemStack("Cheap Chip");
            case MACHINE:
                return b.getBurnt() != null && b.getBurnt().electrical() ? Eln.findItemStack("Cheap Chip") : Eln.findItemStack("Copper Thermal Cable");
            case TRANSFORMER:
                return Eln.findItemStack("Cheap Ferromagnetic Core");
            case CAPACITOR:
                return Eln.findItemStack("Dielectric");
            case ROTATING:
                return Eln.findItemStack("Electrical Motor");
            case CABLE:
            case BATTERY:
            case HV_LINE:
            default:
                return sameItem(b);
        }
    }

    /** The repairPart of the device's descriptor, or null. */
    @javax.annotation.Nullable
    static String descriptorPart(IBurnable b) {
        if (b instanceof mods.eln.node.transparent.TransparentNodeElement t && t.transparentNodeDescriptor != null)
            return t.transparentNodeDescriptor.repairPart;
        if (b instanceof mods.eln.node.six.SixNodeElement e && e.sixNodeElementDescriptor != null)
            return e.sixNodeElementDescriptor.repairPart;
        return null;
    }

    public static ItemStack sameItem(IBurnable b) {
        ItemStack s = b.getDropItemStack();
        if (Utils.isEmpty(s)) return ItemStack.EMPTY;
        s = s.copy();
        s.setCount(1);
        mods.eln.generic.ItemNbt.set(s, null);
        return s;
    }

    /**
     * Right-click on a burnt device. With the repair part in hand: consume one, repair, return true. Otherwise say
     * what's wrong and what repairs it, and return false; the node then does nothing else (Baughn: right-click on a
     * damaged machine should only show the repair message; no GUI). Its contents drop when it is broken.
     */
    public static boolean onActivated(IBurnable b, Player player) {
        if (player.level().isClientSide() || !b.isBurnt()) return false;
        ItemStack part = b.getRepairPart();
        ItemStack held = player.getMainHandItem();
        if (!Utils.isEmpty(part) && !Utils.isEmpty(held) && ItemStack.isSameItem(held, part)) {
            boolean creative = player instanceof ServerPlayer && Utils.isCreative((ServerPlayer) player);
            if (!creative) held.shrink(1);
            b.repair();
            BlockPos pos = new BlockPos(b.getBurnableCoordonate().x, b.getBurnableCoordonate().y, b.getBurnableCoordonate().z);
            player.level().playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.4f);
            Utils.sendMessage(player, "Repaired.");
            return true;
        }
        Utils.sendMessage(player, describe(b));
        return false;
    }

    public static String describe(IBurnable b) {
        return "Burnt out (" + b.getBurnt().text + "). Repair: right-click with 1 " + partName(b) + ".";
    }

    /** The repair part's name, "?" if there is none (the chat message and Jade). */
    public static String partName(IBurnable b) {
        ItemStack part = b.getRepairPart();
        return Utils.isEmpty(part) ? "?" : part.getHoverName().getString();
    }
}
