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
     * The repair part by kind of device. Cables, HV gear and batteries are replaced (one of the same item); everything
     * else gets the part that failed. Placeholders, to be tuned (Baughn didn't vote on these).
     */
    public static ItemStack defaultPart(IBurnable b) {
        Failure.Kind kind = b.getBurntKind();
        if (kind == null) return ItemStack.EMPTY;
        switch (kind) {
            case ELECTRONICS:
                return Eln.findItemStack("Cheap Chip");
            case MACHINE:
                return b.getBurnt() != null && b.getBurnt().electrical() ? Eln.findItemStack("Cheap Chip") : Eln.findItemStack("Machine Block");
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

    public static ItemStack sameItem(IBurnable b) {
        ItemStack s = b.getDropItemStack();
        if (Utils.isEmpty(s)) return ItemStack.EMPTY;
        s = s.copy();
        s.setCount(1);
        mods.eln.generic.ItemNbt.set(s, null);
        return s;
    }

    /**
     * Right-click on a burnt device. With the repair part in hand: consume one, repair, return true.
     * Otherwise say what's wrong and return false, so the usual action (GUI, so its contents can be taken out) runs.
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
        ItemStack part = b.getRepairPart();
        String what = Utils.isEmpty(part) ? "?" : part.getHoverName().getString();
        return "Burnt out (" + b.getBurnt().text + "). Repair: right-click with 1 " + what + ".";
    }
}
