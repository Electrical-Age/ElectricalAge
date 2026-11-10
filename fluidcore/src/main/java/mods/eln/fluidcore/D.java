package mods.eln.fluidcore;

/**
 * A value with its gradient against a fixed set of inputs (forward-mode differentiation, for MixFluid's exact slopes).
 * Immutable; every operation allocates.
 */
final class D {
    final double v;
    final double[] g;

    D(double v, double[] g) {
        this.v = v;
        this.g = g;
    }

    /** A constant (zero gradient) of n inputs. */
    static D c(double v, int n) {
        return new D(v, new double[n]);
    }

    /** Input i of n, at value v. */
    static D in(double v, int i, int n) {
        double[] g = new double[n];
        g[i] = 1;
        return new D(v, g);
    }

    int n() {
        return g.length;
    }

    D add(D o) {
        double[] r = new double[g.length];
        for (int i = 0; i < r.length; i++) r[i] = g[i] + o.g[i];
        return new D(v + o.v, r);
    }

    D sub(D o) {
        double[] r = new double[g.length];
        for (int i = 0; i < r.length; i++) r[i] = g[i] - o.g[i];
        return new D(v - o.v, r);
    }

    D mul(D o) {
        double[] r = new double[g.length];
        for (int i = 0; i < r.length; i++) r[i] = g[i] * o.v + v * o.g[i];
        return new D(v * o.v, r);
    }

    D div(D o) {
        double q = v / o.v;
        double[] r = new double[g.length];
        for (int i = 0; i < r.length; i++) r[i] = (g[i] - q * o.g[i]) / o.v;
        return new D(q, r);
    }

    D add(double c) {
        return new D(v + c, g);
    }

    D mul(double c) {
        double[] r = new double[g.length];
        for (int i = 0; i < r.length; i++) r[i] = g[i] * c;
        return new D(v * c, r);
    }

    D neg() {
        return mul(-1);
    }

    /** f(v) with f'(v) = d: the chain rule. */
    D map(double f, double d) {
        double[] r = new double[g.length];
        for (int i = 0; i < r.length; i++) r[i] = g[i] * d;
        return new D(f, r);
    }

    D exp() {
        double e = Math.exp(v);
        return map(e, e);
    }

    D log() {
        return map(Math.log(v), 1 / v);
    }

    /** a + b x (for sums of terms). */
    static D lin(D a, double b, D x) {
        double[] r = new double[a.g.length];
        for (int i = 0; i < r.length; i++) r[i] = a.g[i] + b * x.g[i];
        return new D(a.v + b * x.v, r);
    }

    /** Smooth max(0, x) over width w: w softplus(x / w), with w itself an input. */
    static D softplus(D x, D w) {
        double t = x.v / w.v;
        double sp = t > 30 ? t : t < -30 ? Math.exp(t) : Math.log1p(Math.exp(t));
        double sig = t > 30 ? 1 : t < -30 ? Math.exp(t) : 1 / (1 + Math.exp(-t));
        double dw = sp - t * sig;
        double[] r = new double[x.g.length];
        for (int i = 0; i < r.length; i++) r[i] = sig * x.g[i] + dw * w.g[i];
        return new D(w.v * sp, r);
    }
}
