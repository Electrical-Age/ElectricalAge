package org.erisia.cad.tex;

import java.util.SplittableRandom;

/**
 * A texture being painted (ARGB texels), the house style: regions of one material each, a base colour with a little
 * seeded noise and a vertical shade so flats don't look like plastic, and features painted on top. Face-local features
 * go only on texels lying wholly inside the face's rectangle, so a texel never straddles two faces of a box.
 */
public final class Sheet {
    public final int w, h;
    public final int[] argb;
    final SplittableRandom rnd;

    public Sheet(int w, int h, long seed) {
        this.w = w;
        this.h = h;
        argb = new int[w * h];
        rnd = new SplittableRandom(seed);
    }

    /** A pattern over a region: the colour (0xRRGGBB) at region-local texel (x, y), or -1 for the base. */
    @FunctionalInterface
    public interface Pattern {
        int at(int x, int y);
    }

    /**
     * Fill u..u+w, v..v+h with rgb: each texel times (1 + noise in +-noise - 0.10 x (its row's place, -0.5..0.5)), the
     * pattern's colour instead of the base where it gives one.
     */
    public void region(int u, int v, int rw, int rh, int rgb, double noise, Pattern p) {
        for (int y = v; y < v + rh; y++)
            for (int x = u; x < u + rw; x++) {
                double f = 1 + (rnd.nextDouble() * 2 - 1) * noise - 0.10 * ((y - v) / (double) Math.max(1, rh - 1) - 0.5);
                int c = rgb;
                if (p != null) {
                    int q = p.at(x - u, y - v);
                    if (q >= 0) c = q;
                }
                set(x, y, shade(c, f));
            }
    }

    public static int shade(int rgb, double f) {
        int r = clamp((int) (((rgb >> 16) & 255) * f)), g = clamp((int) (((rgb >> 8) & 255) * f)), b = clamp((int) ((rgb & 255) * f));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public void set(int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < w && y < h) argb[y * w + x] = c;
    }

    public int get(int x, int y) {
        return argb[y * w + x];
    }

    /**
     * The texels lying wholly inside a face's rectangle {u0, v0, u1, v1} (either order), as {x0, y0, x1, y1} inclusive,
     * or null if none: with fractional box sizes a face's edge falls inside a texel, which then shows on both faces.
     */
    public static int[] inside(float[] rect) {
        double u0 = Math.min(rect[0], rect[2]), u1 = Math.max(rect[0], rect[2]), v0 = Math.min(rect[1], rect[3]), v1 = Math.max(rect[1], rect[3]);
        int x0 = (int) Math.ceil(u0 - 1e-6), x1 = (int) Math.floor(u1 + 1e-6) - 1, y0 = (int) Math.ceil(v0 - 1e-6), y1 = (int) Math.floor(v1 + 1e-6) - 1;
        return x1 < x0 || y1 < y0 ? null : new int[]{x0, y0, x1, y1};
    }

    /**
     * A ring round an entity box (EntityLayout's net, box at (u, v) of size w x h x d): one row of texels across its
     * four side faces (-x, -z, +x, +z share the rows v + d .. v + d + h), the row lying wholly inside them whose middle
     * is nearest frac of the height down from the top. The same row on every side face: the ring meets itself at the
     * box's edges, and stays off its top and bottom. Returns the row.
     */
    public int ring(float u, float v, float w, float h, float d, double frac, int rgb, double noise) {
        double v1 = v + d, v2 = v1 + h, want = v1 + frac * h;
        int best = -1;
        for (int y = (int) Math.ceil(v1 - 1e-6); y + 1 <= v2 + 1e-6; y++)
            if (best < 0 || Math.abs(y + 0.5 - want) < Math.abs(best + 0.5 - want)) best = y;
        if (best < 0) throw new IllegalArgumentException("no whole row of texels in a side strip " + h + " high");
        for (int x = (int) Math.floor(u); x < Math.ceil(u + 2 * (d + w)); x++)
            set(x, best, shade(rgb, 1 + (rnd.nextDouble() * 2 - 1) * noise));
        return best;
    }

    /** Write as PNG. */
    public void writePng(java.nio.file.Path path) throws java.io.IOException {
        var img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, argb, 0, w);
        javax.imageio.ImageIO.write(img, "png", path.toFile());
    }

    public static int[] readPng(java.nio.file.Path path, int[] size) throws java.io.IOException {
        var img = javax.imageio.ImageIO.read(path.toFile());
        size[0] = img.getWidth();
        size[1] = img.getHeight();
        return img.getRGB(0, 0, size[0], size[1], null, 0, size[0]);
    }
}
