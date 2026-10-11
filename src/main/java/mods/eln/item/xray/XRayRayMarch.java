package mods.eln.item.xray;

/**
 * The X-ray scanner's picture (pure, unit-tested): one ray per screen pixel from the eye through a W x H grid
 * spanning the field of view, marched voxel by voxel (Amanatides-Woo) up to the range. Each pixel sums what its ray
 * passes through, weighted by the length inside each block: a faint grey for solid blocks, a dark blue tint for air,
 * and the slot colour for filtered ores (strong). Row 0 is the top of the view, column 0 its left.
 */
public final class XRayRayMarch {
    private XRayRayMarch() {
    }

    /** What is at a block: AIR, SOLID, OTHER (non-solid, e.g. plants, water) or the filter slot 0..3. */
    public interface Lookup {
        int at(int x, int y, int z);
    }

    public static final int AIR = -1, SOLID = -2, OTHER = -3;

    /** Slot colours (r, g, b): red, green, blue, yellow; also the filter screen's frames. */
    public static final float[][] COLORS = {{1f, 0.25f, 0.2f}, {0.25f, 1f, 0.3f}, {0.3f, 0.5f, 1f}, {1f, 0.9f, 0.15f}};
    public static final int[] ARGB = {0xFFFF4033, 0xFF40FF4D, 0xFF4D80FF, 0xFFFFE626};

    /**
     * @param eye      eye position
     * @param forward  unit view direction
     * @param up       unit "up" of the view (perpendicular to forward)
     * @param fovX     horizontal field of view, radians (vertical follows the aspect ratio)
     * @param out      w * h * 3 floats (r, g, b per pixel, row-major), overwritten
     */
    public static void scan(Lookup world, double[] eye, double[] forward, double[] up, double fovX, double range, int w, int h,
                            float[] out) {
        double[] right = {forward[1] * up[2] - forward[2] * up[1], forward[2] * up[0] - forward[0] * up[2],
            forward[0] * up[1] - forward[1] * up[0]};
        double tx = Math.tan(fovX / 2), ty = tx * h / w;
        for (int py = 0; py < h; py++) {
            double v = (1 - 2 * (py + 0.5) / h) * ty;
            for (int px = 0; px < w; px++) {
                double u = (2 * (px + 0.5) / w - 1) * tx;
                double dx = forward[0] + right[0] * u + up[0] * v;
                double dy = forward[1] + right[1] * u + up[1] * v;
                double dz = forward[2] + right[2] * u + up[2] * v;
                double n = Math.sqrt(dx * dx + dy * dy + dz * dz);
                march(world, eye, dx / n, dy / n, dz / n, range, out, (py * w + px) * 3);
            }
        }
    }

    static void march(Lookup world, double[] eye, double dx, double dy, double dz, double range, float[] out, int o) {
        int x = (int) Math.floor(eye[0]), y = (int) Math.floor(eye[1]), z = (int) Math.floor(eye[2]);
        int sx = dx > 0 ? 1 : -1, sy = dy > 0 ? 1 : -1, sz = dz > 0 ? 1 : -1;
        double ddx = dx == 0 ? Double.MAX_VALUE : Math.abs(1 / dx), ddy = dy == 0 ? Double.MAX_VALUE : Math.abs(1 / dy),
            ddz = dz == 0 ? Double.MAX_VALUE : Math.abs(1 / dz);
        double nx = dx == 0 ? Double.MAX_VALUE : (dx > 0 ? (x + 1 - eye[0]) : (eye[0] - x)) * ddx;
        double ny = dy == 0 ? Double.MAX_VALUE : (dy > 0 ? (y + 1 - eye[1]) : (eye[1] - y)) * ddy;
        double nz = dz == 0 ? Double.MAX_VALUE : (dz > 0 ? (z + 1 - eye[2]) : (eye[2] - z)) * ddz;
        double t = 0;
        float r = 0, g = 0, b = 0;
        while (t < range) {
            double next = Math.min(nx, Math.min(ny, nz));
            double len = Math.min(next, range) - t;
            int k = world.at(x, y, z);
            if (k >= 0 && k < COLORS.length) {
                float a = (float) len * 0.9f;
                r += COLORS[k][0] * a;
                g += COLORS[k][1] * a;
                b += COLORS[k][2] * a;
            } else if (k == SOLID) {
                float a = (float) len * 0.045f;
                r += a;
                g += a;
                b += a;
            } else if (k == OTHER) {
                float a = (float) len * 0.02f;
                r += a;
                g += a;
                b += a;
            } else {
                b += (float) len * 0.006f;
            }
            t = next;
            if (nx <= ny && nx <= nz) {
                x += sx;
                nx += ddx;
            } else if (ny <= nz) {
                y += sy;
                ny += ddy;
            } else {
                z += sz;
                nz += ddz;
            }
        }
        out[o] = Math.min(1, r);
        out[o + 1] = Math.min(1, g);
        out[o + 2] = Math.min(1, b);
    }
}
