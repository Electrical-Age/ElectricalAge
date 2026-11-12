package mods.eln.node.voxel;

import java.util.ArrayList;
import java.util.List;

/**
 * How a cable crossing the air hangs (Baughn 2026-11-12: physics run once at placement; the route, its preview and its
 * hitbox stay as routed). A free span runs from where the cable leaves a support to where it lands on the next, round
 * any corner on the way (a drop down past an air block onto a floor is one span). The cable over it is a chain of
 * ~1-voxel links with bending stiffness, a little longer than the route, pulled down by gravity and kept out of blocked
 * voxels. Where it stays glued, the two nodes at each end of the span sit on the route, so it leaves a surface along it
 * (its tangent comes from the stiffness, not from a held link). It may peel a few voxels off each surface first if that
 * lowers its energy: gravity, bending and a cost per voxel peeled (Baughn: energy minimisation, with strain on the wall
 * attachment included). Deterministic. Pure Java: the world is behind CableRouter.Space. Positions in world voxels.
 */
public final class CableHang {
    private CableHang() {
    }

    /** How much longer than the route the chain is. */
    static final double SLACK = 0.03;

    /** Energy weights: per node per voxel of height, per unit of bend (|p[i-1] - 2 p[i] + p[i+1]|^2), per unit of stretch
     *  ((|link| - rest)^2), per voxel peeled off a surface; the descent's step and its limit. */
    static final double E_GRAVITY = 0.2, E_BEND = 3.0, E_STRETCH = 10, E_PEEL = 0.2, ETA = 0.015;
    static final int MAX_STEPS = 4000;
    /** Peel lengths tried at each end (voxels, on the cable lattice). */
    static final int[] PEELS = {0, 2, 4};

    /** The route as a dense polyline: points on the lattice (corners repeated), the normal away from the support at
     *  each point, arc length, and per segment whether it lies on a support. */
    record Route(List<double[]> pts, List<double[]> nrm, double[] s, boolean[] supported) {
        int n() {
            return pts.size();
        }

        double length() {
            return s[s.length - 1];
        }

        /** The point at arc length t (clamped). */
        double[] at(double t) {
            return lerp(pts, t);
        }

        /** The normal at arc length t. */
        double[] normal(double t) {
            return lerp(nrm, t);
        }

        private double[] lerp(List<double[]> v, double t) {
            if (t <= 0) return v.get(0).clone();
            for (int i = 1; i < s.length; i++)
                if (t <= s[i]) {
                    double len = s[i] - s[i - 1];
                    double f = len < 1e-9 ? 1 : (t - s[i - 1]) / len;
                    double[] a = v.get(i - 1), b = v.get(i);
                    return new double[]{a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
                }
            return v.get(v.size() - 1).clone();
        }
    }

    /** A hung span: the chain covering route arc [from, to], its points and the normals carried along. */
    public record Hang(double from, double to, List<double[]> pts, List<double[]> nrm) {
    }

    /** The route of steps as a dense polyline (CableRouter.Step points, attach faces). */
    static Route route(CableRouter.Space space, RouteKind kind, List<CableRouter.Step> steps) {
        List<double[]> pts = new ArrayList<>(), nrm = new ArrayList<>();
        List<Boolean> sup = new ArrayList<>();
        int lattice = kind.lattice();
        for (int i = 0; i < steps.size(); i++) {
            CableRouter.Step b = steps.get(i);
            if (i == 0) {
                pts.add(new double[]{b.x(), b.y(), b.z()});
                nrm.add(away(b.attach()));
                continue;
            }
            CableRouter.Step a = steps.get(i - 1);
            int d = CableRouter.dirOf(a, b);
            if (d < 0) {   // (a corner: the same point, a new face)
                pts.add(new double[]{b.x(), b.y(), b.z()});
                nrm.add(away(b.attach()));
                sup.add(true);
                continue;
            }
            long[] p = {a.x(), a.y(), a.z()};
            long len = Math.abs(b.x() - a.x()) + Math.abs(b.y() - a.y()) + Math.abs(b.z() - a.z());
            for (long t = 0; t < len; t += lattice) {
                sup.add(Router.surfaceSupported(space, kind, p, b.attach(), d));
                for (int j = 0; j < 3; j++) p[j] += CableRouter.DV[d][j] * lattice;
                pts.add(new double[]{p[0], p[1], p[2]});
                nrm.add(away(b.attach()));
            }
        }
        double[] s = new double[pts.size()];
        for (int i = 1; i < s.length; i++) s[i] = s[i - 1] + dist(pts.get(i - 1), pts.get(i));
        boolean[] sp = new boolean[sup.size()];
        for (int i = 0; i < sp.length; i++) sp[i] = sup.get(i);
        return new Route(pts, nrm, s, sp);
    }

