package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A fluid step whose result isn't finite (a solver bug, made here by CoreStep.testAfterStep) is a failed step, put
 * back, never healed into the nodes, its devices' heat given to the next step; in a row, from the second on, its
 * network reads as stalled; and it steps again once the cause goes. (A device's NaN heat doesn't get this far: the
 * core's solve fails on it and keeps its start.)
 */
public class CoreStepNonFiniteTest {
    static final double DT = 0.05;

    @Test
    void nonFiniteStepIsPutBackAndStalls() {
        FluidNetwork net = new FluidNetwork();
        FluidNode a = net.add(new FluidNode(0.01, 0, 0.2, false).fill(0.004, 3e5, 330));
        FluidNode b = net.add(new FluidNode(0.01, 0, 0.2, false).fill(0.004, 2e5, 330));
        net.connect(a, 0.5, b, 0.5, 1e-11);
        for (int i = 0; i < 20; i++) net.step(DT);
        double ua = a.energy, ub = b.energy, ta = a.T();
        long failed0 = CoreStep.failedSteps;
        CoreStep.testAfterStep = n -> n.nodes.get(0).U = Double.NaN;
        a.heat = 5;
        net.step(DT);
        assertTrue(Double.isFinite(a.energy) && Double.isFinite(b.energy), a.energy + " " + b.energy);
        assertEquals(ua, a.energy, 1e-9 * Math.abs(ua), "put back, not healed");
        assertEquals(ub, b.energy, 1e-9 * Math.abs(ub));
        assertEquals(ta, a.T(), 1e-9);
        assertEquals(1, net.core.nonFiniteRun);
        assertEquals(5, a.heat, 1e-12, "its heat given to the next step");
        assertTrue(CoreStep.failedSteps > failed0, "counted as a failed step");
        assertEquals(0, a.stallNow, 0, "one put back isn't a stall yet");
        // the same again: now a stall
        net.step(DT);
        assertEquals(2, net.core.nonFiniteRun);
        assertTrue(a.stallNow > 0, "stalled from the second in a row");
        assertEquals(ua, a.energy, 1e-9 * Math.abs(ua));
        // the cause gone, it steps again
        CoreStep.testAfterStep = null;
        for (int i = 0; i < 20; i++) net.step(DT);
        assertEquals(0, net.core.nonFiniteRun);
        assertEquals(0, a.stallNow, 0);
        assertTrue(Math.abs(a.P() - b.P()) < Math.abs(3e5 - 2e5), "it moved on");
    }
}
