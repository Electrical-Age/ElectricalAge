package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A prescribed flow (FluidEdge.Kind.FLOW) through CoreStep with a species other than water and air. */
class FlowSourceTest {
    /**
     * A source declaring natural gas into a closed node of air: the core carries the species from the first step (its
     * mix is declared while the rate is 0, so turning it on doesn't rebuild the core), and the node gains exactly rate x
     * time of it, with each kg's enthalpy as a gas at the source's temperature.
     */
    @Test
    void aSourceOfAThirdSpeciesAddsExactlyItsRateWithoutARebuild() {
        int gas = TestSpecies.id("natural_gas");
        FluidNetwork net = new FluidNetwork();
        FluidNode atm = net.add(FluidNode.atmosphere(1));
        FluidNode v = net.add(new FluidNode(0.01, 0, 1, false).fill(0, Water.P_ATM, 293.15));
        FluidEdge e = net.connect(atm, 0, v, 0.5, 0).kind(FluidEdge.Kind.FLOW);
        e.shares = new double[gas + 1];
        e.shares[gas] = 1;
        e.kelvin = 350;
        double dt = 0.05, rate = 1e-4;
        for (int s = 0; s < 10; s++) net.step(dt);
        assertEquals(1, net.builds, "core builds while off");
        assertEquals(0, v.extra == null ? 0 : v.extra[gas - 2], 0, "no gas while off");
        double u0 = v.energy, a0 = v.mAir;
        e.rate = rate;
        int steps = 40;
        for (int s = 0; s < steps; s++) {
            net.step(dt);
            assertEquals(rate, e.flow, 1e-15, "delivered, step " + s);
            assertEquals(rate, e.fSpecies[gas], 1e-15, "delivered gas, step " + s);
        }
        double h = SpeciesRegistry.get().byId(gas).hVapour(350);
        assertEquals(1, net.builds, "core builds after turning it on");
        assertEquals(rate * dt * steps, v.extra[gas - 2], 1e-15, "gas gained");
        assertEquals(a0, v.mAir, 1e-15, "air unchanged");
        assertEquals(rate * h, e.fEnthalpy, 1e-9 * rate * Math.abs(h), "enthalpy delivered");
        assertEquals(rate * dt * steps * h, v.energy - u0, 1e-9 * Math.abs(rate * dt * steps * h) + 1e-9, "energy gained");
    }
}
