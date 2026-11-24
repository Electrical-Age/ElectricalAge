package mods.eln.sim.fluid;

/**
 * EA's fluid scale against the core's SI units (notes/pipes/DESIGN.md 6b): 1 mB of water is 6.75e-5 L = 67.5 mg
 * (LITRES_PER_MB), and 1 mB of steam is 1 mg (67.5 mB of steam per mB of water boiled).
 */
public final class EaUnits {
    private EaUnits() {
    }

    /** Real litres per game mB (design v2's litresPerMilliBucket); water is 1 kg/L. */
    public static final double LITRES_PER_MB = 6.75e-5;
    public static final double KG_PER_MB_WATER = LITRES_PER_MB, KG_PER_MB_STEAM = 1e-6;
    /** mB of steam that 1 mB of water boils into (KG_PER_MB_WATER / KG_PER_MB_STEAM). */
    public static final double STEAM_MB_PER_WATER_MB = 67.5;

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
     * grams: "2.1 g steam, 2.0 B water" (Baughn 2026-11-04, mB his call: Jade named only the gas that made the pressure; one
     * line for every fluid container; buckets from 1000 mB and no "of", Baughn 2026-11-11). A gas's share is its partial pressure's share of the space above the liquid; "empty" if none counts.
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
            if (liq > 0 && liq / sp.rhoL > 0.01 * n.volume) parts.put(sp.displayName(), volume(liq / sp.rhoL * Water.RHO_L / KG_PER_MB_WATER) + " " + sp.displayName());
            if (vap > 0 && pSum > 0 && vap * sp.r / pSum * vGas > 0.01 * n.volume) {
                String name = id == SpeciesRegistry.WATER ? "steam" : sp.gas() || liq <= 0 ? sp.displayName() : sp.displayName() + " vapour";
                parts.put(name, grams(vap * 1000) + " " + name);
            }
        }
        return parts.isEmpty() ? "empty" : String.join(", ", parts.values());
    }

    /**
     * Viscosity for players (design-398 item 6 A: thick liquids crawl through pipes, flow being inversely proportional to
     * it, and unseen that read as a bug): mPa s under 1 Pa s, Pa s from it: "0.50 mPa s", "200 mPa s", "50 Pa s".
     */
    public static String viscosity(double paS) {
        return paS < 1 ? compact(paS * 1000) + " mPa s" : compact(paS) + " Pa s";
    }

    /** Its ratio to water's viscosity in the game (Water.MU_L, so it says how much slower it flows in the same pipe). */
    public static double viscosityRatio(double paS) {
        return paS / Water.MU_L;
    }

    /** From this ratio to water a liquid is thick: the hint to heat or dilute it, and the line in Jade's headline. */
    public static final double THICK_RATIO = 200;

    /** The ratio to water as shown: to 0.1 under 10, else two significant figures (the bands go by it, so they agree). */
    private static double shownRatio(double paS) {
        double r = viscosityRatio(paS);
        if (r < 9.95) return Math.round(r * 10) / 10.0;
        double mag = Math.pow(10, Math.floor(Math.log10(r)) - 1);
        return Math.round(r / mag) * mag;
    }

    /** Is it thick (shown at THICK_RATIO x water or more): the fix, and Jade's headline. */
    private static boolean thick(double paS) {
        return shownRatio(paS) >= THICK_RATIO;
    }

    /** "2.0x water", "800x water", "200,000x water": two significant figures, so the big ones read at a glance. */
    private static String timesWater(double paS) {
        double r = shownRatio(paS);
        return mods.eln.i18n.I18N.tr("%1$x water", String.format(java.util.Locale.ROOT, r < 10 ? "%.1f" : "%,.0f", r));
    }

    /** A viscosity's word (Neon's bands by its shown ratio to water): thin under 15x, syrupy under 200x, thick under 5000x, tarry. */
    private static String band(double paS) {
        double r = shownRatio(paS);
        return r < 15 ? mods.eln.i18n.I18N.tr("thin") : r < THICK_RATIO ? mods.eln.i18n.I18N.tr("syrupy")
            : r < 5000 ? mods.eln.i18n.I18N.tr("thick") : mods.eln.i18n.I18N.tr("tarry");
    }

    /** The fix a thick liquid's line ends with. */
    private static String fix() {
        return "; " + mods.eln.i18n.I18N.tr("heat or dilute");
    }

    /**
     * A liquid's viscosity in words: "thin, 0.50 mPa s, 2.0x water", "tarry, 50 Pa s, 200,000x water". No fix: the engines'
     * fuel lines use it, and they don't mind viscosity (Neon: a fix there would read as a fault).
     */
    public static String viscosityText(double paS) {
        return band(paS) + ", " + viscosity(paS) + ", " + timesWater(paS);
    }

    /**
     * The viscosity of each liquid layer but water that fills over 1 % of a node (the floor of composition), {Pa s, main
     * species id} each, bottom first; empty for a node of water or gas only.
     */
    static java.util.List<double[]> layerViscosities(FluidNode n) {
        var out = new java.util.ArrayList<double[]>();
        if (!holdsOthers(n)) return out;
        SpeciesRegistry reg = SpeciesRegistry.get();
        for (int l = 0; l < n.layers(); l++) {
            int main = -1;
            double m3 = 0;
            for (int j = 2; j < n.speciesBound(); j++) {
                if (n.liquid(j) <= 0 || n.layerOf(j) != l) continue;
                m3 += n.liquid(j) / reg.byId(j).rhoL;
                if (main < 0 || n.liquid(j) > n.liquid(main)) main = j;
            }
            if (main >= 0 && m3 > 0.01 * n.volume) out.add(new double[]{FuelQuality.layerViscosity(n, main), main});
        }
        return out;
    }

    /**
     * The Viscosity line of a fluid container, or null for one of water or gas only: one liquid in words and by name,
     * "thick heavy oil, 200 mPa s, 800x water; heat or dilute" (named: in Jade's headline it may stand above the
     * contents), several by name, "heavy oil 200 mPa s, 800x water; gasoline 0.40 mPa s, 1.6x water"; the fix if one is
     * thick.
     */
    public static String viscosity(FluidNode n) {
        return viscosityLine(layerViscosities(n));
    }

    /** viscosity(n) of its layerViscosities. */
    private static String viscosityLine(java.util.List<double[]> layers) {
        if (layers.isEmpty()) return null;
        SpeciesRegistry reg = SpeciesRegistry.get();
        if (layers.size() == 1) {
            double mu = layers.get(0)[0];
            return band(mu) + " " + reg.byId((int) layers.get(0)[1]).displayName() + ", " + viscosity(mu) + ", " + timesWater(mu) + (thick(mu) ? fix() : "");
        }
        StringBuilder b = new StringBuilder();
        boolean thick = false;
        for (double[] l : layers) {
            b.append(b.length() == 0 ? "" : "; ").append(reg.byId((int) l[1]).displayName()).append(' ').append(viscosity(l[0]))
                .append(", ").append(timesWater(l[0]));
            thick |= thick(l[0]);
        }
        return thick ? b.append(fix()).toString() : b.toString();
    }

    /**
     * Put a fluid container's Viscosity line (viscosity(n); none for water or gas only) into its Jade lines: last, or third
     * if the liquid is thick, so cold residue that won't flow says why without sneaking (Jade shows three lines).
     */
    public static void putViscosity(java.util.Map<String, String> info, FluidNode n) {
        var layers = layerViscosities(n);
        String v = viscosityLine(layers);
        if (v == null) return;
        String key = mods.eln.i18n.I18N.tr("Viscosity");
        boolean thick = false;
        for (double[] l : layers) thick |= thick(l[0]);
        if (!thick || info.size() < 2) {
            info.put(key, v);
            return;
        }
        var rest = new java.util.LinkedHashMap<String, String>();
        int i = 0;
        for (var e : info.entrySet()) if (i++ >= 2) rest.put(e.getKey(), e.getValue());
        for (String k : rest.keySet()) info.remove(k);
        info.put(key, v);
        info.putAll(rest);
    }

    /** Two significant figures or so: 0.012, 0.12, 1.2, 12, 120. */
    private static String compact(double v) {
        return String.format(java.util.Locale.ROOT, v >= 10 ? "%.0f" : v >= 1 ? "%.1f" : v >= 0.1 ? "%.2f" : "%.3f", v);
    }

    /** A liquid volume compactly (Baughn 2026-11-11: "18 g air, 14.6 B gasoline, 700 mB diesel"): buckets from 1000 mB. */
    public static String volume(double mB) {
        if (mB < 1000) return compact(mB) + " mB";
        double b = mB / 1000;
        return String.format(b >= 100 ? "%.0f B" : "%.1f B", b);
    }

    /**
     * A tank's fill against its capacity, both as volume: "700 mB / 4.0 B", "3.9 B / 4.0 B". The fill's buckets are
     * rounded down, so a tank short of full doesn't read full.
     */
    public static String tank(double mB, double capacityMb) {
        // (rounded down, so only a full tank reads full; the epsilon absorbs a full tank's mass/rho coming out as 3999.9999)
        double f = Math.max(0, Math.min(mB, capacityMb)) + 1e-6 * Math.max(1, capacityMb);
        String fill;
        if (f < 1) fill = "0 mB";
        else if (f < 10) fill = String.format("%.1f mB", Math.floor(f * 10) / 10);
        else if (f < 1000) fill = String.format("%.0f mB", Math.floor(f));
        else {
            double b = f / 1000;
            fill = b >= 100 ? String.format("%.0f B", Math.floor(b)) : String.format("%.1f B", Math.floor(b * 10) / 10);
        }
        return fill + " / " + volume(capacityMb);
    }

    /** Grams for players: "12 g", "1.2 kg" (two figures). */
    public static String grams(double g) {
        return g >= 1000 ? compact(g / 1000) + " kg" : compact(g) + " g";
    }

    /** A node's other species for players, one per entry: name, then liquid mB and gas grams (empty if it holds none). */
    public static java.util.List<String[]> others(FluidNode n, java.util.function.ToDoubleFunction<Integer> kgPerMbLiquid) {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        if (!holdsOthers(n)) return out;
        SpeciesRegistry reg = SpeciesRegistry.get();
        for (int id = 2; id < n.speciesBound(); id++) {
            if (n.mass(id) <= 0) continue;
            String liq = n.liquid(id) > 0 ? volume(n.liquid(id) / kgPerMbLiquid.applyAsDouble(id)) : "";
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
        add(n, id, m, T, vapour, false);
    }

    /**
     * ... a mixture alias as what it is made of: crude as its cuts. Live (the oil pump's sealed line from the well) with its
     * natural gas; otherwise dead, its gas long gone (Baughn 2026-11-10: "the buckets don't have lids").
     */
    public static void add(FluidNode n, int id, double m, double T, boolean vapour, boolean live) {
        double[][] mix = SpeciesRegistry.get().mixture(id, !live);
        if (mix != null) {
            for (int i = 0; i < mix[0].length; i++) {
                int c = (int) mix[0][i];
                add(n, c, m * mix[1][i], T, SpeciesRegistry.get().byId(c).gas());
            }
            return;
        }
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

    /**
     * The share a mixed liquid's main species must have to count as that species at a bucket or another mod's pipe
     * (Baughn 2026-11-10: 90 %). Under it the view refuses the layer: refine it first.
     */
    public static final double PURE_SHARE = 0.9;

    /** The species with the most liquid in id's layer (id itself for water). */
    public static int mainOfLayer(FluidNode n, int id) {
        if (id == SpeciesRegistry.WATER) return id;
        int layer = n.layerOf(id), best = id;
        for (int j = 2; j < n.speciesBound(); j++) if (n.layerOf(j) == layer && n.liquid(j) > n.liquid(best)) best = j;
        return best;
    }

    /** The liquid (kg) of the layer species id is in: every species mixed in it. */
    public static double layerLiquid(FluidNode n, int id) {
        if (id == SpeciesRegistry.WATER) return n.liquid(id);
        int layer = n.layerOf(id);
        double kg = 0;
        for (int j = 2; j < n.speciesBound(); j++) if (n.liquid(j) > 0 && n.layerOf(j) == layer) kg += n.liquid(j);
        return kg;
    }

    /**
     * Take up to m kg of the layer species id is in out of n, every species in it by its share, each with its enthalpy
     * (Baughn 2026-11-10: a mixed liquid is named by its main species; taken whole, so a bucket can't pull one species out
     * of a mix and distil it for free). Returns what it took.
     */
    public static double drainLayer(FluidNode n, int id, double m) {
        if (id == SpeciesRegistry.WATER) return drainLiquid(n, m);
        double total = layerLiquid(n, id);
        m = Math.min(m, total);
        if (m <= 0) return 0;
        int layer = n.layerOf(id);
        double T = n.T(), f = m / total;
        double[] take = new double[n.speciesBound()];
        for (int j = 2; j < take.length; j++) if (n.liquid(j) > 0 && n.layerOf(j) == layer) take[j] = n.liquid(j) * f;
        for (int j = 2; j < take.length; j++) {
            if (take[j] <= 0) continue;
            n.addMass(j, -take[j]);
            n.energy -= take[j] * SpeciesRegistry.get().byId(j).hLiquid(T);
        }
        n.update();
        return m;
    }

    /**
     * Move up to kg of from's liquid (at most half of it) into to, every liquid in it by its share (all its layers, as
     * the Still Pot's entrainment carries them into its vapour line), each with its liquid enthalpy at from's temperature,
     * out of from and into to, so energy is conserved exactly. Returns what it moved (kg).
     */
    public static double carryLiquid(FluidNode from, FluidNode to, double kg) {
        SpeciesRegistry reg = SpeciesRegistry.get();
        int n = from.speciesBound();
        double all = 0;
        for (int i = 0; i < n; i++) if (i != SpeciesRegistry.AIR) all += from.liquid(i);
        if (all <= 0) return 0;
        kg = Math.min(kg, 0.5 * all);
        double T = from.T(), moved = 0;
        for (int i = 0; i < n; i++) {
            double l = i == SpeciesRegistry.AIR ? 0 : from.liquid(i);
            if (l <= 0) continue;
            double m = kg * l / all, h = i == SpeciesRegistry.WATER ? Water.hLiquid(T) : reg.byId(i).hLiquid(T);
            from.addMass(i, -m);
            from.energy -= m * h;
            to.addMass(i, m);
            to.energy += m * h;
            moved += m;
        }
        from.update();
        to.update();
        return moved;
    }

    /** The heating value (J/kg) of the layer species id is in: its species' by their shares. */
    public static double layerHeatingValue(FluidNode n, int id) {
        double total = layerLiquid(n, id);
        if (total <= 0) return 0;
        int layer = n.layerOf(id);
        SpeciesRegistry reg = SpeciesRegistry.get();
        double hv = 0;
        for (int j = 2; j < n.speciesBound(); j++) if (n.layerOf(j) == layer) hv += n.liquid(j) / total * reg.byId(j).heatingValue;
        return hv;
    }

    /**
     * How much of the layer species ids[0] is in matches the shares given for ids (1 - total variation distance, 0..1):
     * 1 for a layer in exactly those proportions, the matching share for a layer of one of them, less for anything else
     * in it.
     */
    public static double layerOverlap(FluidNode n, double[] ids, double[] shares) {
        int first = (int) ids[0];
        double total = layerLiquid(n, first);
        if (total <= 0) return 0;
        int layer = n.layerOf(first);
        double overlap = 0;
        for (int i = 0; i < ids.length; i++) {
            int id = (int) ids[i];
            double own = n.layerOf(id) == layer ? n.liquid(id) / total : 0;
            overlap += Math.min(own, shares[i]);
        }
        return overlap;
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
