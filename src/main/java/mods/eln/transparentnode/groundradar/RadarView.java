package mods.eln.transparentnode.groundradar;

/**
 * The Ground Radar w/Goggles' fly-through view, without Minecraft: a picture (GroundScan) raymarched from a free camera
 * into an ARGB image. A band-pass on density chooses what shows (Baughn: from air-as-solid to nothing but the dense
 * ores): cells inside [lo, hi] are drawn near-solid, with a softer haze towards the band's edges and nothing outside it.
 * Faded and unknown cells don't show. Distance darkens (a depth cue); the radar's level is a faint grid plane.
 * <p>
 * World coordinates are the level's (blocks); the camera looks along yaw (degrees, 0 = +z, 90 = -x, as Minecraft's) and
 * pitch (degrees, positive down). Rays step by half a cell of the layer they're in, inside the cone's box only.
 */
public final class RadarView {
    final GroundScan p;
    /** The layer at each depth under the radar (blocks), and the cone's total depth. */
    private final int[] layerAtDepth;
    private final int total;
    /** The cone's box (blocks): x and z half-width around the radar, the bottom and top y. */
    private final double half, bottom, top;
    /** Per cell: its colour (RGB, no fade) and its density, signal and whether it shows at all. */
    private final int[] rgb;
    private final float[] dens, sig, peak, share;
    private final int[] peakRgb, flags;
    private final boolean[] shows;

    /** A soft band edge's width (g/cm3), the opacity of a fully-in cell per block, the distance of the fog's e-fold. */
    public static final double EDGE = 0.3, OPACITY = 0.45, FOG = 260;
    /** A cell's densest samples show once they are SHARE_MIN of it, fully at SHARE_FULL: a vein does, one ore block in a
     *  deep cell's 27 samples doesn't (in a modded mountain 1.2 % of samples were ore, and two cells in five held one: a
     *  ray through them all was a gold haze, AdventurAgent), and the
     *  distance from the eye inside which the volume fades out (so the rock the camera is in doesn't blind it). */
    public static final double SHARE_MIN = 0.07, SHARE_FULL = 0.3, NEAR = 2.0;
    /** The grid plane's spacing (blocks) at the radar's level. */
    public static final int GRID = 16;

    public RadarView(GroundScan p) {
        this.p = p;
        GroundScan.Layer last = p.layers[p.layers.length - 1];
        total = last.depth() + last.cell();
        layerAtDepth = new int[total];
        for (int k = 0; k < p.layers.length; k++)
            for (int d = p.layers[k].depth(); d < Math.min(total, p.layers[k].depth() + p.layers[k].cell()); d++) layerAtDepth[d] = k;
        half = p.n * last.cell() / 2.0;
        top = p.cy;   // (the first layer's top is the block under the radar: y = cy - 1, its top face at cy)
        bottom = p.cy - total;
        int cells = p.layers.length * p.n * p.n;
        rgb = new int[cells];
        dens = new float[cells];
        peak = new float[cells];
        share = new float[cells];
        peakRgb = new int[cells];
        flags = new int[cells];
        sig = new float[cells];
        shows = new boolean[cells];
        for (int k = 0; k < p.layers.length; k++)
            for (int i = 0; i < p.n; i++)
                for (int j = 0; j < p.n; j++) {
                    int c = (k * p.n + i) * p.n + j, f = p.flagsAt(k, i, j);
                    flags[c] = f;
                    shows[c] = (f & (GroundScan.UNKNOWN | GroundScan.SKY)) == 0;
                    dens[c] = (float) p.densityAt(k, i, j);
                    sig[c] = (float) p.signalAt(k, i, j);
                    rgb[c] = cellColour(dens[c], f);
                    peak[c] = (float) p.peakAt(k, i, j);
                    share[c] = (float) p.peakShareAt(k, i, j);
                    peakRgb[c] = cellColour(peak[c], f & ~GroundScan.LIQUID);
                }
    }

