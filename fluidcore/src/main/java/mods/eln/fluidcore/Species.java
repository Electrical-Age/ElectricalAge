package mods.eln.fluidcore;

/**
 * One species the fluid core can carry (10-species-plan.md). Every phase has an exact Gibbs energy, so its enthalpy,
 * entropy and energy are consistent:
 * <ul>
 *   <li>WATER: WaterAir's model (Wagner-Pruss curve, virial vapour).</li>
 *   <li>CURVE: liquid with a constant heat capacity (and water's compressibility law), vapour an ideal gas with cpV,
 *       its standard Gibbs energy fixed so it boils at tb under P0 with latent heat lb; the saturation curve follows
 *       (Kirchhoff). No supercritical end (Baughn 2026-11-09: no plans above 30-40 bar).</li>
 *   <li>NONE: a liquid that never boils.</li>
 *   <li>GAS: an ideal gas, never liquid; cv is its heat capacity at constant volume.</li>
 * </ul>
 * Energies are relative to liquid water at T0, as WaterAir's. Liquids of one group mix (Raoult); groups layer.
 */
public final class Species {
    public enum Kind { WATER, CURVE, NONE, GAS }

    public static final double R_UNIVERSAL = 8.314462618;

    public final String key, group;
    public final Kind kind;
    /** Liquid density at P0 (kg/m^3), liquid heat capacity, vapour cp (CURVE) or cv (GAS) (J/kg/K), molar mass (kg/mol). */
    public final double rho, cpL, cpV, molarMass;
    /** CURVE: normal boiling point (K) and latent heat there (J/kg). */
    public final double tb, lb;
    /** Specific gas constant (J/kg/K), liquid volume at P0 (m^3/kg). */
    public final double r, v0;
    /** CURVE: the vapour's enthalpy and standard entropy (at P0) at tb. */
    final double hvb, svb;

    private Species(String key, String group, Kind kind, double rho, double cpL, double cpV, double molarMass, double r, double tb, double lb) {
        this.key = key;
        this.group = group;
        this.kind = kind;
        this.rho = rho;
        this.cpL = cpL;
        this.cpV = cpV;
        this.molarMass = molarMass;
        this.r = r;
        this.tb = tb;
        this.lb = lb;
        this.v0 = kind == Kind.WATER ? WaterAir.V0 : rho > 0 ? 1 / rho : 0;
        if (kind == Kind.CURVE) {
            hvb = cpL * (tb - WaterAir.T0) + WaterAir.P0 * v0 + lb;
            svb = cpL * Math.log(tb / WaterAir.T0) + lb / tb;
        } else {
            hvb = svb = 0;
        }
    }

    public static final Species WATER = new Species("eln:water", "water", Kind.WATER, 1 / WaterAir.V0, WaterAir.CP_L, WaterAir.CP_V,
        R_UNIVERSAL / WaterAir.R_V, WaterAir.R_V, WaterAir.TB, WaterAir.L_TB);
    public static final Species AIR = new Species("eln:air", "air", Kind.GAS, 0, 0, WaterAir.CV_A, R_UNIVERSAL / WaterAir.R_A, WaterAir.R_A, 0, 0);

    public static Species curve(String key, String group, double rho, double cpL, double cpV, double molarMass, double tb, double lb) {
        return new Species(key, group, Kind.CURVE, rho, cpL, cpV, molarMass, R_UNIVERSAL / molarMass, tb, lb);
    }

    public static Species none(String key, String group, double rho, double cpL, double molarMass) {
        return new Species(key, group, Kind.NONE, rho, cpL, 0, molarMass, R_UNIVERSAL / molarMass, 0, 0);
    }

    public static Species gas(String key, double cv, double molarMass) {
        return new Species(key, key, Kind.GAS, 0, 0, cv, molarMass, R_UNIVERSAL / molarMass, 0, 0);
    }

    public boolean liquidCapable() {
        return kind != Kind.GAS;
    }

    public boolean volatileSpecies() {
        return kind == Kind.WATER || kind == Kind.CURVE;
    }

    /** CURVE: ln(ps / P0) over a liquid at pressure p, by the Gibbs equality (values only; tests). */
    public double lnPsCurve(double T, double p) {
        double gl = cpL * (T - WaterAir.T0) + WaterAir.P0 * v0 - T * cpL * Math.log(T / WaterAir.T0) + v0 * WaterAir.K * (1 - Math.exp(-(p - WaterAir.P0) / WaterAir.K));
        double hv = hvb + cpV * (T - tb), s0 = svb + cpV * Math.log(T / tb);
        return (gl - (hv - T * s0)) / (r * T);
    }

    @Override
    public String toString() {
        return key;
    }
}
