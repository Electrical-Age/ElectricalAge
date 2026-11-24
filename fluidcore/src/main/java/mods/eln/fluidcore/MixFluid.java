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
    /** The most Newton steps any one split's root has taken on this node (tests bound it). */
    int splitSteps;
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

    /** The state at (T, Vg, masses), complete: what the network draws and reads included. */
    Eval eval(double Tv, double vgv, double[] mv) {
        return complete(core(Tv, vgv, mv));
    }

    /**
     * The split, pressure, energy and volume residual at (T, Vg, masses): all the flash's own solve reads. The rest (the gas's
     * and the layers' shares and enthalpies, complete()) is for the solved state alone; worked out on every evaluation it
     * was some third of the dual-number work of the polish and the bracket's searches.
     */
    private Eval core(double Tv, double vgv, double[] mv) {
        if (COUNT) EVALS++;
        Network.checkClock();
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
        return e;
    }

    /** core()'s state with the gas's and the layers' shares and enthalpies, the layers' order and their tops. */
    private Eval complete(Eval e) {
        int n = NG;
        D T = e.T, p = e.p;
        double cush = WaterAir.AIR_CUSHION * V;
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
     * space holds (0 for a non-volatile, infinite for water past its no-root point). The liquid's moles N =
     * softplus(X) over EPS_L of the group's moles, where X solves sum n_i / softplus(X + c_i) = 1. Where every
     * X + c_i that matters is some widths above zero this is the unsmoothed equation sum n_i / (X + c_i) = 1, whose
     * root is the liquid's moles, or below zero by what the vapour lacks to saturation (one species alone:
     * X = n - c, exactly WaterAir's softplus split). The softplus under each term is what lets a trace come and go:
     * the unsmoothed root can never pass below -c_i of a species that is present at all, so the first 1e-28 kg of
     * one with c_i under the width (a residue, or diesel in hot gasoline vapour) held it near zero and made some
     * 0.7 % of a superheated vapour liquid. As written a trace's term fades with the trace. Then a common factor
     * lam <= 1 on the c_i makes the shares sum to 1 at that N (softplus rises no faster than its argument, so
     * sum n_i / (N + c_i) <= 1 and lam exists): x_i = n_i / (N + lam c_i) are the liquid's mole fractions and
     * n_l,i = x_i N.
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
            // (a mass that rounding left below zero counts as none, slopes kept: as an amount it gave negative
            // shares, and a negative width where it outweighed the rest)
            D ma = mm[mem[a]];
            nn[a] = (ma.v < 0 ? ma.add(-ma.v) : ma).mul(1 / s.molarMass);
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
        for (int a = 0; a < k; a++) if (!inf[a]) ntot += nn[a].v;
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
        D ntotD = D.c(0, n);
        for (int a = 0; a < k; a++) if (!inf[a]) ntotD = ntotD.add(nn[a]);   // (those past their no-root point are all vapour)
        D wD = ntotD.mul(WaterAir.EPS_L);
        double w = wD.v;
        // X: H(X) = sum n_i / softplus(X + c_i) - 1 = 0, convex and falling, by Newton from the largest
        // softplus^-1(n_i) - c_i: that species' term is 1 there and H >= 0, so no step overshoots, and no other
        // term is above 1, so the root is near
        // (solved for y = X + c_b, b that species: its term is the one that decides, and X itself would round away
        // what a trace of vapour leaves of n_b - c_b)
        int b = -1;
        double x0 = 0;
        for (int a = 0; a < k; a++) {
            if (inf[a] || nn[a].v <= 0) continue;
            double xa = softInv(nn[a].v, w) - c[a].v;
            if (b < 0 || xa > x0) {
                b = a;
                x0 = xa;
            }
        }
        double y = softInv(nn[b].v, w);
        for (int it = 0; it < 100; it++) {
            double Hv = -1, HY = 0;
            for (int a = 0; a < k; a++) {
                if (inf[a] || nn[a].v <= 0) continue;
                // (above the cut the argument itself, not w (x / w): one species alone then stays at y = n to the bit)
                double xa = y + (c[a].v - c[b].v), t = xa / w, sa = t > 30 ? xa : w * soft(t);
                Hv += nn[a].v / sa;
                HY -= nn[a].v * sig(t) / (sa * sa);
            }
            double step = -Hv / HY;
            y += step;
            splitSteps = Math.max(splitSteps, it + 1);
            if (!(step > 1e-15 * Math.max(Math.abs(y), w))) break;   // (from below the steps only shrink: one at rounding's size, or back, is the last)
        }
        // N = softplus(X), its gradient with dX = -H_x / H_X (the implicit function theorem)
        // (written out by species, each slope as softplus'(X) / softplus(X + c_i): an absent species' 1 / softplus(X + c_i)
        // alone overflows in a vapour far from saturation, where the softplus'(X) it meets in N underflows)
        double t0 = (y - c[b].v) / w, sig0 = sig(t0), om0 = soft(t0) - t0 * sig0, hx = 0;
        double[] gN = new double[n];
        for (int a = 0; a < k; a++) {
            if (inf[a]) continue;
            double t = (y + (c[a].v - c[b].v)) / w, st = soft(t), sg = sig(t);
            double r = st > 1e-250 ? sig0 / (w * st) : Math.exp(lnSig(t0) - lnSoft(t)) / w;
            double f = nn[a].v > 0 ? nn[a].v / (w * st) : 0, om = st - t * sg;
            if (f > 0) hx += f * sg / (w * st);
            for (int q = 0; q < n; q++) gN[q] += r * (nn[a].g[q] - f * (sg * c[a].g[q] + om * wD.g[q]));
        }
        for (int q = 0; q < n; q++) gN[q] = gN[q] / hx + om0 * wD.g[q];
        D N = new D(w * soft(t0), gN);
        // n_b - N, b's deficit: the vapour that the liquid leaves of b, written so it keeps its digits where it is a
        // sliver of n_b (a liquid-full node with a bubble of 1e-6 of its volume: N = n_b - c_b to 1e-9). As n_b - N it
        // lost them, and through lam and the shares so did the vapour's mass and partial pressure (noise of 1e-3 Pa,
        // then 1e-12 of the volume through the liquid's compressibility), which held the flash's polish off its
        // tolerance for good: every update fell back to the bracketed solve, 78 % of the flash's work in Baughn's
        // workshop network (2026-11-16). y - w softplus(t0) = c_b + w (t0 - softplus(t0)) = c_b - w ln(1 + e^-t0), and
        // n_b - y is exact where y is within a factor 2 of n_b (b's liquid there; elsewhere, b mostly vapour or a
        // trace, n_b - N is the precise one).
        double defV = t0 > 0 && y >= 0.5 * nn[b].v && y <= 2 * nn[b].v
            ? (nn[b].v - y) + c[b].v - (t0 > 30 ? 0 : w * Math.log1p(Math.exp(-t0)))
            : nn[b].v - N.v;
        double[] gDef = new double[n];
        for (int q = 0; q < n; q++) gDef[q] = nn[b].g[q] - gN[q];
        D def = new D(defV, gDef);
        // lam: sum n_i / (N + lam c_i) = 1 (only the finite, volatile terms depend on lam), b's term written as
        // (def_b - lam c_b) / (N + lam c_b) + 1
        D lam;
        boolean lamMatters = false;
        for (int a = 0; a < k; a++) if (!inf[a] && c[a].v > 0 && nn[a].v > 0) lamMatters = true;
        if (!lamMatters || N.v >= ntotD.v) lam = D.c(1, n);
        else {
            // (started from what one species alone would need, (n - N) / c summed over the volatiles: exact for one,
            // and never at lam = 0, where n / N overflows for a trace of liquid; kept positive)
            double sumNC = 0, rest = defV, cMax = 0, last = Double.POSITIVE_INFINITY;
            for (int a = 0; a < k; a++) if (!inf[a] && c[a].v > 0 && nn[a].v > 0) {
                sumNC += nn[a].v / c[a].v;
                cMax = Math.max(cMax, c[a].v);
            }
            for (int a = 0; a < k; a++) if (!inf[a] && a != b) rest += nn[a].v;
            double l = Math.max(1e-300, sumNC * rest / ntotD.v);
            for (int it = 0; it < 100; it++) {
                double Gv = 0, dG = 0;
                for (int a = 0; a < k; a++) {
                    if (inf[a] || nn[a].v <= 0) continue;
                    double q = N.v + l * c[a].v;
                    Gv += a == b ? (defV - l * c[a].v) / q : nn[a].v / q;
                    dG -= nn[a].v * c[a].v / (q * q);
                }
                if (dG == 0) break;
                double step = -Gv / dG;
                // (N >= N* puts the root at 1 or below; rounding of N can leave none, and the steps then never end)
                if (l + step >= 1) {
                    l = 1;
                    break;
                }
                l = l + step > 0 ? l + step : 0.5 * l;
                // (to where a step changes no share: against lam alone, a trace of liquid under much vapour stopped
                // early and its shares did not sum to 1; then on while the steps still shrink, as the vapours, x_i lam c_i,
                // are only as precise as lam: in a liquid-full node the first stop left them 1e-8 off. Where other
                // liquids share the group, their terms leave G noise at about rounding times N / c, and the steps stop
                // shrinking there)
                double as = Math.abs(step);
                if (!(as > 1e-15 * l) || !(as > 1e-15 * (l + N.v / cMax)) && !(as < 0.5 * last)) break;
                last = as;
            }
            D Gx = D.c(0, n);
            double GL = 0;
            for (int a = 0; a < k; a++) {
                if (inf[a]) continue;
                D q = N.add(c[a].mul(l));
                if (nn[a].v <= 0 && !(q.v > 1e-12 * ntotD.v)) continue;   // (an absent non-volatile in a dry group: no slope through it)
                Gx = Gx.add(a == b ? def.sub(c[a].mul(l)).div(q) : nn[a].div(q));
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
            boolean share = den.v > 1e-12 * ntotD.v || (nn[a].v > 0 && den.v > 0);
            D x = share ? nn[a].div(den) : D.c(0, n);
            e.x[g][a] = x;
            if (s.kind == Species.Kind.NONE) {
                e.ml[i] = mm[i];
                e.mv[i] = mm[i].sub(e.ml[i]);
            } else if (share && mm[i].v >= 0) {
                // the vapour as x lam c (= n - x N, the same by x's definition), the liquid as the rest: as n - x N
                // the vapour of a liquid-full node with a bubble of 1e-6 of its volume was the difference of two
                // numbers 1e8 times its size, and the noise in its partial pressure (1e-3 Pa, then 1e-12 of the
                // volume through the liquid's compressibility) held the flash's polish off its tolerance for good:
                // every update fell back to the bracketed solve, 78 % of the flash's work in Baughn's workshop network
                e.mv[i] = x.mul(lam).mul(c[a]).mul(s.molarMass);
                e.ml[i] = mm[i].sub(e.mv[i]);
            } else {
                e.ml[i] = x.mul(N).mul(s.molarMass);
                e.mv[i] = mm[i].sub(e.ml[i]);
            }
        }
        finishVapour(e, g, T, Vg);
    }

    /** softplus(t) = ln(1 + e^t), cut as D.softplus cuts it. */
    private static double soft(double t) {
        return t > 30 ? t : t < -30 ? Math.exp(t) : Math.log1p(Math.exp(t));
    }

    /** softplus'(t). */
    private static double sig(double t) {
        return t > 30 ? 1 : t < -30 ? Math.exp(t) : 1 / (1 + Math.exp(-t));
    }

    /** ln softplus(t), finite where softplus underflows. */
    private static double lnSoft(double t) {
        return t > 30 ? Math.log(t) : t < -30 ? t : Math.log(Math.log1p(Math.exp(t)));
    }

    /** ln softplus'(t). */
    private static double lnSig(double t) {
        return t > 30 ? 0 : t < -30 ? t : -Math.log1p(Math.exp(-t));
    }

    /** Where w softplus(x / w) is n > 0. */
    private static double softInv(double n, double w) {
        double t = n / w;
        return t > 30 ? n : w * Math.log(Math.expm1(t));
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
        double flo = core(T, Math.exp(lo), mv).rv.v, fhi = core(T, Math.exp(hi), mv).rv.v;
        if (flo >= 0) return Math.exp(lo);
        if (fhi <= 0) return Math.exp(hi);
        int side = 0;
        double x = 0.5 * (lo + hi);
        for (int i = 0; i < 200 && hi - lo > 1e-13; i++) {
            x = flo != fhi ? hi - fhi * (hi - lo) / (fhi - flo) : 0.5 * (lo + hi);
            if (!(x > lo && x < hi) || i % 8 == 7) x = 0.5 * (lo + hi);
            double f = core(T, Math.exp(x), mv).rv.v;
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

    public double energyAt(double[] mv, double T) {
        return core(T, gasVolume(mv, T), mv).E.v;
    }

    /** The energy is outside what the masses hold between T_MIN and T_MAX: T is the bound and doesn't follow U or the masses. */
    private boolean pinned;

    /** Solve for (T, Vg): Newton from the last state with the exact Jacobian, else a bracket on T. */
    private void solve() {
        pinned = false;
        double vg0 = vg, T0 = T;
        if (vg > 0 && newton()) return;
        if (COUNT) BRACKETS++;
        if (BRACKET_LOG != null && BRACKET_LOG.length() < 40000) BRACKET_LOG.append(String.format("bracket V %.3g m %s U %.6g from T %.3f vg %.3g%n", V, java.util.Arrays.toString(m), U, T0, vg0));
        long e0 = EVALS;
        bracket();
        if (COUNT) BRACKET_EVALS += EVALS - e0;
    }

    private void bracket() {
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
        // (more energy than it holds at T_MAX, or less than at T_MIN: the bound is its temperature. Bisecting towards it
        // took some 40 evaluations and a failed polish every update, and the slopes of a free T misled the network's
        // Newton steps: a node boiled dry under a steady heat sat there, and its network took a second a tick)
        if (fb < 0 || fa > 0) {
            pinned = true;
            T = fb < 0 ? b : a;
            vg = gasVolume(m, T);
            st = eval(T, vg, m);
            return;
        }
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

    /**
     * The polish's tolerances: the energy to 1e-13 of the node's energy scale (its |U|, or its masses at 1e5 J/kg, the
     * scale Network judges a node's energy row on), the volume to 1e-14 of the node's. They were 1e-13 max(1, |U|) +
     * 1e-9 J and 1e-13; in Baughn's workshop network (a counterflow exchanger, 2026-11-16) that left the states noisy
     * at the network's own tolerance: 1e-9 J in a 17 mL flue pipe of 14 mg (U ~3 J) is 1e-7 K, and through its pipes an
     * energy row stuck near 1e-6 of the node (STALL_OK; the step failed and sub-stepped), and 1e-13 of a liquid-full
     * 135 mL pipe's volume is 2e-4 Pa, and its neighbours' energy rows crawled at 1e-7. Newton spent half its
     * iterations there (12 a step and 0.4 sub-steps; 6 and none with these). FLASH_ABS, FLASH_RV, OLD_TOL: probe knobs.
     */
    public static double FLASH_ABS = 0, FLASH_RV = 1e-14;
    public static boolean OLD_TOL = false;
    /**
     * The polish's iterations before the bracketed solve takes over (probe knob). 25: its steps in ln Vg are held to 1,
     * and a bubble that shrinks by decades in one network iterate (a liquid-full node's, 1e-3 to 1e-6 of its volume)
     * took 12-15 of them; a bracketed solve costs some 70 evaluations.
     */
    public static int POLISH_MAX = 25;
    /** A shrinking bubble's step taken in 1/Vg (true; false: capped in ln Vg as before, probe knob). */
    public static boolean SHRINK_STEP = true;
    /** The polish accepts a state whose Newton step is below resolution (true; probe knob). */
    public static boolean STEP_FLOOR = true;

    private double energyTol() {
        if (OLD_TOL) return Math.max(1, Math.abs(U)) * 1e-13 + 1e-9;
        double mt = 0;
        for (double x : m) mt += Math.abs(x);
        return 1e-13 * Math.max(1e-6, Math.max(Math.abs(U), 1e5 * mt)) + FLASH_ABS;
    }
    /** Probe counters: flashes that fell back to the bracketed solve, and polish iterations. */
    public static long BRACKETS, POLISH_ITS, SAME, EVALS, BRACKET_EVALS;
    /** Probe: keep the counters (off in the game, where networks step on several threads). */
    public static boolean COUNT = false;
    public static StringBuilder BRACKET_LOG;
    /** An update to the contents it already holds keeps its state (true; false: solved again, probe knob). */
    public static boolean SKIP_SAME = true;
    /** The polish starts from the slopes' first-order guess (false: from the last state; probe knob). */
    public static boolean PREDICT = true;

    private boolean newton() {
        double t = T, x = Math.log(vg), tolE = energyTol(), tPrev = t, xPrev = x;
        for (int it = 0; it < POLISH_MAX; it++) {
            if (COUNT) POLISH_ITS++;
            Eval e = core(t, Math.exp(x), m);
            double r0 = e.E.v - U, r1 = e.rv.v;
            if (!(Double.isFinite(r0) && Double.isFinite(r1)) && it > 0) {
                // (a step past where the liquid can be squeezed: back half way, as a line search would)
                t = 0.5 * (t + tPrev);
                x = 0.5 * (x + xPrev);
                continue;
            }
            if (Math.abs(r0) < tolE && Math.abs(r1) < (OLD_TOL ? 1e-13 : FLASH_RV)) {
                T = t;
                vg = Math.exp(x);
                st = complete(e);
                return true;
            }
            double vgv = Math.exp(x);
            double j11 = e.E.g[0], j12 = e.E.g[1] * vgv, j21 = e.rv.g[0], j22 = e.rv.g[1] * vgv;
            double det = j11 * j22 - j12 * j21;
            if (!(Math.abs(det) > 0) || !Double.isFinite(det)) {
                if (BRACKET_LOG != null && BRACKET_LOG.length() < 40000) BRACKET_LOG.append(String.format("  det fail it %d r0 %.3g r1 %.3g det %.3g x %.3f t %.3f%n", it, r0, r1, det, x, t));
                return false;
            }
            double dT = -(j22 * r0 - j12 * r1) / det, dx = -(-j21 * r0 + j11 * r1) / det;
            if (Math.abs(dT) < 1e-10 && Math.abs(dx) * vgv < 1e-15 * V && Math.abs(r1) < 1e-9 && STEP_FLOOR) {
                // (Newton's step is below what T and Vg can resolve: the residuals left are the evaluation's own rounding.
                // At a bubble squeezed to 1e-14 of its node the energy came out with noise of 1e-6 J against a tolerance
                // of 1e-10, and the polish spent its iterations, then a bracket, on noise)
                T = t;
                vg = Math.exp(x);
                st = complete(e);
                return true;
            }
            tPrev = t;
            xPrev = x;
            double kk = Math.min(1, Math.min(30 / Math.max(1e-30, Math.abs(dT)), 1 / Math.max(1e-30, Math.abs(dx))));
            if (BRACKET_LOG != null && BRACKET_LOG.length() < 40000) BRACKET_LOG.append(String.format("  it %d r0 %.3g r1 %.3g dT %.3g dx %.3g kk %.3g x %.3f%n", it, r0, r1, dT, dx, kk, x));
            if (dx < -1 && SHRINK_STEP) {
                // a bubble shrinking by decades (a line running full of liquid: the pressure that squeezes it follows 1/Vg):
                // the same linear step taken in 1/Vg, Vg / (1 - dx), not capped at one e-fold in ln Vg, which crawled a decade
                // a step towards a bubble 6 decades down while the capped steps scaled dT too and the energy ran away; the
                // polish then fell to the bracket (a still's full condensate line, LabAgent's sensible batch: a third of
                // the still's flash work)
                t += Math.min(1, 30 / Math.max(1e-30, Math.abs(dT))) * dT;
                x -= Math.log1p(-dx);
            } else {
                t += kk * dT;
                x += kk * dx;
            }
            if (!(t > WaterAir.T_MIN && t < WaterAir.T_MAX) || x > Math.log(V) + 1e-9 || !Double.isFinite(x)) {
                if (BRACKET_LOG != null && BRACKET_LOG.length() < 40000) BRACKET_LOG.append(String.format("  bound fail it %d x %.3f t %.3f%n", it, x, t));
                return false;
            }
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
        // (the same contents as its last update: the same state, as solving again from it would end where it starts; a
        // fifth of the updates in Baughn's workshop network, a line search's nodes that its step didn't move)
        boolean same = Unew == U && st != null && dTheta != null;
        for (int i = 0; i < S && same; i++) same = (Math.abs(mNew[i]) < 1e-100 ? 0 : mNew[i]) == m[i];
        if (same && SKIP_SAME) {
            if (COUNT) SAME++;
            return;
        }
        // (the polish starts from the last state moved along its own slopes, d(T, Vg) / d(masses, U): a first-order
        // guess, so most polishes take one Newton step where they took two)
        if (PREDICT && st != null && dTheta != null && !pinned && vg > 0) {
            double dT = dTheta[0][S] * (Unew - U), dV = dTheta[1][S] * (Unew - U);
            for (int i = 0; i < S; i++) {
                double dm = (Math.abs(mNew[i]) < 1e-100 ? 0 : mNew[i]) - m[i];
                dT += dTheta[0][i] * dm;
                dV += dTheta[1][i] * dm;
            }
            if (Double.isFinite(dT) && Double.isFinite(dV)) {
                T += Math.max(-30, Math.min(30, dT));
                vg *= Math.exp(Math.max(-1, Math.min(1, dV / vg)));
            }
        }
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
            dTheta[0][k] = pinned ? 0 : -(j22 * d1 - j12 * d2) / det;
            dTheta[1][k] = pinned ? -d2 / j22 : -(-j21 * d1 + j11 * d2) / det;
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
        int c = slot(f);
        if (c >= 0 && cPort[c] != null) return cPort[c];
        D p = st.p;
        D w = D.c(0.25 * BAND, NG);
        D below = D.softplus(D.c(-f, NG), w);
        for (int k = 0; k < groups.length; k++) {
            D here = D.softplus(st.top[k].add(-f), w);
            p = p.add(st.rhoRef[st.order[k]].mul(here.sub(below)).mul(G * H));
            below = here;
        }
        if (c >= 0) cPort[c] = p;
        return p;
    }

    // (each port's head and draw, as gradients, kept for the state they were worked out on: the network asks for them
    // per edge in every residual and Jacobian, several times over, and a state is solved once)
    private Eval cached;
    private final double[] cF = new double[6];
    private final D[] cPort = new D[6];
    private final D[][] cDraw = new D[6][];
    private int cN;

    /** f's slot in the cache of st's ports (-1: full), the cache emptied first if st has changed. */
    private int slot(double f) {
        if (cached != st) {
            cached = st;
            cN = 0;
            java.util.Arrays.fill(cPort, null);
            java.util.Arrays.fill(cDraw, null);
        }
        for (int i = 0; i < cN; i++) if (cF[i] == f) return i;
        if (cN == cF.length) return -1;
        cF[cN] = f;
        return cN++;
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
        int c = slot(f);
        if (c >= 0 && cDraw[c] != null) return cDraw[c];
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
        if (c >= 0) cDraw[c] = out;
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

    /**
     * The density of liquidDraw's mix. Its weights are shares of the mass, so the volumes add: 1 / sum(w / rho). (The
     * weighted mean of the densities is higher wherever a port sees two layers, by 2 % for water and gasoline in equal
     * parts, and the exchange then sent down more liquid volume than the gas volume it brought up.)
     */
    public double rhoLiquid(double f) {
        double[] w = layerWeights(f);
        double v = 0;
        for (int k = 0; k < w.length; k++) v += w[k] / st.rhoAct[st.order[k]].v;
        return v > 0 ? 1 / v : 0;
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

    public double[][] gasDrawSlope() {
        double[][] o = new double[S + 1][];
        for (int i = 0; i < S; i++) o[i] = slope(st.yG[i]);
        o[S] = slope(st.hG);
        return o;
    }

    public double liquidMass(int i) {
        return st.ml[i] == null ? 0 : st.ml[i].v;
    }

    public double vapourMass(int i) {
        return st.mv[i] == null ? 0 : st.mv[i].v;
    }

    public double partialP(int i) {
        return st.pv[i] == null ? 0 : st.pv[i].v;
    }

    public Object saveState() {
        return new Object[]{T, vg, st, dTheta, m.clone(), U};
    }

    public void restoreState(Object s) {
        Object[] a = (Object[]) s;
        T = (Double) a[0];
        vg = (Double) a[1];
        st = (Eval) a[2];
        dTheta = (double[][]) a[3];
        // (the contents too: update() keeps its state when handed the contents it holds, so they must be the state's)
        System.arraycopy((double[]) a[4], 0, m, 0, m.length);
        U = (Double) a[5];
    }
}
