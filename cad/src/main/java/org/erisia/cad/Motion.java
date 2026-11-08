package org.erisia.cad;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks for what moves (Baughn 2026-11-07: "the belt is passing through a machine block"; board #58's checker gaps):
 * <ul>
 * <li>{@link #sweep}: a part turning a full circle about an axis through its origin. A fixed solid that is clear of
 * it at rest but inside the ring a piece of it sweeps is hit while it turns. Each piece sweeps the ring between its
 * nearest and farthest points from the axis, over its extent along it. The fixed solid is cut to that extent before
 * its distance from the axis is taken.</li>
 * <li>{@link #clearance}: solids that move along a path (a belt's straight runs, given as their envelopes) must keep
 * a gap from everything else. Each envelope already includes the gap.</li>
 * </ul>
 * Solids marked free (or ghosts) are left out on the fixed side.
 */
public final class Motion {
    private Motion() {
    }

    /** Overlap (px) under which two solids only touch. */
    public static final double EPS = Checker.EPS;

    /** A full turn of the moving solids about axis (0 x, 1 y, 2 z) through origin, against the fixed ones. */
    public static List<Checker.Issue> sweep(String what, List<? extends Solid> moving, double[] origin, int axis, List<? extends Solid> fixed) {
        List<Checker.Issue> out = new ArrayList<>();
        List<double[]> rings = new ArrayList<>();   // {r0, r1, a0, a1}
        List<Geom.Hull> rest = new ArrayList<>();
        for (Solid s : moving) {
            if (s.ghost) continue;
            List<Face> fs = s.faces();
            rest.add(new Geom.Hull(fs));
            double r0 = Double.MAX_VALUE, r1 = 0, a0 = Double.MAX_VALUE, a1 = -Double.MAX_VALUE;
            List<double[]> proj = new ArrayList<>();
            for (Face f : fs)
                for (int i = 0; i < f.n; i++) {
                    double[] p = {f.xyz[3 * i], f.xyz[3 * i + 1], f.xyz[3 * i + 2]};
                    double a = p[axis];
                    a0 = Math.min(a0, a);
                    a1 = Math.max(a1, a);
                    double[] q = plane(p, origin, axis);
                    proj.add(q);
                    r1 = Math.max(r1, Math.hypot(q[0], q[1]));
                }
            r0 = minDistance(proj);
            rings.add(new double[]{r0, r1, a0, a1});
        }
        for (Solid m : fixed) {
            if (m.ghost || m.free != null) continue;
            List<Face> mf = m.faces();
            Geom.Hull hm = new Geom.Hull(mf);
            boolean touching = false;
            for (Geom.Hull h : rest) if (Geom.separation(h, hm) < -EPS) touching = true;
            if (touching) continue;   // (a bearing round its shaft: in contact at rest, not hit by the turning)
            for (double[] ring : rings) {
                List<double[]> cut = slab(mf, axis, ring[2] + EPS, ring[3] - EPS);
                if (cut.isEmpty()) continue;
                List<double[]> proj = new ArrayList<>();
                double far = 0;
                for (double[] p : cut) {
                    double[] q = plane(p, origin, axis);
                    proj.add(q);
                    far = Math.max(far, Math.hypot(q[0], q[1]));
                }
                double near = minDistance(proj);
                if (near < ring[1] - EPS && far > ring[0] + EPS) {
                    out.add(new Checker.Issue("swept", m + " is in the way of " + what + " turning (it sweeps " + fmt(ring[0]) + ".." + fmt(ring[1])
                        + " px from its axis; this comes within " + fmt(near) + ")"));
                    break;
                }
            }
        }
        return out;
    }

    /** Envelopes of moving solids (a belt's runs plus the gap they need) against everything else. */
    public static List<Checker.Issue> clearance(String what, List<? extends Solid> envelopes, List<? extends Solid> others) {
        List<Checker.Issue> out = new ArrayList<>();
        List<Geom.Hull> env = new ArrayList<>();
        for (Solid e : envelopes) env.add(new Geom.Hull(e.faces()));
        for (Solid o : others) {
            if (o.ghost || o.free != null) continue;
            Geom.Hull h = new Geom.Hull(o.faces());
            for (Geom.Hull e : env)
                if (Geom.separation(e, h) < -EPS) {
                    out.add(new Checker.Issue("clearance", o + " is too close to " + what));
                    break;
                }
        }
        return out;
    }

    /** p in the plane across the axis, relative to the origin: {u, v}. */
    static double[] plane(double[] p, double[] o, int axis) {
        int u = (axis + 1) % 3, v = (axis + 2) % 3;
        return new double[]{p[u] - o[u], p[v] - o[v]};
    }

    /** The corners of the faces cut to lo <= coordinate axis <= hi (the cut faces' corners, both ends' crossings included). */
    static List<double[]> slab(List<Face> faces, int axis, double lo, double hi) {
        List<double[]> out = new ArrayList<>();
        if (hi <= lo) return out;
        for (Face f : faces) {
            List<double[]> poly = new ArrayList<>();
            for (int i = 0; i < f.n; i++) poly.add(new double[]{f.xyz[3 * i], f.xyz[3 * i + 1], f.xyz[3 * i + 2]});
            poly = clip(poly, axis, lo, true);
            poly = clip(poly, axis, hi, false);
            out.addAll(poly);
        }
        return out;
    }

    /** Sutherland-Hodgman against coordinate axis >= c (keepAbove) or <= c. */
    static List<double[]> clip(List<double[]> poly, int axis, double c, boolean keepAbove) {
        List<double[]> out = new ArrayList<>();
        int n = poly.size();
        for (int i = 0; i < n; i++) {
            double[] a = poly.get(i), b = poly.get((i + 1) % n);
            boolean ina = keepAbove ? a[axis] >= c : a[axis] <= c, inb = keepAbove ? b[axis] >= c : b[axis] <= c;
            if (ina) out.add(a);
            if (ina != inb) {
                double t = (c - a[axis]) / (b[axis] - a[axis]);
                out.add(new double[]{a[0] + t * (b[0] - a[0]), a[1] + t * (b[1] - a[1]), a[2] + t * (b[2] - a[2])});
            }
        }
        return out;
    }

    /** The nearest the convex hull of the points (2D) comes to the origin: 0 if it holds it. */
    static double minDistance(List<double[]> pts) {
        List<double[]> h = hull2(pts);
        if (h.isEmpty()) return Double.MAX_VALUE;
        if (h.size() == 1) return Math.hypot(h.get(0)[0], h.get(0)[1]);
        boolean inside = h.size() >= 3;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < h.size(); i++) {
            double[] a = h.get(i), b = h.get((i + 1) % h.size());
            double ex = b[0] - a[0], ey = b[1] - a[1];
            if (ex * (-a[1]) - ey * (-a[0]) < -1e-12) inside = false;   // (counter-clockwise hull: the origin left of every edge)
            double len2 = ex * ex + ey * ey, t = len2 == 0 ? 0 : Math.max(0, Math.min(1, -(a[0] * ex + a[1] * ey) / len2));
            best = Math.min(best, Math.hypot(a[0] + t * ex, a[1] + t * ey));
        }
        return inside ? 0 : best;
    }

    /** Andrew's monotone chain, counter-clockwise. */
    static List<double[]> hull2(List<double[]> pts) {
        List<double[]> p = new ArrayList<>(pts);
        p.sort((x, y) -> x[0] != y[0] ? Double.compare(x[0], y[0]) : Double.compare(x[1], y[1]));
        if (p.size() < 3) return p;
        double[][] h = new double[2 * p.size()][];
        int k = 0;
        for (double[] q : p) {
            while (k >= 2 && cross(h[k - 2], h[k - 1], q) <= 0) k--;
            h[k++] = q;
        }
        for (int i = p.size() - 2, t = k + 1; i >= 0; i--) {
            double[] q = p.get(i);
            while (k >= t && cross(h[k - 2], h[k - 1], q) <= 0) k--;
            h[k++] = q;
        }
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < k - 1; i++) out.add(h[i]);
        return out;
    }

    static double cross(double[] o, double[] a, double[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }

    static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }
}
