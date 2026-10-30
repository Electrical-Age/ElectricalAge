package org.erisia.cad;

import java.util.ArrayList;
import java.util.List;

/** Geometry for the checker: polygon overlap in a plane, separating-axis distances between convex hulls. */
public final class Geom {
    private Geom() {
    }

    /** The face's corners without repeats (a capped cylinder's triangles come as quads). */
    static double[] distinct(Face f) {
        double[] out = new double[f.xyz.length];
        int m = 0;
        for (int i = 0; i < f.n; i++) {
            double x = f.xyz[3 * i], y = f.xyz[3 * i + 1], z = f.xyz[3 * i + 2];
            boolean dup = false;
            for (int j = 0; j < m && !dup; j++)
                dup = Math.abs(out[3 * j] - x) < 1e-9 && Math.abs(out[3 * j + 1] - y) < 1e-9 && Math.abs(out[3 * j + 2] - z) < 1e-9;
            if (dup) continue;
            out[3 * m] = x;
            out[3 * m + 1] = y;
            out[3 * m + 2] = z;
            m++;
        }
        return java.util.Arrays.copyOf(out, 3 * m);
    }

    /** A basis (u, v) in the plane of normal n. */
    static double[][] basis(double nx, double ny, double nz) {
        double[] u = Math.abs(nx) < 0.9 ? new double[]{0, -nz, ny} : new double[]{nz, 0, -nx};
        double l = Math.sqrt(u[0] * u[0] + u[1] * u[1] + u[2] * u[2]);
        u[0] /= l;
        u[1] /= l;
        u[2] /= l;
        double[] v = {ny * u[2] - nz * u[1], nz * u[0] - nx * u[2], nx * u[1] - ny * u[0]};
        return new double[][]{u, v};
    }

    /** The polygon's corners in the plane basis, counter-clockwise. */
    static double[] project(double[] xyz, double[][] b) {
        int n = xyz.length / 3;
        double[] p = new double[2 * n];
        for (int i = 0; i < n; i++) {
            p[2 * i] = xyz[3 * i] * b[0][0] + xyz[3 * i + 1] * b[0][1] + xyz[3 * i + 2] * b[0][2];
            p[2 * i + 1] = xyz[3 * i] * b[1][0] + xyz[3 * i + 1] * b[1][1] + xyz[3 * i + 2] * b[1][2];
        }
        if (area2(p) < 0) {
            for (int i = 0; i < n / 2; i++) {
                int j = n - 1 - i;
                double tx = p[2 * i], ty = p[2 * i + 1];
                p[2 * i] = p[2 * j];
                p[2 * i + 1] = p[2 * j + 1];
                p[2 * j] = tx;
                p[2 * j + 1] = ty;
            }
        }
        return p;
    }

