package mods.eln.fluid;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mods.eln.sim.fluid.Species;
import mods.eln.sim.fluid.SpeciesRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Fluid species from datapacks (Baughn 2026-11-03: fluids are data): {@code data/<ns>/eln/fluid_species/<name>.json}
 * defines species {@code <ns>:<name>}. A modpack can add one or retune EA's (water and air are built in). Fields:
 * <pre>
 * kind                 "curve" (boils by Clausius-Clapeyron), "none" (never boils), "gas" (never condenses)
 * group                miscibility: liquids of one group mix into one layer; groups layer by density
 * density              liquid density, kg/m^3 (gas: ignored but required positive)
 * cp_liquid            J/kg/K
 * cp_vapour            J/kg/K (gas: its cv)
 * molar_mass           kg/mol
 * boiling_point        K at 1 atm (curve)
 * latent_heat          J/kg at the boiling point (curve)
 * critical_temperature K (curve)
 * viscosity            liquid Pa s: a number, or [[T1, mu1], [T2, mu2]] (an Andrade fit between them)
 * vapour_viscosity     Pa s
 * colour               "#RRGGBB"
 * fluids               {"liquid": [fluid ids], "vapour": [fluid ids]}: the NeoForge fluids that are this species
 * flammable            optional {"flash_point": K, "autoignition": K}: a leak catches from a flame over its flash
 *                      point, by itself over its autoignition temperature
 * </pre>
 * A file that fails to parse or check is skipped with a log line; the species keeps its previous data, if any.
 */
public final class SpeciesLoader extends SimplePreparableReloadListener<Map<String, JsonObject>> {
    public static final String DIRECTORY = "eln/fluid_species";

    /** The NeoForge fluids (ids) each species is, as liquid and as vapour (by species key), from the last load. */
    private static volatile Map<String, List<String>> liquidFluids = Map.of(), vapourFluids = Map.of();

    public static void onAddListeners(AddServerReloadListenersEvent e) {
        e.addListener(Identifier.fromNamespaceAndPath("eln", "fluid_species"), new SpeciesLoader());
    }

    /** The fluid ids that are species key as a liquid (empty if none). */
    public static List<String> liquidFluids(String key) {
        return liquidFluids.getOrDefault(key, List.of());
    }

    /** The fluid ids that are species key as a vapour (empty if none). */
    public static List<String> vapourFluids(String key) {
        return vapourFluids.getOrDefault(key, List.of());
    }

    @Override
    protected Map<String, JsonObject> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, JsonObject> out = new TreeMap<>();   // (sorted: ids are given in key order, the same every load)
        for (var e : manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier file = e.getKey();
            String path = file.getPath().substring(DIRECTORY.length() + 1, file.getPath().length() - ".json".length());
            try (Reader r = e.getValue().openAsReader()) {
                out.put(file.getNamespace() + ":" + path, JsonParser.parseReader(r).getAsJsonObject());
            } catch (Exception ex) {
                mods.eln.Eln.LOG.warn("Fluid species {}: unreadable ({})", file, ex.toString());
            }
        }
        return out;
    }

    @Override
    protected void apply(Map<String, JsonObject> data, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, List<String>> liq = new HashMap<>(), vap = new HashMap<>();
        int n = 0;
        for (var e : data.entrySet()) {
            try {
                Species s = parse(e.getKey(), e.getValue());
                SpeciesRegistry.get().define(s);
                JsonObject f = e.getValue().has("fluids") ? e.getValue().getAsJsonObject("fluids") : new JsonObject();
                liq.put(s.key, strings(f.get("liquid")));
                vap.put(s.key, strings(f.get("vapour")));
                n++;
            } catch (Exception ex) {
                mods.eln.Eln.LOG.warn("Fluid species {}: skipped ({})", e.getKey(), ex.getMessage());
            }
        }
        liquidFluids = Map.copyOf(liq);
        vapourFluids = Map.copyOf(vap);
        mods.eln.Eln.LOG.info("Fluid species: {} loaded, {} known", n, SpeciesRegistry.get().size());
    }

    /** One species from its JSON (the id is a placeholder: the registry gives it one). */
    public static Species parse(String key, JsonObject j) {
        Species.Kind kind = switch (req(j, "kind").getAsString()) {
            case "curve" -> Species.Kind.CURVE;
            case "none" -> Species.Kind.NONE;
            case "gas" -> Species.Kind.GAS;
            default -> throw new IllegalArgumentException("kind must be curve, none or gas");
        };
        boolean curve = kind == Species.Kind.CURVE, gas = kind == Species.Kind.GAS;
        double mu1, t1, mu2, t2;
        JsonElement v = j.get("viscosity");
        if (v == null) {
            if (!gas) throw new IllegalArgumentException("viscosity is missing");
            mu1 = mu2 = 1e-3;
            t1 = t2 = 293;
        } else if (v.isJsonArray()) {
            JsonArray a = v.getAsJsonArray();
            if (a.size() != 2) throw new IllegalArgumentException("viscosity: two [T, mu] points");
            t1 = a.get(0).getAsJsonArray().get(0).getAsDouble();
            mu1 = a.get(0).getAsJsonArray().get(1).getAsDouble();
            t2 = a.get(1).getAsJsonArray().get(0).getAsDouble();
            mu2 = a.get(1).getAsJsonArray().get(1).getAsDouble();
        } else {
            mu1 = mu2 = v.getAsDouble();
            t1 = t2 = 293;
        }
        int colour = 0x808080;
        if (j.has("colour")) {
            String c = j.get("colour").getAsString();
            colour = Integer.parseInt(c.startsWith("#") ? c.substring(1) : c, 16);
        }
        Species s = new Species(-1, key, kind, req(j, "group").getAsString(), req(j, "density").getAsDouble(),
            gas ? 0 : req(j, "cp_liquid").getAsDouble(), req(j, "cp_vapour").getAsDouble(), req(j, "molar_mass").getAsDouble(),
            curve ? req(j, "boiling_point").getAsDouble() : 0, curve ? req(j, "latent_heat").getAsDouble() : 0,
            curve ? req(j, "critical_temperature").getAsDouble() : 0, mu1, t1, mu2, t2,
            j.has("vapour_viscosity") ? j.get("vapour_viscosity").getAsDouble() : 1e-5, colour);
        if (j.has("flammable")) {
            JsonObject f = j.getAsJsonObject("flammable");
            s.autoignition = req(f, "autoignition").getAsDouble();
            s.flashPoint = f.has("flash_point") ? f.get("flash_point").getAsDouble() : 0;
            if (!(s.autoignition > s.flashPoint)) throw new IllegalArgumentException("autoignition must be over the flash point");
        }
        return s;
    }

    private static JsonElement req(JsonObject j, String field) {
        JsonElement e = j.get(field);
        if (e == null) throw new IllegalArgumentException(field + " is missing");
        return e;
    }

    private static List<String> strings(JsonElement e) {
        List<String> out = new ArrayList<>();
        if (e != null && e.isJsonArray()) for (JsonElement s : e.getAsJsonArray()) out.add(s.getAsString());
        return List.copyOf(out);
    }
}
