package mods.eln.fluidcore;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.fluidcore.Network.BAND;
import static mods.eln.fluidcore.Network.G;
import static mods.eln.fluidcore.Network.smoothStep;
import static mods.eln.fluidcore.Network.stepSlope;

/**
 * A node holding any species (10-species-plan.md): liquids in layers, one per miscibility group, the densest lowest;
 * vapours and gases share the space above. Within a group the liquids are an ideal solution (Raoult on the liquid's own
 * mole fractions; Baughn 2026-11-09), split by a Rachford-Rice solve in "negative flash" form and smoothed like
 * WaterAir's single liquid, so one species alone gives exactly WaterAir's split. Everything is evaluated with its
 * gradient against (T, Vg, masses) (D), so the slopes are exact; T and Vg come from the energy and volume residuals by
 * Newton, and their slopes against (masses, U) by the implicit function theorem.
 */
final class MixFluid implements NodeFluid {
    final Species[] sp;
    final int S, NG;
    final double V, H;
    final boolean fixed;
    /** The groups of liquid-capable species: their members' indices. */
    final int[][] groups;

    double[] m;
    double U;
    double T = 300, vg = -1;
    Eval st;
    /** d(T, Vg) / d(masses, U): [2][S + 1]. */
    double[][] dTheta;

    MixFluid(Species[] sp, double V, double H, boolean fixed) {
        this.sp = sp;
        this.S = sp.length;
        this.NG = S + 2;
        this.V = V;
        this.H = H;
        this.fixed = fixed;
        List<String> names = new ArrayList<>();
        List<List<Integer>> gs = new ArrayList<>();
        for (int i = 0; i < S; i++) {
            if (!sp[i].liquidCapable()) continue;
            int g = names.indexOf(sp[i].group);
            if (g < 0) {
                names.add(sp[i].group);
                gs.add(new ArrayList<>());
                g = names.size() - 1;
            }
            gs.get(g).add(i);
        }
        groups = new int[gs.size()][];
        for (int g = 0; g < groups.length; g++) groups[g] = gs.get(g).stream().mapToInt(Integer::intValue).toArray();
        m = new double[S];
    }

    /** Tests: throw on a non-finite state or slope instead of passing it on. */
    static boolean DEBUG_NAN = false;

    public int species() {
        return S;
    }

    // ---- per-species thermodynamics (D in, D out)

    private static final double T0 = WaterAir.T0, P0 = WaterAir.P0, K = WaterAir.K;

    private static D vLiquid(Species s, D p) {
        return p.add(-P0).mul(-1 / K).exp().mul(s.v0);
    }

    private static D eElastic(Species s, D v) {
        D x = v.mul(1 / s.v0);
        return x.mul(x.log()).sub(x).add(1).mul(K * s.v0);
    }

    private static D uLiquid(Species s, D T) {
        return T.add(-T0).mul(s.cpL);
    }

    /** The liquid's Gibbs energy at (T, p). */
    private static D gLiquid(Species s, D T, D p) {
        D thermal = T.add(-T0).mul(s.cpL).sub(T.mul(T.mul(1 / T0).log()).mul(s.cpL)).add(P0 * s.v0);
        D el = p.add(-P0).mul(-1 / K).exp().neg().add(1).mul(s.v0 * K);
        return thermal.add(el);
    }

    /** ln ps over a liquid at pressure p (WATER, CURVE). */
    private static D lnPs(Species s, D T, D p) {
        if (s.kind == Species.Kind.WATER) {
            double[] l = WaterAir.lnPSatD(T.v, p.v);
            double[] g = new double[T.n()];
            for (int i = 0; i < g.length; i++) g[i] = l[1] * T.g[i] + l[2] * p.g[i];
            return new D(l[0], g);
        }
        D hv = T.add(-s.tb).mul(s.cpV).add(s.hvb);
        D s0 = T.mul(1 / s.tb).log().mul(s.cpV).add(s.svb);
        D g0 = hv.sub(T.mul(s0));
        return gLiquid(s, T, p).sub(g0).div(T.mul(s.r)).add(Math.log(P0));
    }

    private static D waterB(D T) {
        WaterAir.Vap v = WaterAir.vap(T.v);
        return T.map(v.b(), v.db());
    }

    private static D waterDB(D T) {
        WaterAir.Vap v = WaterAir.vap(T.v);
        return T.map(v.db(), v.d2b());
    }