    /** A cell's colour in the view: the picture's palette without the fade, brightened (it glows in the dark). */
    static int cellColour(double d, int f) {
        int c;
        if ((f & GroundScan.LAVA) != 0) c = 0xFF6020;
        else if ((f & GroundScan.OIL) != 0) c = 0xF0A830;
        else if ((f & GroundScan.LIQUID) != 0) c = 0x3C8CFF;
        else if (d < 0.5) c = 0x40E0E0;   // (air and caves: cyan, as a cast of the hollow)
        else c = GroundScan.ramp(d);
        if ((f & GroundScan.BUILT) != 0) c = GroundScan.mix(c, 0xB070F0, 0.55);
        return c;
    }

    /** How much a density shows in the band [lo, hi]: 1 inside, easing to 0 over EDGE past either end. */
    public static double band(double d, double lo, double hi) {
        return smooth(lo - EDGE, lo + EDGE, d) * (1 - smooth(hi - EDGE, hi + EDGE, d));
    }

    static double smooth(double a, double b, double x) {
        double t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }

    /** The cell index at a world point, or -1 outside the cone. */
    int cellAt(double x, double y, double z) {
        int d = (int) Math.floor(top - y);
        if (d < 0 || d >= total) return -1;
        int k = layerAtDepth[d];
        int cell = p.layers[k].cell();
        int i = (int) Math.floor((x - (p.cx - p.n * cell / 2.0)) / cell), j = (int) Math.floor((z - (p.cz - p.n * cell / 2.0)) / cell);
        if (i < 0 || j < 0 || i >= p.n || j >= p.n) return -1;
        return (k * p.n + i) * p.n + j;
    }

    /**
     * Render w x h into out (ARGB, row by row) from (ex, ey, ez) looking along yaw and pitch with a vertical field of view
     * fov (degrees), showing densities in [lo, hi]. Rows from row0 every rowStep (the others are left as they are).
     */
    public void render(int[] out, int w, int h, double ex, double ey, double ez, double yaw, double pitch, double fov,
                       double lo, double hi, int row0, int rowStep) {
        render(out, w, h, ex, ey, ez, yaw, pitch, fov, new RadarBand(RadarBand.CUSTOM, lo, hi), row0, rowStep);
    }

    /** As render above, showing what band shows (a named band, or a custom one's densities). */
    public void render(int[] out, int w, int h, double ex, double ey, double ez, double yaw, double pitch, double fov,
                       RadarBand band, int row0, int rowStep) {
        double ya = Math.toRadians(yaw), pa = Math.toRadians(pitch);
        // the camera's axes: forward f, right r, up u (Minecraft: yaw 0 looks +z; pitch positive looks down)
        double fx = -Math.sin(ya) * Math.cos(pa), fy = -Math.sin(pa), fz = Math.cos(ya) * Math.cos(pa);
        double rx = -Math.cos(ya), rz = -Math.sin(ya);   // (right of forward, level)
        double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;   // (up = right x forward)
        double tanY = Math.tan(Math.toRadians(fov) / 2), tanX = tanY * w / h;
        for (int py = row0; py < h; py += rowStep)
            for (int px = 0; px < w; px++) {
                double sx = (2 * (px + 0.5) / w - 1) * tanX, sy = (1 - 2 * (py + 0.5) / h) * tanY;
                double dx = fx + sx * rx + sy * ux, dy = fy + sy * uy, dz = fz + sx * rz + sy * uz;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                out[py * w + px] = trace(ex, ey, ez, dx / len, dy / len, dz / len, band);
            }
    }

    /** One ray's colour (ARGB, opaque: the background is black). */
    int trace(double ex, double ey, double ez, double dx, double dy, double dz, double lo, double hi) {
        return trace(ex, ey, ez, dx, dy, dz, new RadarBand(RadarBand.CUSTOM, lo, hi));
    }

