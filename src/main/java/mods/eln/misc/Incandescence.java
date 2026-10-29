package mods.eln.misc;

import mods.eln.gl.GL11;
import mods.eln.sim.PhysicalConstant;

/**
 * Hot things glow (Baughn 2026-10-28: "everything that heats up on the outside"). One rule for every device whose
 * surface gets hot: the Large Rheostat, the electric heater, power resistors, thermal cables (six-node and voxel).
 * <ul>
 * <li>Colour and brightness follow the temperature: nothing below the Draper point (525 C), dull red, cherry, orange,
 * yellow, white at 1300 C. Brightness goes with black-body radiance (T^4), square-rooted for the eye.</li>
 * <li>Client: the hot part is drawn again on top of itself, full-bright and additive (it adds its own light to whatever
 * the world lights it with), and a second additive pass pushes very hot parts toward yellow-white; a soft camera-facing
 * halo fakes bloom.</li>
 * <li>Server: the device's own block emits block light by temperature ({@link Light}), with hysteresis, a check every
 * half second and a per-tick budget of light changes across the level, so a long hot cable run can't flood the light
 * engine.</li>
 * </ul>
 * Temperatures here are EA's (relative to the ambient, like ThermalLoad.Tc) unless a name says celsius.
 */
public final class Incandescence {
    private Incandescence() {
    }

    /** Where steel starts to glow (C), and where the scale tops out (white). */
    public static final double DRAPER_C = 525, WHITE_C = 1300;
    /** Radiance is 1 here (C): brightness goes past 1 above it, so the additive passes saturate toward white. */
    static final double FULL_C = 1100;

    /** EA's temperature (relative to ambient) to Celsius. */
    public static double celsius(double tc) {
        return tc + PhysicalConstant.Tamb;
    }

    /**
     * For devices that burn out long before they could glow (the power resistor: 130 C): its own scale, so it shows dull
     * red from 60% of its limit to orange at the limit (an overheating warning; Baughn: "for charm").
     */
    public static double warningCelsius(double tc, double warmLimit) {
        double f = (tc - 0.6 * warmLimit) / (0.4 * warmLimit);
        return f <= 0 ? 0 : DRAPER_C + Math.min(1.2, f) * (900 - DRAPER_C);
    }

    /** Brightness of the glow, 0 below the Draper point, 1 at FULL_C, up to 1.3: sqrt of the radiance above dark. */
    public static float intensity(double celsius) {
        if (celsius <= DRAPER_C) return 0;
        double k = celsius + 273.15, k0 = DRAPER_C + 273.15, k1 = FULL_C + 273.15;
        double g = (Math.pow(k / k0, 4) - 1) / (Math.pow(k1 / k0, 4) - 1);
        return (float) Math.min(1.3, Math.sqrt(g));
    }

    /** What a smith sees, by temperature (C): dull red, blood red, cherry, orange-red, orange, yellow-orange, yellow, white. */
    static final double[] HEAT_C = {525, 650, 750, 850, 950, 1050, 1150, 1300};
    static final float[][] HEAT_RGB = {
        {0.45f, 0.02f, 0f}, {0.75f, 0.05f, 0f}, {0.95f, 0.12f, 0f}, {1f, 0.30f, 0.02f},
        {1f, 0.50f, 0.08f}, {1f, 0.70f, 0.20f}, {1f, 0.85f, 0.45f}, {1f, 0.97f, 0.85f}};

    /** The glow's colour at a temperature (C), interpolated in HEAT_RGB. */
    public static float[] color(double celsius) {
        if (celsius <= HEAT_C[0]) return HEAT_RGB[0].clone();
        for (int i = 1; i < HEAT_C.length; i++) {
            if (celsius <= HEAT_C[i]) {
                float f = (float) ((celsius - HEAT_C[i - 1]) / (HEAT_C[i] - HEAT_C[i - 1]));
                float[] a = HEAT_RGB[i - 1], b = HEAT_RGB[i];
                return new float[]{a[0] + f * (b[0] - a[0]), a[1] + f * (b[1] - a[1]), a[2] + f * (b[2] - a[2])};
            }
        }
        return HEAT_RGB[HEAT_RGB.length - 1].clone();
    }