    private static D waterH0(D T) {
        WaterAir.Vap v = WaterAir.vap(T.v);
        return T.map(v.h0(), v.cp0());
    }

    /** The vapour's (or gas's) internal energy and enthalpy at T and its partial pressure pv. */
    private static D uVapour(Species s, D T, D pv) {
        switch (s.kind) {
            case WATER:
                return waterH0(T).sub(T.mul(WaterAir.R_V)).sub(T.mul(waterDB(T)).mul(pv));
            case CURVE:
                return T.add(-s.tb).mul(s.cpV).add(s.hvb).sub(T.mul(s.r));
            default:
                return T.add(-T0).mul(s.cpV);
        }
    }

    private static D hVapour(Species s, D T, D pv) {
        switch (s.kind) {
            case WATER:
                return waterH0(T).add(waterB(T).sub(T.mul(waterDB(T))).mul(pv));
            case CURVE:
                return T.add(-s.tb).mul(s.cpV).add(s.hvb);
            default:
                return T.add(-T0).mul(s.cpV).add(T.mul(s.r));
        }
    }

    // ---- the state at (T, Vg, masses)

    /** Everything a split is made of, with gradients against (T, Vg, masses). */
    final class Eval {
        D T, vg, p, E, rv, gasMass, hG;
        D[] ml = new D[S], mv = new D[S], pv = new D[S], v = new D[S], yG = new D[S];
        /** Per group: mole fractions in its liquid (x), its liquid volume, reference density (for the head), actual density, per-kg shares and enthalpy. */
        D[][] x = new D[groups.length][], y = new D[groups.length][];
        D[] layerVol = new D[groups.length], rhoRef = new D[groups.length], rhoAct = new D[groups.length], hL = new D[groups.length];
        /** Groups from the bottom up (by reference density), and the levels of their tops (shares of the height). */
        int[] order = new int[groups.length];
        D[] top = new D[groups.length];
    }

    Eval eval(double Tv, double vgv, double[] mv) {
        Eval e = new Eval();
        int n = NG;
        D T = D.in(Tv, 0, n), Vg = D.in(vgv, 1, n);
        D[] mm = new D[S];
        for (int i = 0; i < S; i++) mm[i] = D.in(mv[i], 2 + i, n);
        e.T = T;
        e.vg = Vg;
        D RToverVg = T.div(Vg);
        double cush = WaterAir.AIR_CUSHION * V;
        D pCush = RToverVg.mul(cush * WaterAir.R_A);
        // gases: all in the gas space
        D pGas = pCush;
        for (int i = 0; i < S; i++)
            if (!sp[i].liquidCapable()) {
                e.ml[i] = D.c(0, n);
                e.mv[i] = mm[i];
                e.pv[i] = RToverVg.mul(mm[i]).mul(sp[i].r);
                pGas = pGas.add(e.pv[i]);
            }
        D p = pGas;
        for (int pass = 0; pass < 3; pass++) {
            D pNext = pGas;
            for (int g = 0; g < groups.length; g++) {
                split(e, g, T, Vg, p, mm);
                for (int i : groups[g]) pNext = pNext.add(e.pv[i]);
            }
            p = pNext;
        }
        e.p = p;
        // the liquid's volumes, the energy, the volume residual
        D E = T.add(-T0).mul(cush * WaterAir.CV_A), vl = D.c(0, n);
        for (int i = 0; i < S; i++) {
            Species s = sp[i];
            if (s.liquidCapable()) {
                e.v[i] = vLiquid(s, p);
                vl = vl.add(e.ml[i].mul(e.v[i]));
                E = E.add(e.ml[i].mul(uLiquid(s, T).add(eElastic(s, e.v[i]))));
            }
            if (s.kind != Species.Kind.NONE) E = E.add(e.mv[i].mul(uVapour(s, T, e.pv[i])));
        }
        e.E = E;
        e.rv = Vg.add(vl).mul(1 / V).add(-1);
        // the gas: per-kg shares (the cushion's share is drawn as nothing) and enthalpy
        D gm = D.c(cush, n);
        for (int i = 0; i < S; i++) if (sp[i].kind != Species.Kind.NONE) gm = gm.add(e.mv[i]);
        e.gasMass = gm;
        D hG = D.c(0, n);
        for (int i = 0; i < S; i++) {
            if (sp[i].kind == Species.Kind.NONE || gm.v <= 0) {
                e.yG[i] = D.c(0, n);
                continue;
            }
            e.yG[i] = e.mv[i].div(gm);
            hG = hG.add(e.yG[i].mul(hVapour(sp[i], T, e.pv[i])));
        }
        e.hG = hG;
        // the layers: composition from the mole fractions (finite for an empty layer too)
        for (int g = 0; g < groups.length; g++) {
            D massX = D.c(0, n), volRef = D.c(0, n), volAct = D.c(0, n), lv = D.c(0, n);
            for (int k = 0; k < groups[g].length; k++) {
                int i = groups[g][k];
                D xm = e.x[g][k].mul(sp[i].molarMass);
                massX = massX.add(xm);
                volRef = volRef.add(xm.mul(sp[i].v0));
                volAct = volAct.add(xm.mul(e.v[i]));
                lv = lv.add(e.ml[i].mul(e.v[i]));
            }
            e.layerVol[g] = lv;
            e.rhoRef[g] = massX.div(volRef);
            e.rhoAct[g] = massX.div(volAct);
            e.y[g] = new D[groups[g].length];
            D h = D.c(0, n);
            for (int k = 0; k < groups[g].length; k++) {
                int i = groups[g][k];
                e.y[g][k] = e.x[g][k].mul(sp[i].molarMass).div(massX);
                D hl = uLiquid(sp[i], T).add(eElastic(sp[i], e.v[i])).add(p.mul(e.v[i]));
                h = h.add(e.y[g][k].mul(hl));
            }
            e.hL[g] = h;
        }
        // densest lowest (ties by group order: deterministic)
        Integer[] ord = new Integer[groups.length];
        for (int g = 0; g < ord.length; g++) ord[g] = g;
        java.util.Arrays.sort(ord, (a, b) -> a.equals(b) ? 0 : e.rhoRef[a].v != e.rhoRef[b].v ? Double.compare(e.rhoRef[b].v, e.rhoRef[a].v) : Integer.compare(a, b));
        D cum = D.c(0, n);
        for (int k = 0; k < ord.length; k++) {
            e.order[k] = ord[k];
            cum = cum.add(e.layerVol[ord[k]].mul(1 / V));
            e.top[k] = cum;
        }
        return e;
    }

