package mods.eln.sim.fluid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A square sparse matrix and its LU solve, for the fluid network's systems (scale plan, board #61): a node's row
 * touches only its neighbours, so a plant of 2,000 lumped nodes is a few thousand entries, not 4 million. The rows are
 * eliminated in a minimum-degree order on the symmetric pattern (little fill on the near-tree graphs pipe networks
 * are), pivoting within a row's own candidates when its diagonal is too small, and falling back to the dense solve if
 * the matrix turns out singular or badly ordered. The dense Gaussian elimination it replaces took 31 ms a step at 300
 * nodes and 4 s at 2,000.
 */
public final class Sparse {
    final int n;
    /** Row r's columns and values, unsorted, with duplicates summed on insert. */
    final int[][] cols;
    final double[][] vals;
    final int[] len;

    public Sparse(int n) {
        this.n = n;
        cols = new int[n][];
        vals = new double[n][];
        len = new int[n];
        for (int i = 0; i < n; i++) {
            cols[i] = new int[4];
            vals[i] = new double[4];
        }
    }

    public int size() {
        return n;
    }

    /** A[r][c] += v. */
    public void add(int r, int c, double v) {
        int[] cr = cols[r];
        for (int k = 0; k < len[r]; k++)
            if (cr[k] == c) {
                vals[r][k] += v;
                return;
            }
        if (len[r] == cr.length) {
            cols[r] = cr = Arrays.copyOf(cr, cr.length * 2);
            vals[r] = Arrays.copyOf(vals[r], cr.length);
        }
        cr[len[r]] = c;
        vals[r][len[r]++] = v;
    }

    public double get(int r, int c) {
        for (int k = 0; k < len[r]; k++) if (cols[r][k] == c) return vals[r][k];
        return 0;
    }

    /** Row r becomes the unit row (A[r][r] = 1, nothing else). */
    public void unitRow(int r) {
        len[r] = 0;
        add(r, r, 1);
    }

    /** The dense copy (tests, and the fallback). */
    public double[][] dense() {
        double[][] a = new double[n][n];
        for (int r = 0; r < n; r++) for (int k = 0; k < len[r]; k++) a[r][cols[r][k]] += vals[r][k];
        return a;
    }

    /** Solves A x = b (A and b are left as they were). */
    public double[] solve(double[] b) {
        if (n == 0) return new double[0];
        if (n <= DENSE_BELOW) return FluidNetwork.solve(dense(), b.clone());
        double[] x = sparseSolve(b);
        return x != null ? x : FluidNetwork.solve(dense(), b.clone());
    }

