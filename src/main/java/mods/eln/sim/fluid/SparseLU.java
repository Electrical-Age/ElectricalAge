package mods.eln.sim.fluid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * A sparse LU for a fixed pattern (scale plan, board #61): the ordering and the fill are worked out once for a
 * pattern (a network's topology), then each solve only does arithmetic on arrays. Rows and columns are permuted
 * together by a minimum-degree order of the symmetric pattern; no pivoting (the fluid systems are diagonally weighted:
 * 1 + dt s g on each pressure row), with a check that every pivot is sizeable, else the caller falls back.
 */
public final class SparseLU {
    final int n;
    /** perm[k]: the original row/column eliminated k-th; inv[i]: its position. */
    final int[] perm, inv;
    /** The filled pattern of each permuted row, sorted: columns (permuted) below and above the diagonal. */
    final int[][] lower, upper;
    // numeric storage, parallel to lower/upper, plus the diagonal
    final double[][] lv, uv;
    final double[] diag;
    final double[] work;

    /** Builds the symbolic factorisation of a pattern: pattern[r] = the columns of row r (original indices). */
    public SparseLU(int[][] pattern) {
        n = pattern.length;
        // symmetric adjacency
        List<TreeSet<Integer>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new TreeSet<>());
        for (int r = 0; r < n; r++)
            for (int c : pattern[r])
                if (c != r) {
                    adj.get(r).add(c);
                    adj.get(c).add(r);
                }
        // minimum degree on the elimination graph (exact; fine for the sizes here)
        perm = new int[n];
        inv = new int[n];
        boolean[] gone = new boolean[n];
        java.util.PriorityQueue<long[]> heap = new java.util.PriorityQueue<>((p, q) -> p[0] != q[0] ? Long.compare(p[0], q[0]) : Long.compare(p[1], q[1]));
        for (int i = 0; i < n; i++) heap.add(new long[]{adj.get(i).size(), i});
        List<TreeSet<Integer>> g = new ArrayList<>(n);
        for (int i = 0; i < n; i++) g.add(new TreeSet<>(adj.get(i)));
        for (int k = 0; k < n; k++) {
            int p;
            while (true) {
                long[] t = heap.poll();
                p = (int) t[1];
                if (gone[p]) continue;
                if (t[0] != g.get(p).size()) {
                    heap.add(new long[]{g.get(p).size(), p});
                    continue;
                }
                break;
            }
            perm[k] = p;
            inv[p] = k;
            gone[p] = true;
            TreeSet<Integer> nb = g.get(p);
            for (int a : nb) {
                TreeSet<Integer> ga = g.get(a);
                ga.remove(p);
                for (int b : nb) if (b != a) ga.add(b);   // (the clique: fill)
            }
        }
        // the filled pattern in permuted indices: row k holds its original entries plus fill from eliminating lower ones
        List<TreeSet<Integer>> rows = new ArrayList<>(n);
        for (int k = 0; k < n; k++) rows.add(new TreeSet<>());
        for (int r = 0; r < n; r++) for (int c : pattern[r]) rows.get(inv[r]).add(inv[c]);
        for (int r = 0; r < n; r++) for (int c : adj.get(r)) rows.get(inv[r]).add(inv[c]);   // (symmetric, so fill is closed)
        for (int k = 0; k < n; k++) rows.get(k).add(k);
        for (int k = 0; k < n; k++) {
            // row-merge: for each j < k in row k (in increasing order), row k gains row j's upper part
            TreeSet<Integer> rk = rows.get(k);
            Integer j = rk.first();
            while (j != null && j < k) {
                for (int c : rows.get(j).tailSet(j, false)) rk.add(c);
                j = rk.higher(j);
            }
        }
        lower = new int[n][];
        upper = new int[n][];
        lv = new double[n][];
        uv = new double[n][];
        for (int k = 0; k < n; k++) {
            TreeSet<Integer> rk = rows.get(k);
            lower[k] = rk.headSet(k, false).stream().mapToInt(Integer::intValue).toArray();
            upper[k] = rk.tailSet(k, false).stream().mapToInt(Integer::intValue).toArray();
            lv[k] = new double[lower[k].length];
            uv[k] = new double[upper[k].length];
        }
        diag = new double[n];
        work = new double[n];
    }

    /** Factors A (it must fit the pattern) and solves A x = b; null if a pivot is too small. */
    public double[] solve(Sparse a, double[] b) {
        // numeric factorisation, row by row (up-looking Doolittle in permuted order)
        for (int k = 0; k < n; k++) {
            int r = perm[k];
            for (int t = 0; t < a.len[r]; t++) work[inv[a.cols[r][t]]] += a.vals[r][t];
            int[] lo = lower[k];
            for (int t = 0; t < lo.length; t++) {
                int j = lo[t];
                double l = work[j] / diag[j];
                work[j] = 0;
                lv[k][t] = l;
                if (l == 0) continue;
                int[] uj = upper[j];
                double[] vj = uv[j];
                for (int s = 0; s < uj.length; s++) work[uj[s]] -= l * vj[s];
            }
            double d = work[k];
            work[k] = 0;
            int[] up = upper[k];
            double rowMax = Math.abs(d);
            for (int s = 0; s < up.length; s++) {
                uv[k][s] = work[up[s]];
                work[up[s]] = 0;
                rowMax = Math.max(rowMax, Math.abs(uv[k][s]));
            }
            if (!(Math.abs(d) > 1e-12 * rowMax) || !Double.isFinite(d)) {
                Arrays.fill(work, 0);
                return null;
            }
            diag[k] = d;
        }
        // forward and back substitution
        double[] y = new double[n];
        for (int k = 0; k < n; k++) {
            double s = b[perm[k]];
            int[] lo = lower[k];
            for (int t = 0; t < lo.length; t++) s -= lv[k][t] * y[lo[t]];
            y[k] = s;
        }
        double[] x = new double[n];
        for (int k = n - 1; k >= 0; k--) {
            double s = y[k];
            int[] up = upper[k];
            for (int t = 0; t < up.length; t++) s -= uv[k][t] * y[up[t]];
            y[k] = s / diag[k];
            x[perm[k]] = y[k];
            if (!Double.isFinite(y[k])) return null;
        }
        return x;
    }
}
