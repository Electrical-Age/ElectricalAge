package org.erisia.cad.obj;

/**
 * Planar texture coordinates for a face, as tools/models/mcmodel.py makes them (vanilla density, 16 px a sprite):
 * projected along the face's dominant axis, upright on side faces, north up on top and bottom, shifted into 0..1
 * (atlas sprites don't wrap), squeezed if wider than a sprite. V up (OBJ's convention).
 */
public final class PlanarUv {
    private PlanarUv() {
    }

    /** Is the face's dominant axis a tie (a 45 degree face: either projection is right)? */
    public static boolean ambiguous(double nx, double ny, double nz) {
        double[] a = {Math.abs(nx), Math.abs(ny), Math.abs(nz)};
        java.util.Arrays.sort(a);
        return a[2] - a[1] < 1e-3;   // (normals of faces read back from 6-decimal OBJ text)
    }

    /** For corners xyz (px, n of them) with unit normal (nx, ny, nz): u v per corner. */
    public static double[] of(double[] xyz, int n, double nx, double ny, double nz) {
        double ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
        int axis = 0;   // (the first of the largest, as Python's max; near-ties go to the first too)
        if (ay > ax + 1e-9) axis = 1;
        if (az > Math.max(ax, ay) + 1e-9) axis = 2;
        double s = 1 / 16.0;
        double[] uv = new double[2 * n];
        for (int i = 0; i < n; i++) {
            double x = xyz[3 * i], y = xyz[3 * i + 1], z = xyz[3 * i + 2], u, v;
            if (axis == 1) {
                u = x * s;
                v = -z * s;
                if (ny < 0) v = -v;
            } else if (axis == 0) {
                u = (nx > 0 ? -z : z) * s;
                v = y * s;
            } else {
                u = (nz > 0 ? x : -x) * s;
                v = y * s;
            }
            uv[2 * i] = u;
            uv[2 * i + 1] = v;
        }
        double u0 = Double.MAX_VALUE, v0 = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            u0 = Math.min(u0, uv[2 * i]);
            v0 = Math.min(v0, uv[2 * i + 1]);
        }
        u0 = Math.floor(u0 + 1e-6);
        v0 = Math.floor(v0 + 1e-6);
        double k = 0;
        for (int i = 0; i < n; i++) {
            uv[2 * i] -= u0;
            uv[2 * i + 1] -= v0;
            k = Math.max(k, Math.max(uv[2 * i], uv[2 * i + 1]));
        }
        if (k > 1 + 1e-6) for (int i = 0; i < 2 * n; i++) uv[i] /= k;
        return uv;
    }
}
