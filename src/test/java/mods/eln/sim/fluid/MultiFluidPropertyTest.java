package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Property tests for networks holding more species than water and air: random networks as FluidPropertyTest's, whose
 * nodes also hold random oils (layers over water, Raoult mixes, a non-volatile residue heavier than water) and natural
 * gas. Stepped a while, they must keep the books for every species and for energy, stay finite, never hold a negative
 * mass, and step deterministically. Each case's seed is in the failure message.
 */
public class MultiFluidPropertyTest {
    static final int CASES = Integer.getInteger("fluid.cases", 200) / 2;
    static final double DT = 0.05;
    static final String[] OILS = {"crude_oil", "gasoline", "diesel", "heavy_oil", "residue", "natural_gas"};

    static FluidPropertyTest.Case random(long seed, boolean closed) {
        TestSpecies.defineAll();
        FluidPropertyTest.Case c = FluidPropertyTest.random(seed, closed);
        Random r = new Random(seed * 7919);
        for (FluidNode n : c.free()) {
            int k = r.nextInt(3);
            for (int j = 0; j < k; j++) {
                String name = OILS[r.nextInt(OILS.length)];
                int id = TestSpecies.id(name);
                Species s = SpeciesRegistry.get().byId(id);
                // (liquids up to what leaves the node some gas space: an overfull node starts far into its stiff range)
                double free = Math.max(0, 0.9 * n.volume - n.liquidVolume());
                double kg = s.gas() ? r.nextDouble() * 5 * n.volume : r.nextDouble() * 0.5 * free * s.rhoL;
                double T = n.T();
                n.addMass(id, kg);
                n.energy += kg * (s.gas() ? s.uVapour(T) : s.uLiquid(T));
                n.update();
            }
        }
        return c;
    }

    static double speciesSum(List<FluidNode> ns, int id) {
        double s = 0;
        for (FluidNode n : ns) s += n.mass(id);
        return s;
    }

    static int bound(List<FluidNode> ns) {
        int b = SpeciesRegistry.get().size();
        for (FluidNode n : ns) b = Math.max(b, n.speciesBound());
        return b;
    }

    static void sane(FluidPropertyTest.Case c, long seed, int step) {
        for (FluidNode n : c.free()) {
            String at = "seed " + seed + " step " + step + " node V " + n.volume;
            if (!(Double.isFinite(n.P()) && Double.isFinite(n.T()) && Double.isFinite(n.energy))) fail("not finite: " + at);
            for (int i = 0; i < n.speciesBound(); i++)
                assertTrue(n.mass(i) >= -1e-12 * Math.max(1, n.mass()), "negative mass of " + SpeciesRegistry.get().byId(i).key + ": " + n.mass(i) + ": " + at);
            assertTrue(n.P() >= 0, "negative pressure " + n.P() + ": " + at);
        }
    }

    @Test
    void openNetworksKeepTheBooks() {
        for (long seed = 1; seed <= CASES; seed++) {
            FluidPropertyTest.Case c = random(seed, false);
            int nb = bound(c.free());
            double[] m0 = new double[nb], out = new double[nb];
            for (int i = 0; i < nb; i++) m0[i] = speciesSum(c.free(), i);
            double u0 = FluidPropertyTest.sum(c.free(), n -> n.energy);
            double heatIn = 0, outH = 0, loss = 0;
            List<FluidEdge> leaving = new ArrayList<>(c.turbines());
            leaving.addAll(c.vents());
            for (int s = 0; s < 400; s++) {
                for (int i = 0; i < c.free().size(); i++) {
                    FluidNode n = c.free().get(i);
                    n.heat = c.heat()[i];
                    heatIn += c.heat()[i] * DT;
                    loss += n.lossWPerK * (n.T() - Water.T_AMBIENT) * DT;
                }
                c.net().step(DT);
                for (FluidEdge e : leaving) {
                    outH += e.fEnthalpy * DT;
                    if (e.fSpecies != null) for (int i = 0; i < Math.min(nb, e.fSpecies.length); i++) out[i] += e.fSpecies[i] * DT;
                    else {
                        out[SpeciesRegistry.WATER] += (e.fLiquid + e.fVapour) * DT;
                        out[SpeciesRegistry.AIR] += e.fAir * DT;
                    }
                }
                sane(c, seed, s);
            }
            for (int i = 0; i < nb; i++)
                assertEquals(m0[i] - out[i], speciesSum(c.free(), i), 1e-9 * Math.max(1e-3, m0[i]), SpeciesRegistry.get().byId(i).key + ", seed " + seed);
            double u1 = FluidPropertyTest.sum(c.free(), n -> n.energy);
            double scale = Math.max(1e3, Math.abs(u0) + heatIn + Math.abs(outH));
            assertEquals(u0 + heatIn - loss - outH, u1, 1e-7 * scale, "energy, seed " + seed);
        }
    }

    @Test
    void theCasesHoldLayersAndMixes() {
        int layered = 0, mixed = 0;
        for (long seed = 1; seed <= CASES; seed++)
            for (FluidNode n : random(seed, false).free()) {
                if (n.state.layers >= 2) layered++;
                int liquids = 0;
                for (int i = 2; i < n.speciesBound(); i++) if (n.liquid(i) > 0) liquids++;
                if (liquids >= 2) mixed++;
            }
        assertTrue(layered >= 10 && mixed >= 10, "layered " + layered + ", mixed " + mixed);
    }

    @Test
    void closedNetworksConserve() {
        for (long seed = 1001; seed < 1001 + CASES; seed++) {
            FluidPropertyTest.Case c = random(seed, true);
            int nb = bound(c.free());
            double[] m0 = new double[nb];
            for (int i = 0; i < nb; i++) m0[i] = speciesSum(c.free(), i);
            double u0 = FluidPropertyTest.sum(c.free(), n -> n.energy);
            for (int s = 0; s < 1500; s++) {
                c.net().step(DT);
                if (s % 50 == 0) sane(c, seed, s);
            }
            for (int i = 0; i < nb; i++)
                assertEquals(m0[i], speciesSum(c.free(), i), 1e-9 * Math.max(1e-3, m0[i]), SpeciesRegistry.get().byId(i).key + ", seed " + seed);
            assertEquals(u0, FluidPropertyTest.sum(c.free(), n -> n.energy), 1e-7 * Math.max(1e3, Math.abs(u0)), "energy, seed " + seed);
        }
    }

    @Test
    void stepsAreDeterministic() {
        for (long seed = 5001; seed < 5011; seed++) {
            FluidPropertyTest.Case a = random(seed, false), b = random(seed, false);
            for (int s = 0; s < 200; s++) {
                for (int i = 0; i < a.free().size(); i++) {
                    a.free().get(i).heat = a.heat()[i];
                    b.free().get(i).heat = b.heat()[i];
                }
                a.net().step(DT);
                b.net().step(DT);
            }
            for (int i = 0; i < a.free().size(); i++) assertEquals(a.free().get(i).energy, b.free().get(i).energy, 0.0, "seed " + seed);
        }
    }
}
