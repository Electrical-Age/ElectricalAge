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
    /** Softplus width of the liquid, as a share of the water held; the air cushion (kg/m^3). */
    public static double EPS_L = 1e-2, AIR_CUSHION = 1e-6;
    public static final double T_MIN = 120, T_MAX = 4000;

    // ---- the vapour, from the saturation curve (Baughn 2026-11-09: exact and consistent)
    // Wagner and Pruss's saturation pressure (IAPWS-95's auxiliary equation) fixes the vapour's standard Gibbs energy,
    // g_v(T) = g_l(T, P0) - R_V T ln(p_W(T) / P0), so the Gibbs equality gives back p_W exactly at P0; its entropy and
    // enthalpy follow (s = -dg/dT, h = g + T s), so the latent heat is Clausius-Clapeyron's, h_v - h_l = R_V T^2 dln p_W/dT.
    // Above T_JOIN (near the critical point, where the curve ends) the vapour goes on as an ideal gas with the heat
    // capacity it has there (C1 at the join).
    static final double TC = 647.096, PC = 22.064e6, T_JOIN = 600;
    private static final double[] A = {-7.85951783, 1.84408259, -11.7866497, 22.6807411, -15.9618719, 1.80122502};
    static final double H_JOIN, S_JOIN, CP_JOIN;

    /** ln(p_W / PC) and its derivative in T (T below TC). */
    static double lnPw(double T) {
        double t = 1 - T / TC;
        double sum = A[0] * t + A[1] * Math.pow(t, 1.5) + A[2] * t * t * t + A[3] * Math.pow(t, 3.5) + A[4] * t * t * t * t + A[5] * Math.pow(t, 7.5);
        return TC / T * sum;
    }

    static double dLnPw(double T) {
        double t = 1 - T / TC;
        double sum = A[0] * t + A[1] * Math.pow(t, 1.5) + A[2] * t * t * t + A[3] * Math.pow(t, 3.5) + A[4] * t * t * t * t + A[5] * Math.pow(t, 7.5);
        double ds = A[0] + 1.5 * A[1] * Math.sqrt(t) + 3 * A[2] * t * t + 3.5 * A[3] * Math.pow(t, 2.5) + 4 * A[4] * t * t * t + 7.5 * A[5] * Math.pow(t, 6.5);
        return -TC * sum / (T * T) - ds / T;
    }

    /** Wagner and Pruss's saturation pressure (Pa), below TC. */
    public static double pWagner(double T) {
        return PC * Math.exp(lnPw(T));
    }

    // (the liquid's Gibbs energy at the saturation pressure itself, Poynting term and all, so the equality holds at p_W;
    // taken at P0 the curve came out 0.2 % high at 150 C)
    private static double gePw(double T) {
        return gElastic(pWagner(T));
    }

    private static double dGePw(double T) {
        double p = pWagner(T);
        return vLiquid(p) * p * dLnPw(T);
    }

    private static double hVapourCurve(double T) {
        return CP_L * (T - T0) + P0 * V0 + gePw(T) - T * dGePw(T) + R_V * T * T * dLnPw(T);
    }

    private static double sVapourCurve(double T) {
        return CP_L * Math.log(T / T0) - dGePw(T) + R_V * (Math.log(PC / P0) + lnPw(T)) + R_V * T * dLnPw(T);
    }

    static {
        H_JOIN = hVapourCurve(T_JOIN);
        S_JOIN = sVapourCurve(T_JOIN);
        double h = 1e-3;
        CP_JOIN = (hVapourCurve(T_JOIN) - hVapourCurve(T_JOIN - h)) / h;
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

    public static double hVapour(double T) {
        return T <= T_JOIN ? hVapourCurve(T) : H_JOIN + CP_JOIN * (T - T_JOIN);
    }

    public static double uVapour(double T) {
        return hVapour(T) - R_V * T;
    }

    static double sVapourStd(double T) {
        return T <= T_JOIN ? sVapourCurve(T) : S_JOIN + CP_JOIN * Math.log(T / T_JOIN);
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

    /**
     * Solve from a nearby split (the node's last): Newton on (T, ln Vg) with the residuals' own Jacobian (as in
     * derivatives()), a few iterations; falls back to the bracketed solve if it doesn't converge or leaves the range.
     */
    public static Split solve(double mw, double ma, double U, double V, Split near) {
        if (near == null) return solve(mw, ma, U, V, 300);
        double T = near.T(), x = Math.log(near.vg());
        for (int it = 0; it < 12; it++) {
            double vg = Math.exp(x);
            double[] r = residuals(mw, ma, U, V, T, vg);
            double scaleU = Math.max(1, Math.abs(U)) * 1e-13;
            if (Math.abs(r[0]) < scaleU + 1e-9 && Math.abs(r[1]) < 1e-13) return split(mw, ma, V, T, vg);
            double hT = 1e-6 * T, hx = 1e-7;
            double[] rT = residuals(mw, ma, U, V, T + hT, vg), rX = residuals(mw, ma, U, V, T, Math.exp(x + hx));
            double j11 = (rT[0] - r[0]) / hT, j21 = (rT[1] - r[1]) / hT, j12 = (rX[0] - r[0]) / hx, j22 = (rX[1] - r[1]) / hx;
            double det = j11 * j22 - j12 * j21;
            if (!(Math.abs(det) > 0) || !Double.isFinite(det)) break;
            double dT = -(j22 * r[0] - j12 * r[1]) / det, dx = -(-j21 * r[0] + j11 * r[1]) / det;
            // (damped: at most 30 K and a factor e in Vg per iteration)
            double k = Math.min(1, Math.min(30 / Math.max(1e-30, Math.abs(dT)), 1 / Math.max(1e-30, Math.abs(dx))));
            T += k * dT;
            x += k * dx;
            if (!(T > T_MIN && T < T_MAX) || x > Math.log(V) + 1e-9 || !Double.isFinite(x)) break;
        }
        return solve(mw, ma, U, V, near.T());
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

    // ---- what a flux needs, and its slopes

    /**
     * Indices into outputs(): pressure, liquid mass, vapour mass, air (with the cushion), the liquid's specific enthalpy,
     * the energy a kg drawn from the gas carries, liquid level (share of the volume), the vapour's and the real air's
     * shares of a kg drawn from the gas. The cushion's share of the gas is drawn as nothing: it is not part of the
     * contents (a full node's top port drained "cushion air" it hadn't got and drove its air negative).
     */
    public static final int P = 0, ML = 1, MV = 2, MA = 3, HL = 4, HG = 5, LEVEL = 6, YV = 7, YA = 8, OUTPUTS = 9;

    /** The outputs of a split (explicit in T, Vg and the contents). */
    public static double[] outputs(Split s, double V) {
        double gas = s.mv() + s.ma();
        double air = Math.max(0, s.ma() - AIR_CUSHION * V);
        double hl = uLiquidThermal(s.T()) + eElastic(s.v()) + s.p() * s.v();
        double yv = gas > 0 ? s.mv() / gas : 0, ya = gas > 0 ? air / gas : 0;
        double hg = yv * hVapour(s.T()) + ya * hAir(s.T());
        return new double[]{s.p(), s.ml(), s.mv(), s.ma(), hl, hg, s.vl() / V, yv, ya};
    }

    /**
     * The slopes of outputs() against (mw, ma, U) at a solved split: the explicit partials (central differences of the
     * split at fixed T and Vg) plus the chain through T and Vg (derivatives()). Returns [output][mw, ma, U].
     */
    public static double[][] outputSlopes(Split s, double mw, double ma, double U, double V) {
        double[][] d = derivatives(s, mw, ma, U, V);
        double T = s.T(), vg = s.vg();
        double hT = 1e-5 * T, hV = 1e-6 * vg;
        double[] oTp = outputs(split(mw, ma, V, T + hT, vg), V), oTm = outputs(split(mw, ma, V, T - hT, vg), V);
        double[] oVp = outputs(split(mw, ma, V, T, vg + hV), V), oVm = outputs(split(mw, ma, V, T, vg - hV), V);
        double hw = Math.max(1e-12, 1e-6 * mw), ha = Math.max(1e-12, 1e-6 * ma);
        double[] owp = outputs(split(mw + hw, ma, V, T, vg), V), owm = outputs(split(mw - hw, ma, V, T, vg), V);
        double[] oap = outputs(split(mw, ma + ha, V, T, vg), V), oam = outputs(split(mw, ma - ha, V, T, vg), V);
        double[][] out = new double[OUTPUTS][3];
        for (int o = 0; o < OUTPUTS; o++) {
            double dT = (oTp[o] - oTm[o]) / (2 * hT), dV = (oVp[o] - oVm[o]) / (2 * hV);
            double pw = (owp[o] - owm[o]) / (2 * hw), pa = (oap[o] - oam[o]) / (2 * ha);
            out[o][0] = pw + dT * d[0][0] + dV * d[1][0];
            out[o][1] = pa + dT * d[0][1] + dV * d[1][1];
            out[o][2] = dT * d[0][2] + dV * d[1][2];   // (U enters only through T and Vg)
        }
        return out;
    }
}
