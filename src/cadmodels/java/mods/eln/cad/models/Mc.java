package mods.eln.cad.models;

import org.erisia.cad.Mesh;

/**
 * mcmodel.py's primitives (the Python code-CAD the device models were first written in), corner for corner, as
 * Meshes: px, the block 0..16, y up, corner origin. Axes are 'x', 'y', 'z'; c1, c2 are the centre in the other two
 * axes in xyz order. NaN for an optional value means mcmodel's default.
 */
public final class Mc {
    private Mc() {
    }

    public static double[] v(double x, double y, double z) {
        return new double[]{x, y, z};
    }

    // ---- polygons ----

    /** mcmodel.normal: the unit normal from the fan's cross products, or null if degenerate. */
    public static double[] normal(double[] p) {
        double nx = 0, ny = 0, nz = 0;
        int n = p.length / 3;
        for (int i = 1; i < n - 1; i++) {
            double ax = p[3 * i] - p[0], ay = p[3 * i + 1] - p[1], az = p[3 * i + 2] - p[2];
            double bx = p[3 * i + 3] - p[0], by = p[3 * i + 4] - p[1], bz = p[3 * i + 5] - p[2];
            nx += ay * bz - az * by;
            ny += az * bx - ax * bz;
            nz += ax * by - ay * bx;
        }
        double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
        return l < 1e-9 ? null : new double[]{nx / l, ny / l, nz / l};
    }

    /** mcmodel.oriented: the corners wound so the normal points along the hint. */
    public static double[][] oriented(double[][] poly, double[] hint) {
        double[] flat = flat(poly);
        double[] n = normal(flat);
        if (n == null || n[0] * hint[0] + n[1] * hint[1] + n[2] * hint[2] >= 0) return poly;
        double[][] r = new double[poly.length][];
        for (int i = 0; i < poly.length; i++) r[i] = poly[poly.length - 1 - i];
        return r;
    }

    static double[] flat(double[][] poly) {
        double[] p = new double[3 * poly.length];
        for (int i = 0; i < poly.length; i++) System.arraycopy(poly[i], 0, p, 3 * i, 3);
        return p;
    }

    // ---- box ----

    public static Mesh box(double x0, double y0, double z0, double x1, double y1, double z1, String mat) {
        return box(x0, y0, z0, x1, y1, z1, mat, "nsewud");
    }

    /** Faces by letter: u (+y), d (-y), s (+z), n (-z), e (+x), w (-x). */
    public static Mesh box(double x0, double y0, double z0, double x1, double y1, double z1, String mat, String faces) {
        Mesh m = new Mesh(mat);
        if (faces.contains("u")) m.poly(v(x0, y1, z0), v(x0, y1, z1), v(x1, y1, z1), v(x1, y1, z0));
        if (faces.contains("d")) m.poly(v(x0, y0, z0), v(x1, y0, z0), v(x1, y0, z1), v(x0, y0, z1));
        if (faces.contains("s")) m.poly(v(x0, y0, z1), v(x1, y0, z1), v(x1, y1, z1), v(x0, y1, z1));
        if (faces.contains("n")) m.poly(v(x1, y0, z0), v(x0, y0, z0), v(x0, y1, z0), v(x1, y1, z0));
        if (faces.contains("e")) m.poly(v(x1, y0, z1), v(x1, y0, z0), v(x1, y1, z0), v(x1, y1, z1));
        if (faces.contains("w")) m.poly(v(x0, y0, z0), v(x0, y0, z1), v(x0, y1, z1), v(x0, y1, z0));
        m.closed = m.convex = faces.length() == 6;
        return m;
    }

    // ---- axis frames ----

    /** (a, c1, c2) -> x y z for an axis. */
    static double[] f(char axis, double a, double p, double q) {
        return axis == 'x' ? v(a, p, q) : axis == 'y' ? v(p, a, q) : v(p, q, a);
    }

    /** Is (c1, c2, a) right-handed for this axis (keeps the winding counter-clockwise)? */
    static boolean rh(char axis) {
        return axis != 'y';
    }

    static double[][] ring(char axis, double c1, double c2, double a, double rr, int n, double ph) {
        double[][] r = new double[n][];
        for (int i = 0; i < n; i++) r[i] = f(axis, a, c1 + rr * Math.cos(ph + 2 * Math.PI * i / n), c2 + rr * Math.sin(ph + 2 * Math.PI * i / n));
        return r;
    }

    // ---- prism ----

    public static Mesh prism(char axis, double c1, double c2, double r, double a0, double a1, String mat) {
        return prism(axis, c1, c2, r, a0, a1, mat, 12, Double.NaN, true, true, Double.NaN);
    }

    public static Mesh prism(char axis, double c1, double c2, double r, double a0, double a1, String mat, int n) {
        return prism(axis, c1, c2, r, a0, a1, mat, n, Double.NaN, true, true, Double.NaN);
    }

    public static Mesh prism(char axis, double c1, double c2, double r, double a0, double a1, String mat, int n, boolean cap0, boolean cap1) {
        return prism(axis, c1, c2, r, a0, a1, mat, n, Double.NaN, cap0, cap1, Double.NaN);
    }

