package mods.eln.robotics.computer;

import com.mojang.blaze3d.platform.NativeImage;
import mods.eln.robotics.computer.screen.SceneCells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * The monitor's picture (Baughn 2026-11-18): the shown tab in 8 x 8 cells, as a colour text mode on a CRT (80 x 25
 * is 640 x 200, stretched to the 4:3 glass), the glyphs from the game's own ascii font. Painted again only when the
 * scene, the tab or the power changes. Client only; close() when the render goes.
 */
final class ScreenTexture {
    static int next;
    static final int CELL = 8;
    /** The ascii font's glyphs as 8 x 8 bit rows (glyph c's row r: bit x set where it is inked), null until read. */
    static long[] glyphs;
    static boolean glyphsTried;

    final Identifier id;
    DynamicTexture texture;
    int tw, th;
    /** What was painted: the scene's version, the tab, on or off. */
    long drawn = Long.MIN_VALUE, paintedAt;
    static final long PAINT_MS = 100;

    ScreenTexture() {
        id = Identifier.fromNamespaceAndPath("eln", "dynamic/computer_screen_" + next++);
    }

    /** A texture, or null where there is no GPU (a test's render). */
    static ScreenTexture make() {
        try {
            ScreenTexture t = new ScreenTexture();
            t.size(80, 25);
            return t;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    void size(int cols, int rows) {
        int w = cols * CELL, h = rows * CELL;
        if (texture != null && w == tw && h == th) return;
        if (texture != null) Minecraft.getInstance().getTextureManager().release(id);
        tw = w;
        th = h;
        texture = new DynamicTexture(() -> "eln computer screen", w, h, false);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        drawn = Long.MIN_VALUE;
    }

    /** Paints the cells if key (what they were made from) changed; cells may be null for a dark screen. */
    Identifier update(long key, java.util.function.Supplier<SceneCells> cells) {
        long now = System.currentTimeMillis();
        if (key == drawn || now - paintedAt < PAINT_MS) return id;   // (a busy log is painted at most 10 times a second)
        drawn = key;
        paintedAt = now;
        SceneCells c = cells.get();
        if (c != null) size(c.w, c.h);
        NativeImage img = texture.getPixels();
        if (img == null) return id;
        img.fillRect(0, 0, tw, th, 0xFF0A0C0B);
        if (c != null) {
            long[] g = glyphs();
            for (int y = 0; y < c.h; y++)
                for (int x = 0; x < c.w; x++) {
                    int i = y * c.w + x, paper = c.paper[i];
                    img.fillRect(x * CELL, y * CELL, CELL, CELL, paper);
                    int cp = c.glyph[i];
                    if (cp == ' ' || g == null) continue;
                    long bits = g[cp > 0 && cp < 256 ? cp : '?'];
                    int ink = c.ink[i];
                    for (int r = 0; r < CELL; r++)
                        for (int b = 0; b < CELL; b++)
                            if ((bits >>> (r * CELL + b) & 1) != 0) img.setPixel(x * CELL + b, y * CELL + r, ink);
                }
        }
        texture.upload();
        return id;
    }

    /**
     * The glyphs of minecraft:textures/font/ascii.png (16 x 16 cells; a resource pack's larger font is sampled down),
     * each moved to the middle of its cell by its inked width, as the screen centres characters in cells.
     */
    static long[] glyphs() {
        if (glyphsTried) return glyphs;
        glyphsTried = true;
        try (var in = Minecraft.getInstance().getResourceManager()
            .open(Identifier.withDefaultNamespace("textures/font/ascii.png"))) {
            NativeImage img = NativeImage.read(in);
            int cell = img.getWidth() / 16;
            long[] out = new long[256];
            for (int c = 0; c < 256; c++) {
                boolean[][] on = new boolean[CELL][CELL];
                int right = -1;
                for (int r = 0; r < CELL; r++)
                    for (int b = 0; b < CELL; b++) {
                        int px = (c % 16) * cell + b * cell / CELL, py = (c / 16) * cell + r * cell / CELL;
                        if ((img.getPixel(px, py) >>> 24) > 0x40) {
                            on[r][b] = true;
                            right = Math.max(right, b);
                        }
                    }
                int shift = right < 0 ? 0 : Math.max(0, (CELL - (right + 1)) / 2);
                long bits = 0;
                for (int r = 0; r < CELL; r++)
                    for (int b = 0; b + shift < CELL; b++) if (on[r][b]) bits |= 1L << (r * CELL + b + shift);
                out[c] = bits;
            }
            img.close();
            glyphs = out;
        } catch (Exception e) {
            glyphs = null;
        }
        return glyphs;
    }

    void close() {
        if (texture != null) Minecraft.getInstance().getTextureManager().release(id);
    }
}
