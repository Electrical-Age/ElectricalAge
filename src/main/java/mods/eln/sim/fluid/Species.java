package mods.eln.sim.fluid;

/**
 * One chemical species the fluid core can carry (Baughn 2026-11-03: fluids are data, so more can come later): its
 * liquid and vapour properties, as the core needs them. Water and air are built in (SpeciesRegistry ids 0 and 1, on
 * Water's own numbers); the others come from datapack JSON (SpeciesLoader). Energies are relative to liquid at 0 C, as
 * Water's.
 * <ul>
 *   <li>WATER: Water's model (Wagner-Pruss saturation, the critical ramp).</li>
 *   <li>CURVE: a condensable liquid by Clausius-Clapeyron from its normal boiling point and latent heat, with the same
 *       kind of critical ramp as water, so its vapour's energy stays above its liquid's and a node's energy keeps rising
 *       with temperature (Argon's review: else the temperature bisection finds the wrong root).</li>
 *   <li>NONE: a liquid that never boils in the game's range (residue).</li>
 *   <li>GAS: a non-condensing gas (air, natural gas).</li>
 * </ul>
 */
public final class Species {
    public enum Kind { WATER, CURVE, NONE, GAS }

    /** The universal gas constant (J/mol/K). */
    public static final double R_UNIVERSAL = 8.314462618;

    /** Dense id (SpeciesRegistry: water 0, air 1), and the key saves use ("eln:water"). */
    public final int id;
    public final String key;
    public final Kind kind;
    /** Liquids of one group mix into one layer (and boil by Raoult within it); groups layer by density. */
    public final String group;
    /** Liquid density (kg/m^3; constant: no thermal expansion, Argon), liquid and vapour heat capacity (J/kg/K; GAS: cv). */
    public final double rhoL, cpL, cpV;
    /** Molar mass (kg/mol) and specific gas constant (J/kg/K). */
    public final double molarMass, r;
    /** CURVE: normal boiling point (K), latent heat there (J/kg), critical temperature (K). */
    public final double tb, latentTb, tc;
    /** Liquid viscosity (Pa s) at t1 and t2 (K): an Andrade fit between them, constant if they agree; vapour (Pa s). */
    public final double mu1, t1, mu2, t2, muV;
    /** Colour (0xRRGGBB) for sight glasses, jets and sensors. */
    public final int colour;
    /**
     * Flammability (K; 0: it doesn't burn): over its flash point a leak of it catches from a flame near it, over its
     * autoignition temperature by itself (Baughn 2026-11-03: fire from hot flammable leaks). Set by the loader.
     */
    public double flashPoint, autoignition;

    private final double andradeA, andradeB, tSteep;

    public Species(int id, String key, Kind kind, String group, double rhoL, double cpL, double cpV, double molarMass,
                   double tb, double latentTb, double tc, double mu1, double t1, double mu2, double t2, double muV, int colour) {
        this(id, key, kind, group, rhoL, cpL, cpV, molarMass, R_UNIVERSAL / molarMass, tb, latentTb, tc, mu1, t1, mu2, t2, muV, colour);
    }

    /** With the gas constant given (the built-ins: Water's R_V and R_AIR exactly, so water and air give today's numbers). */
    Species(int id, String key, Kind kind, String group, double rhoL, double cpL, double cpV, double molarMass, double r,
            double tb, double latentTb, double tc, double mu1, double t1, double mu2, double t2, double muV, int colour) {
        this.id = id;
        this.key = key;
        this.kind = kind;
        this.group = group;
        this.rhoL = rhoL;
        this.cpL = cpL;
        this.cpV = cpV;
        this.molarMass = molarMass;
        this.r = r;
        this.tb = tb;
        this.latentTb = latentTb;
        this.tc = tc;
        this.mu1 = mu1;
        this.t1 = t1;
        this.mu2 = mu2;
        this.t2 = t2;
        this.muV = muV;
        this.colour = colour;
        if (mu1 > 0 && mu2 > 0 && t1 > 0 && t2 > 0 && t1 != t2 && mu1 != mu2) {
            andradeB = Math.log(mu1 / mu2) / (1 / t1 - 1 / t2);
            andradeA = Math.log(mu1) - andradeB / t1;
        } else {
            andradeB = 0;
            andradeA = Math.log(Math.max(mu1, 1e-6));
        }
        // (where the liquid's heat capacity starts to climb towards the critical point: water's proportion)
        tSteep = tb + (Water.T_STEEP - Water.TB) / (Water.TC - Water.TB) * (tc - tb);
    }

    /** A copy of this species' data under another id (the registry keeps ids stable across reloads). */
    Species withId(int newId) {
        Species s = new Species(newId, key, kind, group, rhoL, cpL, cpV, molarMass, r, tb, latentTb, tc, mu1, t1, mu2, t2, muV, colour);
        s.flashPoint = flashPoint;
        s.autoignition = autoignition;
        return s;
    }

