package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Still Pot's entrainment, outside the fluid core's solve: liquid moved into the neck keeps mass and energy exactly. */
class StillPotCarryTest {
    @Test
    void carryConservesMassAndEnergyAndKeepsTheMix() {
        int gas = TestSpecies.id("gasoline"), res = TestSpecies.id("residue"), dsl = TestSpecies.id("diesel");
        FluidNode pot = TestSpecies.node(0.002, 0, 2, 480, Water.P_ATM, gas, 0.1, dsl, 0.3, res, 0.2, SpeciesRegistry.WATER, 0.05);
        FluidNode neck = TestSpecies.node(3e-5, 2, 0.25, 300, Water.P_ATM);
        double m0 = pot.mass() + neck.mass(), u0 = pot.energy + neck.energy, resShare = pot.liquid(res) / (pot.liquid(gas) + pot.liquid(dsl) + pot.liquid(res) + pot.liquid(0));
        double moved = mods.eln.oil.StillPotElement.carry(pot, neck, 0.01);
        assertEquals(0.01, moved, 1e-12);
        assertEquals(m0, pot.mass() + neck.mass(), 1e-12 * m0);
        assertEquals(u0, pot.energy + neck.energy, 1e-9 * Math.abs(u0));
        assertEquals(resShare * 0.01, neck.mass(res), 1e-9, "residue by its share of the liquid");
        // never more than half the pot's liquid
        FluidNode dry = TestSpecies.node(0.002, 0, 2, 480, Water.P_ATM, res, 0.001);
        assertTrue(mods.eln.oil.StillPotElement.carry(dry, neck, 1) <= 0.0005 + 1e-12);
    }
}
