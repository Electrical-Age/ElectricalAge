package mods.eln.misc;

import java.util.ArrayList;
import java.util.List;

/**
 * A device's shape from its model's triangles (Baughn 2026-10-30: "hitboxes should be computed from geometry"): the
 * triangles are voxelised on a grid over the block (0..1), and the solid cells merged into a few boxes. A cell is solid
 * when its centre is inside the mesh (two of three axis rays cross it an odd number of times, so an open flap doesn't
 * fill a half-space) or a triangle passes through it; a triangle that lies on cell faces only (a panel on a grid line)
 * fills the cells beside it. With closing (a dilation then an erosion by one cell), gaps up to two cells wide and inner
 * corners are filled (Baughn's collision choice A: players stand on what they see without slipping into slots or
 * snagging on bolts). Over maxBoxes boxes, the grid is halved. Pure.
 */
public final class ModelShape {
    private ModelShape() {
    }

    /** Collision and outline grids (cells a block) and the most boxes a shape keeps before it coarsens. */
    public static final int FINE = 16, COARSE = 8, MAX_BOXES = 24;

    /**
     * The boxes (x0 y0 z0 x1 y1 z1, block units) of triangles (9 coordinates each, block units; what lies outside
     * 0..1 is cut off) on a grid of n cells a block, coarsened while over maxBoxes; empty for no triangles.
     */
    public static List<double[]> boxes(List<double[]> triangles, int n, boolean close, int maxBoxes) {
        for (int g = n; ; g /= 2) {
            List<double[]> b = boxesAt(triangles, g, close);
            if (b.size() <= maxBoxes || g <= 2) return b;
        }
    }

    /** The outline: fine grid, no closing. */
    public static List<double[]> outline(List<double[]> triangles) {
        return boxes(triangles, FINE, false, MAX_BOXES);
    }

    /** The collision: coarse grid (2 px cells), gaps up to two cells closed. */
    public static List<double[]> collision(List<double[]> triangles) {
        return boxes(triangles, COARSE, true, MAX_BOXES);
    }

    static List<double[]> boxesAt(List<double[]> triangles, int n, boolean close) {
        boolean[] solid = voxels(triangles, n);
        if (close) solid = erode(dilate(solid, n), n);
        return merge(solid, n);
    }

    /** The solid cells (index (x * n + y) * n + z). */
    static boolean[] voxels(List<double[]> tris, int n) {
        boolean[] solid = new boolean[n * n * n];
        if (tris.isEmpty()) return solid;
        // rays: along each axis from each cell centre column, the triangles whose shadow on the other two axes holds it
        List<List<double[]>>[] bins = binned(tris, n);
        double h = 1.0 / n;
        for (int x = 0; x < n; x++)
            for (int y = 0; y < n; y++)
                for (int z = 0; z < n; z++) {
                    // (jittered off the grid lines, so a ray through an edge shared by two triangles isn't counted twice)
                    double cx = (x + 0.5) * h + 1.3e-7, cy = (y + 0.5) * h + 2.1e-7, cz = (z + 0.5) * h + 3.7e-7;
                    int odd = 0;
                    if ((crossings(bins[0].get(y * n + z), 0, cx, cy, cz) & 1) == 1) odd++;
                    if ((crossings(bins[1].get(x * n + z), 1, cx, cy, cz) & 1) == 1) odd++;
                    if (odd < 2 && (crossings(bins[2].get(x * n + y), 2, cx, cy, cz) & 1) == 1) odd++;
                    if (odd >= 2) solid[(x * n + y) * n + z] = true;
                }
        double eps = 1e-6 * h;
        for (double[] t : tris) {
            int[] r = range(t, n);
            if (r == null) continue;
            boolean any = false;
            for (int x = r[0]; x <= r[3]; x++)
                for (int y = r[1]; y <= r[4]; y++)
                    for (int z = r[2]; z <= r[5]; z++)
                        if (overlaps(t, (x + 0.5) * h, (y + 0.5) * h, (z + 0.5) * h, h / 2 - eps)) {
                            solid[(x * n + y) * n + z] = true;
                            any = true;
                        }
            if (any) continue;
            // on cell faces only: the cells it touches, unless one of them is solid already (a face of a filled body)
            List<Integer> touched = new ArrayList<>();
            boolean held = false;
            for (int x = r[0]; x <= r[3]; x++)
                for (int y = r[1]; y <= r[4]; y++)
                    for (int z = r[2]; z <= r[5]; z++)
                        if (overlaps(t, (x + 0.5) * h, (y + 0.5) * h, (z + 0.5) * h, h / 2 + eps)) {
                            int i = (x * n + y) * n + z;
                            touched.add(i);
                            held |= solid[i];
                        }
            if (!held) for (int i : touched) solid[i] = true;
        }
        return solid;
    }