    /**
     * One group's split at (T, Vg, p): Rachford-Rice in negative-flash form. c_i = moles of saturated vapour i the gas
     * space holds (0 for a non-volatile, infinite for water past its no-root point); N* solves sum n_i / (N + c_i) = 1;
     * the liquid's moles N = softplus over EPS_L of the group's moles; a common factor lam on the c_i makes the shares
     * sum to N, so x_i = n_i / (N + lam c_i) are the liquid's mole fractions and n_l,i = x_i N (one species alone:
     * exactly WaterAir's softplus split).
     */
    private void split(Eval e, int g, D T, D Vg, D p, D[] mm) {
        int[] mem = groups[g];
        int k = mem.length, n = NG;
        D[] nn = new D[k], c = new D[k];
        boolean[] inf = new boolean[k];
        double ntot = 0;
        boolean anyFinite = false;
        for (int a = 0; a < k; a++) {
            Species s = sp[mem[a]];
            nn[a] = mm[mem[a]].mul(1 / s.molarMass);
            ntot += Math.max(0, nn[a].v);
            if (s.kind == Species.Kind.NONE) {
                c[a] = D.c(0, n);
                anyFinite = true;
                continue;
            }
            D ps = lnPs(s, T, p).exp();
            D cm;
            if (s.kind == Species.Kind.WATER) {
                D den = T.mul(WaterAir.R_V).div(ps).add(waterB(T));
                if (den.v <= 0) {
                    inf[a] = true;
                    c[a] = null;
                    continue;
                }
                cm = Vg.div(den);
            } else cm = ps.mul(Vg).div(T.mul(s.r));
            c[a] = cm.mul(1 / s.molarMass);
            anyFinite = true;
        }
        e.x[g] = new D[k];
        // an empty group (or one whose present species are all past their no-root point): a first trace of a volatile
        // is vapour, of a non-volatile liquid (right-hand slopes)
        if (ntot <= 1e-30 || !anyFinite || pick(c, nn, inf) < 0) {   // (1e-30 mol: below it the solve's squares underflow)
            for (int a = 0; a < k; a++) {
                int i = mem[a];
                boolean liquid = sp[i].kind == Species.Kind.NONE;
                e.ml[i] = liquid ? mm[i] : D.c(0, n);
                e.mv[i] = liquid ? D.c(0, n) : mm[i];
                e.x[g][a] = D.c(1.0 / k, n);
            }
            finishVapour(e, g, T, Vg);
            return;
        }
        // N*: Newton from the root of the smallest present c (F >= 0 there; F convex and falling: no overshoot)
        // (solved for u = N* + c_k0, the smallest present c: N* = n_k0 - c_k0 would lose n_k0 entirely when it is far
        // below c_k0, as a trace of vapour is, and q = N* + c_k0 came out 0)
        int k0 = pick(c, nn, inf);
        double u = nn[k0].v;
        for (int it = 0; it < 100; it++) {
            double F = -1, dF = 0;
            for (int a = 0; a < k; a++) {
                if (inf[a] || nn[a].v <= 0) continue;
                double q = u + (c[a].v - c[k0].v);
                F += nn[a].v / q;
                dF -= nn[a].v / (q * q);
            }
            double step = -F / dF;
            u += step;
            if (!(Math.abs(step) > 1e-15 * u)) break;
        }
        // its gradient by the implicit function theorem: du = -F_x / F_u
        D Fx = D.c(-1, n);
        double FU = 0;
        for (int a = 0; a < k; a++) {
            if (inf[a]) continue;
            D q = c[a].sub(c[k0]).add(u);
            if (q.v <= 0) continue;   // (an absent species that would sit below the root: no slope through it)
            Fx = Fx.add(nn[a].div(q));
            FU -= nn[a].v / (q.v * q.v);
        }
        double[] gU = new double[n];
        for (int j = 0; j < n; j++) gU[j] = -Fx.g[j] / FU;
        D Nstar = new D(u, gU).sub(c[k0]);
        D ntotD = D.c(0, n);
        for (int a = 0; a < k; a++) ntotD = ntotD.add(nn[a]);
        D N = D.softplus(Nstar, ntotD.mul(WaterAir.EPS_L));
        // lam: sum n_i / (N + lam c_i) = 1 (only the finite, volatile terms depend on lam)
        D lam;
        boolean lamMatters = false;
        for (int a = 0; a < k; a++) if (!inf[a] && c[a].v > 0 && nn[a].v > 0) lamMatters = true;
        if (!lamMatters || N.v >= ntotD.v) lam = D.c(1, n);
        else {
            // (started from what one species alone would need, (n - N) / c summed over the volatiles: exact for one,
            // and never at lam = 0, where n / N overflows for a trace of liquid; kept positive)
            double sumNC = 0;
            for (int a = 0; a < k; a++) if (!inf[a] && c[a].v > 0 && nn[a].v > 0) sumNC += nn[a].v / c[a].v;
            double l = Math.max(1e-300, sumNC * (1 - N.v / ntotD.v));
            for (int it = 0; it < 100; it++) {
                double Gv = -1, dG = 0;
                for (int a = 0; a < k; a++) {
                    if (inf[a] || nn[a].v <= 0) continue;
                    double q = N.v + l * c[a].v;
                    Gv += nn[a].v / q;
                    dG -= nn[a].v * c[a].v / (q * q);
                }
                if (dG == 0) break;
                double step = -Gv / dG;
                l = l + step > 0 ? l + step : 0.5 * l;
                if (!(Math.abs(step) > 1e-15 * Math.max(1, l))) break;
            }
            D Gx = D.c(-1, n);
            double GL = 0;
            for (int a = 0; a < k; a++) {
                if (inf[a]) continue;
                D q = N.add(c[a].mul(l));
                if (nn[a].v <= 0 && !(q.v > 1e-12 * ntotD.v)) continue;   // (an absent non-volatile in a dry group: no slope through it)
                Gx = Gx.add(nn[a].div(q));
                GL -= nn[a].v * c[a].v / (q.v * q.v);
            }
            double[] gl = new double[n];
            for (int j = 0; j < n; j++) gl[j] = GL == 0 ? 0 : -Gx.g[j] / GL;
            lam = new D(l, gl);
        }
        for (int a = 0; a < k; a++) {
            int i = mem[a];
            Species s = sp[i];
            if (inf[a]) {
                e.x[g][a] = D.c(0, n);
                e.ml[i] = D.c(0, n);
                e.mv[i] = mm[i];
                continue;
            }
            D den = N.add(c[a].mul(lam));
            D x = den.v > 1e-12 * ntotD.v || (nn[a].v > 0 && den.v > 0) ? nn[a].div(den) : D.c(0, n);
            e.x[g][a] = x;
            D nl = s.kind == Species.Kind.NONE ? nn[a] : x.mul(N);
            e.ml[i] = nl.mul(s.molarMass);
            e.mv[i] = mm[i].sub(e.ml[i]);
        }
        finishVapour(e, g, T, Vg);
    }

