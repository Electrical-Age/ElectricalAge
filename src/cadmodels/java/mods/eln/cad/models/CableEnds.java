package mods.eln.cad.models;

import org.erisia.cad.Mesh;

/**
 * Where a device's terminal meets a voxel signal cable (Baughn 2026-10-30, "zoom in on the connectors: a gap"; 2026-10-31,
 * "they z-fight with the cable knuckle"): a
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

    /** How far a terminal reaches past the block's edge (px): just past the cable's end and knuckle, never level with them. */
    static final double PAST = 0.1;
    /** The least a terminal may stand clear of a cable knuckle's faces (px; the checker's z-fight distance). */
    static final double CLEAR = 0.05;

    /**
     * The terminal meets a cable ending at the edge whose outward normal is axis n (1 y, 2 z) at 16 (atHigh) or 0, the
     * cable's point p along axis a. It must cover the cable's end, and a cable knuckle there (where a floor cable turns
     * up a wall to a wall-mounted device: a 1 px cube in the corner, x 8..9, 0..1 in from the edge, p-1..p+1 across)
     * must lie inside it, clear of all its faces but the one on the face the device is fixed to (Baughn 2026-10-31: the
     * connectors' sides z-fought with the knuckle's, at the same distance from the block).
     */
    static void check(String model, Mesh terminal, int n, boolean atHigh, int a, double p) {
        double[] b = bounds(terminal);
        String why = null;
        double past = atHigh ? b[n + 3] - 16 : -b[n];
        if (past < CLEAR || past > 0.5) why = "should reach just past the block's edge (by " + CLEAR + " to 0.5 px), reaches " + past;
        else if (b[0] > 8 + 0.01 || b[3] < 9 + CLEAR) why = "doesn't clear the cable's and knuckle's height (x 8..9)";
        else if (b[a] > p - 1 - CLEAR || b[a + 3] < p + 1 + CLEAR) why = "doesn't clear the cable's end and knuckle across (" + (p - 1) + ".." + (p + 1) + ")";
        else if (atHigh ? b[n] > 15 - CLEAR : b[n + 3] < 1 + CLEAR) why = "doesn't clear the knuckle's inner face (1 px in from the edge)";
        if (why != null)
            throw new IllegalStateException(model + ": a terminal " + why + ": " + java.util.Arrays.toString(b));
    }
}
