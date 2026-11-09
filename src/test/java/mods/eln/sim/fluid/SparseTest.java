package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Sparse.solve agrees with the dense solve on random graph-shaped systems, small and past the dense cut-off. */
class SparseTest {
    @Test
    void agreesWithDense() {
        for (long seed = 1; seed <= 200; seed++) {
            Random r = new Random(seed);
            int n = 2 + r.nextInt(seed % 4 == 0 ? 300 : 60);
            Sparse a = new Sparse(n);
            // a tree plus a few loops, rows diagonally weighted as the fluid systems are (1 + dt s g on the diagonal)
            for (int i = 0; i < n; i++) a.add(i, i, 1);
            for (int i = 1; i < n; i++) {
                int j = r.nextInt(i);
                double g = Math.pow(10, -2 + 4 * r.nextDouble());
                a.add(i, i, g);
                a.add(i, j, -g);
                double h = Math.pow(10, -2 + 4 * r.nextDouble());
                a.add(j, j, h);
                a.add(j, i, -h);
            }
            for (int k = 0; k < n / 10; k++) {
                int i = r.nextInt(n), j = r.nextInt(n);
                if (i != j) a.add(i, j, -0.1 * r.nextDouble());
            }
            double[] b = new double[n];
            for (int i = 0; i < n; i++) b[i] = r.nextGaussian() * 1e5;
            double[] xs = a.solve(b), xd = FluidNetwork.solve(a.dense(), b.clone()), xc = a.solve(b, new java.util.HashMap<>());
            for (int i = 0; i < n; i++) {
                assertEquals(xd[i], xs[i], 1e-7 * Math.max(1, Math.abs(xd[i])), "seed " + seed + " n " + n + " row " + i);
                assertEquals(xd[i], xc[i], 1e-7 * Math.max(1, Math.abs(xd[i])), "cached, seed " + seed + " n " + n + " row " + i);
            }
        }
    }
}