    private static int pick(D[] c, D[] nn, boolean[] inf) {
        int best = -1;
        for (int a = 0; a < c.length; a++)
            if (!inf[a] && nn[a].v > 0 && (best < 0 || c[a].v < c[best].v)) best = a;
        return best;
    }

    /** The group's vapours' partial pressures (water with its virial). */
    private void finishVapour(Eval e, int g, D T, D Vg) {
        for (int i : groups[g]) {
            Species s = sp[i];
            if (s.kind == Species.Kind.NONE) {
                e.pv[i] = D.c(0, NG);
                e.mv[i] = D.c(0, NG);
                continue;
            }
            D num = e.mv[i].mul(T).mul(s.r);
            e.pv[i] = s.kind == Species.Kind.WATER ? num.div(Vg.sub(e.mv[i].mul(waterB(T)))) : num.div(Vg);
        }
    }

    // ---- the solve

    /** The gas volume at T that fills V (the volume residual rises with Vg). */
    double gasVolume(double[] mv, double T) {
        double lo = Math.log(1e-14 * V), hi = Math.log(V);
        double flo = eval(T, Math.exp(lo), mv).rv.v, fhi = eval(T, Math.exp(hi), mv).rv.v;
        if (flo >= 0) return Math.exp(lo);
        if (fhi <= 0) return Math.exp(hi);
        int side = 0;
        double x = 0.5 * (lo + hi);
        for (int i = 0; i < 200 && hi - lo > 1e-13; i++) {
            x = flo != fhi ? hi - fhi * (hi - lo) / (fhi - flo) : 0.5 * (lo + hi);
            if (!(x > lo && x < hi) || i % 8 == 7) x = 0.5 * (lo + hi);
            double f = eval(T, Math.exp(x), mv).rv.v;
            if (Math.abs(f) < 1e-14) break;
            if (f < 0) {
                lo = x;
                flo = f;
                if (side == -1) fhi *= 0.5;
                side = -1;
            } else {
                hi = x;
                fhi = f;
                if (side == 1) flo *= 0.5;
                side = 1;
            }
        }
        return Math.exp(x);
    }

