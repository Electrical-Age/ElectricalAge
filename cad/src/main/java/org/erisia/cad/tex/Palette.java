package org.erisia.cad.tex;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * The global palette (board #44): every material by name, one look everywhere: a base colour, noise (each texel's
 * channels moved together by up to +-noise) and an optional shade pattern (a per-texel change, cell-local x y in 0..15,
 * row 0 at the top). Painted into 16 px atlas cells by {@link Atlas}. The entries are those of the code-CAD models'
 * atlases (tools/models/oilkit.py and converter.py), so a ported model keeps its colours; the noise is reseeded.
 */
public final class Palette {
    private Palette() {
    }

    /** A change per texel: {dr, dg, db}, or null for none. */
    @FunctionalInterface
    public interface Shade {
        int[] at(int x, int y);
    }

    public record Entry(String name, int r, int g, int b, int noise, Shade shade) {
    }

    static final Map<String, Entry> ALL = new LinkedHashMap<>();

    static void put(String name, int r, int g, int b, int noise, Shade shade) {
        ALL.put(name, new Entry(name, r, g, b, noise, shade));
    }

    static int[] all(int k) {
        return k == 0 ? null : new int[]{k, k, k};
    }

    static {
        put("steel", 128, 134, 140, 8, null);
        put("darksteel", 62, 64, 68, 6, null);
        put("castiron", 88, 84, 80, 10, (x, y) -> all((x * 7 + y * 3) % 11 == 0 ? -10 : 0));
        put("copper", 196, 110, 70, 14, null);
        put("brass", 200, 170, 80, 10, null);
        put("glass", 230, 232, 224, 4, (x, y) -> all(x == 0 || x == 15 || y == 0 || y == 15 ? -60 : 0));
        put("rubber", 34, 34, 36, 4, null);
        put("ceramic", 226, 222, 208, 5, (x, y) -> all(y % 4 == 3 ? -22 : 0));
        put("porcelain", 226, 222, 210, 6, (x, y) -> all(y % 4 == 3 ? -28 : 0));
        put("coil", 238, 120, 40, 12, (x, y) -> (x + y) % 3 == 0 ? new int[]{-150, -90, -30} : (x + y) % 3 == 1 ? new int[]{12, 20, 0} : null);
        put("hotcopper", 214, 116, 64, 10, (x, y) -> {
            int k = x % 4 == 0 || y % 4 == 0 ? -26 : 0;
            if (y >= 3 && y <= 6) return new int[]{-46 + k, -6 + k, 50 + k};
            if (y >= 7 && y <= 9) return new int[]{24 + k, 34 + k, -24 + k};
            return all(k);
        });
        put("rivet", 100, 106, 112, 4, (x, y) -> all(y % 8 == 2 && x % 2 == 0 ? 52 : y % 8 == 3 && x % 2 == 0 ? -26 : y % 8 == 4 ? -22 : 0));
        put("water", 200, 214, 222, 4, (x, y) -> y > 7 ? new int[]{-150, -90, -10} : y == 7 ? all(-60) : null);
        put("red", 176, 36, 30, 10, null);
        put("froth", 58, 44, 30, 10, (x, y) -> all((x * 5 + y * 3) % 13 == 0 ? 70 : (x + y * 7) % 5 == 0 ? -14 : 0));
        put("sand", 214, 196, 140, 14, null);
        put("louvre", 72, 74, 78, 4, (x, y) -> all(y % 3 == 0 ? -48 : y % 3 == 1 ? 14 : 0));
        put("oil", 24, 22, 20, 4, (x, y) -> all((x + y) % 9 == 0 ? 40 : 0));
        put("soot", 22, 20, 20, 4, null);
        put("lamp", 250, 186, 60, 6, (x, y) -> all(x >= 5 && x <= 10 && y >= 5 && y <= 10 ? 30 : 0));
        put("firebrick", 164, 92, 62, 10, (x, y) -> all(y % 4 == 0 || (x + (y / 4) * 4) % 8 == 0 ? -60 : 0));
        // tier paints (EA's: green 50 V / LVU, blue 200 V / MVU, orange HVU) and the boiler's red
        put("paint_green", 56, 156, 84, 8, null);
        put("paint_blue", 52, 102, 176, 8, null);
        put("paint_orange", 226, 132, 44, 8, null);
        put("paint_boiler", 122, 44, 36, 8, null);
        put("export", 150, 46, 40, 8, (x, y) -> all((x + y) % 8 < 2 ? -28 : 0));
    }

    public static Entry get(String name) {
        Entry e = ALL.get(name);
        if (e == null) throw new IllegalArgumentException("no palette entry " + name + " (have " + ALL.keySet() + ")");
        return e;
    }

    public static java.util.Set<String> names() {
        return ALL.keySet();
    }

    /** Paint a 16 x 16 cell of the material at (x0, y0). */
    public static void paint(Sheet s, int x0, int y0, Entry e, SplittableRandom rnd) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int d = e.noise() == 0 ? 0 : rnd.nextInt(-e.noise(), e.noise() + 1);
                int[] k = e.shade() == null ? null : e.shade().at(x, y);
                int r = e.r() + d + (k == null ? 0 : k[0]), g = e.g() + d + (k == null ? 0 : k[1]), b = e.b() + d + (k == null ? 0 : k[2]);
                s.set(x0 + x, y0 + y, 0xFF000000 | clamp(r) << 16 | clamp(g) << 8 | clamp(b));
            }
    }

    static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
