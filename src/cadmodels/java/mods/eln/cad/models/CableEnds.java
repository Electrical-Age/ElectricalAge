package mods.eln.cad.models;

import org.erisia.cad.Mesh;

/**
 * Where a device's terminal meets a voxel signal cable (Baughn 2026-10-30, "zoom in on the connectors: a gap"): a
 * signal cable is 1 x 1 px, lying on the face the device is fixed to (x 8..9 in a six-node model), and it ends at the
 * block's edge; across, it covers p..p+1 from its point p, or p-1..p seen turned the other way round. A terminal meant to
 * meet it must reach the edge and cover that end either way. Checked when the models are made (the build fails if not).
 */
final class CableEnds {
    private CableEnds() {
    }

    /** Bounds of a mesh: {x0, y0, z0, x1, y1, z1}. */
    static double[] bounds(Mesh m) {
        double[] b = {1e9, 1e9, 1e9, -1e9, -1e9, -1e9};
        for (double[] p : m.polys)
            for (int i = 0; i + 2 < p.length; i += 3)
                for (int a = 0; a < 3; a++) {
                    b[a] = Math.min(b[a], p[i + a]);
                    b[a + 3] = Math.max(b[a + 3], p[i + a]);
                }
        return b;
    }

    /**
     * The terminal meets a cable ending at the edge whose outward normal is axis n (1 y, 2 z) at 16 (atHigh) or 0, the
     * cable's point p along axis a.
     */
    static void check(String model, Mesh terminal, int n, boolean atHigh, int a, double p) {
        double[] b = bounds(terminal);
        double eps = 0.01;
        String why = null;
        if (atHigh ? Math.abs(b[n + 3] - 16) > eps : Math.abs(b[n]) > eps) why = "doesn't reach the block's edge";
        else if (b[0] > 8 + eps || b[3] < 9 - eps) why = "doesn't cover the cable's height (x 8..9)";
        else if (b[a] > p - 1 + eps || b[a + 3] < p + 1 - eps) why = "doesn't cover the cable's end (" + (p - 1) + ".." + (p + 1) + " across)";
        if (why != null)
            throw new IllegalStateException(model + ": a terminal " + why + ": " + java.util.Arrays.toString(b));
    }
}
