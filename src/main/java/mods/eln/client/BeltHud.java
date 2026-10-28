package mods.eln.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import mods.eln.item.belt.BatteryBeltItem;

/**
 * The worn battery belt's charge on the HUD (Baughn 2026-10-27): a green bar above the hotbar's right end, where Stasis
 * Delvers' suit gauge goes; while that shows (BatteryBeltItem.hudAbove) the two stack, slimmer, the belt's under it.
 */
public final class BeltHud {
    private BeltHud() {
    }

    static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var belt = BatteryBeltItem.worn(mc.player);
        if (belt.isEmpty()) return;
        double share = BatteryBeltItem.batteryShare(belt);
        if (Double.isNaN(share)) return;
        boolean stacked = BatteryBeltItem.hudAbove.getAsBoolean();
        int w = g.guiWidth(), h = g.guiHeight();
        int x = w / 2 + 96, bw = 40, bh = stacked ? 3 : 5, y = stacked ? h - 21 : h - 22;
        g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, 0xFF303038);
        g.fill(x, y, x + bw, y + bh, 0xFF101014);
        g.fill(x, y, x + (int) (bw * Math.max(0, Math.min(1, share))), y + bh, 0xFF60C080);
    }
}
