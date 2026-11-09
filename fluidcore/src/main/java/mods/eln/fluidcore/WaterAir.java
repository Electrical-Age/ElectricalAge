package mods.eln.fluidcore;

/**
 * Thermodynamics of a closed volume holding water (liquid and vapour) and air: a consistent, smooth model for the fluid
 * core (board #61, 08-core-plan.md M1).
 * <ul>
 *   <li>Liquid: heat capacity CP_L, specific volume v0 exp(-(p - p0)/K) (bulk modulus K, so a full volume compresses its
 *       liquid instead of failing), with the elastic energy that goes with it.</li>
 *   <li>Vapour and air: ideal gases with constant heat capacities.</li>
 *   <li>The vapour pressure is not a fitted curve but the equality of the liquid's and the vapour's Gibbs energy (Poynting
 *       term included), so the model has an exact entropy (the availability argument in the plan needs one). The
 *       constants put the boiling point at 100 C at 1 atm with a latent heat of 2257 kJ/kg there.</li>
 *   <li>Smoothing, for Newton: the liquid mass is a softplus of the saturated liquid mass over a width EPS_L of the water
 *       held (no kink where the last drop evaporates), and a cushion of AIR_CUSHION kg of air per m^3 keeps a gas space
 *       (no kink where the last bubble condenses: a full volume compresses the liquid against it).</li>
 * </ul>
 * Given (water, air, U, V), solve() finds T and the gas volume; derivatives() gives their slopes against the contents by
 * the implicit function theorem at the solution (no differentiating through the root finding).
 */
public final class WaterAir {
    private WaterAir() {
    }

    public static final double T0 = 273.15, TB = 373.15, P0 = 101325;
    public static final double R_V = 461.52, R_A = 287.05;
    public static final double CP_L = 4186, CP_V = 1900, CV_V = CP_V - R_V, CV_A = 718;
    public static final double V0 = 1 / 958.0, K = 2e9;
    public static final double L_TB = 2.257e6;
    /** Vapour's internal energy at T0 and entropy at (T0, P0): boiling at TB under P0 with latent heat L_TB. */
    static final double U_V0, S_V0;
    /** Softplus width of the liquid, as a share of the water held; the air cushion (kg/m^3). */
    public static double EPS_L = 1e-2, AIR_CUSHION = 1e-6;
    public static final double T_MIN = 120, T_MAX = 4000;

    static {
        // h_v(TB) - h_l(TB, P0) = L_TB, with h_l = u_l + P0 V0 (incompressible at P0) and h_v = u_v + R_V T
        double hl = CP_L * (TB - T0) + P0 * V0;
        U_V0 = hl + L_TB - R_V * TB - CV_V * (TB - T0);
        // g_v(TB, P0) = g_l(TB, P0): g = h - T s, s_l = CP_L ln(T/T0)
        double sl = CP_L * Math.log(TB / T0);
        S_V0 = sl + L_TB / TB - CP_V * Math.log(TB / T0);
    }

    // ---- per-phase functions (SI; energies relative to liquid at T0 and P0)

    public static double uLiquidThermal(double T) {
        return CP_L * (T - T0);
    }

    static double sLiquid(double T) {
        return CP_L * Math.log(T / T0);
    }

    /** The liquid's specific volume at pressure p. */
    public static double vLiquid(double p) {
        return V0 * Math.exp(-(p - P0) / K);
    }

    /** Elastic energy per kg of liquid at specific volume v (0 at V0): K V0 (x ln x - x + 1), x = v / V0. */
    static double eElastic(double v) {
        double x = v / V0;
        return K * V0 * (x * Math.log(x) - x + 1);
    }

    /** The pressure part of the liquid's Gibbs energy, the integral of v dp from P0 (Poynting). */
    static double gElastic(double p) {
        return V0 * K * (1 - Math.exp(-(p - P0) / K));
    }

    public static double uVapour(double T) {
        return U_V0 + CV_V * (T - T0);
    }

    public static double hVapour(double T) {
        return uVapour(T) + R_V * T;
    }

