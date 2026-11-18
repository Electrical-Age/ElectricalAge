package mods.eln.robotics.computer;

import com.mojang.blaze3d.platform.NativeImage;
import mods.eln.robotics.computer.screen.SceneCells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * The monitor's picture (Baughn 2026-11-18): the screen (SceneCells) in 8 x 8 cells, as a colour text mode on a CRT (80 x 25
 * is 640 x 200, stretched to the 4:3 glass), the glyphs from the game's own ascii font. Painted again only when the
 * scene or the power changes. Client only; close() when the render goes.
 */
final class ScreenTexture {
    static int next;
    static final int CELL = 8;
    /** The ascii font's glyphs as 8 x 8 bits by code point (row r, column x: bit r * 8 + x), null until read. */
    static java.util.Map<Integer, Long> glyphs;
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
            java.util.Map<Integer, Long> g = glyphs();
            for (int y = 0; y < c.h; y++)
                for (int x = 0; x < c.w; x++) img.fillRect(x * CELL, y * CELL, CELL, CELL, c.paper[y * c.w + x]);
            // the pictures, dot by dot, over the cells they show on
            for (SceneCells.Image im : c.images) {
                var p = im.pixmap();
                int pw = im.cw() * CELL, ph = im.ch() * CELL;
                for (int y = im.y(); y < im.y() + im.ch(); y++)
                    for (int x = im.x(); x < im.x() + im.cw(); x++) {
                        if (!im.shows(c, x, y)) continue;
                        for (int r = 0; r < CELL; r++)
                            for (int b = 0; b < CELL; b++) {
                                int u = ((x - im.x()) * CELL + b) * p.w() / pw, v = ((y - im.y()) * CELL + r) * p.h() / ph;
                                int argb = p.argb()[v * p.w() + u];
                                if ((argb >>> 24) != 0) img.setPixel(x * CELL + b, y * CELL + r, argb);
                            }
                    }
            }
            for (int y = 0; y < c.h; y++)
                for (int x = 0; x < c.w; x++) {
                    int i = y * c.w + x;
                    int cp = c.glyph[i];
                    if (cp == ' ' || g == null) continue;
                    Long bits = g.getOrDefault(cp, g.get((int) '?'));
                    if (bits == null) continue;
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
     * each moved to the middle of its cell by its inked width, as the screen centres characters in cells. Which code
     * point each cell is comes from the font's own map (font/include/default.json: its rows of characters), so the
     * box-drawing glyphs Wire draws frames with (│ ─ ┘ ╔ ═ ■ ▒) are found where the font has them. Keyed by code point.
     */
    static java.util.Map<Integer, Long> glyphs() {
        if (glyphsTried) return glyphs;
        glyphsTried = true;
        var rm = Minecraft.getInstance().getResourceManager();
        try (var in = rm.open(Identifier.withDefaultNamespace("textures/font/ascii.png"))) {
            NativeImage img = NativeImage.read(in);
            String[] rows = charRows();
            int cell = img.getWidth() / 16;
            java.util.Map<Integer, Long> out = new java.util.HashMap<>();
            for (int c = 0; c < 256; c++) {
                int cp = rows == null ? c : c / 16 < rows.length && c % 16 < rows[c / 16].codePointCount(0, rows[c / 16].length())
                    ? rows[c / 16].codePointAt(rows[c / 16].offsetByCodePoints(0, c % 16)) : 0;
                if (cp == 0) continue;
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
                // (box-drawing glyphs fill their cell edge to edge: they aren't centred)
                boolean box = cp >= 0x2500 && cp <= 0x25FF;
                int shift = box || right < 0 ? 0 : Math.max(0, (CELL - (right + 1)) / 2);
                long bits = 0;
                for (int r = 0; r < CELL; r++)
                    for (int b = 0; b + shift < CELL; b++) if (on[r][b]) bits |= 1L << (r * CELL + b + shift);
                out.put(cp, bits);
            }
            img.close();
            glyphs = out;
        } catch (Exception e) {
            glyphs = null;
        }
        return glyphs;
    }

    /** The ascii.png provider's "chars" rows from minecraft:font/include/default.json, or null (then cell = code point). */
    static String[] charRows() {
        try (var in = Minecraft.getInstance().getResourceManager().open(Identifier.withDefaultNamespace("font/include/default.json"));
             var r = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
            var root = com.google.gson.JsonParser.parseReader(r).getAsJsonObject();
            for (var p : root.getAsJsonArray("providers")) {
                var o = p.getAsJsonObject();
                if (!o.has("file") || !o.get("file").getAsString().endsWith("ascii.png")) continue;
                var a = o.getAsJsonArray("chars");
                String[] rows = new String[a.size()];
                for (int i = 0; i < rows.length; i++) rows[i] = a.get(i).getAsString();
                return rows;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    void close() {
        if (texture != null) Minecraft.getInstance().getTextureManager().release(id);
    }
}
