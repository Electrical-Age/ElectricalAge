package mods.eln.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Why a kit's route click failed (Eln.packetRouteNote, node.voxel.KitPreview.note), on a line of its own just above the
 * action bar for a few seconds (Baughn 2026-11-18, #521: the route preview's length line replaced it on the action bar
 * at once). It fades out over its last half second; a newer note replaces it.
 */
public final class RouteNoteHud {
    private RouteNoteHud() {
    }

    static final long SHOW_MS = 3000, FADE_MS = 500;
    static String text;
    static long until;

    public static void read(DataInputStream stream) throws IOException {
        text = stream.readUTF();
        until = System.currentTimeMillis() + SHOW_MS;
    }

    static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
        var mc = Minecraft.getInstance();
        long left = until - System.currentTimeMillis();
        if (text == null || left <= 0 || mc.player == null) return;
        int alpha = (int) (255 * Math.min(1.0, left / (double) FADE_MS));
        if (alpha < 8) return;   // (near 0 the font draws it opaque)
        // (the action bar's line is centred 68 px above the bottom; this one a line above it)
        int w = mc.font.width(text);
        g.text(mc.font, text, (g.guiWidth() - w) / 2, g.guiHeight() - 68 - 4 - 12, (alpha << 24) | 0xFF9090, true);
    }
}
