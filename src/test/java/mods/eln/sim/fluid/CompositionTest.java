package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The one-line contents of a fluid container for players (Baughn 2026-11-04: every share over 1% of the volume). */
public class CompositionTest {
    @Test
    void steamOverWaterShowsBoth() {
        // a 2 L pipe holding 1 L of water at 150 °C: a little steam, which made Jade say only "steam"
        FluidNode n = new FluidNode(0.002, 0, 1, false).fill(1.0 * Water.RHO_L / 1000, Water.psat(423), 423);
        String c = EaUnits.composition(n);
        assertTrue(c.matches("[0-9.]+ g of steam, 1\\.0 L of water"), c);
    }

    @Test
    void aTraceStaysOut() {
        // a full pipe of water: the bubble of vapour over it is far under 1%
        FluidNode n = new FluidNode(0.002, 0, 1, false).fill(0.002 * Water.RHO_L, Water.P_ATM, 300);
        assertEquals("2.0 L of water", EaUnits.composition(n));
    }

    @Test
    void otherSpeciesAlphabetically() {
        TestSpecies.defineAll();
        int oil = TestSpecies.id("diesel");
        double V = 0.01;
        FluidNode n = TestSpecies.node(V, 0, 1, 300, Water.P_ATM, SpeciesRegistry.WATER, 0.4 * V * Water.RHO_L, oil, 0.3 * V * 840);
        String c = EaUnits.composition(n);
        assertTrue(c.matches("[0-9.]+ g of air, 3\\.0 L of diesel, [0-9.]+ g of steam, 4\\.0 L of water"), c);
    }
}
