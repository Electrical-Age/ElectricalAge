package mods.eln.item.xray;

/**
 * The x-ray scanner's boot progress bar (Baughn 2026-10-30: a progress bar for the startup, slightly irregular). The
 * boot time is cut into a few segments; each fills by a random share, some by none (a pause), so the bar stalls and
 * jumps. The shares come from a seed fixed for one boot, so the bar never flickers, only moves forward, and is full
 * exactly when the scanner is ready.
 */
public final class XRayBoot {
    private XRayBoot() {
    }

    static final int SEGMENTS = 12;

    /** The bar's fill (0..1) at boot time t (0..1) for a boot's seed: monotonic, 0 at 0, 1 at 1. */
    public static double fill(double t, long seed) {
        if (!(t > 0)) return 0;
        if (t >= 1) return 1;
        java.util.Random r = new java.util.Random(seed);
        double[] w = new double[SEGMENTS];
        double sum = 0;
        for (int i = 0; i < SEGMENTS; i++) {
            w[i] = r.nextDouble() < 0.3 ? 0 : 0.4 + r.nextDouble();
            sum += w[i];
        }
        if (sum <= 0) return t;
        double x = t * SEGMENTS;
        int seg = (int) x;
        double done = 0;
        for (int i = 0; i < seg; i++) done += w[i];
        done += w[seg] * (x - seg);
        return Math.min(1, done / sum);
    }

    /** Boot time (0..1) from the tick counter, which counts down from boot to 1 on the boot's last tick. */
    public static double time(int counter, int bootTicks) {
        return Math.max(0, Math.min(1, (bootTicks - counter) / (double) (bootTicks - 1)));
    }
}