    double energyAt(double[] mv, double T) {
        return eval(T, gasVolume(mv, T), mv).E.v;
    }

    /** Solve for (T, Vg): Newton from the last state with the exact Jacobian, else a bracket on T. */
    private void solve() {
        if (vg > 0 && newton()) return;
        double lo = WaterAir.T_MIN, hi = WaterAir.T_MAX;
        double t0 = Math.max(lo, Math.min(hi, T)), w = 1;
        double a = Math.max(lo, t0 - w), b = Math.min(hi, t0 + w);
        double fa = energyAt(m, a) - U, fb = energyAt(m, b) - U;
        while (fa > 0 && a > lo) {
            b = a;
            fb = fa;
            w *= 4;
            a = Math.max(lo, t0 - w);
            fa = energyAt(m, a) - U;
        }
        while (fb < 0 && b < hi) {
            a = b;
            fa = fb;
            w *= 4;
            b = Math.min(hi, t0 + w);
            fb = energyAt(m, b) - U;
        }
        double t = 0.5 * (a + b), tol = 1e-13 * Math.max(1, Math.abs(U));
        int side = 0;
        for (int i = 0; i < 200 && b - a > 1e-11; i++) {
            t = fb != fa ? b - fb * (b - a) / (fb - fa) : 0.5 * (a + b);
            if (!(t > a && t < b) || i % 8 == 7) t = 0.5 * (a + b);
            double f = energyAt(m, t) - U;
            if (Math.abs(f) <= tol) break;
            if (f < 0) {
                a = t;
                fa = f;
                if (side == -1) fb *= 0.5;
                side = -1;
            } else {
                b = t;
                fb = f;
                if (side == 1) fa *= 0.5;
                side = 1;
            }
        }
        T = t;
        vg = gasVolume(m, t);
        newton();   // (polish, and leaves st at the solution)
        st = eval(T, vg, m);
    }

