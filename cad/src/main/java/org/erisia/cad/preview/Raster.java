package org.erisia.cad.preview;

import org.erisia.cad.Face;

import java.util.List;

/**
 * An offline preview: faces drawn with a z-buffer, textured (nearest texel), lit by one light, orthographic from a yaw
 * and pitch, back faces culled as the game culls them. For before/after pictures, not for judging z-fights (it draws one
 * of two coplanar faces, consistently; the game flickers).
 */
public final class Raster {
    public final int size;
    public final int[] rgb;
    final double[] depth;

    /** The colour of a face at texture coordinate (u, v), ARGB (alpha under 128: a hole). */
    @FunctionalInterface
    public interface Sampler {
        int at(Face f, double u, double v);
    }

    public Raster(int size, int background) {
        this.size = size;
        rgb = new int[size * size];
        depth = new double[size * size];
        java.util.Arrays.fill(rgb, background);
        java.util.Arrays.fill(depth, Double.MAX_VALUE);
    }

    /**
     * Draw the faces seen from yaw (about y, degrees) and pitch (degrees, looking down), scaled to fit the box
     * {x0 y0 z0 x1 y1 z1} into the picture. Y is up.
     */
    public void draw(List<Face> faces, Sampler s, double yaw, double pitch, double[] fit) {
        double cy = Math.cos(Math.toRadians(yaw)), sy = Math.sin(Math.toRadians(yaw)), cp = Math.cos(Math.toRadians(pitch)), sp = Math.sin(Math.toRadians(pitch));
        double mx = (fit[0] + fit[3]) / 2, my = (fit[1] + fit[4]) / 2, mz = (fit[2] + fit[5]) / 2;
        double ext = Math.max(fit[3] - fit[0], Math.max(fit[4] - fit[1], fit[5] - fit[2])) * 0.62;
        double scale = size / (2 * ext);
        // light from the upper left front
        double lx = -0.4, ly = 0.8, lz = -0.45, ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
        for (Face f : faces) {
            // view space: x right, y up, z away from the eye
            double[] v = new double[3 * f.n];
            for (int i = 0; i < f.n; i++) {
                double x = f.xyz[3 * i] - mx, y = f.xyz[3 * i + 1] - my, z = f.xyz[3 * i + 2] - mz;
                double x1 = cy * x - sy * z, z1 = sy * x + cy * z;
                double y2 = cp * y - sp * z1, z2 = sp * y + cp * z1;
                v[3 * i] = size / 2.0 - x1 * scale;   // (looking along +z with y up, +x is on the left)
                v[3 * i + 1] = size / 2.0 - y2 * scale;
                v[3 * i + 2] = z2;
            }
            double nz1 = sy * f.nx + cy * f.nz, nvz = sp * f.ny + cp * nz1;
            if (nvz >= 0) continue;   // facing away
            double light = 0.45 + 0.55 * Math.max(0, (f.nx * lx + f.ny * ly + f.nz * lz) / ll);
            for (int t = 1; t + 1 < f.n; t++) tri(f, s, v, 0, t, t + 1, light);
        }
    }

    void tri(Face f, Sampler s, double[] v, int a, int b, int c, double light) {
        double ax = v[3 * a], ay = v[3 * a + 1], bx = v[3 * b], by = v[3 * b + 1], cx = v[3 * c], cy = v[3 * c + 1];
        double area = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        if (Math.abs(area) < 1e-9) return;
        int x0 = Math.max(0, (int) Math.floor(Math.min(ax, Math.min(bx, cx)))), x1 = Math.min(size - 1, (int) Math.ceil(Math.max(ax, Math.max(bx, cx))));
        int y0 = Math.max(0, (int) Math.floor(Math.min(ay, Math.min(by, cy)))), y1 = Math.min(size - 1, (int) Math.ceil(Math.max(ay, Math.max(by, cy))));
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                double px = x + 0.5, py = y + 0.5;
                double wa = ((bx - px) * (cy - py) - (by - py) * (cx - px)) / area, wb = ((cx - px) * (ay - py) - (cy - py) * (ax - px)) / area, wc = 1 - wa - wb;
                if (wa < -1e-9 || wb < -1e-9 || wc < -1e-9) continue;
                double z = wa * v[3 * a + 2] + wb * v[3 * b + 2] + wc * v[3 * c + 2];
                int i = y * size + x;
                if (z >= depth[i] - 1e-6) continue;
                double u = 0, w = 0;
                if (f.uv != null) {
                    u = wa * f.uv[2 * a] + wb * f.uv[2 * b] + wc * f.uv[2 * c];
                    w = wa * f.uv[2 * a + 1] + wb * f.uv[2 * b + 1] + wc * f.uv[2 * c + 1];
                }
                int col = s.at(f, u, w);
                if ((col >>> 24) < 128) continue;
                depth[i] = z;
                int r = (int) Math.min(255, ((col >> 16) & 255) * light), g = (int) Math.min(255, ((col >> 8) & 255) * light), bl = (int) Math.min(255, (col & 255) * light);
                rgb[i] = 0xFF000000 | r << 16 | g << 8 | bl;
            }
    }

    /** Multiply two ARGB colours. */
    public static int tint(int c, int t) {
        int r = ((c >> 16) & 255) * ((t >> 16) & 255) / 255, g = ((c >> 8) & 255) * ((t >> 8) & 255) / 255, b = (c & 255) * (t & 255) / 255;
        return (c & 0xFF000000) | r << 16 | g << 8 | b;
    }

    public void writePng(java.nio.file.Path path) throws java.io.IOException {
        var img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, size, size, rgb, 0, size);
        javax.imageio.ImageIO.write(img, "png", path.toFile());
    }

    /** Several pictures side by side, with a gap. */
    public static void writeRow(java.nio.file.Path path, List<Raster> pics) throws java.io.IOException {
        int s = pics.get(0).size, gap = 4;
        var img = new java.awt.image.BufferedImage(pics.size() * (s + gap) - gap, s, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < pics.size(); i++) img.setRGB(i * (s + gap), 0, s, s, pics.get(i).rgb, 0, s);
        javax.imageio.ImageIO.write(img, "png", path.toFile());
    }
}
