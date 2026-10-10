package mods.eln.sim;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Simulator.tick interleaves electrical steps (electricalPeriod) and fast thermal steps (thermalPeriod) inside
 * each 50 ms server tick, then runs one slow thermal step and the slow processes. Simulated time must track
 * game time: over N ticks every list advances by N * 0.05 s.
 */
public class SimulatorTimingTest {

    static class Counter implements IProcess {
        int calls;
        double time;

        @Override
        public void process(double dt) {
            calls++;
            time += dt;
        }
    }

    @Test
    public void simulatedTimeTracksGameTime() {
        SimHarness h = new SimHarness();
        Counter el = new Counter(), fast = new Counter(), slow = new Counter(), slowProc = new Counter(), pre = new Counter();
        h.sim.addElectricalProcess(el);
        h.sim.addThermalFastProcess(fast);
        h.sim.addThermalSlowProcess(slow);
        h.sim.addSlowProcess(slowProc);
        h.sim.addSlowPreProcess(pre);
        int n = 10000;
        h.ticks(n);
        double t = n * SimHarness.TICK;
        assertEquals(t, el.time, 2 * SimHarness.ELECTRICAL_PERIOD);
        assertEquals(t, fast.time, 2 * SimHarness.THERMAL_PERIOD);
        assertEquals(t, slow.time, 1e-6);
        assertEquals(t, slowProc.time, 1e-6);
        assertEquals(t, pre.time, 1e-6);
        assertEquals(n, el.calls, 1);
        assertEquals(20 * n, fast.calls, 1);
    }

    /** Per-tick regularity: every tick runs exactly one electrical step and 20 fast thermal steps (no jitter). */
    @Test
    public void stepsPerTickAreRegular() {
        SimHarness h = new SimHarness();
        Counter el = new Counter(), fast = new Counter();
        h.sim.addElectricalProcess(el);
        h.sim.addThermalFastProcess(fast);
        h.tick(); // first tick may include the t=0 steps
        for (int i = 0; i < 2000; i++) {
            int e0 = el.calls, f0 = fast.calls;
            h.tick();
            assertEquals("electrical steps in tick " + i, 1, el.calls - e0);
            assertEquals("fast thermal steps in tick " + i, 20, fast.calls - f0);
        }
    }
}