    /**
     * Solves with a cached symbolic factorisation (cache[0]), rebuilt when this matrix has an entry outside its
     * pattern; falls back to the general solve if a pivot is too small.
     */
    /**
     * Solves with a cached symbolic factorisation (cache.get(0L)) whose pattern only grows: the union of every pattern
     * this network has used (the liquid-gain unknowns a step needs, valves shut or open), so it is rebuilt only when a new
     * entry appears; falls back to the general solve if a pivot is too small.
     */
    public double[] solve(double[] b, java.util.Map<Long, SparseLU> cache) {
        if (n <= DENSE_BELOW) return solve(b);
        SparseLU lu = cache.get(0L);
        if (lu == null || lu.n != n || !fits(lu)) {
            int[][] pattern = new int[n][];
            for (int r = 0; r < n; r++) {
                java.util.TreeSet<Integer> cs = new java.util.TreeSet<>();
                for (int k = 0; k < len[r]; k++) cs.add(cols[r][k]);
                if (lu != null && lu.n == n) {
                    int kr = lu.inv[r];
                    for (int c : lu.lower[kr]) cs.add(lu.perm[c]);
                    for (int c : lu.upper[kr]) cs.add(lu.perm[c]);
                }
                pattern[r] = cs.stream().mapToInt(Integer::intValue).toArray();
            }
            lu = new SparseLU(pattern);
            cache.put(0L, lu);
        }
        double[] x = lu.solve(this, b);
        return x != null ? x : solve(b);
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Is every entry of this matrix inside lu's filled pattern? */
    private boolean fits(SparseLU lu) {
        for (int r = 0; r < n; r++) {
            int kr = lu.inv[r];
            for (int t = 0; t < len[r]; t++) {
                int kc = lu.inv[cols[r][t]];
                if (kc == kr) continue;
                int[] row = kc < kr ? lu.lower[kr] : lu.upper[kr];
                if (Arrays.binarySearch(row, kc) < 0) return false;
            }
        }
        return true;
    }

    /** Below this size the dense solve is as quick and needs no ordering. */
    static int DENSE_BELOW = 24;

    /**
     * Gaussian elimination with rows and columns taken in a minimum-degree order of the symmetric pattern: a pivot
     * (r, r) is eliminated from the rows that hold column r. Returns null if a pivot is too small (the caller falls back
     * to dense elimination with partial pivoting).
     */
    double[] sparseSolve(double[] b) {
        // working rows as maps col -> value (copied: A stays as it was)
        java.util.HashMap<Integer, Double>[] rows = rowMaps();
        // columns -> rows holding them (the symmetric pattern for the ordering, kept up to date as fill appears)
        List<java.util.HashSet<Integer>> colRows = new ArrayList<>(n);
        for (int i = 0; i < n; i++) colRows.add(new java.util.HashSet<>());
        for (int r = 0; r < n; r++) for (int c : rows[r].keySet()) colRows.get(c).add(r);
        double[] rhs = b.clone();
        boolean[] done = new boolean[n];
        int[] order = new int[n];
        // a min-heap by current degree (lazy: stale entries skipped)
        java.util.PriorityQueue<long[]> heap = new java.util.PriorityQueue<>((p, q) -> Long.compare(p[0], q[0]));
        for (int i = 0; i < n; i++) heap.add(new long[]{degree(i, rows, colRows), i});
        for (int step = 0; step < n; step++) {
            int p;
            while (true) {
                long[] top = heap.poll();
                if (top == null) return null;
                p = (int) top[1];
                if (done[p]) continue;
                long d = degree(p, rows, colRows);
                if (d != top[0]) {
                    heap.add(new long[]{d, p});
                    continue;
                }
                break;
            }
            order[step] = p;
            done[p] = true;
            java.util.HashMap<Integer, Double> pr = rows[p];
            Double pv = pr.get(p);
            double rowMax = 0;
            for (double v : pr.values()) rowMax = Math.max(rowMax, Math.abs(v));
            if (pv == null || Math.abs(pv) < 1e-12 * Math.max(rowMax, 1e-300)) return null;
            // eliminate column p from every other unfinished row holding it
            for (int r : new ArrayList<>(colRows.get(p))) {
                if (r == p || done[r]) continue;
                java.util.HashMap<Integer, Double> rr = rows[r];
                Double rv = rr.get(p);
                if (rv == null || rv == 0) continue;
                double f = rv / pv;
                for (var en : pr.entrySet()) {
                    int c = en.getKey();
                    if (c == p) continue;
                    double nv = rr.getOrDefault(c, 0.0) - f * en.getValue();
                    if (!rr.containsKey(c)) colRows.get(c).add(r);
                    rr.put(c, nv);
                }
                rr.remove(p);
                colRows.get(p).remove(r);
                rhs[r] -= f * rhs[p];
            }
        }
        // back substitution in reverse order: row order[k] holds its pivot and only columns eliminated after it
        double[] x = new double[n];
        for (int k = n - 1; k >= 0; k--) {
            int p = order[k];
            double s = rhs[p];
            double pv = 0;
            for (var en : rows[p].entrySet()) {
                int c = en.getKey();
                if (c == p) pv = en.getValue();
                else s -= en.getValue() * x[c];
            }
            x[p] = s / pv;
            if (!Double.isFinite(x[p])) return null;
        }
        return x;
    }

    @SuppressWarnings("unchecked")
    private java.util.HashMap<Integer, Double>[] rowMaps() {
        java.util.HashMap<Integer, Double>[] rows = new java.util.HashMap[n];
        for (int r = 0; r < n; r++) {
            rows[r] = new java.util.HashMap<>(len[r] * 2 + 1);
            for (int k = 0; k < len[r]; k++) rows[r].merge(cols[r][k], vals[r][k], Double::sum);
        }
        return rows;
    }

    /** The number of other rows and columns p touches (its degree in the elimination graph). */
    private static long degree(int p, java.util.HashMap<Integer, Double>[] rows, List<java.util.HashSet<Integer>> colRows) {
        return rows[p].size() + colRows.get(p).size();
    }
}
