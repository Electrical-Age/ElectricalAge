package mods.eln.sim.fluid;

/**
 * Water and steam properties for the fluid core (notes/pipes/DESIGN.md section 6), in SI units. Accurate enough for a
 * game from 0 C to ~250 C: saturation pressure by Wagner and Pruss (IAPWS-95's auxiliary equation), liquid as
 * incompressible with a constant heat capacity, vapour as an ideal gas (the latent heat follows from the two).
 * Energies are relative to liquid water at 0 C. Air (a second, non-condensing species) is here too.
 */
public final class Water {
    private Water() {
    }

    public static final double T0 = 273.15, TC = 647.096, PC = 22.064e6, TB = 373.15;
    /** Specific gas constants (J/kg/K), heat capacities (J/kg/K). */
    public static final double R_V = 461.52, R_AIR = 287.05;
    public static final double CP_L = 4186, CP_V = 1900, CV_AIR = 718;
    /** Liquid density (kg/m^3; at ~100 C) and its bulk modulus once a node is full of liquid (Pa; ~2 GPa, 1 % at 200 bar). */
    public static final double RHO_L = 958, K_LIQUID = 2e9;
    public static final double L_TB = 2.257e6;
    /** Dynamic viscosities (Pa s, ~100-200 C). */
    public static final double MU_L = 2.5e-4, MU_V = 1.3e-5, MU_AIR = 2.0e-5;
    public static final double P_ATM = 101325, T_AMBIENT = 293.15;

    private static final double[] A = {-7.85951783, 1.84408259, -11.7866497, 22.6807411, -15.9618719, 1.80122502};

    /** Saturation pressure (Pa) at T (K); clamped to the critical point. */
    public static double psat(double T) {
        if (T >= TC) return PC;
        if (T < 200) T = 200;
        double t = 1 - T / TC;
        double s = A[0] * t + A[1] * Math.pow(t, 1.5) + A[2] * t * t * t + A[3] * Math.pow(t, 3.5)
            + A[4] * t * t * t * t + A[5] * Math.pow(t, 7.5);
        return PC * Math.exp(TC / T * s);
    }

    /** Saturation temperature (K) at P (Pa), by bisection on psat. */
    public static double tsat(double P) {
        if (P >= PC) return TC;
        double lo = 200, hi = TC;
        for (int i = 0; i < 60; i++) {
            double m = 0.5 * (lo + hi);
            if (psat(m) < P) lo = m;
            else hi = m;
        }
        return 0.5 * (lo + hi);
    }

    /** Latent heat of vaporization (J/kg) at T (K): the gap between the vapour's and the liquid's enthalpy. */
    public static double latent(double T) {
        return Math.max(0, hVapour(T) - hLiquid(T));
    }

    /** From here the liquid's heat capacity climbs so its enthalpy meets the vapour's at the critical point (the latent
     *  heat goes to 0 there, as it should; with constant heat capacities the two crossed at ~530 C: Gemini). */
    static final double T_STEEP = 573.15;

    /** Specific internal energy of liquid (J/kg); above the critical point there is no liquid: the vapour's. */
    public static double uLiquid(double T) {
        if (T <= T_STEEP) return CP_L * (T - T0);
        if (T >= TC) return uVapour(T);
        double h0 = CP_L * (T_STEEP - T0), h1 = uVapour(TC);
        return h0 + (h1 - h0) * (T - T_STEEP) / (TC - T_STEEP);
    }

    /** Specific enthalpy of liquid (J/kg; the pressure term is negligible). */
    public static double hLiquid(double T) {
        return uLiquid(T);
    }

    /**
     * Specific enthalpy of vapour (J/kg), an ideal gas: 2257 kJ/kg above liquid at 100 C, and cp 1900 J/kg/K. Within
     * a few % of steam tables to ~230 C (the latent heat it implies is ~25% high at 300 C).
     */
    public static double hVapour(double T) {
        return hLiquid(TB) + L_TB + CP_V * (T - TB);
    }

    /** Specific internal energy of vapour (J/kg): h - p v = h - R T. */
    public static double uVapour(double T) {
        return hVapour(T) - R_V * T;
    }

    public static double uAir(double T) {
        return CV_AIR * (T - T0);
    }

    public static double hAir(double T) {
        return uAir(T) + R_AIR * T;
    }

    /** Specific entropy of liquid (J/kg/K), from 0 C: constant heat capacity (as uLiquid below T_STEEP). */
    public static double sLiquid(double T) {
        return CP_L * Math.log(T / T0);
    }

    /** Entropy of vapour at 100 C and 1 atm: the liquid's plus the latent heat over TB. */
    static final double S_V_REF = CP_L * Math.log(TB / T0) + L_TB / TB;

    /** Specific entropy of vapour (J/kg/K) at T and its partial pressure p: an ideal gas, as hVapour. */
    public static double sVapour(double T, double p) {
        return S_V_REF + CP_V * Math.log(T / TB) - R_V * Math.log(p / P_ATM);
    }

    /**
     * The enthalpy drop (J/kg) of vapour at T (K) and partial pressure p expanding without loss (at constant entropy) to
     * the pressure pOut: the work an ideal turbine gets from it (Baughn 2026-10-26: steam turbines by the expansion).
     * Wet below the saturation line (by its quality), superheated above. 0 if pOut is not below p. Consistent with this
     * class's own model: within ~5% of steam tables to 10 bar, ~7% at 25 (10 bar saturated to 1 atm: 409 kJ/kg, tables 389).
     */
    public static double isentropicDrop(double T, double p, double pOut) {
        if (!(pOut < p) || p <= 0 || pOut <= 0) return 0;
        double s = sVapour(T, p), tOut = tsat(pOut);
        double sl = sLiquid(tOut), sv = sVapour(tOut, pOut);
        double hOut;
        if (s >= sv) hOut = hVapour(TB * Math.exp((s - S_V_REF + R_V * Math.log(pOut / P_ATM)) / CP_V));
        else {
            double x = Math.max(0, (s - sl) / (sv - sl));
            hOut = hLiquid(tOut) + x * (hVapour(tOut) - hLiquid(tOut));
        }
        return Math.max(0, hVapour(T) - hOut);
    }
}