    private boolean newton() {
        double t = T, x = Math.log(vg);
        for (int it = 0; it < 12; it++) {
            Eval e = eval(t, Math.exp(x), m);
            double r0 = e.E.v - U, r1 = e.rv.v;
            if (Math.abs(r0) < Math.max(1, Math.abs(U)) * 1e-13 + 1e-9 && Math.abs(r1) < 1e-13) {
                T = t;
                vg = Math.exp(x);
                st = e;
                return true;
            }
            double vgv = Math.exp(x);
            double j11 = e.E.g[0], j12 = e.E.g[1] * vgv, j21 = e.rv.g[0], j22 = e.rv.g[1] * vgv;
            double det = j11 * j22 - j12 * j21;
            if (!(Math.abs(det) > 0) || !Double.isFinite(det)) return false;
            double dT = -(j22 * r0 - j12 * r1) / det, dx = -(-j21 * r0 + j11 * r1) / det;
            double kk = Math.min(1, Math.min(30 / Math.max(1e-30, Math.abs(dT)), 1 / Math.max(1e-30, Math.abs(dx))));
            t += kk * dT;
            x += kk * dx;
            if (!(t > WaterAir.T_MIN && t < WaterAir.T_MAX) || x > Math.log(V) + 1e-9 || !Double.isFinite(x)) return false;
        }
        return false;
    }

    /** Fill: the energy of masses m all at T (tests, Node.fill). */
    double energyOf(double[] mv, double Tv) {
        return energyAt(mv, Tv);
    }

    public void update(double[] mNew, double Unew) {
        // (masses under 1e-100 kg count as none: denormal traces, which Newton's iterates leave behind, overflow the
        // split's reciprocals; the node's own masses, and so conservation, are untouched)
        for (int i = 0; i < S; i++) m[i] = Math.abs(mNew[i]) < 1e-100 ? 0 : mNew[i];
        U = Unew;
        solve();
        if (DEBUG_NAN && !(Double.isFinite(st.p.v) && Double.isFinite(T))) throw new IllegalStateException("NaN state: m " + java.util.Arrays.toString(m) + " U " + U + " T " + T + " vg " + vg);
        // d(T, Vg)/d(masses, U) = -J^-1 dR/d(masses, U)
        double j11 = st.E.g[0], j12 = st.E.g[1], j21 = st.rv.g[0], j22 = st.rv.g[1];
        double det = j11 * j22 - j12 * j21;
        dTheta = new double[2][S + 1];
        for (int k = 0; k <= S; k++) {
            double d1 = k < S ? st.E.g[2 + k] : -1, d2 = k < S ? st.rv.g[2 + k] : 0;
            dTheta[0][k] = -(j22 * d1 - j12 * d2) / det;
            dTheta[1][k] = -(-j21 * d1 + j11 * d2) / det;
        }
    }

    /** A gradient against (T, Vg, masses) as slopes against (masses, U). */
    double[] slope(D d) {
        double[] s = new double[S + 1];
        if (fixed) return s;
        for (int k = 0; k <= S; k++) s[k] = (k < S ? d.g[2 + k] : 0) + d.g[0] * dTheta[0][k] + d.g[1] * dTheta[1][k];
        if (DEBUG_NAN) for (double x : s) if (!Double.isFinite(x)) throw new IllegalStateException("NaN slope " + java.util.Arrays.toString(s) + " of " + d.v + " " + java.util.Arrays.toString(d.g) + " theta " + java.util.Arrays.deepToString(dTheta) + " m " + java.util.Arrays.toString(m) + " U " + U + " T " + T + " vg " + vg);
        return s;
    }

    // ---- what Network asks

    public double T() {
        return T;
    }

    public double[] tSlope() {
        double[] s = new double[S + 1];
        if (fixed) return s;
        for (int k = 0; k <= S; k++) s[k] = dTheta[0][k];
        return s;
    }

    public double P() {
        return st.p.v;
    }

    public double level() {
        return groups.length == 0 ? 0 : st.top[groups.length - 1].v;
    }