    /** The unit vector from a cable on face attach toward its body (away from its support). */
    static double[] away(int attach) {
        int[] v = CableRouter.DV[CableRouter.reverse(attach)];
        return new double[]{v[0], v[1], v[2]};
    }

    static double dist(double[] a, double[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** The free spans of a route: arc ranges [from, to] from the last supported point before a stretch of unsupported
     *  segments to the first supported point after it (corners on the way included). */
    static List<double[]> spans(Route r) {
        List<double[]> out = new ArrayList<>();
        int i = 0;
        while (i < r.supported().length) {
            if (r.supported()[i]) {
                i++;
                continue;
            }
            int j = i;
            // (through corners, which have no length: a drop then a turn onto the floor is one span until it lands)
            while (j < r.supported().length && (!r.supported()[j] || r.s()[j + 1] - r.s()[j] < 1e-9)) j++;
            out.add(new double[]{r.s()[i], r.s()[j]});
            i = j;
        }
        return out;
    }

    /** Every span of the route, hung (the lowest-energy of the peel lengths tried at its two ends). */
    public static List<Hang> hangs(CableRouter.Space space, RouteKind kind, List<CableRouter.Step> steps) {
        Route r = route(space, kind, steps);
        List<double[]> sp = spans(r);
        List<Hang> out = new ArrayList<>();
        for (int k = 0; k < sp.size(); k++) {
            double lo = k == 0 ? 0 : sp.get(k - 1)[1], hi = k + 1 < sp.size() ? sp.get(k + 1)[0] : r.length();
            Hang best = null;
            double bestE = Double.MAX_VALUE;
            for (int pa : PEELS)
                for (int pb : PEELS) {
                    double from = sp.get(k)[0] - pa, to = sp.get(k)[1] + pb;
                    if (from - 1 < lo - 1e-9 && pa > 0 || to + 1 > hi + 1e-9 && pb > 0) continue;   // (not into the next span)
                    double[] e = new double[1];
                    Hang h = solve(space, r, from, to, e);
                    double energy = e[0] + E_PEEL * (pa + pb);
                    if (energy < bestE) {
                        bestE = energy;
                        best = h;
                    }
                }
            if (best != null) out.add(best);
        }
        return out;
    }

    /** The chain over route arc [from, to], relaxed; energy[0] = its energy over the route's (gravity, bending,
     *  stretch). One glued node before and after (on the route) give it the surface's direction where it leaves and
     *  lands. Projected gradient descent on that energy: a node that would enter a blocked voxel stays put. */
    static Hang solve(CableRouter.Space space, Route r, double from, double to, double[] energy) {
        double a = Math.max(0, from - 1), b = Math.min(r.length(), to + 1);
        double span = b - a;
        int n = (int) Math.max(4, Math.round(span));
        double[][] x = new double[n + 1][], nr = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double t = a + span * i / n;
            x[i] = r.at(t);
            nr[i] = r.normal(t);
        }
        double[] rest = new double[n];
        for (int i = 0; i < n; i++) rest[i] = span / n * (1 + SLACK);
        boolean[] free = new boolean[n + 1];
        for (int i = 2; i <= n - 2; i++) free[i] = true;
        java.util.Map<Long, Boolean> seen = new java.util.HashMap<>();
        double e0 = energy(x, rest, free);
        double[][] g = new double[n + 1][3];
        for (int step = 0; step < MAX_STEPS; step++) {
            gradient(x, rest, free, g);
            double moved = 0;
            for (int i = 0; i <= n; i++) {
                if (!free[i]) continue;
                double[] np = {x[i][0] - ETA * g[i][0], x[i][1] - ETA * g[i][1], x[i][2] - ETA * g[i][2]};
                if (blockedAt(space, seen, np, nr[i])) continue;
                moved = Math.max(moved, dist(np, x[i]));
                x[i] = np;
            }
            if (moved < 1e-5) break;
        }
        energy[0] = energy(x, rest, free) - e0;
        List<double[]> pts = new ArrayList<>(), nrm = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            pts.add(x[i]);
            nrm.add(nr[i]);
        }
        return new Hang(a, b, pts, smooth(nrm));
    }

