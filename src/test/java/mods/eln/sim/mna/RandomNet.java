package mods.eln.sim.mna;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A random DC network for the property tests, and a reference solve of it independent of RootSystem/SubSystem:
 * plain modified nodal analysis (node voltages + one current per voltage source), dense Gaussian elimination with
 * partial pivoting. Node -1 is ground. Voltage sources only go node -> ground, at most one per node (so the
 * problem is always well posed).
 */
public final class RandomNet {
    /** A resistor (or connection) between nodes a and b (b = -1: ground). */
    public record Edge(int a, int b, double r) {
    }

    /** A current source pushing i from b into a (a, b = -1: ground). */
    public record Cs(int a, int b, double i) {
    }

    public final int n;
    public final List<Edge> edges = new ArrayList<>();
    /** Voltage of the source from node k to ground, or NaN for none. */
    public final double[] vs;
    public final List<Cs> cs = new ArrayList<>();

    public RandomNet(int n) {
        this.n = n;
        this.vs = new double[n];
        java.util.Arrays.fill(vs, Double.NaN);
    }

    /** log-uniform in [lo, hi] */
    public static double logUniform(Random rnd, double lo, double hi) {
        return Math.exp(Math.log(lo) + rnd.nextDouble() * (Math.log(hi) - Math.log(lo)));
    }

    /**
     * Connected (spanning tree plus extra edges), grounded at least once (a resistor or a source to ground),
     * resistances log-uniform in [rLo, rHi].
     */
    public static RandomNet random(Random rnd, int n, double rLo, double rHi, int currentSources) {
        RandomNet net = new RandomNet(n);
        for (int i = 1; i < n; i++) net.edges.add(new Edge(i, rnd.nextInt(i), logUniform(rnd, rLo, rHi)));
        int extra = rnd.nextInt(n + 1);
        for (int k = 0; k < extra; k++) {
            int a = rnd.nextInt(n), b = rnd.nextInt(n);
            if (a != b) net.edges.add(new Edge(a, b, logUniform(rnd, rLo, rHi)));
        }
        boolean grounded = false;
        for (int i = 0; i < n; i++) {
            if (rnd.nextDouble() < 0.3) {
                net.edges.add(new Edge(i, -1, logUniform(rnd, rLo, rHi)));
                grounded = true;
            }
            if (rnd.nextDouble() < 0.2) {
                net.vs[i] = Math.round((rnd.nextDouble() * 1000 - 500) * 100) / 100.0;
                grounded = true;
            }
        }
        if (!grounded) net.edges.add(new Edge(rnd.nextInt(n), -1, logUniform(rnd, rLo, rHi)));
        for (int k = 0; k < currentSources; k++) {
            int a = rnd.nextInt(n + 1) - 1, b = rnd.nextInt(n + 1) - 1;
            if (a == b) continue;
            net.cs.add(new Cs(a, b, Math.round((rnd.nextDouble() * 20 - 10) * 1000) / 1000.0));
        }
        return net;
    }

    /** Which nodes have a path to ground (through edges or a voltage source); only these have a defined voltage. */
    public boolean[] grounded(List<Edge> active) {
        boolean[] g = new boolean[n];
        for (int i = 0; i < n; i++) g[i] = !Double.isNaN(vs[i]);
        for (Edge e : active) if (e.b < 0) g[e.a] = true;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Edge e : active) {
                if (e.b < 0) continue;
                if (g[e.a] != g[e.b]) {
                    g[e.a] = g[e.b] = true;
                    changed = true;
                }
            }
        }
        return g;
    }

    /** Reference node voltages for the given edges (the grounded nodes only; others NaN). */
    public double[] solve(List<Edge> active) {
        boolean[] g = grounded(active);
        int[] idx = new int[n];
        int m = 0;
        for (int i = 0; i < n; i++) idx[i] = g[i] ? m++ : -1;
        int nv = 0;
        for (int i = 0; i < n; i++) if (g[i] && !Double.isNaN(vs[i])) nv++;
        int size = m + nv;
        double[][] a = new double[size][size + 1];
        for (Edge e : active) {
            int p = idx[e.a], q = e.b < 0 ? -1 : idx[e.b];
            if (p < 0) continue;
            double y = 1 / e.r;
            a[p][p] += y;
            if (q >= 0) {
                a[q][q] += y;
                a[p][q] -= y;
                a[q][p] -= y;
            }
        }
        int row = m;
        for (int i = 0; i < n; i++) {
            if (!g[i] || Double.isNaN(vs[i])) continue;
            a[idx[i]][row] += 1;
            a[row][idx[i]] += 1;
            a[row][size] = vs[i];
            row++;
        }
        for (Cs c : cs) {
            if (c.a >= 0 && idx[c.a] >= 0) a[idx[c.a]][size] += c.i;
            if (c.b >= 0 && idx[c.b] >= 0) a[idx[c.b]][size] -= c.i;
        }
        double[] x = gauss(a, size);
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = idx[i] < 0 ? Double.NaN : x[idx[i]];
        return out;
    }

    static double[] gauss(double[][] a, int size) {
        for (int c = 0; c < size; c++) {
            int piv = c;
            for (int r = c + 1; r < size; r++) if (Math.abs(a[r][c]) > Math.abs(a[piv][c])) piv = r;
            double[] t = a[c];
            a[c] = a[piv];
            a[piv] = t;
            for (int r = 0; r < size; r++) {
                if (r == c || a[r][c] == 0) continue;
                double f = a[r][c] / a[c][c];
                for (int k = c; k <= size; k++) a[r][k] -= f * a[c][k];
            }
        }
        double[] x = new double[size];
        for (int r = 0; r < size; r++) x[r] = a[r][size] / a[r][r];
        return x;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Net(n=" + n + ", edges=" + edges + ", vs=[");
        for (int i = 0; i < n; i++) if (!Double.isNaN(vs[i])) s.append(i).append(':').append(vs[i]).append(' ');
        return s.append("], cs=").append(cs).append(')').toString();
    }
}