    /**
     * The head over a port at f: g H sum over layers rho_k (softplus(top_k - f) - softplus(top_k-1 - f)), the floor
     * being layer 0's bottom; smooth at each surface, and an empty layer adds nothing whatever its composition (its
     * composition jumps when a first trace arrives).
     */
    private D portPD(double f) {
        D p = st.p;
        D w = D.c(0.25 * BAND, NG);
        D below = D.softplus(D.c(-f, NG), w);
        for (int k = 0; k < groups.length; k++) {
            D here = D.softplus(st.top[k].add(-f), w);
            p = p.add(st.rhoRef[st.order[k]].mul(here.sub(below)).mul(G * H));
            below = here;
        }
        return p;
    }

    public double portP(double f) {
        return portPD(f).v;
    }

    public double[] portPSlope(double f) {
        return slope(portPD(f));
    }

    /** The port's windows: x_k = how far it sits below layer k's top (every window starts at its surface). */
    private D[] windows(double f) {
        int L = groups.length;
        D[] xs = new D[L];
        for (int k = 0; k < L; k++) {
            D arg = st.top[k].add(-Math.min(f, 1 - BAND)).mul(2 / BAND).add(-1);
            xs[k] = arg.map(smoothStep(arg.v), stepSlope(arg.v));
        }
        return xs;
    }

    /** {per species shares..., energy} drawn per kg at f, as gradients. */
    private D[] drawD(double f) {
        D[] xs = windows(f);
        int L = groups.length;
        D[] out = new D[S + 1];
        for (int i = 0; i <= S; i++) out[i] = D.c(0, NG);
        D below = D.c(0, NG);
        for (int k = 0; k < L; k++) {
            D wk = xs[k].sub(below);
            below = xs[k];
            int g = st.order[k];
            for (int a = 0; a < groups[g].length; a++) out[groups[g][a]] = out[groups[g][a]].add(wk.mul(st.y[g][a]));
            out[S] = out[S].add(wk.mul(st.hL[g]));
        }
        D gas = below.neg().add(1);
        for (int i = 0; i < S; i++) out[i] = out[i].add(gas.mul(st.yG[i]));
        out[S] = out[S].add(gas.mul(st.hG));
        return out;
    }

    public double[] draw(double f) {
        D[] d = drawD(f);
        double[] o = new double[S + 1];
        for (int i = 0; i <= S; i++) o[i] = d[i].v;
        return o;
    }

    public double[][] drawSlope(double f) {
        D[] d = drawD(f);
        double[][] o = new double[S + 1][];
        for (int i = 0; i <= S; i++) o[i] = slope(d[i]);
        return o;
    }

    public double liquidWindow(double f) {
        D[] xs = windows(f);
        return xs.length == 0 ? 0 : xs[xs.length - 1].v;
    }

    /** The liquid drawn at f: the layers' mix by their windows (the bottom layer's if none). */
    private double[] layerWeights(double f) {
        D[] xs = windows(f);
        double[] w = new double[groups.length];
        double below = 0, tot = 0;
        for (int k = 0; k < w.length; k++) {
            w[k] = xs[k].v - below;
            below = xs[k].v;
            tot += w[k];
        }
        if (tot <= 0) {
            if (w.length > 0) w[0] = 1;
            return w;
        }
        for (int k = 0; k < w.length; k++) w[k] /= tot;
        return w;
    }

    public double rhoLiquid(double f) {
        double[] w = layerWeights(f);
        double r = 0;
        for (int k = 0; k < w.length; k++) r += w[k] * st.rhoAct[st.order[k]].v;
        return r;
    }

    public double rhoGas() {
        return st.gasMass.v / vg;
    }

    public double[] liquidDraw(double f) {
        double[] w = layerWeights(f);
        double[] o = new double[S + 1];
        for (int k = 0; k < w.length; k++) {
            int g = st.order[k];
            for (int a = 0; a < groups[g].length; a++) o[groups[g][a]] += w[k] * st.y[g][a].v;
            o[S] += w[k] * st.hL[g].v;
        }
        return o;
    }

    public double[] gasDraw() {
        double[] o = new double[S + 1];
        for (int i = 0; i < S; i++) o[i] = st.yG[i].v;
        o[S] = st.hG.v;
        return o;
    }

    public Object saveState() {
        return new Object[]{T, vg, st, dTheta};
    }

    public void restoreState(Object s) {
        Object[] a = (Object[]) s;
        T = (Double) a[0];
        vg = (Double) a[1];
        st = (Eval) a[2];
        dTheta = (double[][]) a[3];
    }
}
