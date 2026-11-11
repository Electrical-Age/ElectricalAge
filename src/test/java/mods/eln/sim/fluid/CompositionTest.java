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
        assertTrue(c.matches("[0-9.]+ g steam, 14\\.[0-9] B water"), c);   // (1 L is 14193 mB at 958 kg/m³: 14.2 B)
    }

    @Test
    void aTraceStaysOut() {
        // a full pipe of water: the bubble of vapour over it is far under 1%
        FluidNode n = new FluidNode(0.002, 0, 1, false).fill(0.002 * Water.RHO_L, Water.P_ATM, 300);
        assertEquals(String.format("%.1f B water", 0.002 * Water.RHO_L / EaUnits.KG_PER_MB_WATER / 1000), EaUnits.composition(n));
    }

    @Test
    void underABucketInMillibuckets() {
        // 500 mB of water in a 2 L pipe (1.7 % of it): under a bucket, in mB
        double kg = 500 * EaUnits.KG_PER_MB_WATER;
        FluidNode n = new FluidNode(0.002, 0, 1, false).fill(kg, Water.P_ATM, 300);
        assertTrue(EaUnits.composition(n).matches("[0-9.]+ g air, ([0-9.]+ g steam, )?500 mB water"), EaUnits.composition(n));
    }

    @Test
    void otherSpeciesAlphabetically() {
        TestSpecies.defineAll();
        int oil = TestSpecies.id("diesel");
        double V = 0.01;
        FluidNode n = TestSpecies.node(V, 0, 1, 300, Water.P_ATM, SpeciesRegistry.WATER, 0.4 * V * Water.RHO_L, oil, 0.3 * V * 840);
        String c = EaUnits.composition(n);
        assertTrue(c.matches("[0-9.]+ g air, 42\\.[0-9] B diesel, [0-9.]+ g steam, 56\\.[0-9] B water"), c);
    }
}
