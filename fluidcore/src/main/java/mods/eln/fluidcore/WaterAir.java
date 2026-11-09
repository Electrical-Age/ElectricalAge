package mods.eln.fluidcore;

/**
 * Thermodynamics of a closed volume holding water (liquid and vapour) and air: a consistent, smooth model for the fluid
 * core (board #61, 08-core-plan.md M1).
 * <ul>
 *   <li>Liquid: heat capacity CP_L, specific volume v0 exp(-(p - p0)/K) (bulk modulus K, so a full volume compresses its
 *       liquid instead of failing), with the elastic energy that goes with it.</li>
 *   <li>Air: an ideal gas with a constant heat capacity. Vapour: a gas with a second virial term, v = R T / p + B(T)
 *       (Baughn 2026-11-09: people set thermostats by vapour curves; the ideal gas overstated the latent heat by 11 % at
 *       200 C). The two share the gas space, each at its partial pressure.</li>
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

    // The vapour's second virial coefficient: Harvey and Lemmon's fit for water, B = sum a_i (T / 100 K)^b_i in
    // dm^3/mol, here in m^3/kg (-0.0251 at 100 C). The vapour is v = R T / p + B, so g_v(T, p) = g0(T) + R T ln(p / P0)
    // + B p, s_v = s0 - R ln(p / P0) - B' p, h_v = h0 + (B - T B') p and u_v = h0 - R T - T B' p; g0 is fixed by the
    // Gibbs equality at p_W (below), and h0, s0 are its enthalpy and entropy.
    private static final double[] BA = {0.34404, -0.75826, -24.219, -3978.2}, BE = {-0.5, -0.8, -3.35, -8.3};
    private static final double B_UNIT = 1e-3 / 0.018015268;

    // (above T_JOIN, B goes on as B_J (T_JOIN / T)^n, C1 at the join, so it stays negative: the fit turns positive
    // near 1,500 K, and a positive B bounds the vapour a volume can hold, which the smoothed split can overrun)
    private static double bFit(double T) {
        double t = T / 100, b = 0;
        for (int i = 0; i < 4; i++) b += BA[i] * Math.pow(t, BE[i]);
        return b * B_UNIT;
    }

    private static double dBFit(double T) {
        double t = T / 100, b = 0;
        for (int i = 0; i < 4; i++) b += BA[i] * BE[i] * Math.pow(t, BE[i] - 1);
        return b * B_UNIT / 100;
    }

    private static final double B_J = bFit(600), N_J = -600 * dBFit(600) / bFit(600);

    /** The vapour's second virial coefficient (m^3/kg). */
    static double bVirial(double T) {
        return T <= T_JOIN ? bFit(T) : B_J * Math.pow(T_JOIN / T, N_J);
    }

    /** dB/dT (m^3/kg/K). */
    static double dBVirial(double T) {
        return T <= T_JOIN ? dBFit(T) : -N_J * bVirial(T) / T;
    }

    // h0 and s0 from g0(T) = g_l(T, p_W) - R T ln(p_W / P0) - B p_W (so the vapour at p_W has the liquid's Gibbs energy)
    private static double hVapourCurve(double T) {
        return curve(T)[0];
    }

    private static double sVapourCurve(double T) {
        return curve(T)[1];
    }

    private static final double LN_PC_P0 = Math.log(PC / P0);

    /**
     * {h0, s0} below T_JOIN, with every power of t = 1 - T / TC from one square root and B's terms from one logarithm
     * (the separate functions above cost about thirty pow and exp calls a temperature; the step spent most of its time
     * here). Same values as lnPw, dLnPw, pWagner, gePw, dGePw, bFit and dBFit.
     */
    private static double[] curve(double T) {
        double t = 1 - T / TC, rt = Math.sqrt(t), t2 = t * t, t3 = t2 * t, t4 = t3 * t, t6 = t3 * t3;
        double sum = A[0] * t + A[1] * t * rt + A[2] * t3 + A[3] * t3 * rt + A[4] * t4 + A[5] * t6 * t * rt;
        double ds = A[0] + 1.5 * A[1] * rt + 3 * A[2] * t2 + 3.5 * A[3] * t2 * rt + 4 * A[4] * t3 + 7.5 * A[5] * t6 * rt;
        double dds = 0.75 * A[1] / rt + 6 * A[2] * t + 8.75 * A[3] * t * rt + 12 * A[4] * t2 + 48.75 * A[5] * t4 * t * rt;
        double lnpw = TC / T * sum, dl = -TC * sum / (T * T) - ds / T;
        double d2l = 2 * TC * sum / (T * T * T) + 2 * ds / (T * T) + dds / (TC * T);
        double pw = PC * Math.exp(lnpw);
        double ex = Math.exp(-(pw - P0) / K);
        double ge = V0 * K * (1 - ex), dge = V0 * ex * pw * dl;
        double lt = Math.log(T / 100), bsum = 0, dbsum = 0, d2bsum = 0;
        for (int i = 0; i < 4; i++) {
            double term = BA[i] * Math.exp(BE[i] * lt);
            bsum += term;
            dbsum += BE[i] * term;
            d2bsum += BE[i] * (BE[i] - 1) * term;
        }
        double b = bsum * B_UNIT, db = dbsum * B_UNIT / T, d2b = d2bsum * B_UNIT / (T * T);
        double h0 = CP_L * (T - T0) + P0 * V0 + ge - T * dge + R_V * T * T * dl - b * pw + T * db * pw + T * b * pw * dl;
        double s0 = CP_L * Math.log(T / T0) - dge + R_V * (LN_PC_P0 + lnpw) + R_V * T * dl + db * pw + b * pw * dl;
        // dh0/dT (= T ds0/dT): the same terms differentiated once more
        double pwp = pw * dl, exp_ = -ex * pwp / K;
        double dgeP = V0 * (exp_ * pw * dl + ex * pwp * dl + ex * pw * d2l);
        double cp0 = CP_L - T * dgeP + R_V * (2 * T * dl + T * T * d2l) + T * pw * (d2b + 2 * db * dl + b * dl * dl + b * d2l);
        return new double[]{h0, s0, b, db, cp0, d2b};
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

    /** What the vapour needs at one temperature: h0, s0, g0 = h0 - T s0, B and B'. */
    record Vap(double T, double h0, double s0, double g0, double b, double db, double cp0, double d2b) {
    }

    // (the curve costs a dozen pow and exp calls, and a split asks for it at one T many times over: a few entries per
    // thread, round robin, as the slopes alternate between T and T +- h)
    private static final class VapCache {
        final Vap[] e = new Vap[4];
        int next;
    }

    private static final ThreadLocal<VapCache> VAP_CACHE = ThreadLocal.withInitial(VapCache::new);

    static Vap vap(double T) {
        VapCache c = VAP_CACHE.get();
        for (Vap v : c.e) if (v != null && v.T() == T) return v;
        Vap v;
        if (T <= T_JOIN) {
            double[] c4 = curve(T);
            v = new Vap(T, c4[0], c4[1], c4[0] - T * c4[1], c4[2], c4[3], c4[4], c4[5]);
        } else {
            double h0 = H_JOIN + CP_JOIN * (T - T_JOIN), s0 = S_JOIN + CP_JOIN * Math.log(T / T_JOIN);
            double bb = bVirial(T);
            v = new Vap(T, h0, s0, h0 - T * s0, bb, dBVirial(T), CP_JOIN, N_J * (N_J + 1) * bb / (T * T));
        }
        c.e[c.next] = v;
        c.next = (c.next + 1) % c.e.length;
        return v;
    }

    /** h0: the vapour's enthalpy less its virial part. */
    static double hVapour0(double T) {
        return vap(T).h0();
    }

    /** The vapour's specific enthalpy at T and its partial pressure p. */
    public static double hVapour(double T, double p) {
        Vap v = vap(T);
        return v.h0() + (v.b() - T * v.db()) * p;
    }

    /** The vapour's specific internal energy at T and its partial pressure p. */
    public static double uVapour(double T, double p) {
        Vap v = vap(T);
        return v.h0() - R_V * T - T * v.db() * p;
    }

    static double sVapourStd(double T) {
        return vap(T).s0();
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
        Vap vp = vap(T);
        double c = gl - vp.g0(), rt = R_V * T, bp = vp.b() * P0;
        // R T x + B P0 e^x = c in x = ln(ps / P0), B < 0: concave, rising to its top at e^x = R T / (-B P0), where the
        // vapour's volume R T / p + B reaches zero. With no root there (hot: the vapour can't match the liquid's Gibbs
        // energy at any density) every drop evaporates: the top is returned, where split() finds room for any vapour.
        // Otherwise Newton from left of the root (where f < 0) climbs to it without overshooting.
        double xTop = Math.log(rt / -bp);
        if (rt * xTop - rt - c <= 0) return P0 * Math.exp(xTop);
        double x = Math.min(c / rt, xTop - 1);
        while (rt * x + bp * Math.exp(x) - c >= 0) x -= 1;
        for (int i = 0; i < 60; i++) {
            double e = Math.exp(x), f = rt * x + bp * e - c, d = rt + bp * e;
            if (!(d > 0)) break;
            double dx = -f / d;
            x += dx;
            if (Math.abs(dx) < 1e-15 * Math.max(1, Math.abs(x))) break;
        }
        return P0 * Math.exp(x);
    }

    /** softplus of z over width w (w > 0): smooth max(0, z), within w ln 2 of it. */
    static double softplus(double z, double w) {
        double x = z / w;
        return x > 30 ? z : x < -30 ? w * Math.exp(x) : w * Math.log1p(Math.exp(x));
    }

    // ---- the state at given T and gas volume (explicit: what the residuals and the outputs are made of)

    /** The split at T and gas volume Vg for water mw and air ma: liquid mass, vapour mass, pressure, liquid volume. */
    public record Split(double T, double vg, double ml, double mv, double ma, double p, double v, double pv) {
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
        double pa = mac * R_A * T / vg, b = vap(T).b();
        double p = pa, pv = 0;
        double ml = 0, mv = mw;
        for (int i = 0; i < 3; i++) {
            double ps = pSat(T, p);
            double den = R_V * T / ps + b;
            double mvs = den > 0 ? vg / den : Double.POSITIVE_INFINITY;
            ml = softplus(mw - mvs, EPS_L * Math.max(mw, 1e-12));
            mv = mw - ml;
            pv = mv * R_V * T / (vg - mv * b);
            p = pv + pa;
        }
        return new Split(T, vg, ml, mv, mac, p, vLiquid(p), pv);
    }

    /** Internal energy of a split. */
    public static double energy(Split s) {
        return s.ml() * (uLiquidThermal(s.T()) + eElastic(s.v())) + s.mv() * uVapour(s.T(), s.pv()) + s.ma() * uAir(s.T());
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
    static double[][] derivativesFD(Split s, double mw, double ma, double U, double V) {
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
            // (a mass at or near zero: a forward difference, as a central one would ask about negative contents; at
            // exactly no water the Jacobian came out pointing the wrong way, and a pump's first step into an empty riser
            // held the riser's water at zero against its own inflow)
            boolean forward = (k == 0 && mw < h[k]) || (k == 1 && ma < h[k]);
            double[] p = residualsAt(k, +h[k], mw, ma, U, V, T, vg), m = forward ? residualsAt(k, 0, mw, ma, U, V, T, vg) : residualsAt(k, -h[k], mw, ma, U, V, T, vg);
            double den = forward ? h[k] : 2 * h[k];
            double d1 = (p[0] - m[0]) / den, d2 = (p[1] - m[1]) / den;
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
        double hg = yv * hVapour(s.T(), s.pv()) + ya * hAir(s.T());
        return new double[]{s.p(), s.ml(), s.mv(), s.ma(), hl, hg, s.vl() / V, yv, ya};
    }

    /**
     * The slopes of outputs() against (mw, ma, U) at a solved split: the explicit partials (central differences of the
     * split at fixed T and Vg) plus the chain through T and Vg (derivatives()). Returns [output][mw, ma, U].
     */
    static double[][] outputSlopesFD(Split s, double mw, double ma, double U, double V) {
        double[][] d = derivativesFD(s, mw, ma, U, V);
        double T = s.T(), vg = s.vg();
        double hT = 1e-5 * T, hV = 1e-6 * vg;
        double[] oTp = outputs(split(mw, ma, V, T + hT, vg), V), oTm = outputs(split(mw, ma, V, T - hT, vg), V);
        double[] oVp = outputs(split(mw, ma, V, T, vg + hV), V), oVm = outputs(split(mw, ma, V, T, vg - hV), V);
        double hw = Math.max(1e-12, 1e-6 * mw), ha = Math.max(1e-12, 1e-6 * ma);
        boolean fw = mw < hw, fa = ma < ha;   // (forward differences at or near zero, as in derivatives())
        double[] owp = outputs(split(mw + hw, ma, V, T, vg), V), owm = outputs(split(fw ? mw : mw - hw, ma, V, T, vg), V);
        double[] oap = outputs(split(mw, ma + ha, V, T, vg), V), oam = outputs(split(mw, fa ? ma : ma - ha, V, T, vg), V);
        double[][] out = new double[OUTPUTS][3];
        for (int o = 0; o < OUTPUTS; o++) {
            double dT = (oTp[o] - oTm[o]) / (2 * hT), dV = (oVp[o] - oVm[o]) / (2 * hV);
            double pw = (owp[o] - owm[o]) / (fw ? hw : 2 * hw), pa = (oap[o] - oam[o]) / (fa ? ha : 2 * ha);
            out[o][0] = pw + dT * d[0][0] + dV * d[1][0];
            out[o][1] = pa + dT * d[0][1] + dV * d[1][1];
            out[o][2] = dT * d[0][2] + dV * d[1][2];   // (U enters only through T and Vg)
        }
        return out;
    }

    // ---- analytic slopes (forward mode through split(): the same three fixed-point passes, differentiated)

    /** Gradient slots: temperature, gas volume, water, air. */
    private static final int GT = 0, GV = 1, GW = 2, GA = 3;

    /** A split with the gradients of what it's made of against (T, Vg, mw, ma). */
    static final class SplitD {
        Split s;
        Vap vap;
        double[] dml = new double[4], dmv = new double[4], dp = new double[4], dpv = new double[4], dv = new double[4];
    }

    /** ln ps and its slopes {value, d/dT, d/dp} (pSat's root, differentiated by the implicit function theorem). */
    static double[] lnPSatD(double T, double p) {
        Vap vp = vap(T);
        double ps = pSat(T, p), rt = R_V * T, bps = vp.b() * ps;
        double x = Math.log(ps / P0);
        if (rt + bps <= 1e-9 * rt) return new double[]{Math.log(ps), 1 / T - vp.db() / vp.b(), 0};   // (no root: the top)
        double den = rt + bps;
        double dcdT = vp.s0() - sLiquid(T);
        return new double[]{Math.log(ps), (dcdT - R_V * x - vp.db() * ps) / den, vLiquid(p) / den};
    }

    static SplitD splitD(double mw, double ma, double V, double T, double vg) {
        SplitD o = new SplitD();
        Vap vp = vap(T);
        o.vap = vp;
        double b = vp.b(), db = vp.db();
        double mac = ma + AIR_CUSHION * V;
        double pa = mac * R_A * T / vg;
        double[] dpa = {pa / T, -pa / vg, 0, R_A * T / vg};
        double p = pa, pv = 0, ml = 0, mv = mw;
        double[] dp = dpa.clone(), dpv = new double[4], dml = new double[4], dmv = {0, 0, 1, 0};
        double w = EPS_L * Math.max(mw, 1e-12);
        double dwdmw = mw > 1e-12 ? EPS_L : 0;
        for (int i = 0; i < 3; i++) {
            double[] lp = lnPSatD(T, p);
            double ps = Math.exp(lp[0]);
            double[] dln = new double[4];
            for (int k = 0; k < 4; k++) dln[k] = lp[2] * dp[k];
            dln[GT] += lp[1];
            double den = R_V * T / ps + b;
            if (den > 0) {
                double mvs = vg / den;
                double[] dz = new double[4];
                for (int k = 0; k < 4; k++) {
                    double dden = -(R_V * T / ps) * dln[k] + (k == GT ? R_V / ps + db : 0);
                    double dmvs = (k == GV ? 1 / den : 0) - mvs / den * dden;
                    dz[k] = (k == GW ? 1 : 0) - dmvs;
                }
                double z = mw - mvs, x = z / w;
                double sp, sig;
                if (x > 30) {
                    sp = x;
                    sig = 1;
                } else if (x < -30) {
                    sp = Math.exp(x);
                    sig = sp;
                } else {
                    sp = Math.log1p(Math.exp(x));
                    sig = 1 / (1 + Math.exp(-x));
                }
                ml = w * sp;
                double dmlw = sp - x * sig;   // (d ml / d w)
                for (int k = 0; k < 4; k++) dml[k] = sig * dz[k] + (k == GW ? dmlw * dwdmw : 0);
            } else {
                ml = 0;
                java.util.Arrays.fill(dml, 0);
            }
            mv = mw - ml;
            for (int k = 0; k < 4; k++) dmv[k] = (k == GW ? 1 : 0) - dml[k];
            double q = vg - mv * b, num = mv * R_V * T;
            pv = num / q;
            for (int k = 0; k < 4; k++) {
                double dnum = R_V * (T * dmv[k] + (k == GT ? mv : 0));
                double dq = (k == GV ? 1 : 0) - b * dmv[k] - (k == GT ? mv * db : 0);
                dpv[k] = dnum / q - pv * dq / q;
                dp[k] = dpv[k] + dpa[k];
            }
            p = pv + pa;
        }
        double v = vLiquid(p);
        for (int k = 0; k < 4; k++) o.dv[k] = -v / K * dp[k];
        o.s = new Split(T, vg, ml, mv, mac, p, v, pv);
        o.dml = dml;
        o.dmv = dmv;
        o.dp = dp;
        o.dpv = dpv;
        return o;
    }

    /** The residuals' gradients against (T, Vg, mw, ma): {d energy, d volume residual}. */
    private static double[][] residualGrads(SplitD d, double V) {
        Split s = d.s;
        Vap vp = d.vap;
        double T = s.T(), x = s.v() / V0;
        double uL = uLiquidThermal(T) + eElastic(s.v()), uV = uVapour(T, s.pv());
        double duVdT = vp.cp0() - R_V - (vp.db() + T * vp.d2b()) * s.pv(), duVdpv = -T * vp.db();
        double[] dE = new double[4], dR = new double[4];
        for (int k = 0; k < 4; k++) {
            double dT = k == GT ? 1 : 0;
            dE[k] = d.dml[k] * uL + s.ml() * (CP_L * dT + K * V0 * Math.log(x) / V0 * d.dv[k])
                + d.dmv[k] * uV + s.mv() * (duVdT * dT + duVdpv * d.dpv[k])
                + (k == GA ? uAir(T) : 0) + s.ma() * CV_A * dT;
            dR[k] = ((k == GV ? 1 : 0) + d.dml[k] * s.v() + s.ml() * d.dv[k]) / V;
        }
        return new double[][]{dE, dR};
    }

    /**
     * The slopes of (T, Vg) against (mw, ma, U) at a solved split, by the implicit function theorem on the residuals'
     * analytic gradients. Returns {{dT/dmw, dT/dma, dT/dU}, {dVg/dmw, dVg/dma, dVg/dU}}.
     */
    public static double[][] derivatives(Split s, double mw, double ma, double U, double V) {
        return derivatives(splitD(mw, ma, V, s.T(), s.vg()), V);
    }

    private static double[][] derivatives(SplitD sd, double V) {
        double[][] g = residualGrads(sd, V);
        double j11 = g[0][GT], j12 = g[0][GV], j21 = g[1][GT], j22 = g[1][GV];
        double det = j11 * j22 - j12 * j21;
        double[][] out = new double[2][3];
        for (int k = 0; k < 3; k++) {
            double d1 = k == 0 ? g[0][GW] : k == 1 ? g[0][GA] : -1;
            double d2 = k == 0 ? g[1][GW] : k == 1 ? g[1][GA] : 0;
            out[0][k] = -(j22 * d1 - j12 * d2) / det;
            out[1][k] = -(-j21 * d1 + j11 * d2) / det;
        }
        return out;
    }

    /**
     * The slopes of outputs() against (mw, ma, U) at a solved split: the outputs' analytic gradients in (T, Vg, mw, ma),
     * chained through T and Vg (derivatives()). Returns [output][mw, ma, U].
     */
    public static double[][] outputSlopes(Split s0, double mw, double ma, double U, double V) {
        SplitD sd = splitD(mw, ma, V, s0.T(), s0.vg());
        double[][] th = derivatives(sd, V);
        Split s = sd.s;
        Vap vp = sd.vap;
        double T = s.T();
        double gas = s.mv() + s.ma(), air = Math.max(0, s.ma() - AIR_CUSHION * V);
        double yv = gas > 0 ? s.mv() / gas : 0, ya = gas > 0 ? air / gas : 0;
        double hV = hVapour(T, s.pv()), hA = hAir(T);
        double dhVdT = vp.cp0() - T * vp.d2b() * s.pv(), dhVdpv = vp.b() - T * vp.db();
        double lx = Math.log(s.v() / V0);
        double[][] g = new double[OUTPUTS][4];
        for (int k = 0; k < 4; k++) {
            double dT = k == GT ? 1 : 0;
            double dgas = sd.dmv[k] + (k == GA ? 1 : 0), dair = k == GA && s.ma() - AIR_CUSHION * V >= 0 ? 1 : 0;   // (at no air: the right-hand slope, as a forward difference gave)
            double dyv = gas > 0 ? (sd.dmv[k] - yv * dgas) / gas : 0, dya = gas > 0 ? (dair - ya * dgas) / gas : 0;
            g[P][k] = sd.dp[k];
            g[ML][k] = sd.dml[k];
            g[MV][k] = sd.dmv[k];
            g[MA][k] = k == GA ? 1 : 0;
            g[HL][k] = CP_L * dT + K * lx * sd.dv[k] + s.v() * sd.dp[k] + s.p() * sd.dv[k];
            g[HG][k] = dyv * hV + yv * (dhVdT * dT + dhVdpv * sd.dpv[k]) + dya * hA + ya * (CV_A + R_A) * dT;
            g[LEVEL][k] = (sd.dml[k] * s.v() + s.ml() * sd.dv[k]) / V;
            g[YV][k] = dyv;
            g[YA][k] = dya;
        }
        double[][] out = new double[OUTPUTS][3];
        for (int o = 0; o < OUTPUTS; o++) {
            out[o][0] = g[o][GW] + g[o][GT] * th[0][0] + g[o][GV] * th[1][0];
            out[o][1] = g[o][GA] + g[o][GT] * th[0][1] + g[o][GV] * th[1][1];
            out[o][2] = g[o][GT] * th[0][2] + g[o][GV] * th[1][2];
        }
        return out;
    }
}