    static double sVapourStd(double T) {
        return S_V0 + CP_V * Math.log(T / T0);
    }

    public static double uAir(double T) {
        return CV_A * (T - T0);
    }

    public static double hAir(double T) {
        return uAir(T) + R_A * T;
    }

    /** The vapour pressure over liquid at T under pressure p: Gibbs equality, with the Poynting term. */
    public static double pSat(double T, double p) {
        double gl = uLiquidThermal(T) + P0 * V0 - T * sLiquid(T) + gElastic(p);
        double gv0 = hVapour(T) - T * sVapourStd(T);
        return P0 * Math.exp((gl - gv0) / (R_V * T));
    }

    /** softplus of z over width w (w > 0): smooth max(0, z), within w ln 2 of it. */
    static double softplus(double z, double w) {
        double x = z / w;
        return x > 30 ? z : x < -30 ? w * Math.exp(x) : w * Math.log1p(Math.exp(x));
    }

    // ---- the state at given T and gas volume (explicit: what the residuals and the outputs are made of)

    /** The split at T and gas volume Vg for water mw and air ma: liquid mass, vapour mass, pressure, liquid volume. */
    public record Split(double T, double vg, double ml, double mv, double ma, double p, double v) {
        public double vl() {
            return ml * v;
        }
    }

    /**
     * The split for (T, Vg). The pressure is the gas's, from the vapour the saturated liquid would leave and the air; the
     * liquid's mass is the softplus of what water the saturated vapour doesn't take, and its specific volume is that
     * pressure's. Pressure enters the saturation pressure only through the Poynting term, so one fixed-point refinement
     * of the pressure is plenty (the term is 1e-5 of the pressure per bar).
     */
    public static Split split(double mw, double ma, double V, double T, double vg) {
        double mac = ma + AIR_CUSHION * V;
        double p = mac * R_A * T / vg;
        double ml = 0, mv = mw;
        for (int i = 0; i < 3; i++) {
            double ps = pSat(T, p);
            double mvs = ps * vg / (R_V * T);
            ml = softplus(mw - mvs, EPS_L * Math.max(mw, 1e-12));
            mv = mw - ml;
            p = (mv * R_V + mac * R_A) * T / vg;
        }
        return new Split(T, vg, ml, mv, mac, p, vLiquid(p));
    }

    /** Internal energy of a split. */
    public static double energy(Split s) {
        return s.ml() * (uLiquidThermal(s.T()) + eElastic(s.v())) + s.mv() * uVapour(s.T()) + s.ma() * uAir(s.T());
    }

    /** The two residuals at (T, Vg): energy against U, and volume (gas + liquid) against V. */
    static double[] residuals(double mw, double ma, double U, double V, double T, double vg) {
        Split s = split(mw, ma, V, T, vg);
        return new double[]{energy(s) - U, (vg + s.vl()) / V - 1};
    }

    // ---- the solve

