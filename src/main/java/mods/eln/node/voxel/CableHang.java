package mods.eln.node.voxel;

import java.util.ArrayList;
import java.util.List;

/**
 * How a cable crossing the air hangs (Baughn 2026-11-12: chain-link physics run once at placement; the route and its
 * preview stay straight). The air stretch becomes a chain of ~1-voxel links between its two ends, a few percent longer
 * than the span; the two links at each end are held on the routed line, so the cable leaves and reaches its surface
 * (or a device's face) flat. Gravity pulls the rest down; links keep their length; a node that would enter a blocked
 * voxel (a block, another part) stays where it was. Relaxed for a fixed number of steps: a one-off, deterministic.
 * Pure Java: the world is behind CableRouter.Space. Positions in world voxels.
 */
public final class CableHang {
    private CableHang() {
    }

    /** How much longer than its span the chain is: ~12 % of the span deep at the middle. */
    static final double SLACK = 0.04;
    static final int STEPS = 200, PASSES = 8;
    static final double GRAVITY = 0.02, DAMPING = 0.9;

    /**
     * The chain for a stretch (CablePlacer.airStretches: {axis, other0, other1, from, to}) at height y0 of its routed
     * line (the line's own y if the axis is horizontal): node i sits at from + i * span / n on the line before it hangs.
     * Returns n + 1 points {x, y, z}.
     */
    static List<double[]> hang(CableRouter.Space space, long[] stretch) {
        int ax = (int) stretch[0];
        int[] o = VoxelGeometry.others(ax);
        double from = stretch[3], to = stretch[4], span = to - from;
        int n = (int) Math.max(4, Math.round(span));
        double rest = span * (1 + SLACK) / n;
        double[][] x = new double[n + 1][3], prev = new double[n + 1][3];
        for (int i = 0; i <= n; i++) {
            x[i][ax] = from + i * span / n;
            x[i][o[0]] = stretch[1];
            x[i][o[1]] = stretch[2];
            prev[i] = x[i].clone();
        }
        for (int step = 0; step < STEPS; step++) {
            for (int i = 2; i <= n - 2; i++) {
                double[] p = x[i], q = prev[i];
                double[] was = p.clone();
                for (int k = 0; k < 3; k++) p[k] += (p[k] - q[k]) * DAMPING;
                p[1] -= GRAVITY;
                prev[i] = was;
                if (blocked(space, p)) {
                    x[i] = was.clone();
                    prev[i] = was;
                }
            }
            for (int pass = 0; pass < PASSES; pass++)
                for (int i = 0; i < n; i++) {
                    double[] a = x[i], b = x[i + 1];
                    double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (d < 1e-9) continue;
                    double f = (d - rest) / d;
                    boolean fa = i < 2 || i > n - 2, fb = i + 1 < 2 || i + 1 > n - 2;   // (held: the two end links)
                    if (fa && fb) continue;
                    double wa = fa ? 0 : fb ? 1 : 0.5, wb = fb ? 0 : fa ? 1 : 0.5;
                    double[] na = {a[0] + dx * f * wa, a[1] + dy * f * wa, a[2] + dz * f * wa};
                    double[] nb = {b[0] - dx * f * wb, b[1] - dy * f * wb, b[2] - dz * f * wb};
                    if (!fa && !blocked(space, na)) x[i] = na;
                    if (!fb && !blocked(space, nb)) x[i + 1] = nb;
                }
        }
        List<double[]> out = new ArrayList<>();
        for (double[] p : x) out.add(p);
        return out;
    }

    /** Is the voxel a node sits in blocked? */
    static boolean blocked(CableRouter.Space space, double[] p) {
        long vx = (long) Math.floor(p[0]), vy = (long) Math.floor(p[1]), vz = (long) Math.floor(p[2]);
        return space.blocked(vx, vy, vz, vx + 1, vy + 1, vz + 1);
    }
}