    /** n-gon cylinder, or a frustum to rEnd, along axis from a0 to a1 (a0 < a1); phase (radians) default pi / n: a flat side up. */
    public static Mesh prism(char axis, double c1, double c2, double r, double a0, double a1, String mat, int n, double rEnd, boolean cap0,
                             boolean cap1, double phase) {
        boolean right = rh(axis);
        double re = Double.isNaN(rEnd) ? r : rEnd, ph = Double.isNaN(phase) ? Math.PI / n : phase;
        double[][] lo = ring(axis, c1, c2, a0, r, n, ph), hi = ring(axis, c1, c2, a1, re, n, ph);
        Mesh m = new Mesh(mat);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            if (right) m.poly(lo[i], lo[j], hi[j], hi[i]);
            else m.poly(lo[j], lo[i], hi[i], hi[j]);
        }
        if (cap0 && r > 0) m.poly(right ? reversed(lo) : lo);
        if (cap1 && re > 0) m.poly(right ? hi : reversed(hi));
        m.closed = m.convex = cap0 && cap1 && r > 0 && re > 0;
        return m;
    }

    static double[][] reversed(double[][] a) {
        double[][] r = new double[a.length][];
        for (int i = 0; i < a.length; i++) r[i] = a[a.length - 1 - i];
        return r;
    }

    // ---- tube ----

    public static Mesh tube(char axis, double c1, double c2, double rIn, double rOut, double a0, double a1, String mat) {
        return tube(axis, c1, c2, rIn, rOut, a0, a1, mat, 12, Double.NaN, true);
    }

    public static Mesh tube(char axis, double c1, double c2, double rIn, double rOut, double a0, double a1, String mat, int n) {
        return tube(axis, c1, c2, rIn, rOut, a0, a1, mat, n, Double.NaN, true);
    }

    /** Annulus: outer wall, inner wall (inner false: it sits on a body), flat ring ends. */
    public static Mesh tube(char axis, double c1, double c2, double rIn, double rOut, double a0, double a1, String mat, int n, double phase,
                            boolean inner) {
        boolean right = rh(axis);
        double ph = Double.isNaN(phase) ? Math.PI / n : phase;
        double[][] o0 = ring(axis, c1, c2, a0, rOut, n, ph), o1 = ring(axis, c1, c2, a1, rOut, n, ph), i0 = ring(axis, c1, c2, a0, rIn, n, ph),
            i1 = ring(axis, c1, c2, a1, rIn, n, ph);
        Mesh m = new Mesh(mat);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            if (right) {
                m.poly(o0[i], o0[j], o1[j], o1[i]);
                if (inner) m.poly(i0[j], i0[i], i1[i], i1[j]);
                m.poly(o0[j], o0[i], i0[i], i0[j]);
                m.poly(o1[i], o1[j], i1[j], i1[i]);
            } else {
                m.poly(o0[j], o0[i], o1[i], o1[j]);
                if (inner) m.poly(i0[i], i0[j], i1[j], i1[i]);
                m.poly(o0[i], o0[j], i0[j], i0[i]);
                m.poly(o1[j], o1[i], i1[i], i1[j]);
            }
        }
        m.closed = inner;
        return m;
    }

    // ---- volute ----

    /** A scroll casing (mcmodel.volute): radius r0 to r1 over a turn from angle start, a radial tongue wall, caps fanned from the centre. */
    public static Mesh volute(char axis, double c1, double c2, double r0, double r1, double a0, double a1, String mat, int n, double start) {
        int ai = axis - 'x';
        Mesh m = new Mesh(mat);
        double[] angs = new double[n + 1], rad = new double[n + 1];
        for (int k = 0; k <= n; k++) {
            angs[k] = start + 2 * Math.PI * k / n;
            rad[k] = r0 + (r1 - r0) * k / n;
        }
        for (int k = 0; k < n; k++) {
            double[][] q = {pt(axis, c1, c2, a0, angs[k], rad[k]), pt(axis, c1, c2, a0, angs[k + 1], rad[k + 1]),
                pt(axis, c1, c2, a1, angs[k + 1], rad[k + 1]), pt(axis, c1, c2, a1, angs[k], rad[k])};
            m.poly(oriented(q, radial(axis, (angs[k] + angs[k + 1]) / 2)));
            for (int s = -1; s <= 1; s += 2) {
                double a = s < 0 ? a0 : a1;
                double[][] tri = {f(axis, a, c1, c2), pt(axis, c1, c2, a, angs[k], rad[k]), pt(axis, c1, c2, a, angs[k + 1], rad[k + 1])};
                double[] u = new double[3];
                u[ai] = s;
                m.poly(oriented(tri, u));
            }
        }
        double[] tang = f(axis, 0, -Math.sin(start), Math.cos(start));
        m.poly(oriented(new double[][]{pt(axis, c1, c2, a0, start, r0), pt(axis, c1, c2, a0, start, r1), pt(axis, c1, c2, a1, start, r1),
            pt(axis, c1, c2, a1, start, r0)}, tang));
        m.closed = true;
        return m;
    }

    static double[] pt(char axis, double c1, double c2, double a, double ang, double rr) {
        return f(axis, a, c1 + rr * Math.cos(ang), c2 + rr * Math.sin(ang));
    }

    static double[] radial(char axis, double ang) {
        return f(axis, 0, Math.cos(ang), Math.sin(ang));
    }
}