    /** The gas volume at T that fills V (the volume residual is monotone in Vg: bisection-safeguarded false position on ln Vg). */
    static double gasVolume(double mw, double ma, double V, double T, double hint) {
        double lo = Math.log(1e-14 * V), hi = Math.log(V);
        double flo = residuals(mw, ma, 0, V, T, Math.exp(lo))[1], fhi = residuals(mw, ma, 0, V, T, Math.exp(hi))[1];
        if (flo >= 0) return Math.exp(lo);
        if (fhi <= 0) return Math.exp(hi);
        int side = 0;
        double x = 0.5 * (lo + hi);
        for (int i = 0; i < 200 && hi - lo > 1e-13; i++) {
            x = flo != fhi ? hi - fhi * (hi - lo) / (fhi - flo) : 0.5 * (lo + hi);
            if (!(x > lo && x < hi) || i % 8 == 7) x = 0.5 * (lo + hi);
            double f = residuals(mw, ma, 0, V, T, Math.exp(x))[1];
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

    /** Solve (water mw, air ma, internal energy U, volume V) for the split; tHint speeds the bracket. */
    public static Split solve(double mw, double ma, double U, double V, double tHint) {
        double lo = T_MIN, hi = T_MAX;
        double t0 = Math.max(lo, Math.min(hi, tHint > 0 ? tHint : 300));
        double w = 1;
        double a = Math.max(lo, t0 - w), b = Math.min(hi, t0 + w);
        double fa = energyAt(mw, ma, V, a) - U, fb = energyAt(mw, ma, V, b) - U;
        while (fa > 0 && a > lo) {
            b = a;
            fb = fa;
            w *= 4;
            a = Math.max(lo, t0 - w);
            fa = energyAt(mw, ma, V, a) - U;
        }
        while (fb < 0 && b < hi) {
            a = b;
            fa = fb;
            w *= 4;
            b = Math.min(hi, t0 + w);
            fb = energyAt(mw, ma, V, b) - U;
        }
        double t = 0.5 * (a + b), tol = 1e-13 * Math.max(1, Math.abs(U));
        int side = 0;
        for (int i = 0; i < 200 && b - a > 1e-11; i++) {
            t = fb != fa ? b - fb * (b - a) / (fb - fa) : 0.5 * (a + b);
            if (!(t > a && t < b) || i % 8 == 7) t = 0.5 * (a + b);
            double f = energyAt(mw, ma, V, t) - U;
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
        return split(mw, ma, V, t, gasVolume(mw, ma, V, t, 0));
    }

    /** The energy of the contents in equilibrium at T. */
    public static double energyAt(double mw, double ma, double V, double T) {
        return energy(split(mw, ma, V, T, gasVolume(mw, ma, V, T, 0)));
    }

    // ---- derivatives (implicit function theorem)

    /**
     * The slopes of (T, Vg) against (mw, ma, U) at a solved split: -J^-1 dR/dθ, with J = dR/d(T, Vg). The residuals are
     * explicit and smooth, so their partial derivatives are taken by central differences of those functions (no solve
     * inside). Returns {{dT/dmw, dT/dma, dT/dU}, {dVg/dmw, dVg/dma, dVg/dU}}.
     */
    public static double[][] derivatives(Split s, double mw, double ma, double U, double V) {
        double T = s.T(), vg = s.vg();
        double hT = 1e-5 * T, hV = 1e-6 * vg;
        double[] rTp = residuals(mw, ma, U, V, T + hT, vg), rTm = residuals(mw, ma, U, V, T - hT, vg);
        double[] rVp = residuals(mw, ma, U, V, T, vg + hV), rVm = residuals(mw, ma, U, V, T, vg - hV);
        double j11 = (rTp[0] - rTm[0]) / (2 * hT), j21 = (rTp[1] - rTm[1]) / (2 * hT);
        double j12 = (rVp[0] - rVm[0]) / (2 * hV), j22 = (rVp[1] - rVm[1]) / (2 * hV);
        double det = j11 * j22 - j12 * j21;
        double[] h = {Math.max(1e-12, 1e-6 * mw), Math.max(1e-12, 1e-6 * ma), Math.max(1e-6, 1e-9 * Math.abs(U))};
        double[][] out = new double[2][3];
        for (int k = 0; k < 3; k++) {
            double[] p = residualsAt(k, +h[k], mw, ma, U, V, T, vg), m = residualsAt(k, -h[k], mw, ma, U, V, T, vg);
            double d1 = (p[0] - m[0]) / (2 * h[k]), d2 = (p[1] - m[1]) / (2 * h[k]);
            // [dT, dVg] = -J^-1 [d1, d2]
            out[0][k] = -(j22 * d1 - j12 * d2) / det;
            out[1][k] = -(-j21 * d1 + j11 * d2) / det;
        }
        return out;
    }

    private static double[] residualsAt(int k, double d, double mw, double ma, double U, double V, double T, double vg) {
        return residuals(mw + (k == 0 ? d : 0), ma + (k == 1 ? d : 0), U + (k == 2 ? d : 0), V, T, vg);
    }
}
