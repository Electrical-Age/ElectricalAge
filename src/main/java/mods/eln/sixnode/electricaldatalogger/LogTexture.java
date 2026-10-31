package mods.eln.sixnode.electricaldatalogger;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * A logger's in-world graph as a small texture, painted again when a point comes (Baughn 2026-10-31: the in-world
 * lines were drawn as thick strips and z-fought with themselves; it should look like the screen's graph). Drawn as the
 * screen's graph is in lines mode (LogGraph): newest at the right, each channel's range over a point as a faint band,
 * its mean as a 1 px line, a grid, the frame on the right and the bottom. Client only; close() when the render goes.
 */
public final class LogTexture {
    static int next;
    static final int BACKGROUND = 0xFF101814, GRID = 0xFF22302A, FRAME = 0xFF8A948E;

    public final Identifier id;
    final DynamicTexture texture;
    final int w, h;
    int drawn = -1;

    public LogTexture(int w, int h) {
        this.w = w;
        this.h = h;
        id = Identifier.fromNamespaceAndPath("eln", "dynamic/log_graph_" + next++);
        texture = new DynamicTexture(() -> "eln logger graph", w, h, true);
        Minecraft.getInstance().getTextureManager().register(id, texture);
    }

    /** A texture for a graph of this aspect (width / height), 128 px wide; null where there is no GPU (a test's render). */
    public static LogTexture of(float aspect) {
        try {
            return new LogTexture(128, Math.max(16, Math.min(256, Math.round(128 / Math.max(0.1f, aspect)))));
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /** Paints the graph if the log changed since; colour(c) is channel c's ink (ARGB). Returns the texture to bind. */
    public Identifier update(DataLogs log, java.util.function.IntUnaryOperator colour) {
        if (log.version == drawn) return id;
        drawn = log.version;
        NativeImage img = texture.getPixels();
        if (img == null) return id;
        img.fillRect(0, 0, w, h, BACKGROUND);
        int gh = h - 2, gw = w - 2;   // (the frame's row and column)
        for (int k = 1; k < 4; k++) {
            int y = gh - k * gh / 4;
            for (int x = 0; x < gw; x++) img.setPixel(x, y, GRID);
        }
        LogChannel first = log.channels[0];
        if (first.min < 0 && first.max > 0 || first.min > 0 && first.max < 0) {
            int y = Math.round(first.max / (first.max - first.min) * (gh - 1));
            for (int x = 0; x < gw; x++) img.setPixel(x, Math.max(0, Math.min(gh - 1, y)), 0xFF46524C);
        }
        int n = log.size();
        for (int c = 0; c < log.channelCount() && n > 0; c++) {
            int ink = colour.applyAsInt(c), band = mix(BACKGROUND, ink, 0.35f);
            int prev = Integer.MIN_VALUE;
            for (int px = 0; px < gw; px++) {
                float[] v = LogGraph.column(log, c, px, gw, n);
                if (v == null) {
                    prev = Integer.MIN_VALUE;
                    continue;
                }
                int yHi = row(v[2], gh), yLo = row(v[1], gh), y = row(v[0], gh);
                for (int yy = yHi; yy <= yLo; yy++) if (img.getPixel(px, yy) != ink) img.setPixel(px, yy, band);
                int y0 = prev == Integer.MIN_VALUE ? y : Math.min(prev, y), y1 = prev == Integer.MIN_VALUE ? y : Math.max(prev, y);
                for (int yy = y0; yy <= y1; yy++) img.setPixel(px, yy, ink);
                prev = y;
            }
        }
        for (int x = 0; x < w; x++) img.setPixel(x, h - 1, FRAME);
        for (int y = 0; y < h; y++) img.setPixel(w - 1, y, FRAME);
        texture.upload();
        return id;
    }

    /** The row of a signal fraction (0 at the bottom of the graph, 1 at its top). */
    static int row(float v, int gh) {
        return gh - 1 - Math.round(Math.max(0, Math.min(1, v)) * (gh - 1));
    }

    static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t),
            bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    public void close() {
        Minecraft.getInstance().getTextureManager().release(id);
    }
}
