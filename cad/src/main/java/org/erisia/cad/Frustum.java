package org.erisia.cad;

/**
 * A capped prism or frustum along an axis (0 x, 1 y, 2 z of its frame), centred at (c1, c2) on the other two (in x y z
 * order), radius r0 at a0 and r1 at a1, with S sides whose corners sit at angles 2 pi (k + phase) / S (phase 0.5: a flat
 * side facing each axis direction when S is a multiple of 4). A cylinder is a frustum with r0 = r1. Faces: kind SIDE
 * (index k), CAP0 at a0 and CAP1 at a1 (index k: one triangle per side, as a quad with the centre repeated).
 */
public final class Frustum extends Solid {
    public static final int SIDE = 2, CAP0 = 3, CAP1 = 4;
    public final int axis, sides;
    public final double c1, c2, r0, r1, a0, a1, phase;
    public boolean cap0 = true, cap1 = true;

    public Frustum(int axis, double c1, double c2, double r0, double r1, double a0, double a1, int sides, double phase) {
        this.axis = axis;
        this.c1 = c1;
        this.c2 = c2;
        this.r0 = r0;
        this.r1 = r1;
        this.a0 = a0;
        this.a1 = a1;
        this.sides = sides;
        this.phase = phase;
    }

    /** A cylinder: 12 sides, a flat side facing each axis direction. */
    public static Frustum cylinder(int axis, double c1, double c2, double r, double a0, double a1) {
        return new Frustum(axis, c1, c2, r, r, a0, a1, 12, 0.5);
    }

    public Frustum open(boolean at0, boolean at1) {
        cap0 = !at0;
        cap1 = !at1;
        return this;
    }

    /** The two axes across it, in order. */
    public static int[] others(int axis) {
        return axis == 0 ? new int[]{1, 2} : axis == 1 ? new int[]{0, 2} : new int[]{0, 1};
    }

    public double radius() {
        return Math.max(r0, r1);
    }

    @Override
    public double thickness() {
        return Math.min(2 * Math.max(r0, r1), a1 - a0);
    }

    @Override
    public void emit(FaceSink sink) {
        int S = sides;
        int[] o = others(axis);
        double g = layer * LAYER, lo = a0 - g, hi = a1 + g;
        double[][] ring0 = new double[S][], ring1 = new double[S][];
        for (int k = 0; k < S; k++) {
            double ang = 2 * Math.PI * (k + phase) / S;
            double[] v0 = new double[3], v1 = new double[3];
            v0[o[0]] = c1 + (r0 + g) * Math.cos(ang);
            v0[o[1]] = c2 + (r0 + g) * Math.sin(ang);
            v1[o[0]] = c1 + (r1 + g) * Math.cos(ang);
            v1[o[1]] = c2 + (r1 + g) * Math.sin(ang);
            v0[axis] = lo;
            v1[axis] = hi;
            ring0[k] = v0;
            ring1[k] = v1;
        }
        Frame f = placeOrNull();
        for (int k = 0; k < S; k++) {
            int k1 = (k + 1) % S;
            double ang = 2 * Math.PI * (k + phase + 0.5) / S;
            double[] nrm = new double[3];
            nrm[o[0]] = Math.cos(ang);
            nrm[o[1]] = Math.sin(ang);
            quad(sink, f, ring0[k], ring0[k1], ring1[k1], ring1[k], nrm[0], nrm[1], nrm[2], SIDE, k, k, k1, S + k1, S + k);
        }
        double[] cc0 = new double[3], cc1 = new double[3];
        cc0[o[0]] = cc1[o[0]] = c1;
        cc0[o[1]] = cc1[o[1]] = c2;
        cc0[axis] = lo;
        cc1[axis] = hi;
        double[] n0 = new double[3], n1 = new double[3];
        n0[axis] = -1;
        n1[axis] = 1;
        for (int k = 0; k < S; k++) {
            int k1 = (k + 1) % S;
            if (cap0) quad(sink, f, cc0, cc0, ring0[k1], ring0[k], n0[0], n0[1], n0[2], CAP0, k, 2 * S, 2 * S, k1, k);
            if (cap1) quad(sink, f, cc1, cc1, ring1[k], ring1[k1], n1[0], n1[1], n1[2], CAP1, k, 2 * S + 1, 2 * S + 1, S + k, S + k1);
        }
    }
}
