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
        out.addAll(connectivity(m));
        return out;
    }

    /** The contact share two solids of a body must reach to count as joined. */
    public static final double CONNECT_SHARE = 0.5;

    /**
     * Every drawn solid hangs together with its body (Solid.body; else its moving part, else the model): linked to the
     * body's main piece (its biggest solid's) through contacts that each hold over CONNECT_SHARE of the narrower
     * piece's outline, seen along the contact. Solids marked free are left out. Reports each solid that isn't linked,
     * with its best link to the rest if it has a weak one.
     */
    public static List<Issue> connectivity(Model m) {
        Map<Object, List<Solid>> bodies = new java.util.LinkedHashMap<>();
        Object root = new Object();
        for (Solid s : m.solids) {
            if (s.ghost || s.free != null) continue;
            Object key = s.body != null ? s.body : s.part != null && s.part.movingAncestor() != null ? s.part.movingAncestor() : root;
            bodies.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }
        List<Issue> out = new ArrayList<>();
        Map<Solid, List<Face>> faces = new HashMap<>();
        Map<Solid, Geom.Hull> hulls = new HashMap<>();
        for (List<Solid> body : bodies.values()) {
            int n = body.size();
            if (n < 2) continue;
            int[] comp = new int[n];
            for (int i = 0; i < n; i++) comp[i] = i;
            double[][] share = new double[n][n];
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++) {
                    Solid a = body.get(i), b = body.get(j);
                    Geom.Hull ha = hulls.computeIfAbsent(a, k -> new Geom.Hull(faces.computeIfAbsent(k, Solid::faces)));
                    Geom.Hull hb = hulls.computeIfAbsent(b, k -> new Geom.Hull(faces.computeIfAbsent(k, Solid::faces)));
                    double[] sa = Geom.separationAxis(ha, hb);
                    if (sa[0] > EPS) continue;
                    double s = Geom.joint(faces.get(a), faces.get(b), new double[]{sa[1], sa[2], sa[3]}, EPS, false);
                    share[i][j] = share[j][i] = s;
                    if (s >= CONNECT_SHARE - 1e-9) union(comp, i, j);
                }
            // the main piece: the component with the most bulk (its solids' bounding volumes), named by its biggest solid
            Map<Integer, Double> bulk = new HashMap<>();
            double[] vol = new double[n];
            for (int i = 0; i < n; i++) {
                double[] bb = bounds(faces.computeIfAbsent(body.get(i), Solid::faces));
                vol[i] = (bb[3] - bb[0] + 0.01) * (bb[4] - bb[1] + 0.01) * (bb[5] - bb[2] + 0.01);
                bulk.merge(find(comp, i), vol[i], Double::sum);
            }
            int main = 0;
            for (int i = 0; i < n; i++)
                if (bulk.get(find(comp, i)) > bulk.get(find(comp, main)) || find(comp, i) == find(comp, main) && vol[i] > vol[main]) main = i;
            int mainRoot = find(comp, main);
            for (int i = 0; i < n; i++) {
                if (find(comp, i) == mainRoot) continue;
                int best = -1;
                for (int j = 0; j < n; j++) if (find(comp, j) != find(comp, i) && (best < 0 || share[i][j] > share[i][best])) best = j;
                String why = best >= 0 && share[i][best] > 0 ? "its best link, to " + body.get(best) + ", holds " + Math.round(100 * share[i][best]) + " %"
                    : "it touches nothing else of its body";
                out.add(new Issue("connected", body.get(i) + " isn't joined to the main piece (" + body.get(main) + "): " + why));
            }
        }
        return out;
    }

    static int find(int[] c, int i) {
        while (c[i] != i) i = c[i] = c[c[i]];
        return i;
    }

    static void union(int[] c, int i, int j) {
        c[find(c, i)] = find(c, j);
    }

    static double[] bounds(List<Face> faces) {
        double[] r = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (Face f : faces)
            for (int i = 0; i < f.n; i++)
                for (int k = 0; k < 3; k++) {
                    r[k] = Math.min(r[k], f.xyz[3 * i + k]);
                    r[k + 3] = Math.max(r[k + 3], f.xyz[3 * i + k]);
                }
        return r;
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
                        double[] axis = {sa[1], sa[2], sa[3]};
                        double share = Geom.joint(a.a().faces(), a.b().faces(), axis, EPS, true);
                        double[] c = Geom.contact(a.a().faces(), a.b().faces(), axis, EPS);
                        if (share < a.minShare() - 1e-9 || c[0] < a.minArea())
                            out.add(new Issue("attach", a.a() + " barely attached to " + a.b() + ": " + Math.round(100 * share) + " % contact ("
                                + fmt(c[0]) + " px^2 across the contact; needs " + Math.round(100 * a.minShare()) + " %"
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
