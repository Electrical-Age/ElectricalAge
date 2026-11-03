package mods.eln.sim.fluid;

/**
 * EA's fluid scale against the core's SI units (notes/pipes/DESIGN.md 6b): 1 mB of water is 6.75e-5 L = 67.5 mg
 * (OilPumpDescriptor.LITRES_PER_MB), and 1 mB of steam is 1 mg (67.5 mB of steam per mB of water boiled).
 */
public final class EaUnits {
    private EaUnits() {
    }

    public static final double KG_PER_MB_WATER = 6.75e-5, KG_PER_MB_STEAM = 1e-6;

    /** Steam for players (Baughn: mB of a gas never made sense; grams do): mB of steam (or a rate in mB/s) as grams. */
    public static String steamGrams(double mB) {
        double g = mB * KG_PER_MB_STEAM * 1000;
        return String.format(g < 10 ? "%.2f g" : "%.1f g", g);
    }

    /** What a node's pressure is of, for its label: water when it is full of it, else its gas (steam, air or both). */
    public static String contents(FluidNode n) {
        if (n.mLiquid() / Water.RHO_L > 0.95 * n.volume) return "water";
        double share = n.P() > 0 ? n.pVapour() / n.P() : 0;
        return share > 0.9 ? "steam" : share < 0.1 ? "air" : "steam and air";
    }

    /** The vapour a node can give out (kg): what is above what 1 atm would hold. */
    public static double steamAvailable(FluidNode n) {
        double p = n.P();
        return p <= Water.P_ATM ? 0 : n.mVapour() * (1 - Water.P_ATM / p);
    }

    /** Take m kg of gas (vapour and air in proportion) out of n, with its enthalpy; returns the vapour taken (kg). */
    public static double drainGas(FluidNode n, double m) {
        double mv = n.mVapour(), ma = n.mAir, mg = mv + ma;
        if (mg <= 0 || m <= 0) return 0;
        m = Math.min(m, 0.9 * mg);
        double v = m * mv / mg, a = m * ma / mg, T = n.T();
        n.mWater -= v;
        n.mAir -= a;
        n.energy -= v * Water.hVapour(T) + a * Water.hAir(T);
        n.update();
        return v;
    }

    /** Take m kg of vapour only out of n (a pipe drawing steam through the fluid capability). */
    public static void drainVapour(FluidNode n, double m) {
        m = Math.min(m, n.mVapour());
        if (m <= 0) return;
        n.mWater -= m;
        n.energy -= m * Water.hVapour(n.T());
        n.update();
    }

    /** Take m kg of liquid water out of n (a bucket, or the fluid capability), with its enthalpy; returns what it took. */
    public static double drainLiquid(FluidNode n, double m) {
        m = Math.min(m, n.mLiquid());
        if (m <= 0) return 0;
        n.mWater -= m;
        n.energy -= m * Water.hLiquid(n.T());
        n.update();
        return m;
    }

    /** Put m kg of water in at temperature T (liquid) or of steam (vapour at T). */
    public static void addWater(FluidNode n, double m, double T, boolean vapour) {
        if (m <= 0) return;
        n.mWater += m;
        n.energy += m * (vapour ? Water.hVapour(T) : Water.hLiquid(T));
        n.update();
    }

    /** The vapour of species id a node can give out (kg): what is above what 1 atm would hold (water: steamAvailable). */
    public static double vapourAvailable(FluidNode n, int id) {
        if (id == SpeciesRegistry.WATER) return steamAvailable(n);
        double p = n.P();
        return p <= Water.P_ATM ? 0 : n.vapour(id) * (1 - Water.P_ATM / p);
    }

    /** Put m kg of species id in at temperature T, as liquid or vapour (water: addWater). */
    public static void add(FluidNode n, int id, double m, double T, boolean vapour) {
        if (id == SpeciesRegistry.WATER) {
            addWater(n, m, T, vapour);
            return;
        }
        if (m <= 0) return;
        Species s = SpeciesRegistry.get().byId(id);
        n.addMass(id, m);
        n.energy += m * (vapour || s.gas() ? s.hVapour(T) : s.hLiquid(T));
        n.update();
    }

    /** Take up to m kg of species id's liquid out of n, with its enthalpy; returns what it took. */
    public static double drainLiquid(FluidNode n, int id, double m) {
        if (id == SpeciesRegistry.WATER) return drainLiquid(n, m);
        m = Math.min(m, n.liquid(id));
        if (m <= 0) return 0;
        n.addMass(id, -m);
        n.energy -= m * SpeciesRegistry.get().byId(id).hLiquid(n.T());
        n.update();
        return m;
    }

    /** Take up to m kg of species id's vapour out of n, with its enthalpy. */
    public static void drainVapour(FluidNode n, int id, double m) {
        if (id == SpeciesRegistry.WATER) {
            drainVapour(n, m);
            return;
        }
        m = Math.min(m, n.vapour(id));
        if (m <= 0) return;
        n.addMass(id, -m);
        n.energy -= m * SpeciesRegistry.get().byId(id).hVapour(n.T());
        n.update();
    }
}
