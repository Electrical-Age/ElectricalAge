package org.erisia.cad;

/**
 * A flat ring (a hollow cylinder) along an axis, centred at (c1, c2), radii rIn..rOut, from a0 to a1, in n segments with
 * corners at angles 2 pi k / n. Faces per segment k: OUTER, INNER, END0 (at a0), END1 (at a1).
 */
public final class Ring extends Solid {
    public static final int OUTER = 5, INNER = 6, END0 = 7, END1 = 8;
    public final int axis, n;
    public final double c1, c2, rIn, rOut, a0, a1;
    /** Corners at 2 pi (k + phase) / n; no inner wall (it sits on a body). */
    public double phase;
    public boolean inner = true;

    public Ring(int axis, double c1, double c2, double rIn, double rOut, double a0, double a1, int n) {
        this.axis = axis;
        this.c1 = c1;
        this.c2 = c2;
        this.rIn = rIn;
        this.rOut = rOut;
        this.a0 = a0;
        this.a1 = a1;
        this.n = n;
    }

    @Override
    public double thickness() {
        return Math.min(rOut - rIn, a1 - a0);
    }

    double[] at(int[] o, double r, double t, double a) {
        double[] v = new double[3];
        v[o[0]] = c1 + r * Math.cos(t);
        v[o[1]] = c2 + r * Math.sin(t);
        v[axis] = a;
        return v;
    }

    @Override
    public void emit(FaceSink sink) {
        int[] o = Frustum.others(axis);
        double g = layer * LAYER, lo = a0 - g, hi = a1 + g, ri = rIn - g, ro = rOut + g;
        Frame f = placeOrNull();
        for (int k = 0; k < n; k++) {
            double t0 = 2 * Math.PI * (k + phase) / n, t1 = 2 * Math.PI * (k + 1 + phase) / n, tm = (t0 + t1) / 2;
            double[] i00 = at(o, ri, t0, lo), i10 = at(o, ri, t1, lo), i01 = at(o, ri, t0, hi), i11 = at(o, ri, t1, hi);
            double[] o00 = at(o, ro, t0, lo), o10 = at(o, ro, t1, lo), o01 = at(o, ro, t0, hi), o11 = at(o, ro, t1, hi);
            double[] out = new double[3], down = new double[3];
            out[o[0]] = Math.cos(tm);
            out[o[1]] = Math.sin(tm);
            down[axis] = -1;
            int k1 = (k + 1) % n, N = n;
            quad(sink, f, o00, o10, o11, o01, out[0], out[1], out[2], OUTER, k, 2 * N + k, 2 * N + k1, 3 * N + k1, 3 * N + k);
            if (inner) quad(sink, f, i00, i10, i11, i01, -out[0], -out[1], -out[2], INNER, k, k, k1, N + k1, N + k);
            quad(sink, f, i00, i10, o10, o00, down[0], down[1], down[2], END0, k, k, k1, 2 * N + k1, 2 * N + k);
            quad(sink, f, i01, i11, o11, o01, -down[0], -down[1], -down[2], END1, k, N + k, N + k1, 3 * N + k1, 3 * N + k);
        }
    }
}
