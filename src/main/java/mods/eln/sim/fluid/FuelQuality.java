package mods.eln.sim.fluid;

/**
 * Fuel quality (refinery milestone M1, Baughn 2026-11-15): what an engine or burner makes of the liquid layer it burns.
 * One mechanism for the furnace burner's coking and the engines' derating and fouling:
 * <ul>
 * <li>the off-window share: the mass share of the layer that boils outside the engine's window (a species without a
 * boiling point, residue, is too heavy for any window). An engine loses DERATE_PER_SHARE (2) % of its power per % of
 * it: a 90 % cut gives 80 %, a 50 % one nothing.</li>
 * <li>the coking index: each species' `coking` by its share (residue 1, heavy oil 0.15, diesel 0.01, light cuts 0).
 * Over the device's balance it fouls (0..FOUL_MAX, that share of its power gone), under it it clears again; no
 * damage.</li>
 * <li>the choke (Baughn 2026-11-23): from ENGINE_CHOKE an engine's injectors choke. It takes no fuel and gives no power
 * until the fuel in it is better; no damage. The furnace burner never chokes: it is meant to burn dirty fuel.</li>
 * </ul>
 */
public final class FuelQuality {
    private FuelQuality() {
    }

    /** Power lost per share of the fuel outside the window. */
    public static final double DERATE_PER_SHARE = 2;
    /** The most a burner or engine fouls: it still gives a tenth. */
    public static final double FOUL_MAX = 0.9;
    /**
     * The coking index at which a device neither fouls nor clears. The furnace burner's: a plausible refinery's fuel
     * (Baughn 2026-11-11). An engine's is stricter (Baughn 2026-11-15: hard for a pot still without compromises, easy in
     * the endgame): diesel with over 7 % heavy oil or 1 % residue in it fouls an engine; a pot still's one-pass diesel
     * (78-88 % pure) does, its second pass or a tray column's (97-99 %) doesn't.
     */
    public static final double FURNACE_BALANCE = 0.05, ENGINE_BALANCE = 0.02;
    /**
     * The coking index from which an engine chokes (Baughn 2026-11-23: off-spec fuel loses power at once, fouls over a
     * threshold, and chokes the engine past a further one, set so that a halfway sensible distillery never triggers it
     * but a badly built or uncontrolled one can). PLACEHOLDER: to be calibrated from the M2 pot-still tests (Baughn
     * 2026-11-23). For now 9x ENGINE_BALANCE, between heavy oil (0.15), which the gas turbine burns (fouling), and dead
     * crude (0.216): crude chokes, and so does diesel with over 17 % residue in it; no distillate does today, as residue
     * doesn't boil.
     */
    public static final double ENGINE_CHOKE = 0.18;
    /** Fouling per unit of index over the balance: dead crude (index 0.2159) fouls a furnace burner at rate 1. */
    public static final double FOUL_PER_INDEX = 1 / (0.2159 - FURNACE_BALANCE);
    /** At rate 1 and full power a device fouls completely in this long (s). */
    public static final double FOUL_SECONDS = 2 * 3600;
    /**
     * Engines foul (and clear) this many times slower than a furnace burner (Baughn 2026-11-15: "4x lower seems fine;
     * that allows players time to consider a fix"): heavy oil takes ~10 % of a gas turbine's power an hour, not 39 %.
     */
    public static final double ENGINE_SLOWER = 4;

    /** The gas turbine's boiling window (K): naphtha to heavy oil (it burns heavy oil today, and real ones do, fouling). */
    public static final double TURBINE_LO = 330, TURBINE_HI = 720;
    /** The Fuel Generator's (K): a spark-ignition engine, gasoline and naphtha (design-398); diesel and heavier derate it. */
    public static final double GENERATOR_LO = 330, GENERATOR_HI = 400;

    /** What a layer is to an engine with a window: its shares too light and too heavy (0..1), and its coking index. */
    public record Rating(double light, double heavy, double coking) {
        public static final Rating CLEAN = new Rating(0, 0, 0);

        /** The share outside the window. */
        public double off() {
            return light + heavy;
        }

        /** The share of its power an engine gets on it: 1 - 2 x the share outside the window, not under 0. */
        public double power() {
            return powerFactor(off());
        }