    /** Gravity (per free node, its height), bending (|p[i-1] - 2 p[i] + p[i+1]|^2) and stretch ((|d| - rest)^2). */
    static double energy(double[][] x, double[] rest, boolean[] free) {
        double e = 0;
        int n = x.length - 1;
        for (int i = 0; i <= n; i++) if (free[i]) e += E_GRAVITY * x[i][1];
        for (int i = 1; i < n; i++) {
            double bx = x[i - 1][0] - 2 * x[i][0] + x[i + 1][0], by = x[i - 1][1] - 2 * x[i][1] + x[i + 1][1], bz = x[i - 1][2] - 2 * x[i][2] + x[i + 1][2];
            e += E_BEND * (bx * bx + by * by + bz * bz);
        }
        for (int i = 0; i < n; i++) {
            double d = dist(x[i], x[i + 1]) - rest[i];
            e += E_STRETCH * d * d;
        }
        return e;
    }

    static void gradient(double[][] x, double[] rest, boolean[] free, double[][] g) {
        int n = x.length - 1;
        for (double[] v : g) java.util.Arrays.fill(v, 0);
        for (int i = 0; i <= n; i++) if (free[i]) g[i][1] += E_GRAVITY;
        for (int i = 1; i < n; i++)
            for (int c = 0; c < 3; c++) {
                double bc = 2 * E_BEND * (x[i - 1][c] - 2 * x[i][c] + x[i + 1][c]);
                g[i - 1][c] += bc;
                g[i][c] -= 2 * bc;
                g[i + 1][c] += bc;
            }
        for (int i = 0; i < n; i++) {
            double d = dist(x[i], x[i + 1]);
            if (d < 1e-9) continue;
            double f = 2 * E_STRETCH * (d - rest[i]) / d;
            for (int c = 0; c < 3; c++) {
                double v = f * (x[i + 1][c] - x[i][c]);
                g[i][c] -= v;
                g[i + 1][c] += v;
            }
        }
    }

    /** blocked(), remembered per voxel for one solve (the solver asks about the same few voxels many times). */
    static boolean blockedAt(CableRouter.Space space, java.util.Map<Long, Boolean> seen, double[] p, double[] n) {
        long vx = (long) Math.floor(p[0] + 0.5 * n[0]), vy = (long) Math.floor(p[1] + 0.5 * n[1]), vz = (long) Math.floor(p[2] + 0.5 * n[2]);
        long key = (vx & 0x1FFFFF) << 42 | (vy & 0x1FFFFF) << 21 | (vz & 0x1FFFFF);
        return seen.computeIfAbsent(key, k -> space.blocked(vx, vy, vz, vx + 1, vy + 1, vz + 1));
    }

