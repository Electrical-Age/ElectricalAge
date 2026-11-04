package mods.eln.node.voxel;

import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Species;
import mods.eln.sim.fluid.SpeciesRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A leak's jet by what it carries: hot oil scalds by its own enthalpy over 55 C, warm oil doesn't; it sprays in its colour. */
public class SteamJetSpeciesTest {
    static int oil() {
        int id = SpeciesRegistry.get().id("test:jet_oil");
        if (id >= 0) return id;
        return SpeciesRegistry.get().define(new Species(-1, "test:jet_oil", Species.Kind.NONE, "test_jet", 900, 2000, 1500, 0.3,
            0, 0, 0, 1e-2, 293, 1e-2, 293, 1e-5, 0x123456)).id;
    }

    static FluidEdge jet(int id, double kgPerS, double T) {
        FluidEdge e = new FluidEdge(new FluidNode(1, 0, 1, true), 0, new FluidNode(1, 0, 1, true), 0, 1e-12);
        e.fSpecies = new double[id + 1];
        e.fSpecies[id] = kgPerS;
        e.fEnthalpy = kgPerS * SpeciesRegistry.get().byId(id).hLiquid(T);
        return e;
    }

    @Test
    void hotOilScaldsWarmOilDoesnt() {
        int id = oil();
        assertTrue(SteamJet.heatFlux(jet(id, 0.1, 473)) > 20000, "0.1 kg/s of oil at 200 C");
        assertEquals(0, SteamJet.heatFlux(jet(id, 0.1, 313)), 1e-9, "at 40 C");
    }

    @Test
    void liquidOilSpraysInItsColourAsALiquid() {
        int id = oil();
        FluidEdge e = jet(id, 0.1, 473);
        assertEquals(0, SteamJet.gasShare(e), 1e-12);
        assertEquals(0x123456, SteamJet.liquidColour(e));
    }
}
