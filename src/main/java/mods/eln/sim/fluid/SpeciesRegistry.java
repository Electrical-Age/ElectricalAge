package mods.eln.sim.fluid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The species the fluid core knows, by dense id: water (0) and air (1) built in on Water's numbers, then the ones
 * datapacks define (SpeciesLoader), in the order first seen. An id is never reused while the game runs: a species whose
 * data disappears on a reload keeps its id and its last data (nodes may still hold it), and one only known from a save
 * gets placeholder data until its JSON arrives (docs/persistence.md: never delete what a save holds).
 */
public final class SpeciesRegistry {
    public static final int WATER = 0, AIR = 1;
    public static final String WATER_KEY = "eln:water", AIR_KEY = "eln:air";

    private static final SpeciesRegistry INSTANCE = new SpeciesRegistry();

    private final List<Species> byId = new ArrayList<>();
    /** Snapshots for the solver's inner loops (replaced, never changed in place): species by id, group representative by id. */
    private volatile Species[] all = new Species[0];
    private volatile int[] groupRep = new int[0];
    private final Map<String, Integer> idOf = new HashMap<>();
    /** Keys only known from saves (placeholder data), logged once each. */
    private final java.util.Set<String> placeholders = new java.util.HashSet<>();

    private SpeciesRegistry() {
        reset();
    }

    public static SpeciesRegistry get() {
        return INSTANCE;
    }

    /** Back to the two built-in species only (tests; a world unload before the next datapack load). */
    public synchronized void reset() {
        byId.clear();
        idOf.clear();
        placeholders.clear();
        add(new Species(WATER, WATER_KEY, Species.Kind.WATER, "water", Water.RHO_L, Water.CP_L, Water.CP_V,
            Species.R_UNIVERSAL / Water.R_V, Water.R_V, Water.TB, Water.L_TB, Water.TC, Water.MU_L, 373, Water.MU_L, 373, Water.MU_V, 0x3060E0));
        add(new Species(AIR, AIR_KEY, Species.Kind.GAS, "air", 1.2, 0, Water.CV_AIR,
            Species.R_UNIVERSAL / Water.R_AIR, Water.R_AIR, 0, 0, 0, Water.MU_AIR, 293, Water.MU_AIR, 293, Water.MU_AIR, 0xE0E0E0));
    }

    private void add(Species s) {
        idOf.put(s.key, s.id);
        byId.add(s);
        snapshot();
    }

    private void snapshot() {
        Species[] a = byId.toArray(new Species[0]);
        int[] g = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            g[i] = i;
            for (int j = 0; j < i; j++) if (a[j].group.equals(a[i].group)) { g[i] = j; break; }
        }
        all = a;
        groupRep = g;
    }

    /** All species by id (a snapshot: don't change it). */
    public Species[] all() {
        return all;
    }

    /** The lowest id in each species' group, by id (a snapshot: don't change it). */
    public int[] groupReps() {
        return groupRep;
    }

    /** The species with this id (ids come from this registry, so it exists). */
    public Species byId(int id) {
        return all[id];
    }

    /** Mixture aliases by key: component key -> mass share (SpeciesLoader.mixture). */
    private final Map<String, Map<String, Double>> mixtures = new HashMap<>();

    /** Set (or clear, with null) the mixture a species stands for in nodes. */
    public synchronized void setMixture(String key, Map<String, Double> shares) {
        if (shares == null) mixtures.remove(key);
        else mixtures.put(key, Map.copyOf(shares));
    }

    /**
     * What a species is made of in a node: {ids, mass shares}, or null if it is no mixture or a component isn't known.
     * dead: without its gases, the rest renormalised (a save's crude: Baughn 2026-11-10, saves load as dead crude, so
     * a sealed crude pipe doesn't burst on load).
     */
    public synchronized double[][] mixture(int id, boolean dead) {
        if (id < 0 || id >= byId.size()) return null;
        Map<String, Double> m = mixtures.get(byId.get(id).key);
        if (m == null) return null;
        List<double[]> parts = new ArrayList<>();
        double sum = 0;
        for (var e : m.entrySet()) {
            Integer c = idOf.get(e.getKey());
            if (c == null || c == id || mixtures.containsKey(e.getKey())) return null;
            if (dead && byId.get(c).gas()) continue;
            parts.add(new double[]{c, e.getValue()});
            sum += e.getValue();
        }
        if (parts.isEmpty() || !(sum > 0)) return null;
        double[][] out = new double[2][parts.size()];
        for (int i = 0; i < parts.size(); i++) {
            out[0][i] = parts.get(i)[0];
            out[1][i] = parts.get(i)[1] / sum;
        }
        return out;
    }

    /** The id for a key, or -1. */
    public synchronized int id(String key) {
        Integer i = idOf.get(key);
        return i == null ? -1 : i;
    }

    public int size() {
        return byId.size();
    }

    /**
     * Define or redefine a species from its data (the id in it is ignored); returns it under its id (a new one for a new
     * key). The built-ins can't be redefined: Water's model and the goldens rest on them.
     */
    public synchronized Species define(Species data) {
        String why = data.check();
        if (why != null) throw new IllegalArgumentException(data.key + ": " + why);
        Integer i = idOf.get(data.key);
        if (i != null && i <= AIR) throw new IllegalArgumentException(data.key + " is built in");
        Species s = data.withId(i != null ? i : byId.size());
        if (i != null) {
            byId.set(i, s);
            snapshot();
        } else add(s);
        placeholders.remove(s.key);
        return s;
    }

    /**
     * The id for a key read from a save: its species, or a placeholder (a non-volatile liquid of water's density) kept
     * under that key so the mass isn't lost, logged once.
     */
    public synchronized int forSave(String key) {
        Integer i = idOf.get(key);
        if (i != null) return i;
        Species s = new Species(byId.size(), key, Species.Kind.NONE, "unknown:" + key, Water.RHO_L, Water.CP_L, Water.CP_V,
            0.1, 0, 0, 0, 1e-3, 293, 1e-3, 293, Water.MU_V, 0x808080);
        add(s);
        if (placeholders.add(key)) org.slf4j.LoggerFactory.getLogger("eln").warn("Fluid species {} is in a save but not defined; kept as a plain liquid", key);
        return s.id;
    }
}