    /** Normals averaged with their neighbours twice (a turn at a corner spread over a few links, for the drawing). */
    static List<double[]> smooth(List<double[]> v) {
        List<double[]> cur = v;
        for (int pass = 0; pass < 2; pass++) {
            List<double[]> out = new ArrayList<>();
            for (int i = 0; i < cur.size(); i++) {
                double[] a = cur.get(Math.max(0, i - 1)), b = cur.get(i), c = cur.get(Math.min(cur.size() - 1, i + 1));
                double[] m = {a[0] + 2 * b[0] + c[0], a[1] + 2 * b[1] + c[1], a[2] + 2 * b[2] + c[2]};
                double l = Math.sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2]);
                out.add(l < 1e-9 ? b : new double[]{m[0] / l, m[1] / l, m[2] / l});
            }
            cur = out;
        }
        return cur;
    }

    /** Is the voxel half a voxel off the line, on the cable's body side (normal n), blocked? (The line lies on its
     *  support's surface: the support itself is right under it.) */
    static boolean blocked(CableRouter.Space space, double[] p, double[] n) {
        double x = p[0] + 0.5 * n[0], y = p[1] + 0.5 * n[1], z = p[2] + 0.5 * n[2];
        long vx = (long) Math.floor(x), vy = (long) Math.floor(y), vz = (long) Math.floor(z);
        return space.blocked(vx, vy, vz, vx + 1, vy + 1, vz + 1);
    }

    // --- per piece: what is stored (CablePart.hang2) ---

    /**
     * The hangs' share for one piece (block-local; CablePart.hang2), or null: per hang crossing it, {s0, s1 (the arc
     * range of the piece's own polyline it draws over, in eighths of a voxel), count, then count points x y z and
     * normals nx ny nz, then tangents tx ty tz (eighths, and 64ths; the normal square to the tangent)}. The piece's place on the route comes from projecting its two ends on it.
     */
    static int[] encode(CableRouter.Piece pc, Route r, List<Hang> hangs) {
        if (hangs.isEmpty() || pc.face() < 0) return null;
        long[] base = {pc.bx() * 16, pc.by() * 16, pc.bz() * 16};
        int[] pts = pc.points();
        double[] first = {base[0] + pts[0], base[1] + pts[1], base[2] + pts[2]};
        int k = pts.length - 3;
        double[] last = {base[0] + pts[k], base[1] + pts[k + 1], base[2] + pts[k + 2]};
        double p0 = project(r, first), p1 = project(r, last);
        boolean reversed = p1 < p0;
        double lo = Math.min(p0, p1), hi = Math.max(p0, p1), plen = 0;
        for (int i = 3; i < pts.length; i += 3) plen += Math.abs(pts[i] - pts[i - 3]) + Math.abs(pts[i + 1] - pts[i - 2]) + Math.abs(pts[i + 2] - pts[i - 1]);
        List<Integer> out = new ArrayList<>();
        for (Hang h : hangs) {
            double a = Math.max(lo, h.from()), b = Math.min(hi, h.to());
            if (b - a < 1e-6) continue;
            // piece-local arc (along the piece's own polyline, from its first point)
            double s0 = reversed ? hi - b : a - lo, s1 = reversed ? hi - a : b - lo;
            s0 = Math.max(0, Math.min(plen, s0));
            s1 = Math.max(0, Math.min(plen, s1));
            List<double[]> cp = new ArrayList<>(), cn = new ArrayList<>(), ct = new ArrayList<>();
            int n = h.pts().size() - 1;
            // (the direction along the curve from the whole chain: the pieces either side of a block's face then
            // carry the same cross-section there, with no crack between them; AdventurAgent saw hairlines)
            List<double[]> tan = new ArrayList<>();
            for (int i = 0; i <= n; i++) {
                double[] pa = h.pts().get(Math.max(0, i - 1)), pb = h.pts().get(Math.min(n, i + 1));
                tan.add(unit(new double[]{pb[0] - pa[0], pb[1] - pa[1], pb[2] - pa[2]}));
            }
            double fa = (a - h.from()) / (h.to() - h.from()) * n, fb = (b - h.from()) / (h.to() - h.from()) * n;
            cp.add(chainAt(h.pts(), fa));
            cn.add(chainAt(h.nrm(), fa));
            ct.add(unit(chainAt(tan, fa)));
            for (int i = 0; i <= n; i++) {
                double t = h.from() + (h.to() - h.from()) * i / n;
                if (t > a + 1e-9 && t < b - 1e-9) {
                    cp.add(h.pts().get(i));
                    cn.add(h.nrm().get(i));
                    ct.add(tan.get(i));
                }
            }
            cp.add(chainAt(h.pts(), fb));
            cn.add(chainAt(h.nrm(), fb));
            ct.add(unit(chainAt(tan, fb)));
            if (reversed) {
                java.util.Collections.reverse(cp);
                java.util.Collections.reverse(cn);
                java.util.Collections.reverse(ct);
                for (double[] t : ct) for (int c = 0; c < 3; c++) t[c] = -t[c];
            }
            out.add((int) Math.round(s0 * 8));
            out.add((int) Math.round(s1 * 8));
            out.add(cp.size());
            for (int i = 0; i < cp.size(); i++) {
                double[] t = ct.get(i), nr = cn.get(i);
                double dot = nr[0] * t[0] + nr[1] * t[1] + nr[2] * t[2];
                double[] sq = unit(new double[]{nr[0] - dot * t[0], nr[1] - dot * t[1], nr[2] - dot * t[2]});
                for (int c = 0; c < 3; c++) out.add((int) Math.round((cp.get(i)[c] - base[c]) * 8));
                for (int c = 0; c < 3; c++) out.add((int) Math.round(sq[c] * 64));
                for (int c = 0; c < 3; c++) out.add((int) Math.round(t[c] * 64));
            }
        }
        if (out.isEmpty()) return null;
        int[] res = new int[out.size()];
        for (int i = 0; i < res.length; i++) res[i] = out.get(i);
        return res;
    }

    /** The arc length on the route nearest point q. */
    static double project(Route r, double[] q) {
        double best = 0, bd = Double.MAX_VALUE;
        for (int i = 1; i < r.n(); i++) {
            double[] a = r.pts().get(i - 1), b = r.pts().get(i);
            double len = r.s()[i] - r.s()[i - 1];
            double f = 0;
            if (len > 1e-9) {
                f = ((q[0] - a[0]) * (b[0] - a[0]) + (q[1] - a[1]) * (b[1] - a[1]) + (q[2] - a[2]) * (b[2] - a[2])) / (len * len);
                f = Math.max(0, Math.min(1, f));
            }
            double[] p = {a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
            double d = dist(p, q);
            if (d < bd - 1e-9) {
                bd = d;
                best = r.s()[i - 1] + len * f;
            }
        }
        return best;
    }

    static double[] unit(double[] v) {
        double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return l < 1e-12 ? new double[]{1, 0, 0} : new double[]{v[0] / l, v[1] / l, v[2] / l};
    }

    /** The chain's point at fractional node index f (linear between nodes). */
    static double[] chainAt(List<double[]> chain, double f) {
        int i = (int) Math.max(0, Math.min(chain.size() - 2, Math.floor(f)));
        double t = Math.max(0, Math.min(1, f - i));
        double[] a = chain.get(i), b = chain.get(i + 1);
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    /** Hangs for laying: the route and its hung spans (both needed to encode each piece's share). */
    public record Laid(Route route, List<Hang> hangs) {
    }

    public static Laid laid(CableRouter.Space space, RouteKind kind, List<CableRouter.Step> steps) {
        Route r = route(space, kind, steps);
        return new Laid(r, hangs(space, kind, steps));
    }
}