    /** For each axis a, the triangles by the cell column (of the other two axes) their shadow touches. */
    @SuppressWarnings("unchecked")
    static List<List<double[]>>[] binned(List<double[]> tris, int n) {
        List<List<double[]>>[] bins = new List[3];
        for (int a = 0; a < 3; a++) {
            bins[a] = new ArrayList<>(n * n);
            for (int i = 0; i < n * n; i++) bins[a].add(new ArrayList<>());
        }
        for (double[] t : tris) {
            int[] r = range(t, n);
            if (r == null) continue;
            for (int a = 0; a < 3; a++) {
                int u = a == 0 ? 1 : 0, v = a == 2 ? 1 : 2;
                for (int i = r[u]; i <= r[u + 3]; i++) for (int j = r[v]; j <= r[v + 3]; j++) bins[a].get(i * n + j).add(t);
            }
        }
        return bins;
    }

    /** The cells (inclusive, clamped to the grid) of t's bounding box, or null if it misses the block. */
    static int[] range(double[] t, int n) {
        int[] r = new int[6];
        for (int a = 0; a < 3; a++) {
            double lo = Math.min(t[a], Math.min(t[a + 3], t[a + 6])), hi = Math.max(t[a], Math.max(t[a + 3], t[a + 6]));
            if (hi < 0 || lo > 1) return null;
            r[a] = Math.max(0, Math.min(n - 1, (int) Math.floor(lo * n - 1e-9)));
            r[a + 3] = Math.max(0, Math.min(n - 1, (int) Math.floor(hi * n + 1e-9)));
        }
        return r;
    }

    /** How many of tris a ray from (cx, cy, cz) towards +axis crosses. */
    static int crossings(List<double[]> tris, int axis, double cx, double cy, double cz) {
        int u = axis == 0 ? 1 : 0, v = axis == 2 ? 1 : 2;
        double[] c = {cx, cy, cz};
        double pu = c[u], pv = c[v], pa = c[axis];
        int k = 0;
        for (double[] t : tris) {
            double u0 = t[u], v0 = t[v], u1 = t[3 + u], v1 = t[3 + v], u2 = t[6 + u], v2 = t[6 + v];
            double d = (v1 - v2) * (u0 - u2) + (u2 - u1) * (v0 - v2);
            if (Math.abs(d) < 1e-15) continue;   // (seen edge-on: the ray runs along it)
            double l0 = ((v1 - v2) * (pu - u2) + (u2 - u1) * (pv - v2)) / d;
            double l1 = ((v2 - v0) * (pu - u2) + (u0 - u2) * (pv - v2)) / d;
            double l2 = 1 - l0 - l1;
            if (l0 < 0 || l1 < 0 || l2 < 0) continue;
            double hit = l0 * t[axis] + l1 * t[3 + axis] + l2 * t[6 + axis];
            if (hit > pa) k++;
        }
        return k;
    }