    /** The glow's look in 25 K steps (0: dark): publish a device's temperature when this changes. */
    public static int step(double celsius) {
        return celsius <= DRAPER_C - 25 ? 0 : 1 + (int) ((celsius - (DRAPER_C - 25)) / 25);
    }

    /** A step's temperature (C), the middle of its 25 K band (the client's side of {@link #step}). */
    public static double stepCelsius(int step) {
        return step <= 0 ? 0 : DRAPER_C - 25 + (step - 0.5) * 25;
    }

    // ---------------------------------------------------------------- block light (server)

    /** Light level thresholds (C): 1 at the Draper point, ~2 dull red, ~8 orange (900), 13-15 white-hot. */
    static final double[] LIGHT_C = {525, 575, 650, 700, 750, 800, 850, 900, 950, 1000, 1050, 1100, 1150, 1250, 1350};
    /** A light only dims once the part is this much (K) below the step it went up at: no flicker at a step. */
    public static final double HYSTERESIS = 25;
    /** At most this many light changes a tick in a level (each relights a block); the rest wait for the next check. */
    public static final int BUDGET_PER_TICK = 16;
    /** Ticks between a device's checks (half a second). */
    public static final int CHECK_TICKS = 10;

    /** The block light (0..15) of a part at a temperature (C), without hysteresis. */
    public static int lightLevel(double celsius) {
        int n = 0;
        for (double t : LIGHT_C) if (celsius >= t) n++;
        return n;
    }

    /** The next light level from the current one at a temperature (C): up at once, down only HYSTERESIS below. */
    public static int nextLevel(int current, double celsius) {
        int up = lightLevel(celsius);
        if (up >= current) return up;
        return Math.min(current, lightLevel(celsius + HYSTERESIS));
    }

    private static final java.util.Map<Object, long[]> BUDGET = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** Take one light change from the level's budget for this tick (false: spent, try at the next check). */
    static boolean spend(net.minecraft.world.level.Level level, long gameTime) {
        if (level == null) return true;
        long[] b = BUDGET.computeIfAbsent(level, k -> new long[]{-1, 0});
        synchronized (b) {
            if (b[0] != gameTime) {
                b[0] = gameTime;
                b[1] = 0;
            }
            if (b[1] >= BUDGET_PER_TICK) return false;
            b[1]++;
            return true;
        }
    }

    /** A device's glow light: call {@link #update} from its slow process; it says when the level changed. */
    public static final class Light {
        public int level;
        private long due = Long.MIN_VALUE;
        /** The first check always reports: a node loaded from a save keeps the light it had (Node "lastLight"). */
        private boolean fresh = true;

        /** True if the light level changed (the caller sets its node's light value then). */
        public boolean update(net.minecraft.world.level.Level world, double celsius) {
            long now = world == null ? 0 : world.getGameTime();
            if (world != null && now < due) return false;
            due = now + CHECK_TICKS;
            int next = nextLevel(fresh ? 0 : level, celsius);
            if ((next == level && !fresh) || !spend(world, now)) return false;
            level = next;
            fresh = false;
            return true;
        }
    }

    // ---------------------------------------------------------------- drawing (client, through the GL11 shim)

