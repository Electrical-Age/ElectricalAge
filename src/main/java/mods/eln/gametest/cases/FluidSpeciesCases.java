package mods.eln.gametest.cases;

import mods.eln.fluid.SpeciesLoader;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sim.fluid.SpeciesRegistry;

import java.util.List;

/** The fluid species EA ships as datapack JSON (Baughn 2026-11-03: fluids are data) load in a real server. */
public final class FluidSpeciesCases {
    private FluidSpeciesCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Loaded());
    }

    static final class Loaded implements TestCase {
        public String id() {
            return "fluid_species_loaded";
        }

        public int width() {
            return 2;
        }

        public int ticks() {
            return 2;
        }

        public void build(TestContext c) {
        }

        public void measure(TestContext c) {
            SpeciesRegistry reg = SpeciesRegistry.get();
            for (String n : new String[]{"crude_oil", "gasoline", "diesel", "heavy_oil", "residue", "natural_gas"}) {
                int id = reg.id("eln:" + n);
                c.check("fluid species: eln:" + n + " is defined", id >= 2, "id " + id);
                if (id >= 2) c.check("fluid species: eln:" + n + " passes its checks", reg.byId(id).check() == null, "" + reg.byId(id).check());
            }
            c.check("fluid species: water and air are built in", reg.id("eln:water") == 0 && reg.id("eln:air") == 1, "");
            c.check("fluid species: crude oil is the eln:crude_oil fluid", SpeciesLoader.liquidFluids("eln:crude_oil").contains("eln:crude_oil"),
                "" + SpeciesLoader.liquidFluids("eln:crude_oil"));
            c.check("fluid species: natural gas is the eln:naturalgas fluid as a gas", SpeciesLoader.vapourFluids("eln:natural_gas").contains("eln:naturalgas"),
                "" + SpeciesLoader.vapourFluids("eln:natural_gas"));
        }
    }
}
