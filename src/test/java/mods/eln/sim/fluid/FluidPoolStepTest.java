package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A tick's networks solved on the pool (CoreStep.solveAll: begin, run on several threads, finish) end bit for bit where
 * the same networks stepped one by one do; and what a solve throws is thrown at its own network's finish.
 */
public class FluidPoolStepTest {
    static final double DT = 0.05;

    /** A small plant, different for each k: a hot wet vessel, a cool one, a third above them, pipes between. */
    static FluidNetwork plant(int k) {
        FluidNetwork net = new FluidNetwork();
        FluidNode a = net.add(new FluidNode(0.01 + 0.001 * k, 0, 0.2, false).fill(0.5 + 0.1 * k, 3e5 + 1e4 * k, 380 + k));
        FluidNode b = net.add(new FluidNode(0.02, 0, 0.3, false).fill(0.004, 1e5, 300 + 2 * k));
        FluidNode c = net.add(new FluidNode(0.005, 0.5, 0.2, false).fill(0.001 * (1 + k % 3), 1.5e5, 330));
        net.connect(a, 0.9, b, 0.5, 1e-9);
        net.connect(b, 0.1, c, 0.1, 2e-10 * (1 + k % 4));
        net.connect(a, 0.05, c, 0.9, 5e-11);
        a.heat = 50 * k;
        return net;
    }

    @Test
    void pooledTicksEndWhereSerialOnesDo() {
        int n = 24;
        List<FluidNetwork> serial = new ArrayList<>(), pooled = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            serial.add(plant(k));
            pooled.add(plant(k));
        }
        int ran = 0;
        try (var pool = new mods.eln.fluidcore.FluidPool(4)) {
            for (int tick = 0; tick < 200; tick++) {
                for (int k = 0; k < n; k++) {
                    // (devices' heat each tick, as the game adds it between steps)
                    serial.get(k).nodes.get(0).heat += 20 + k;
                    pooled.get(k).nodes.get(0).heat += 20 + k;
                }
                for (FluidNetwork net : serial) net.step(DT);
                List<FluidNetwork> begun = new ArrayList<>();
                for (FluidNetwork net : pooled) if (net.begin(DT)) begun.add(net);
                if (CoreStep.solveAll(begun, pool, 0)) ran++;
                for (FluidNetwork net : pooled) net.finish();
            }
        }
        assertEquals(200, ran, "every tick went through the pool");
        for (int k = 0; k < n; k++)
            for (int i = 0; i < 3; i++) {
                FluidNode s = serial.get(k).nodes.get(i), p = pooled.get(k).nodes.get(i);
                assertEquals(s.energy, p.energy, 0.0, "plant " + k + " node " + i + " energy");
                assertEquals(s.mWater, p.mWater, 0.0, "plant " + k + " node " + i + " water");
                assertEquals(s.mAir, p.mAir, 0.0, "plant " + k + " node " + i + " air");
            }
        assertTrue(Math.abs(serial.get(3).nodes.get(0).P() - (3e5 + 3e4)) > 1, "and they moved");
    }

    @Test
    void whatASolveThrowsIsThrownAtItsNetworksFinish() {
        List<FluidNetwork> nets = new ArrayList<>();
        for (int k = 0; k < 6; k++) nets.add(plant(k));
        for (FluidNetwork net : nets) net.step(DT);
        double u0 = nets.get(4).nodes.get(0).energy;
        // (a core node gone: network 2's load fails, on whichever thread takes it)
        nets.get(2).core.cn[0] = null;
        try (var pool = new mods.eln.fluidcore.FluidPool(3)) {
            List<FluidNetwork> begun = new ArrayList<>();
            for (FluidNetwork net : nets) if (net.begin(DT)) begun.add(net);
            assertTrue(CoreStep.solveAll(begun, pool, 0));
            nets.get(0).finish();
            nets.get(1).finish();
            assertThrows(RuntimeException.class, () -> nets.get(2).finish());
            for (int k = 3; k < 6; k++) nets.get(k).finish();   // (the others' steps are whole)
        }
        assertTrue(nets.get(4).nodes.get(0).energy != u0, "the others stepped");
    }
}