    /** Does triangle t meet the cube of half side e about (cx, cy, cz)? (Separating axes, Akenine-Moller.) */
    static boolean overlaps(double[] t, double cx, double cy, double cz, double e) {
        double[] v0 = {t[0] - cx, t[1] - cy, t[2] - cz}, v1 = {t[3] - cx, t[4] - cy, t[5] - cz}, v2 = {t[6] - cx, t[7] - cy, t[8] - cz};
        for (int a = 0; a < 3; a++)   // the cube's faces
            if (Math.min(v0[a], Math.min(v1[a], v2[a])) > e || Math.max(v0[a], Math.max(v1[a], v2[a])) < -e) return false;
        double[][] edges = {sub(v1, v0), sub(v2, v1), sub(v0, v2)};
        double[] nrm = cross(edges[0], edges[1]);
        double r0 = e * (Math.abs(nrm[0]) + Math.abs(nrm[1]) + Math.abs(nrm[2]));
        if (Math.abs(dot(nrm, v0)) > r0) return false;   // the triangle's plane
        for (double[] f : edges)
            for (int a = 0; a < 3; a++) {   // the edges against the cube's axes
                double[] ax = cross(unit(a), f);
                double p0 = dot(ax, v0), p1 = dot(ax, v1), p2 = dot(ax, v2);
                double r = e * (Math.abs(ax[0]) + Math.abs(ax[1]) + Math.abs(ax[2]));
                if (Math.min(p0, Math.min(p1, p2)) > r || Math.max(p0, Math.max(p1, p2)) < -r) return false;
            }
        return true;
    }

    static boolean[] dilate(boolean[] s, int n) {
        boolean[] o = new boolean[s.length];
        for (int x = 0; x < n; x++)
            for (int y = 0; y < n; y++)
                for (int z = 0; z < n; z++) {
                    boolean v = false;
                    for (int dx = -1; dx <= 1 && !v; dx++)
                        for (int dy = -1; dy <= 1 && !v; dy++)
                            for (int dz = -1; dz <= 1 && !v; dz++) {
                                int a = x + dx, b = y + dy, c = z + dz;
                                if (a >= 0 && b >= 0 && c >= 0 && a < n && b < n && c < n) v = s[(a * n + b) * n + c];
                            }
                    o[(x * n + y) * n + z] = v;
                }
        return o;
    }

    /** Erosion in the grid (the block's sides count as solid, so a body against them keeps its outer cells). */
    static boolean[] erode(boolean[] s, int n) {
        boolean[] o = new boolean[s.length];
        for (int x = 0; x < n; x++)
            for (int y = 0; y < n; y++)
                for (int z = 0; z < n; z++) {
                    boolean v = s[(x * n + y) * n + z];
                    for (int dx = -1; dx <= 1 && v; dx++)
                        for (int dy = -1; dy <= 1 && v; dy++)
                            for (int dz = -1; dz <= 1 && v; dz++) {
                                int a = x + dx, b = y + dy, c = z + dz;
                                if (a >= 0 && b >= 0 && c >= 0 && a < n && b < n && c < n) v = s[(a * n + b) * n + c];
                            }
                    o[(x * n + y) * n + z] = v;
                }
        return o;
    }

    /** Greedy boxes: from each free solid cell, as far as possible along z, then y, then x. */
    static List<double[]> merge(boolean[] s, int n) {
        boolean[] used = new boolean[s.length];
        List<double[]> out = new ArrayList<>();
        double h = 1.0 / n;
        for (int x = 0; x < n; x++)
            for (int y = 0; y < n; y++)
                for (int z = 0; z < n; z++) {
                    if (!s[(x * n + y) * n + z] || used[(x * n + y) * n + z]) continue;
                    int z1 = z;
                    while (z1 + 1 < n && free(s, used, n, x, x, y, y, z1 + 1, z1 + 1)) z1++;
                    int y1 = y;
                    while (y1 + 1 < n && free(s, used, n, x, x, y1 + 1, y1 + 1, z, z1)) y1++;
                    int x1 = x;
                    while (x1 + 1 < n && free(s, used, n, x1 + 1, x1 + 1, y, y1, z, z1)) x1++;
                    for (int a = x; a <= x1; a++) for (int b = y; b <= y1; b++) for (int c = z; c <= z1; c++) used[(a * n + b) * n + c] = true;
                    out.add(new double[]{x * h, y * h, z * h, (x1 + 1) * h, (y1 + 1) * h, (z1 + 1) * h});
                }
        return out;
    }

    static boolean free(boolean[] s, boolean[] used, int n, int x0, int x1, int y0, int y1, int z0, int z1) {
        for (int a = x0; a <= x1; a++)
            for (int b = y0; b <= y1; b++)
                for (int c = z0; c <= z1; c++) {
                    int i = (a * n + b) * n + c;
                    if (!s[i] || used[i]) return false;
                }
        return true;
    }

    static double[] sub(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] unit(int a) {
        double[] u = new double[3];
        u[a] = 1;
        return u;
    }
}
