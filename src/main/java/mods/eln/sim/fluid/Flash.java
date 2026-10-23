package mods.eln.sim.fluid;

/**
 * The state of a closed volume of water (liquid and vapour) and air from its contents: given the masses, the internal
 * energy and the volume, find the temperature at which they are in equilibrium, how much water is liquid, and the
 * pressure. Liquid sits at the bottom; vapour and air share the space above it. When liquid fills the volume, the
 * pressure rises steeply (a softened bulk modulus, Water.K_LIQUID), so a full node stays solvable.
 */
public final class Flash {
    /** Smallest gas space, as a fraction of the volume (below it the liquid is "full"). */
    static final double GAS_MIN = 0.02;
    public static final double T_MIN = 80, T_MAX = 6000;   // (gas expanding fast cools a lot; ice is not modelled)

    public double T, P, mLiquid, mVapour, vGas;
    /** Partial pressures (Pa) of vapour and air in the gas space, and the liquid's extra pressure when full. */
    public double pVapour, pAir, pFull;

    public Flash() {
    }

    public Flash copy() {
        Flash f = new Flash();
        f.T = T; f.P = P; f.mLiquid = mLiquid; f.mVapour = mVapour; f.vGas = vGas;
        f.pVapour = pVapour; f.pAir = pAir; f.pFull = pFull;
        return f;
    }

    /** Fraction of the volume taken by liquid (the liquid level, 0 bottom to 1 top). */
    public double level(double V) {
        return V <= 0 ? 0 : Math.min(1, mLiquid / Water.RHO_L / V);
    }

    /** Equilibrium split of the water at temperature T into this (no energy check). */
    void split(double mw, double ma, double V, double T) {
        double dMin = GAS_MIN * V;
        double ps = Water.psat(T);
        double mv;
        if (mw <= 0) mv = 0;
        else if (T >= Water.TC) mv = mw;                                   // supercritical: one gas phase
        else if (mw * Water.R_V * T / V <= ps) mv = mw;                  // all vapour (superheated)
        else {
            double denom = 1 - ps / (Water.RHO_L * Water.R_V * T);
            mv = ps * (V - mw / Water.RHO_L) / (Water.R_V * T) / denom;
            if (mv < 0 || V - (mw - mv) / Water.RHO_L < dMin) mv = Math.min(mw, ps * dMin / (Water.R_V * T));
            mv = Math.max(0, Math.min(mw, mv));
        }
        this.T = T;
        mVapour = mv;
        mLiquid = mw - mv;
        double vLiq = mLiquid / Water.RHO_L;
        vGas = Math.max(V - vLiq, dMin);
        pVapour = mv * Water.R_V * T / vGas;
        pAir = ma * Water.R_AIR * T / vGas;
        pFull = Math.max(0, vLiq - (V - dMin)) * Water.K_LIQUID / V;
        P = pVapour + pAir + pFull;
    }

    double energy(double ma) {
        return mLiquid * Water.uLiquid(T) + mVapour * Water.uVapour(T) + ma * Water.uAir(T);
    }

    /**
     * Solve for the contents mw (water, kg), ma (air, kg), U (J) in V (m^3). An empty volume keeps tHint and has no
     * pressure. Bisection on T (the energy rises with T at fixed contents: warmer, and more of it vapour).
     */
    public Flash solve(double mw, double ma, double U, double V, double tHint) {
        if (mw + ma <= 1e-15) {
            split(0, 0, V, tHint);
            return this;
        }
        double lo = T_MIN, hi = T_MAX;
        split(mw, ma, V, lo);
        if (energy(ma) >= U) return this;
        split(mw, ma, V, hi);
        if (energy(ma) <= U) return this;
        for (int i = 0; i < 60 && hi - lo > 1e-7; i++) {
            double m = 0.5 * (lo + hi);
            split(mw, ma, V, m);
            if (energy(ma) < U) lo = m;
            else hi = m;
        }
        split(mw, ma, V, 0.5 * (lo + hi));
        return this;
    }

    /** The internal energy (J) of mw water and ma air at temperature T in V, in equilibrium (for setting up a state). */
    public static double energyAt(double mw, double ma, double V, double T) {
        Flash f = new Flash();
        f.split(mw, ma, V, T);
        return f.energy(ma);
    }
}
