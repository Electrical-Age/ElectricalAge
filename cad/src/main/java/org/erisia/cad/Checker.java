package org.erisia.cad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks a model (board #44; Baughn: stop whack-a-moling clipping bugs). It reports, it never nudges:
 * <ul>
 * <li>degenerate: a solid thinner than EPS on some dimension, or a face of no area;</li>
 * <li>z-fight: two faces of different solids facing the same way, in the same plane (within EPS), overlapping by more
 * than AREA, that look different there (another material, colour or texture mapping). Opposite-facing faces in one
 * plane don't z-fight: back faces are culled;</li>
 * <li>the model's declared relations (Relation).</li>
 * </ul>
 * Decals that are meant to sit over a surface say so with Solid.layer.
 */
public final class Checker {
    private Checker() {
    }

    /** Distance (px) under which two planes are the same and a solid is too thin. */
    public static final double EPS = 0.05;
    /** Overlap (px^2) over which coplanar faces z-fight. */
    public static final double AREA = 1e-4;

    public record Issue(String rule, String what) {
        @Override
        public String toString() {
            return rule + ": " + what;
        }
    }

    public static List<Issue> check(Model m) {
        List<Issue> out = new ArrayList<>();
        for (Solid s : m.solids)
            if (s.thickness() < EPS) out.add(new Issue("degenerate", s + " is " + fmt(s.thickness()) + " px thin"));
        List<Solid> convex = new ArrayList<>();
        for (Solid s : m.solids) if (!s.ghost && (s instanceof Box || s instanceof Frustum)) convex.add(s);
        out.addAll(faces(m.faces(), convex));
        out.addAll(relations(m));
        return out;
    }

    /** Degenerate faces and z-fights in a soup of faces (a model's, or another engine's output). */
    public static List<Issue> faces(List<Face> faces) {
        return faces(faces, List.of());
    }

    /**
     * ... where an overlap lying wholly inside one of the occluders (convex solids: a fitting ring over a bend) is
     * hidden and not reported.
     */
    public static List<Issue> faces(List<Face> faces, List<Solid> occluders) {
        return scan(faces, occluders, null);
    }

    /** The pairs of faces that z-fight (for pictures that mark them). */
    public static List<Face[]> fights(List<Face> faces) {
        List<Face[]> pairs = new ArrayList<>();
        scan(faces, List.of(), pairs);
        return pairs;
    }

    static List<Issue> scan(List<Face> faces, List<Solid> occluders, List<Face[]> pairs) {
        Map<Solid, List<Face>> occ = new HashMap<>();
        List<Issue> out = new ArrayList<>();
        Map<Long, List<Face>> byNormal = new HashMap<>();
        for (Face f : faces) {
            if (Geom.distinct(f).length < 9 || f.area() < 1e-6) {
                out.add(new Issue("degenerate", "a face of no area: " + f));
                continue;
            }
            byNormal.computeIfAbsent(key(f), k -> new ArrayList<>()).add(f);
        }
        double[] area = new double[1];
        for (List<Face> group : byNormal.values()) {
            group.sort((a, b) -> Double.compare(a.plane(), b.plane()));
            for (int i = 0; i < group.size(); i++) {
                Face a = group.get(i);
                for (int j = i + 1; j < group.size(); j++) {
                    Face b = group.get(j);
                    if (b.plane() - a.plane() >= EPS) break;
                    if (a.solid != null && a.solid == b.solid) continue;
                    if (a.nx * b.nx + a.ny * b.ny + a.nz * b.nz < 1 - 1e-4) continue;
                    double[] poly = Geom.overlap(a, b, area);
                    if (poly == null || area[0] <= AREA) continue;
                    if (looksSame(a, b, poly) || hidden(poly, a.solid, b.solid, occluders, occ)) continue;
                    out.add(new Issue("z-fight", fmt(area[0]) + " px^2 of " + a + "\n    and " + b));
                    if (pairs != null) pairs.add(new Face[]{a, b});
                }
            }
        }
        return out;
    }

    /** The normal, quantised, as a map key (a neighbour bucket's faces are rarer than exact ones; both are checked by dot). */
    static long key(Face f) {
        long x = Math.round(f.nx * 1000), y = Math.round(f.ny * 1000), z = Math.round(f.nz * 1000);
        return (x + 1000) * 4_000_000L + (y + 1000) * 2001 + (z + 1000);
    }

    /** Is the polygon inside one solid (not a or b), clear of its surface? */
    static boolean hidden(double[] poly, Solid a, Solid b, List<Solid> occluders, Map<Solid, List<Face>> cache) {
        for (Solid s : occluders) {
            if (s == a || s == b) continue;
            boolean in = true;
            for (Face f : cache.computeIfAbsent(s, Solid::faces)) {
                if (f.nx == 0 && f.ny == 0 && f.nz == 0) continue;
                double d = f.plane();
                for (int i = 0; i < poly.length && in; i += 3) in = f.nx * poly[i] + f.ny * poly[i + 1] + f.nz * poly[i + 2] - d < -1e-6;
                if (!in) break;
            }
            if (in) return true;
        }
        return false;
    }

    /** Same material and colour, and the same texture coordinates over the overlap: drawn the same, no visible fight. */
    static boolean looksSame(Face a, Face b, double[] poly) {
        if (a.argb() != b.argb() || !java.util.Objects.equals(a.material(), b.material())) return false;
        if (a.uv == null && b.uv == null) return true;
        if (a.uv == null || b.uv == null) return false;
        for (int i = 0; i < poly.length; i += 3) {
            double[] ua = Geom.uvAt(a, poly, i), ub = Geom.uvAt(b, poly, i);
            if (ua == null || ub == null || Math.abs(ua[0] - ub[0]) > 1e-4 || Math.abs(ua[1] - ub[1]) > 1e-4) return false;
        }
        return true;
    }

    public static List<Issue> relations(Model m) {
        List<Issue> out = new ArrayList<>();
        Map<Solid, Geom.Hull> hulls = new HashMap<>();
        java.util.function.Function<Solid, Geom.Hull> hull = s -> hulls.computeIfAbsent(s, k -> new Geom.Hull(k.faces()));
        for (Relation r : m.relations) {
            switch (r) {
                case Relation.Attach a -> {
                    double[] sa = Geom.separationAxis(hull.apply(a.a()), hull.apply(a.b()));
                    double sep = sa[0];
                    if (sep > EPS) out.add(new Issue("attach", a.a() + " floats " + fmt(sep) + " px clear of " + a.b()));
                    else if (-sep > a.maxSink() + EPS)
                        out.add(new Issue("attach", a.a() + " is sunk " + fmt(-sep) + " px into " + a.b() + " (at most " + fmt(a.maxSink()) + ")"));
                    else {
                        double[] c = Geom.contact(a.a().faces(), a.b().faces(), new double[]{sa[1], sa[2], sa[3]}, EPS);
                        double share = c[1] > 0 ? c[0] / c[1] : 0;
                        if (share < a.minShare() - 1e-9 || c[0] < a.minArea())
                            out.add(new Issue("attach", a.a() + " barely attached to " + a.b() + ": " + Math.round(100 * share) + " % contact ("
                                + fmt(c[0]) + " of " + fmt(c[1]) + " px^2; needs " + Math.round(100 * a.minShare()) + " %"
                                + (a.minArea() > 0 ? " and " + fmt(a.minArea()) + " px^2" : "") + ")"));
                    }
                }
                case Relation.Disjoint d -> {
                    double sep = Geom.separation(hull.apply(d.a()), hull.apply(d.b()));
                    if (sep < -EPS) out.add(new Issue("disjoint", d.a() + " clips " + d.b() + " by " + fmt(-sep) + " px"));
                }
                case Relation.Within w -> {
                    double worst = 0;
                    for (Face f : w.outer().faces()) {
                        double d = f.plane();
                        for (Face g : w.inner().faces())
                            for (int i = 0; i < g.n; i++)
                                worst = Math.max(worst, f.nx * g.xyz[3 * i] + f.ny * g.xyz[3 * i + 1] + f.nz * g.xyz[3 * i + 2] - d);
                    }
                    if (worst > EPS) out.add(new Issue("within", w.inner() + " pokes " + fmt(worst) + " px out of " + w.outer()));
                }
                case Relation.Ratio q -> {
                    if (q.value() < q.min() - 1e-9 || q.value() > q.max() + 1e-9)
                        out.add(new Issue("proportion", q.what() + " is " + fmt(q.value()) + ", not in " + fmt(q.min()) + ".." + fmt(q.max())));
                }
            }
        }
        return out;
    }

    static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.3g", v);
    }
}