    /** Signed area of a 2D polygon. */
    static double area2(double[] p) {
        int n = p.length / 2;
        double s = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            s += p[2 * i] * p[2 * j + 1] - p[2 * j] * p[2 * i + 1];
        }
        return s / 2;
    }

    /** The intersection of two convex counter-clockwise 2D polygons (Sutherland-Hodgman). */
    static double[] clip(double[] subject, double[] clipper) {
        double[] out = subject;
        int m = clipper.length / 2;
        for (int e = 0; e < m && out.length > 0; e++) {
            double ax = clipper[2 * e], ay = clipper[2 * e + 1], bx = clipper[2 * ((e + 1) % m)], by = clipper[2 * ((e + 1) % m) + 1];
            List<Double> next = new ArrayList<>();
            int n = out.length / 2;
            for (int i = 0; i < n; i++) {
                double px = out[2 * i], py = out[2 * i + 1], qx = out[2 * ((i + 1) % n)], qy = out[2 * ((i + 1) % n) + 1];
                double sp = (bx - ax) * (py - ay) - (by - ay) * (px - ax), sq = (bx - ax) * (qy - ay) - (by - ay) * (qx - ax);
                if (sp >= 0) {
                    next.add(px);
                    next.add(py);
                }
                if ((sp >= 0) != (sq >= 0)) {
                    double t = sp / (sp - sq);
                    next.add(px + t * (qx - px));
                    next.add(py + t * (qy - py));
                }
            }
            out = new double[next.size()];
            for (int i = 0; i < out.length; i++) out[i] = next.get(i);
        }
        return out;
    }

    /** The overlap of two faces in a shared plane (px^2), and its polygon in 3D on face a's plane (null if none). */
    static double[] overlap(Face a, Face b, double[] areaOut) {
        double[] pa = distinct(a), pb = distinct(b);
        areaOut[0] = 0;
        if (pa.length < 9 || pb.length < 9) return null;
        double[][] bs = basis(a.nx, a.ny, a.nz);
        double[] p2 = clip(project(pa, bs), project(pb, bs));
        if (p2.length < 6) return null;
        areaOut[0] = Math.abs(area2(p2));
        double d = a.plane();
        int n = p2.length / 2;
        double[] xyz = new double[3 * n];
        for (int i = 0; i < n; i++)
            for (int k = 0; k < 3; k++) xyz[3 * i + k] = p2[2 * i] * bs[0][k] + p2[2 * i + 1] * bs[1][k] + d * (k == 0 ? a.nx : k == 1 ? a.ny : a.nz);
        return xyz;
    }

    /**
     * The face's texture coordinates at point p (on its plane), from the affine map its corners define; null if the
     * face has no UVs or its corners are collinear.
     */
    static double[] uvAt(Face f, double[] p, int o) {
        if (f.uv == null) return null;
        // three corners spanning the face
        int[] idx = new int[3];
        int m = 0;
        for (int i = 0; i < f.n && m < 3; i++) {
            boolean dup = false;
            for (int j = 0; j < m; j++)
                dup |= Math.abs(f.xyz[3 * i] - f.xyz[3 * idx[j]]) + Math.abs(f.xyz[3 * i + 1] - f.xyz[3 * idx[j] + 1]) + Math.abs(f.xyz[3 * i + 2] - f.xyz[3 * idx[j] + 2]) < 1e-9;
            if (!dup) idx[m++] = i;
        }
        if (m < 3) return null;
        double[][] b = basis(f.nx, f.ny, f.nz);
        double[] q = new double[6];
        for (int j = 0; j < 3; j++)
            for (int k = 0; k < 2; k++) q[2 * j + k] = f.xyz[3 * idx[j]] * b[k][0] + f.xyz[3 * idx[j] + 1] * b[k][1] + f.xyz[3 * idx[j] + 2] * b[k][2];
        double px = p[o] * b[0][0] + p[o + 1] * b[0][1] + p[o + 2] * b[0][2], py = p[o] * b[1][0] + p[o + 1] * b[1][1] + p[o + 2] * b[1][2];
        double e1x = q[2] - q[0], e1y = q[3] - q[1], e2x = q[4] - q[0], e2y = q[5] - q[1], det = e1x * e2y - e1y * e2x;
        if (Math.abs(det) < 1e-12) return null;
        double s = ((px - q[0]) * e2y - (py - q[1]) * e2x) / det, t = (e1x * (py - q[1]) - e1y * (px - q[0])) / det;
        double[] uv = new double[2];
        for (int k = 0; k < 2; k++)
            uv[k] = f.uv[2 * idx[0] + k] + s * (f.uv[2 * idx[1] + k] - f.uv[2 * idx[0] + k]) + t * (f.uv[2 * idx[2] + k] - f.uv[2 * idx[0] + k]);
        return uv;
    }

    /** A solid's hull: its corners and the directions of its faces' normals and edges. */
    static final class Hull {
        final double[] pts;
        final List<double[]> normals = new ArrayList<>(), edges = new ArrayList<>();

        Hull(List<Face> faces) {
            List<Double> p = new ArrayList<>();
            for (Face f : faces) {
                for (double v : f.xyz) p.add(v);
                if (f.nx != 0 || f.ny != 0 || f.nz != 0) addDir(normals, f.nx, f.ny, f.nz);
                double[] d = distinct(f);
                int n = d.length / 3;
                for (int i = 0; i < n; i++) {
                    int j = (i + 1) % n;
                    double ex = d[3 * j] - d[3 * i], ey = d[3 * j + 1] - d[3 * i + 1], ez = d[3 * j + 2] - d[3 * i + 2];
                    double l = Math.sqrt(ex * ex + ey * ey + ez * ez);
                    if (l > 1e-9) addDir(edges, ex / l, ey / l, ez / l);
                }
            }
            pts = new double[p.size()];
            for (int i = 0; i < pts.length; i++) pts[i] = p.get(i);
        }

        static void addDir(List<double[]> list, double x, double y, double z) {
            for (double[] d : list) if (Math.abs(Math.abs(d[0] * x + d[1] * y + d[2] * z) - 1) < 1e-9) return;
            list.add(new double[]{x, y, z});
        }

        double[] span(double[] ax) {
            double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
            for (int i = 0; i < pts.length; i += 3) {
                double v = pts[i] * ax[0] + pts[i + 1] * ax[1] + pts[i + 2] * ax[2];
                lo = Math.min(lo, v);
                hi = Math.max(hi, v);
            }
            return new double[]{lo, hi};
        }
    }

    /**
     * Separating-axis distance between two convex hulls: positive, they are apart by at least that much; zero or
     * negative, they touch or interpenetrate by that much along the axis of least overlap.
     */
    static double separation(Hull a, Hull b) {
        return separationAxis(a, b)[0];
    }

    /** The separation and the axis it is found along: {sep, x, y, z}. */
    static double[] separationAxis(Hull a, Hull b) {
        List<double[]> axes = new ArrayList<>(a.normals);
        for (double[] n : b.normals) Hull.addDir(axes, n[0], n[1], n[2]);
        for (double[] e : a.edges)
            for (double[] f : b.edges) {
                double x = e[1] * f[2] - e[2] * f[1], y = e[2] * f[0] - e[0] * f[2], z = e[0] * f[1] - e[1] * f[0], l = Math.sqrt(x * x + y * y + z * z);
                if (l > 1e-6) Hull.addDir(axes, x / l, y / l, z / l);
            }
        double best = -Double.MAX_VALUE;
        double[] axis = {0, 1, 0};
        for (double[] ax : axes) {
            double[] sa = a.span(ax), sb = b.span(ax);
            double g = Math.max(sb[0] - sa[1], sa[0] - sb[1]);
            if (g > best) {
                best = g;
                axis = ax;
            }
        }
        return new double[]{best, axis[0], axis[1], axis[2]};
    }

    /** a's faces as polygons (repeats dropped). */
    static List<double[]> polys(List<Face> a) {
        List<double[]> polys = new ArrayList<>();
        for (Face f : a) {
            double[] d = distinct(f);
            if (d.length >= 9) polys.add(d);
        }
        return polys;
    }

    /** The part of a inside b grown by eps, as polygons (b convex: its faces' planes; a ring's outer hull). */
    static List<double[]> inside(List<Face> a, List<Face> b, double eps) {
        List<double[]> polys = polys(a);
        for (Face g : b) {
            if (g.nx == 0 && g.ny == 0 && g.nz == 0 || g.kind == Ring.INNER) continue;   // (a ring: its outer hull)
            double d = g.plane() + eps;
            List<double[]> next = new ArrayList<>();
            List<Double> cut = new ArrayList<>();
            for (double[] p : polys) {
                double[] q = clipPlane(p, g.nx, g.ny, g.nz, d, cut);
                if (q.length >= 9) next.add(q);
            }
            if (cut.size() >= 9) {   // the cap where the plane cut a: its points, in any order (only the outline is wanted)
                double[] c = new double[cut.size()];
                for (int i = 0; i < c.length; i++) c[i] = cut.get(i);
                next.add(c);
            }
            polys = next;
            if (polys.isEmpty()) break;
        }
        return polys;
    }

    /**
     * How much of a is held by b, seen along the axis: {area of the outline of a's part inside b grown by eps, area of
     * a's own outline}, both projected on the plane across the axis (px^2). a and b convex (their faces).
     */
    static double[] contact(List<Face> a, List<Face> b, double[] axis, double eps) {
        List<double[]> in = inside(a, b, eps);
        return new double[]{in.isEmpty() ? 0 : outlineArea(in, axis)[0], outlineArea(polys(a), axis)[0]};
    }

    /**
     * The joint's cross-section share: the most, over the directions the two are seen along (the separating axis and
     * each one's face normals), of the outline of where they meet over the outline of the one held (a), or of the
     * smaller of the two when held is null. A tube's end inside another is held over its whole section seen along the
     * tube; a box on a face, over its footprint seen down onto the face.
     */
    static double joint(List<Face> a, List<Face> b, double[] sepAxis, double eps, boolean aHeld) {
        List<double[]> in = inside(a, b, eps);
        if (in.isEmpty()) return 0;
        List<double[]> axes = new ArrayList<>();
        Hull.addDir(axes, sepAxis[0], sepAxis[1], sepAxis[2]);
        for (List<Face> fs : List.of(a, b))
            for (Face f : fs) if (f.nx != 0 || f.ny != 0 || f.nz != 0) Hull.addDir(axes, f.nx, f.ny, f.nz);
        List<double[]> pa = polys(a), pb = aHeld ? null : polys(b);
        double best = 0;
        for (double[] ax : axes) {
            double own = outlineArea(pa, ax)[0];
            if (pb != null) own = Math.min(own, outlineArea(pb, ax)[0]);
            if (own > 1e-9) best = Math.max(best, outlineArea(in, ax)[0] / own);
        }
        return Math.min(1, best);
    }

    /** The polygon's part on the side n.p <= d (points on the cut also go into cut). */
    static double[] clipPlane(double[] p, double nx, double ny, double nz, double d, List<Double> cut) {
        int n = p.length / 3;
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double si = nx * p[3 * i] + ny * p[3 * i + 1] + nz * p[3 * i + 2] - d, sj = nx * p[3 * j] + ny * p[3 * j + 1] + nz * p[3 * j + 2] - d;
            if (si <= 0) for (int k = 0; k < 3; k++) out.add(p[3 * i + k]);
            if ((si <= 0) != (sj <= 0)) {
                double t = si / (si - sj);
                for (int k = 0; k < 3; k++) {
                    double v = p[3 * i + k] + t * (p[3 * j + k] - p[3 * i + k]);
                    out.add(v);
                    cut.add(v);
                }
            }
        }
        double[] r = new double[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }

    /** The area of the convex outline of all the points, projected along the axis: {area}. */
    static double[] outlineArea(List<double[]> polys, double[] axis) {
        double[][] b = basis(axis[0], axis[1], axis[2]);
        List<double[]> pts = new ArrayList<>();
        for (double[] p : polys)
            for (int i = 0; i < p.length; i += 3)
                pts.add(new double[]{p[i] * b[0][0] + p[i + 1] * b[0][1] + p[i + 2] * b[0][2], p[i] * b[1][0] + p[i + 1] * b[1][1] + p[i + 2] * b[1][2]});
        if (pts.size() < 3) return new double[]{0};
        // monotone chain hull
        pts.sort((u, v) -> u[0] != v[0] ? Double.compare(u[0], v[0]) : Double.compare(u[1], v[1]));
        double[][] h = new double[2 * pts.size()][];
        int k = 0;
        for (int pass = 0; pass < 2; pass++) {
            int start = k;
            for (int i = 0; i < pts.size(); i++) {
                double[] q = pts.get(pass == 0 ? i : pts.size() - 1 - i);
                while (k >= start + 2 && (h[k - 1][0] - h[k - 2][0]) * (q[1] - h[k - 2][1]) - (h[k - 1][1] - h[k - 2][1]) * (q[0] - h[k - 2][0]) <= 1e-12) k--;
                h[k++] = q;
            }
            k--;
        }
        double area = 0;
        for (int i = 0; i < k; i++) {
            int j = (i + 1) % k;
            area += h[i][0] * h[j][1] - h[j][0] * h[i][1];
        }
        return new double[]{Math.abs(area) / 2};
    }
}