        /** Does it choke an engine (coking index from ENGINE_CHOKE): no fuel taken, no power. */
        public boolean choked() {
            return coking >= ENGINE_CHOKE;
        }
    }

    public static double powerFactor(double off) {
        return Math.max(0, Math.min(1, 1 - DERATE_PER_SHARE * off));
    }

    /** Rate the liquid layer species id is in, for the window lo..hi (K, normal boiling points). */
    public static Rating rate(FluidNode n, int id, double lo, double hi) {
        double total = id < 0 ? 0 : EaUnits.layerLiquid(n, id);
        if (total <= 0) return Rating.CLEAN;
        int layer = n.layerOf(id);
        SpeciesRegistry reg = SpeciesRegistry.get();
        double light = 0, heavy = 0, coking = 0;
        for (int j = 2; j < n.speciesBound(); j++) {
            if (n.liquid(j) <= 0 || n.layerOf(j) != layer) continue;
            Species sp = reg.byId(j);
            double share = n.liquid(j) / total;
            coking += share * sp.coking;
            if (sp.kind != Species.Kind.CURVE || sp.tb > hi) heavy += share;   // (no boiling point: it never boils, residue)
            else if (sp.tb < lo) light += share;
        }
        return new Rating(light, heavy, coking);
    }

    /** The coking index of the layer species id is in: its species' `coking` by their shares. */
    public static double layerCoking(FluidNode n, int id) {
        return rate(n, id, 0, Double.MAX_VALUE).coking();
    }

    /** Fouling per FOUL_SECONDS at full power on fuel of this coking index (under 0: it clears). */
    public static double foulRate(double coking, double balance) {
        return FOUL_PER_INDEX * (coking - balance);
    }

    /** The fouling (0..FOUL_MAX) after dt s at this share of full power on fuel of this coking index. */
    public static double foul(double foul, double coking, double balance, double load, double dt) {
        return Math.max(0, Math.min(FOUL_MAX, foul + foulRate(coking, balance) * load * dt / FOUL_SECONDS));
    }

    /** An engine's fouling after dt s: the same law over ENGINE_BALANCE, ENGINE_SLOWER times slower. */
    public static double foulEngine(double foul, double coking, double load, double dt) {
        return foul(foul, coking, ENGINE_BALANCE, load, dt / ENGINE_SLOWER);
    }

    /** An engine's fouling per hour at full power on fuel of this coking index (under 0: it clears). */
    public static double engineFoulPerHour(double coking) {
        return foulRate(coking, ENGINE_BALANCE) * 3600 / (FOUL_SECONDS * ENGINE_SLOWER);
    }

    /** The viscosity (Pa s) of the liquid layer species id is in at the node's temperature: a log mix by mass (Arrhenius). */
    public static double layerViscosity(FluidNode n, int id) {
        double total = id < 0 ? 0 : EaUnits.layerLiquid(n, id);
        if (total <= 0) return 0;
        int layer = n.layerOf(id);
        SpeciesRegistry reg = SpeciesRegistry.get();
        double ln = 0, T = n.T();
        for (int j = 2; j < n.speciesBound(); j++)
            if (n.liquid(j) > 0 && n.layerOf(j) == layer) ln += n.liquid(j) / total * Math.log(Math.max(1e-9, reg.byId(j).muLiquid(T)));
        return Math.exp(ln);
    }

    /**
     * "87 % in range, 13 % too heavy: 74 % power" for a readout; for fuel that chokes an engine, that instead (its power
     * would mislead: it gives none).
     */
    public static String describe(Rating r) {
        if (r.choked()) return String.format("chokes engine; coking %.3f, limit %.2f", r.coking, ENGINE_CHOKE);
        StringBuilder s = new StringBuilder(String.format("%.0f%% in range", (1 - r.off()) * 100));
        if (r.light > 0.0005) s.append(String.format(", %.0f%% too light", r.light * 100));
        if (r.heavy > 0.0005) s.append(String.format(", %.0f%% too heavy", r.heavy * 100));
        return s.append(String.format(": %.0f%% power", r.power() * 100)).toString();
    }
}
