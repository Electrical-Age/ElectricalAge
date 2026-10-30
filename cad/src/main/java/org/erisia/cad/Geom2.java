package org.erisia.cad;

/** Geom's helpers for the other packages. */
public final class Geom2 {
    private Geom2() {
    }

    public static double[] distinct(Face f) {
        return Geom.distinct(f);
    }

    public static double[] uvAt(Face f, double[] p) {
        return Geom.uvAt(f, p, 0);
    }

    /** Is p (on the face's plane) inside the face (convex)? */
    public static boolean inside(Face f, double[] p) {
        double[] d = Geom.distinct(f);
        int n = d.length / 3;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double ex = d[3 * j] - d[3 * i], ey = d[3 * j + 1] - d[3 * i + 1], ez = d[3 * j + 2] - d[3 * i + 2];
            double px = p[0] - d[3 * i], py = p[1] - d[3 * i + 1], pz = p[2] - d[3 * i + 2];
            double cx = ey * pz - ez * py, cy = ez * px - ex * pz, cz = ex * py - ey * px;
            if (cx * f.nx + cy * f.ny + cz * f.nz < -1e-6) return false;
        }
        return true;
    }
}