    /**
     * Draw a hot part's glow over the part already drawn (normally lit): the same geometry again, full-bright and
     * additive in the heat colour, and once more toward white when very hot. Nothing below the Draper point.
     */
    public static void drawGlow(double celsius, Runnable part) {
        float e = intensity(celsius);
        if (e <= 0) return;
        float[] c = color(celsius);
        GL11.glPushAttrib(0);
        GL11.setFullBright(true);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        float k = Math.min(1f, e);
        GL11.glColor4f(c[0] * k, c[1] * k, c[2] * k, 1f);
        part.run();
        float white = (float) Math.max(0, Math.min(1, (celsius - 1000) / 300)) * 0.6f + Math.max(0, e - 1f);
        if (white > 0) {
            GL11.glColor4f(white, white * 0.95f, white * 0.8f, 1f);
            part.run();
        }
        GL11.glPopAttrib();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    /**
     * The outer surface of a voxel block's hot cable boxes, once per publish (VoxelNodeEntity): a face cell is kept only
     * if the voxel just outside it isn't inside another box (the joints between a run's pieces, overlaps at bends) and
     * not on the block's boundary (the joint with the next block); coincident faces count once; each face's cells are
     * merged into rectangles. Additive boxes drawn whole doubled up there: bright lines at every joint and cap (AdventurAgent).
     * In: {celsius, x0, y0, z0, x1, y1, z1} in voxels. Out: {celsius, axis, max side 0/1, plane, u0, v0, u1, v1}.
     */
    public static java.util.List<float[]> cableSurface(java.util.List<float[]> boxes) {
        java.util.List<float[]> out = new java.util.ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        int n = boxes.size();
        for (int i = 0; i < n; i++) {
            float[] b = boxes.get(i);
            int[] lo = {(int) b[1], (int) b[2], (int) b[3]}, hi = {(int) b[4], (int) b[5], (int) b[6]};
            for (int a = 0; a < 3; a++) {
                int u = (a + 1) % 3, v = (a + 2) % 3;
                for (int side = 0; side < 2; side++) {
                    int plane = side == 1 ? hi[a] : lo[a];
                    if (plane <= 0 || plane >= 16) continue;   // (the joint with the next block, or the block's edge)
                    boolean[][] cell = new boolean[16][16];
                    boolean any = false;
                    for (int cu = lo[u]; cu < hi[u]; cu++)
                        for (int cv = lo[v]; cv < hi[v]; cv++) {
                            double[] q = new double[3];
                            q[a] = plane + (side == 1 ? 0.5 : -0.5);
                            q[u] = cu + 0.5;
                            q[v] = cv + 0.5;
                            boolean hidden = false;
                            for (int j = 0; j < n && !hidden; j++) {
                                if (j == i) continue;
                                float[] o = boxes.get(j);
                                hidden = q[0] > o[1] && q[0] < o[4] && q[1] > o[2] && q[1] < o[5] && q[2] > o[3] && q[2] < o[6];
                            }
                            long key = ((((long) a * 2 + side) * 17 + plane) * 17 + cu) * 17 + cv;
                            if (hidden || !seen.add(key)) continue;
                            cell[cu][cv] = true;
                            any = true;
                        }
                    if (!any) continue;
                    // greedy rectangles: a row's run, grown over the following rows while they have the same run
                    for (int cv = 0; cv < 16; cv++)
                        for (int cu = 0; cu < 16; cu++) {
                            if (!cell[cu][cv]) continue;
                            int end = cu;
                            while (end + 1 < 16 && cell[end + 1][cv]) end++;
                            int top = cv;
                            grow:
                            while (top + 1 < 16) {
                                for (int k = cu; k <= end; k++) if (!cell[k][top + 1]) break grow;
                                top++;
                            }
                            for (int r = cv; r <= top; r++) for (int k = cu; k <= end; k++) cell[k][r] = false;
                            out.add(new float[]{b[0], a, side, plane, cu, cv, end + 1, top + 1});
                            cu = end;
                        }
                }
            }
        }
        return out;
    }

    /**
     * The glow of a voxel block's hot thermal cables (SixNodeRender, origin at the block's centre): the surface strips of
     * {@link #cableSurface}, a hair proud of the cable, additive and untextured (the cable is in the chunk mesh), and
     * one small halo per block at the hottest strip.
     */
    public static void drawCableGlow(java.util.List<float[]> strips, int daylight) {
        if (strips.isEmpty()) return;
        float hot = 0, cx = 0, cy = 0, cz = 0;
        GL11.glPushAttrib(0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        for (float[] f : strips) {
            int a = (int) f[1], u = (a + 1) % 3, v = (a + 2) % 3;
            float plane = (f[3] + (f[2] == 1 ? 0.1f : -0.1f)) / 16 - 0.5f;
            float u0 = f[4] / 16 - 0.5f, v0 = f[5] / 16 - 0.5f, u1 = f[6] / 16 - 0.5f, v1 = f[7] / 16 - 0.5f;
            float[][] corners = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
            float[] nrm = new float[3];
            nrm[a] = f[2] == 1 ? 1 : -1;
            drawGlow(f[0], () -> {
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glNormal3f(nrm[0], nrm[1], nrm[2]);
                for (float[] c : corners) {
                    float[] p = new float[3];
                    p[a] = plane;
                    p[u] = c[0];
                    p[v] = c[1];
                    GL11.glVertex3f(p[0], p[1], p[2]);
                }
                GL11.glEnd();
            });
            if (f[0] > hot) {
                hot = f[0];
                float[] p = new float[3];
                p[a] = plane;
                p[u] = (u0 + u1) / 2;
                p[v] = (v0 + v1) / 2;
                cx = p[0];
                cy = p[1];
                cz = p[2];
            }
        }
        GL11.glPopAttrib();
        GL11.glPushMatrix();
        GL11.glTranslatef(cx, cy, cz);
        drawHalo(hot, 0.4f, daylight);
        GL11.glPopMatrix();
    }

    static final int HALO_SEGMENTS = 16;

    /**
     * A soft halo round the current origin, facing the camera: an additive disc bright in the middle, black (adds
     * nothing) at its rim. size = its radius (blocks) at full glow.
     */
    public static void drawHalo(double celsius, float size, net.minecraft.world.level.block.entity.BlockEntity be) {
        drawHalo(celsius, size, be == null ? 0 : UtilsClient.hooks.skyLight(be.getLevel(), be.getBlockPos()));
    }

    /** As above, with the daylight (0..15) on the block: in full daylight a halo keeps 15% (Baughn: subtle by day). */
    public static void drawHalo(double celsius, float size, int daylight) {
        float e = intensity(celsius);
        if (e <= 0) return;
        float[] c = color(celsius);
        float day = 1f - 0.85f * Math.max(0, Math.min(15, daylight)) / 15f;
        float r = size * (0.45f + 0.55f * Math.min(1.3f, e)), k = 0.35f * Math.min(1f, e) * day;
        GL11.glPushMatrix();
        if (!GL11.faceCamera()) {
            GL11.glPopMatrix();
            return;
        }
        GL11.glPushAttrib(0);
        GL11.setFullBright(true);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glBegin(GL11.GL_QUADS);
        // two rings: a bright core to half the radius, then a fall-off to black at the rim
        float half = r * 0.4f;
        for (int i = 0; i < HALO_SEGMENTS; i++) {
            double a0 = 2 * Math.PI * i / HALO_SEGMENTS, a1 = 2 * Math.PI * (i + 1) / HALO_SEGMENTS;
            float x0 = (float) Math.cos(a0), y0 = (float) Math.sin(a0), x1 = (float) Math.cos(a1), y1 = (float) Math.sin(a1);
            GL11.glColor4f(c[0] * k, c[1] * k, c[2] * k, 1f);
            GL11.glVertex3f(0, 0, 0);
            GL11.glVertex3f(0, 0, 0);
            GL11.glColor4f(c[0] * k * 0.55f, c[1] * k * 0.55f, c[2] * k * 0.55f, 1f);
            GL11.glVertex3f(x1 * half, y1 * half, 0);
            GL11.glVertex3f(x0 * half, y0 * half, 0);

            GL11.glVertex3f(x0 * half, y0 * half, 0);
            GL11.glVertex3f(x1 * half, y1 * half, 0);
            GL11.glColor4f(0, 0, 0, 1f);
            GL11.glVertex3f(x1 * r, y1 * r, 0);
            GL11.glVertex3f(x0 * r, y0 * r, 0);
        }
        GL11.glEnd();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}