    /** Does it burn. */
    public boolean flammable() {
        return autoignition > 0;
    }

    public boolean condensable() {
        return kind == Kind.WATER || kind == Kind.CURVE;
    }

    /** Is it a gas whatever the temperature (it has no liquid). */
    public boolean gas() {
        return kind == Kind.GAS;
    }

    /** Saturation pressure (Pa) at T (K): 0 for NONE and GAS; clamped at the critical point. */
    public double psat(double T) {
        switch (kind) {
            case WATER: return Water.psat(T);
            case CURVE: {
                double t = Math.max(Math.min(T, tc), 0.3 * tb);
                return Water.P_ATM * Math.exp(latentTb / r * (1 / tb - 1 / t));
            }
            default: return 0;
        }
    }

    /** Above it there is no liquid (one gas phase). NONE: never; GAS: always. */
    public double critical() {
        switch (kind) {
            case WATER: return Water.TC;
            case CURVE: return tc;
            case NONE: return Double.MAX_VALUE;
            default: return 0;
        }
    }

    /** Specific internal energy of the liquid (J/kg); above the critical point the vapour's. */
    public double uLiquid(double T) {
        switch (kind) {
            case WATER: return Water.uLiquid(T);
            case CURVE: {
                if (T <= tSteep) return cpL * (T - Water.T0);
                if (T >= tc) return uVapour(T);
                double h0 = cpL * (tSteep - Water.T0), h1 = uVapour(tc);
                return h0 + (h1 - h0) * (T - tSteep) / (tc - tSteep);
            }
            case NONE: return cpL * (T - Water.T0);
            default: return uVapour(T);
        }
    }

    /** Specific enthalpy of the liquid (J/kg; the pressure term is negligible). */
    public double hLiquid(double T) {
        return uLiquid(T);
    }

    /** Specific enthalpy of the vapour or gas (J/kg), an ideal gas. */
    public double hVapour(double T) {
        switch (kind) {
            case WATER: return Water.hVapour(T);
            case CURVE: return cpL * (tb - Water.T0) + latentTb + cpV * (T - tb);
            case GAS: return uVapour(T) + r * T;
            default: return hLiquid(T);
        }
    }

    /** Specific internal energy of the vapour or gas (J/kg): h - R T; GAS: cv (T - 0 C). */
    public double uVapour(double T) {
        switch (kind) {
            case WATER: return Water.uVapour(T);
            case GAS: return cpV * (T - Water.T0);
            case NONE: return uLiquid(T);
            default: return hVapour(T) - r * T;
        }
    }

    /** Liquid viscosity (Pa s) at T: water's constant (a golden constraint: conductances unchanged), else Andrade. */
    public double muLiquid(double T) {
        if (kind == Kind.WATER) return Water.MU_L;
        if (andradeB == 0) return Math.exp(andradeA);
        double t = Math.max(T, 150);
        return Math.max(1e-5, Math.min(1e4, Math.exp(andradeA + andradeB / t)));
    }

    /**
     * Why this species' data can't be used, or null: its vapour's energy must stay above its liquid's below the critical
     * point, and both must rise with temperature, over Flash's range (Argon: else U(T) is not monotone).
     */
    public String check() {
        if (!(rhoL > 0) || !(molarMass > 0)) return "density and molar mass must be positive";
        if (kind == Kind.GAS) return cpV > 0 ? null : "a gas needs a positive heat capacity";
        if (!(cpL > 0)) return "the liquid needs a positive heat capacity";
        if (kind != Kind.CURVE) return null;
        if (!(tb > 0) || !(latentTb > 0) || !(tc > tb)) return "a boiling liquid needs a boiling point, a latent heat and a critical temperature above it";
        if (!(cpV > r)) return "its vapour's heat capacity must exceed its gas constant (" + Math.round(r) + " J/kg/K)";
        double lastL = -Double.MAX_VALUE, lastV = -Double.MAX_VALUE;
        for (double T = Flash.T_MIN; T <= 2000; T += 1) {
            double ul = uLiquid(T), uv = uVapour(T);
            if (T < tc && uv < ul) return "its vapour's energy falls below its liquid's at " + Math.round(T) + " K";
            if (ul < lastL || uv < lastV) return "its energy falls with temperature at " + Math.round(T) + " K";
            lastL = ul;
            lastV = uv;
        }
        return null;
    }

    /** A name for players: the key's path in words ("eln:crude_oil": crude oil; water and air as such). */
    public String displayName() {
        String path = key.substring(key.indexOf(':') + 1);
        return path.replace('_', ' ');
    }

    @Override
    public String toString() {
        return key + "#" + id;
    }
}
