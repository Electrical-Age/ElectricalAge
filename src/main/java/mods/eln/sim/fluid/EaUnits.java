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

    /** Does it hold any species but water and air. */
    public static boolean holdsOthers(FluidNode n) {
        if (n.extra == null) return false;
        for (double m : n.extra) if (m > 0) return true;
        return false;
    }

    /**
     * What a node's pressure is of, for its label: water when it is full of it, else its gas (steam, air or both); with
     * other species, the liquids it is full of, or the gases that make most of its pressure.
     */
    public static String contents(FluidNode n) {
        if (holdsOthers(n)) return contentsOfAll(n);
        if (n.mLiquid() / Water.RHO_L > 0.95 * n.volume) return "water";
        double share = n.P() > 0 ? n.pVapour() / n.P() : 0;
        return share > 0.9 ? "steam" : share < 0.1 ? "air" : "steam and air";
    }

    private static String contentsOfAll(FluidNode n) {
        SpeciesRegistry reg = SpeciesRegistry.get();
        StringBuilder b = new StringBuilder();
        if (n.liquidVolume() > 0.95 * n.volume) {
            for (int id = 0; id < n.speciesBound(); id++)
                if (n.liquid(id) > 0.05 * n.liquidVolume() * reg.byId(id).rhoL) b.append(b.length() == 0 ? "" : " and ").append(reg.byId(id).displayName());
            return b.length() == 0 ? "liquid" : b.toString();
        }
        double p = n.P();
        for (int id = 0; id < n.speciesBound(); id++) {
            double pi = n.vapour(id) * reg.byId(id).r * n.T() / Math.max(1e-12, n.state.vGas);
            if (p > 0 && pi > 0.1 * p) b.append(b.length() == 0 ? "" : " and ").append(id == SpeciesRegistry.WATER ? "steam" : reg.byId(id).displayName());
        }
        return b.length() == 0 ? "gas" : b.toString();
    }

    /**
     * Everything that makes up more than 1% of a node's volume, by name and compact, liquids in mB and gases in
     * grams: "2.1 g of steam, 2000 mB of water" (Baughn 2026-11-04, mB his call: Jade named only the gas that made the pressure; one
     * line for every fluid container). A gas's share is its partial pressure's share of the space above the liquid; "empty" if none counts.
     */
    public static String composition(FluidNode n) {
        SpeciesRegistry reg = SpeciesRegistry.get();
        Flash s = n.state;
        double pSum = 0, vGas = Math.max(0, n.volume - n.liquidVolume());   // (not the flash's floor: a full pipe holds no gas)
        for (int id = 0; id < n.speciesBound(); id++) if (s.vapour(id) > 0) pSum += s.vapour(id) * reg.byId(id).r;
        var parts = new java.util.TreeMap<String, String>();
        for (int id = 0; id < n.speciesBound(); id++) {
            Species sp = reg.byId(id);
            double liq = s.liquid(id), vap = s.vapour(id);
            if (liq > 0 && liq / sp.rhoL > 0.01 * n.volume) parts.put(sp.displayName(), millibuckets(liq / sp.rhoL * Water.RHO_L / KG_PER_MB_WATER) + " of " + sp.displayName());
            if (vap > 0 && pSum > 0 && vap * sp.r / pSum * vGas > 0.01 * n.volume) {
                String name = id == SpeciesRegistry.WATER ? "steam" : sp.gas() || liq <= 0 ? sp.displayName() : sp.displayName() + " vapour";
                parts.put(name, grams(vap * 1000) + " of " + name);
            }
        }
        return parts.isEmpty() ? "empty" : String.join(", ", parts.values());
    }

    /** Two significant figures or so: 0.012, 0.12, 1.2, 12, 120. */
    private static String compact(double v) {
        return String.format(v >= 10 ? "%.0f" : v >= 1 ? "%.1f" : v >= 0.1 ? "%.2f" : "%.3f", v);
    }

    private static String millibuckets(double mB) {
        return compact(mB) + " mB";
    }

    private static String grams(double g) {
        return g >= 1000 ? compact(g / 1000) + " kg" : compact(g) + " g";
    }

    /** A node's other species for players, one per entry: name, then liquid mB and gas grams (empty if it holds none). */
    public static java.util.List<String[]> others(FluidNode n, java.util.function.ToDoubleFunction<Integer> kgPerMbLiquid) {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        if (!holdsOthers(n)) return out;
        SpeciesRegistry reg = SpeciesRegistry.get();
        for (int id = 2; id < n.speciesBound(); id++) {
            if (n.mass(id) <= 0) continue;
            String liq = n.liquid(id) > 0 ? String.format("%.0f mB", n.liquid(id) / kgPerMbLiquid.applyAsDouble(id)) : "";
            String gas = n.vapour(id) > 1e-7 ? String.format("%.1f g gas", n.vapour(id) * 1000) : "";
            out.add(new String[]{reg.byId(id).displayName(), liq.isEmpty() ? gas : gas.isEmpty() ? liq : liq + ", " + gas});
        }
        return out;
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
        n.energy -= v * n.hVapour(T) + a * Water.hAir(T);
        n.update();
        return v;
    }

    /** Take m kg of vapour only out of n (a pipe drawing steam through the fluid capability). */
    public static void drainVapour(FluidNode n, double m) {
        m = Math.min(m, n.mVapour());
        if (m <= 0) return;
        n.mWater -= m;
        n.energy -= m * n.hVapour(n.T());
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
        n.energy += m * (vapour ? n.hVapour(T) : Water.hLiquid(T));
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
