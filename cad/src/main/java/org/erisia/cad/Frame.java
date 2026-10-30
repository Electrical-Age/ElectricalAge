package org.erisia.cad;

/** An affine map (3x3 rotation-or-any matrix and a translation), px. Immutable. */
public final class Frame {
    public static final Frame IDENTITY = new Frame(new double[]{1, 0, 0, 0, 1, 0, 0, 0, 1}, 0, 0, 0);
    /** Row-major 3x3. */
    final double[] m;
    final double tx, ty, tz;

    Frame(double[] m, double tx, double ty, double tz) {
        this.m = m;
        this.tx = tx;
        this.ty = ty;
        this.tz = tz;
    }

    public static Frame translation(double x, double y, double z) {
        return new Frame(IDENTITY.m, x, y, z);
    }

    /** Rotation about x by a (radians, right-handed). */
    public static Frame rotX(double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Frame(new double[]{1, 0, 0, 0, c, -s, 0, s, c}, 0, 0, 0);
    }

    public static Frame rotY(double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Frame(new double[]{c, 0, s, 0, 1, 0, -s, 0, c}, 0, 0, 0);
    }

    public static Frame rotZ(double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Frame(new double[]{c, -s, 0, s, c, 0, 0, 0, 1}, 0, 0, 0);
    }

    public boolean identity() {
        return this == IDENTITY || (m == IDENTITY.m && tx == 0 && ty == 0 && tz == 0);
    }

    /** this after other: x -> this(other(x)). */
    public Frame then(Frame inner) {
        double[] r = new double[9];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) r[3 * i + j] = m[3 * i] * inner.m[j] + m[3 * i + 1] * inner.m[3 + j] + m[3 * i + 2] * inner.m[6 + j];
        double x = m[0] * inner.tx + m[1] * inner.ty + m[2] * inner.tz + tx;
        double y = m[3] * inner.tx + m[4] * inner.ty + m[5] * inner.tz + ty;
        double z = m[6] * inner.tx + m[7] * inner.ty + m[8] * inner.tz + tz;
        return new Frame(r, x, y, z);
    }

    /** Map the point at p[o..o+2] in place. */
    public void apply(double[] p, int o) {
        double x = p[o], y = p[o + 1], z = p[o + 2];
        p[o] = m[0] * x + m[1] * y + m[2] * z + tx;
        p[o + 1] = m[3] * x + m[4] * y + m[5] * z + ty;
        p[o + 2] = m[6] * x + m[7] * y + m[8] * z + tz;
    }
}
