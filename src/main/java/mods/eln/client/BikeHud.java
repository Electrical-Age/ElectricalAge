package mods.eln.client;

import mods.eln.bike.BikePhysics;
import mods.eln.bike.ElectricBike;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * The electric bike's speedometer (Baughn 2026-10-28: tell players it needs a battery): while you ride one, a round dial
 * left of the hotbar (clear of the offhand slot, hearts and jump bar) with its needle from 0 to the powered top speed in
 * km/h, a digital readout, and the battery beside it; with no battery, or a flat one, the battery turns into a red
 * empty-battery symbol that blinks slowly. Drawn with fills, so it stays crisp at any GUI scale.
 */
public final class BikeHud {
    private BikeHud() {
    }

    static final int R = 17;
    /** The needle sweeps 270 degrees, from bottom-left (0) clockwise to bottom-right (top speed). */
    static final float START = 225, SWEEP = 270;
    static float shownSpeed;

    static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof ElectricBike bike)) {
            shownSpeed = 0;
            return;
        }
        // the bike's horizontal speed, smoothed (b/s)
        double v = Math.hypot(bike.getX() - bike.xo, bike.getZ() - bike.zo) * 20;
        shownSpeed += (float) (v - shownSpeed) * 0.15f;
        double top = BikePhysics.VMAX_POWERED * 20 * 3.6;   // km/h
        double kmh = shownSpeed * 3.6;
        int w = g.guiWidth(), h = g.guiHeight();
        int cx = w / 2 - 91 - 29 - 6 - R, cy = h - R - 4;
        // face: a dark disc with a light rim
        disc(g, cx, cy, R + 1, 0xFF9AA4AE);
        disc(g, cx, cy, R, 0xFF15181C);
        // ticks every tenth, longer every half
        for (int i = 0; i <= 10; i++) {
            float a = angle(i / 10f);
            int len = i % 5 == 0 ? 4 : 2;
            for (int k = 0; k < len; k++) dot(g, cx + Mth.cos(a) * (R - 1 - k), cy - Mth.sin(a) * (R - 1 - k), i == 10 ? 0xFFE05050 : 0xFFD8DCE0);
        }
        // needle
        float a = angle((float) Math.min(1.05, kmh / top));
        for (float r = 0; r < R - 3; r += 0.5f) dot(g, cx + Mth.cos(a) * r, cy - Mth.sin(a) * r, 0xFFFF7030);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFB0B8C0);
        // readout, in the open bottom of the dial
        String s = String.valueOf(Math.round(kmh));
        g.text(mc.font, s, cx - mc.font.width(s) / 2, cy + 5, 0xFFFFFFFF, false);
        // the battery, left of the dial
        battery(g, cx - R - 12, cy - 9, bike.syncedCharge(), mc.player.tickCount);
    }

    /** Dial angle (radians, maths convention) for a share of top speed. */
    static float angle(float share) {
        return (float) Math.toRadians(START - SWEEP * share);
    }

    static void dot(GuiGraphicsExtractor g, float x, float y, int color) {
        int ix = Math.round(x), iy = Math.round(y);
        g.fill(ix, iy, ix + 1, iy + 1, color);
    }

    static void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.floor(Math.sqrt(r * r - dy * dy + 0.5));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    /** An upright battery 8 x 18 at (x, y): filled by charge, green to red; with none or flat, a red outline with a slash. */
    static void battery(GuiGraphicsExtractor g, int x, int y, float charge, int ticks) {
        boolean empty = charge <= 0.001f;
        int bw = 8, bh = 18;
        if (empty) {
            if ((ticks / 10) % 3 == 2) return;   // (blinks: on 1 s, off 0.5 s)
            int red = 0xFFE03030;
            g.fill(x + 2, y - 2, x + bw - 2, y, red);
            g.fill(x, y, x + bw, y + bh, red);
            g.fill(x + 1, y + 1, x + bw - 1, y + bh - 1, 0xFF15181C);
            for (int i = 0; i < bh - 2; i++) dot(g, x + 1 + (bw - 3) * (1 - i / (float) (bh - 3)), y + 1 + i, red);
            return;
        }
        g.fill(x + 2, y - 2, x + bw - 2, y, 0xFF9AA4AE);
        g.fill(x, y, x + bw, y + bh, 0xFF9AA4AE);
        g.fill(x + 1, y + 1, x + bw - 1, y + bh - 1, 0xFF15181C);
        int fill = Math.max(1, Math.round((bh - 2) * Math.min(1, charge)));
        int c = charge > 0.5f ? 0xFF50C060 : charge > 0.2f ? 0xFFD0B030 : 0xFFE05030;
        g.fill(x + 1, y + bh - 1 - fill, x + bw - 1, y + bh - 1, c);
    }
}
