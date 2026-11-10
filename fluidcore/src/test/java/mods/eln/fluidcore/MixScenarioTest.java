package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import static mods.eln.fluidcore.MixFluidTest.BUTADIENE;
import static mods.eln.fluidcore.MixFluidTest.DIESEL;
import static mods.eln.fluidcore.MixFluidTest.GASOLINE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plants with oils (10-species-plan.md test 8). */
class MixScenarioTest {
    static final double DT = 0.05, RHO_AIR = 1.2;

    static MixFluid mf(Network.Node n) {
        return (MixFluid) n.fluid;
    }

    /** An oil-water separator: water and gasoline settle; the floor outlet takes water, one in the gasoline layer gasoline. */
    @Test
    void separatorSplitsWaterAndGasoline() {
        Species[] sp = {Species.WATER, GASOLINE, Species.AIR};
        Network net = new Network();
        var tank = net.add(new Network.Node(0.1, 0, 1, false, sp).fillMasses(new double[]{30, 20, 2 * RHO_AIR * 0.04}, 300));
        // water fills to about 0.31 of the height, gasoline on top to about 0.58: outlets at 0 and 0.5
        var low = net.add(new Network.Node(0.05, -1, 1, false, sp).fillMasses(new double[]{0, 0, RHO_AIR * 0.05}, 300));
        var high = net.add(new Network.Node(0.05, -0.5, 1, false, sp).fillMasses(new double[]{0, 0, RHO_AIR * 0.05}, 300));
        net.connect(tank, 0, low, 1, 1e-6);
        net.connect(tank, 0.5, high, 1, 1e-6);
        for (int s = 0; s < 600; s++) net.step(DT);
        double lowWater = low.m[0] / (low.m[0] + low.m[1] + 1e-12), highGas = high.m[1] / (high.m[0] + high.m[1] + 1e-12);
        assertTrue(low.m[0] > 1, "water reached the low outlet: " + low.m[0]);
        assertTrue(high.m[1] > 1, "gasoline reached the high outlet: " + high.m[1]);
        assertTrue(lowWater > 0.95, "the low outlet's liquid is water: " + lowWater);
        assertTrue(highGas > 0.95, "the high outlet's liquid is gasoline: " + highGas);
        assertEquals(0, net.stats.failures, "failures");
    }

    /** A still: a heated pot of gasoline and diesel boils mostly gasoline over into a cooled condenser above it. */
    @Test
    void stillBoilsOffTheLightFraction() {
        Species[] sp = {GASOLINE, DIESEL, Species.AIR};
        Network net = new Network();
        var pot = net.add(new Network.Node(0.01, 0, 0.5, false, sp).fillMasses(new double[]{2, 2, RHO_AIR * 0.005}, 300));
        pot.heat = 10e3;
        var condenser = net.add(new Network.Node(0.01, 0.5, 0.5, false, sp).fillMasses(new double[]{0, 0, RHO_AIR * 0.01}, 300));
        var cold = net.add(new Network.Node(1, 1, 0.5, true, sp).fillMasses(new double[]{0, 0, RHO_AIR}, 290));
        net.connect(pot, 1, condenser, 0, 1e-6);
        var wall = net.connect(condenser, 1, cold, 0, 0);
        wall.conductance = 2000;
        double g0 = pot.m[0] + condenser.m[0], d0 = pot.m[1] + condenser.m[1];
        // (until half a kilogram has condensed: a batch still's first cut)
        MixFluid c = mf(condenser);
        for (int s = 0; s < 8000 && c.st.ml[0].v + c.st.ml[1].v < 0.5; s++) net.step(DT);
        double lg = c.st.ml[0].v, ld = c.st.ml[1].v;
        assertTrue(lg > 0.3, "gasoline condensed: " + lg + " kg (pot T " + pot.T() + ", condenser T " + condenser.T() + ")");
        assertTrue(lg / (lg + ld) > 0.7, "the distillate is mostly gasoline: " + lg / (lg + ld));
        assertEquals(g0, pot.m[0] + condenser.m[0], 1e-9 * g0, "gasoline kept");
        assertEquals(d0, pot.m[1] + condenser.m[1], 1e-9 * d0, "diesel kept");
        assertEquals(0, net.stats.failures, "failures");
    }

    /** Butadiene heated past its critical temperature stays two-phase (no supercritical end) and solvable. */
    @Test
    void butadieneHeatedPastItsCriticalPoint() {
        Species[] sp = {BUTADIENE, Species.AIR};
        Network net = new Network();
        var tank = net.add(new Network.Node(0.01, 0, 1, false, sp).fillMasses(new double[]{3, RHO_AIR * 0.005}, 300));
        tank.heat = 5e3;
        double maxP = 0;
        for (int s = 0; s < 4000; s++) {
            net.step(DT);
            maxP = Math.max(maxP, tank.P());
        }
        assertTrue(tank.T() > 440, "heated past 425 K: " + tank.T());
        assertTrue(mf(tank).st.ml[0].v > 0.1, "still two-phase: " + mf(tank).st.ml[0].v + " kg liquid");
        assertTrue(maxP < 1e7, "pressure " + maxP);
        assertEquals(0, net.stats.failures, "failures");
    }
}
