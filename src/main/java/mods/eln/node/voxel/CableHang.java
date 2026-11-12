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

    /** Energy weights: per voxel of length per voxel of height, per turn squared per voxel, per voxel of length, per
     *  voxel peeled off a surface. */
    static double E_GRAVITY = 0.2, E_BEND = 3.0, E_TENSION = 1.0, E_PEEL = 0.3;
    /** How far it may peel off each surface (voxels, in steps of 2); the handle lengths (shares of the chord) and sags
     *  (voxels) of the coarse grid. */
    static double MAX_PEEL = 8;
    /** How much longer than the route it replaces a curve may be (the kit's give: a straight bridge can sag). */
    static final double SLACK = 0.05;
    static double[] HANDLES = {0.25, 0.5, 0.8, 1.2}, SAGS = {0, 1, 2, 3, 4};

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

    /** Every span of the route, hung: the lowest-energy curve found for it (none where nothing fits: drawn as routed). */
    public static List<Hang> hangs(CableRouter.Space space, RouteKind kind, List<CableRouter.Step> steps) {
        Route r = route(space, kind, steps);
        List<double[]> sp = spans(r);
        List<Hang> out = new ArrayList<>();
        java.util.Map<Long, Boolean> seen = new java.util.HashMap<>();
        for (int k = 0; k < sp.size(); k++) {
            double lo = k == 0 ? 0 : sp.get(k - 1)[1], hi = k + 1 < sp.size() ? sp.get(k + 1)[0] : r.length();
            Hang h = solve(space, seen, r, sp.get(k)[0], sp.get(k)[1], lo, hi, kind.height());
            if (h != null) out.add(h);
        }
        return out;
    }

    /**
     * The span [a, b] of the route hung (lo, hi: how far it may peel back). The unknowns are where it leaves the surface
     * before it (a - pa), where it lands after (b + pb), and the curve between: a Hermite curve along the surfaces'
     * directions there (handle lengths ha, hb) lowered by a smooth bump (depth sag) that keeps the ends and their
     * directions. Every candidate is scored over the same stretch of route, [a - maxPeel, b + maxPeel], the peeled part
     * by the curve and the rest as routed: gravity (height per voxel of length), bending (turn squared per voxel),
     * tension (per voxel of length: so a shorter slant can win; the routed length plus SLACK is only an upper limit) and a cost
     * per voxel peeled (Baughn: strain on the attachment). A candidate whose cable would touch a blocked voxel is out.
     * A coarse grid, then a local search round the best. Null when nothing fits.
     */
    static Hang solve(CableRouter.Space space, java.util.Map<Long, Boolean> seen, Route r, double a, double b, double lo, double hi, double height) {
        double maxA = Math.min(MAX_PEEL, a - lo), maxB = Math.min(MAX_PEEL, hi - b);
        double from0 = a - Math.max(0, maxA), to0 = b + Math.max(0, maxB);
        double best = Double.MAX_VALUE;
        double[] bestP = null;
        for (double pa = 0; pa <= maxA + 1e-9; pa += 2)
            for (double pb = 0; pb <= maxB + 1e-9; pb += 2) {
                double chord = dist(r.at(a - pa), r.at(b + pb));
                for (double ha : HANDLES)
                    for (double hb : HANDLES)
                        for (double sag : SAGS) {
                            double e = score(space, seen, r, a - pa, b + pb, ha * chord, hb * chord, sag, from0, to0, height);
                            if (e + E_PEEL * (pa + pb) < best) {
                                best = e + E_PEEL * (pa + pb);
                                bestP = new double[]{pa, pb, ha * chord, hb * chord, sag};
                            }
                        }
            }
        if (bestP == null) return null;
        // refine the curve's three shape numbers round the best (the peels stay)
        double[] step = {0, 0, Math.max(0.5, bestP[2] / 4), Math.max(0.5, bestP[3] / 4), 0.5};
        for (int round = 0; round < 24; round++) {
            boolean better = false;
            for (int q = 2; q < 5; q++)
                for (int sign = -1; sign <= 1; sign += 2) {
                    double[] t = bestP.clone();
                    t[q] = Math.max(q == 4 ? 0 : 0.1, t[q] + sign * step[q]);
                    double e = score(space, seen, r, a - t[0], b + t[1], t[2], t[3], t[4], from0, to0, height) + E_PEEL * (t[0] + t[1]);
                    if (e < best - 1e-9) {
                        best = e;
                        bestP = t;
                        better = true;
                    }
                }
            if (!better) for (int q = 2; q < 5; q++) step[q] /= 2;
        }
        double from = a - bestP[0], to = b + bestP[1];
        List<double[]> pts = curve(r, from, to, bestP[2], bestP[3], bestP[4]);
        List<double[]> nrm = new ArrayList<>();
        int n = pts.size() - 1;
        for (int i = 0; i <= n; i++) nrm.add(r.normal(from + (to - from) * i / n));
        return new Hang(from, to, pts, smooth(nrm));
    }

    /** Points along the curve from route arc from to to (~1 voxel apart): Hermite along the route's directions there
     *  (handles ha, hb), lowered by sag at its middle (16 u^2 (1-u)^2: ends and their directions kept). */
    static List<double[]> curve(Route r, double from, double to, double ha, double hb, double sag) {
        double[] p0 = r.at(from), p1 = r.at(to);
        double[] t0 = unit(sub(r.at(Math.min(r.length(), from + 0.5)), r.at(Math.max(0, from - 0.5))));
        double[] t1 = unit(sub(r.at(Math.min(r.length(), to + 0.5)), r.at(Math.max(0, to - 0.5))));
        int n = (int) Math.max(4, Math.ceil(dist(p0, p1) + (ha + hb) / 3));
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            double u = (double) i / n, u2 = u * u, u3 = u2 * u;
            double h00 = 2 * u3 - 3 * u2 + 1, h10 = u3 - 2 * u2 + u, h01 = -2 * u3 + 3 * u2, h11 = u3 - u2;
            double[] q = new double[3];
            for (int c = 0; c < 3; c++) q[c] = h00 * p0[c] + h10 * ha * t0[c] + h01 * p1[c] + h11 * hb * t1[c];
            q[1] -= sag * 16 * u2 * (1 - u) * (1 - u);
            out.add(q);
        }
        return out;
    }

    /** The energy of a candidate over route arc [from0, to0] (see solve), or MAX_VALUE if it touches a blocked voxel or
     *  is longer than the route it replaces. */
    static double score(CableRouter.Space space, java.util.Map<Long, Boolean> seen, Route r, double from, double to, double ha, double hb, double sag,
                        double from0, double to0, double height) {
        List<double[]> c = curve(r, from, to, ha, hb, sag);
        int n = c.size() - 1;
        double len = 0;
        for (int i = 1; i <= n; i++) len += dist(c.get(i - 1), c.get(i));
        if (len > (to - from) * (1 + SLACK) + 1e-6) return Double.MAX_VALUE;   // (a little more than the route laid)
        double e = 0;
        for (int i = 1; i <= n; i++) {
            double[] p = c.get(i - 1), q = c.get(i);
            double ds = dist(p, q);
            e += (E_GRAVITY * (p[1] + q[1]) / 2 + E_TENSION) * ds;
        }
        for (int i = 1; i < n; i++) {
            double[] d0 = unit(sub(c.get(i), c.get(i - 1))), d1 = unit(sub(c.get(i + 1), c.get(i)));
            double cos = Math.max(-1, Math.min(1, d0[0] * d1[0] + d0[1] * d1[1] + d0[2] * d1[2]));
            double turn = Math.acos(cos), ds = (dist(c.get(i), c.get(i - 1)) + dist(c.get(i + 1), c.get(i))) / 2;
            e += E_BEND * turn * turn / Math.max(ds, 0.1);
        }
        // its cable stays clear of blocks: the body above the line, at its middle and near its top
        for (int i = 1; i < n; i++) {
            double[] nr = r.normal(from + (to - from) * i / n), p = c.get(i);
            for (double f : new double[]{0.25, Math.max(0.5, height - 0.25)})
                if (blockedAt(space, seen, new double[]{p[0] + nr[0] * f, p[1] + nr[1] * f, p[2] + nr[2] * f})) return Double.MAX_VALUE;
        }
        // the rest of [from0, to0] as routed: on its surfaces, at their height
        e += routed(r, from0, from) + routed(r, to, to0);
        return e;
    }

    /** Gravity and tension of the route between arcs a and b (it lies flat on its surfaces: no bending). */
    static double routed(Route r, double a, double b) {
        if (b - a < 1e-9) return 0;
        double e = 0;
        int n = (int) Math.max(1, Math.ceil(b - a));
        for (int i = 0; i < n; i++) {
            double[] p = r.at(a + (b - a) * i / n), q = r.at(a + (b - a) * (i + 1) / n);
            double ds = dist(p, q);
            e += (E_GRAVITY * (p[1] + q[1]) / 2 + E_TENSION) * ds;
        }
        return e;
    }

    static double[] sub(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    /** Is the voxel holding point p blocked? Remembered per voxel (the search asks about the same few many times). */
    static boolean blockedAt(CableRouter.Space space, java.util.Map<Long, Boolean> seen, double[] p) {
        long vx = (long) Math.floor(p[0]), vy = (long) Math.floor(p[1]), vz = (long) Math.floor(p[2]);
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
        double x = p[0] + 0.25 * n[0], y = p[1] + 0.25 * n[1], z = p[2] + 0.25 * n[2];
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
