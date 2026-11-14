package mods.eln.sim.fluid;

import com.google.gson.JsonParser;
import mods.eln.fluid.SpeciesLoader;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** EA's built-in species data (the datapack JSON), defined in the registry for unit tests. */
final class TestSpecies {
    static final String[] NAMES = {"crude_oil", "diesel", "gasoline", "heavy_oil", "natural_gas", "residue", "butadiene", "latex"};

    private TestSpecies() {
    }

    /** Define every built-in species (idempotent: a redefinition keeps its id); returns the id of eln:name. */
    static int id(String name) {
        defineAll();
        return SpeciesRegistry.get().id("eln:" + name);
    }

    static synchronized void defineAll() {
        for (String n : NAMES) {
            if (SpeciesRegistry.get().id("eln:" + n) >= 0) continue;
            try (var in = TestSpecies.class.getResourceAsStream("/data/eln/eln/fluid_species/" + n + ".json")) {
                if (in == null) throw new IllegalStateException("no data for " + n);
                var j = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                SpeciesRegistry.get().define(SpeciesLoader.parse("eln:" + n, j));
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** A node holding these masses (species id, kg pairs) at T, gas space made up with air to p if there is room. */
    static FluidNode node(double V, double z, double h, double T, double airPa, double... idKg) {
        FluidNode n = new FluidNode(V, z, h, false);
        double vLiq = 0;
        for (int i = 0; i < idKg.length; i += 2) {
            int id = (int) idKg[i];
            n.addMass(id, idKg[i + 1]);
            Species s = SpeciesRegistry.get().byId(id);
            if (!s.gas()) vLiq += idKg[i + 1] / s.rhoL;
        }
        n.mAir += airPa * Math.max(V - vLiq, Flash.GAS_MIN * V) / (Water.R_AIR * T);
        n.energy = n.energyAt(T);
        n.update();
        return n;
    }
}