    /** One ray's colour through what band shows. */
    int trace(double ex, double ey, double ez, double dx, double dy, double dz, RadarBand band) {
        // (the ray's span inside the cone's box, by slabs; no arrays: this runs for every pixel)
        double t0 = 0, t1 = 4 * (half + total + 64);
        for (int a = 0; a < 3; a++) {
            double e = a == 0 ? ex : a == 1 ? ey : ez, d = a == 0 ? dx : a == 1 ? dy : dz;
            double lo3 = a == 0 ? p.cx - half : a == 1 ? bottom : p.cz - half, hi3 = a == 0 ? p.cx + half : a == 1 ? top : p.cz + half;
            if (Math.abs(d) < 1e-12) {
                if (e < lo3 || e > hi3) return 0xFF000000;
                continue;
            }
            double ta = (lo3 - e) / d, tb = (hi3 - e) / d;
            t0 = Math.max(t0, Math.min(ta, tb));
            t1 = Math.min(t1, Math.max(ta, tb));
        }
        double r = 0, g = 0, b = 0, alpha = 0;
        // the grid plane at the radar's level, where the ray crosses it inside the box
        double tPlane = Math.abs(dy) > 1e-9 ? (top - ey) / dy : -1;
        boolean planeDone = !(tPlane > 0);
        if (t1 > t0) {
            double t = t0;
            while (t < t1 && alpha < 0.97) {
                if (!planeDone && tPlane <= t) {
                    planeDone = true;
                    double pf = plane(ex + dx * tPlane, ez + dz * tPlane, tPlane);
                    if (pf > 0) {
                        r += (1 - alpha) * 0.2 * pf * PLANE_A;
                        g += (1 - alpha) * 0.9 * pf * PLANE_A;
                        b += (1 - alpha) * 0.4 * pf * PLANE_A;
                        alpha += (1 - alpha) * PLANE_A;
                    }
                }
                double x = ex + dx * t, y = ey + dy * t, z = ez + dz * t;
                int c = cellAt(x, y, z);
                double step = 0.5;
                if (c >= 0) {
                    int k = c / (p.n * p.n);
                    step = Math.max(0.5, p.layers[k].cell() * 0.5);
                    if (shows[c]) {
                        // (the band on the cell's mean, or on its densest sample, drawn a little fainter: an ore vein a
                        // cell's mean hides still shows in a dense band)
                        double ps = smooth(SHARE_MIN, SHARE_FULL, share[c]);
                        double wgt = band.weight(flags[c], dens[c], peak[c], ps) * sig[c];
                        if (wgt > 0.002) {
                            double a = (1 - Math.exp(-OPACITY * wgt * step)) * Math.min(1, t / NEAR);
                            double fog = Math.exp(-t / FOG);
                            int col = band.byPeak(flags[c], dens[c], peak[c], ps) ? peakRgb[c] : rgb[c];
                            r += (1 - alpha) * a * ((col >> 16) & 255) / 255.0 * fog;
                            g += (1 - alpha) * a * ((col >> 8) & 255) / 255.0 * fog;
                            b += (1 - alpha) * a * (col & 255) / 255.0 * fog;
                            alpha += (1 - alpha) * a;
                        }
                    }
                }
                t += step;
            }
        }
        if (!planeDone) {
            double pf = plane(ex + dx * tPlane, ez + dz * tPlane, tPlane);
            if (pf > 0) {
                r += (1 - alpha) * 0.2 * pf * PLANE_A;
                g += (1 - alpha) * 0.9 * pf * PLANE_A;
                b += (1 - alpha) * 0.4 * pf * PLANE_A;
            }
        }
        int ri = (int) Math.min(255, Math.round(r * 255)), gi = (int) Math.min(255, Math.round(g * 255)), bi = (int) Math.min(255, Math.round(b * 255));
        return 0xFF000000 | ri << 16 | gi << 8 | bi;
    }

    /** The grid plane's opacity. */
    static final double PLANE_A = 0.5;

    /** The grid plane at (x, z) seen from t away: its fog factor (> 0) on a line inside the cone's top, else 0. */
    double plane(double x, double z, double t) {
        double ox = x - p.cx, oz = z - p.cz;
        if (Math.abs(ox) > half || Math.abs(oz) > half) return 0;
        double lx = Math.abs(ox - Math.round(ox / GRID) * GRID), lz = Math.abs(oz - Math.round(oz / GRID) * GRID);
        double width = Math.max(0.15, t * 0.004);   // (lines a pixel or so wide however far)
        if (Math.min(lx, lz) > width) return 0;
        return Math.exp(-t / FOG);
    }

    /** The depth (blocks under the radar) at y. */
    public double depthAt(double y) {
        return top - y;
    }

    /** The cone's box: half its width, its bottom and top y. */
    public double half() {
        return half;
    }

    public double bottom() {
        return bottom;
    }

    public double top() {
        return top;
    }
}
