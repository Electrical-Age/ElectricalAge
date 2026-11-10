package mods.eln.oil;

import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNetwork;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Baughn's base on Incognito (2026-11-04): a condenser's pump feeds a heated boiler through a liquid-full pipe (a little
 * air left in it) and the boiler's feed check valve, the boiler's steam drawn off. Its pipe flipped between 11 and 13 bar
 * every step: the feed valve, shut for a whole step on a drive a few Pa backwards at its start, let the pump raise the
 * stiff pipe 2 bar; open, it drained it a few Pa under again. Numbers from a trace of the live network (fluidtrace).
 */
public class BoilerFeedTest {
    @org.junit.jupiter.api.Disabled("the pump law (open and boost from last tick's back pressure) runs outside the solve and swings the stiff pipe; returns with the fluid core's PUMP component (end-state P1)")
    @Test
    void theFeedPipeHoldsOnePressure() {
        FluidNetwork net = new FluidNetwork();
        FluidNode vessel = net.add(new FluidNode(0.00085, 143, 1, true).fill(0.0096, 0.04e5, 302));
        FluidNode pipe = net.add(new FluidNode(0.0006, 141.5, 2.06, false));
        pipe.mWater = 0.563535;
        pipe.mAir = 4.01e-5;
        pipe.energy = 70109.5;
        pipe.update();
        FluidNode boiler = net.add(new FluidNode(0.008, 143, 1, false));
        boiler.mWater = 0.298965;
        boiler.energy = 304945;
        boiler.update();
        FluidNode sink = net.add(new FluidNode(1, 144, 1, true).fill(0, 1e5, 400));
        CondensatePump curve = CondensatePump.forTurbine(10e5, 3.7e-3);
        double speed = 0.76;
        FluidEdge pump = net.connect(vessel, 0, pipe, 0.727, curve.freeKgPerS / curve.shutoffPa * Water.MU_L / Water.RHO_L).kind(FluidEdge.Kind.CHECK);
        FluidEdge feed = net.connect(pipe, 0.873, boiler, 0.3, 9.908e-12).kind(FluidEdge.Kind.CHECK);
        // (the turbine's draw: steam off the boiler's top into a fixed exhaust, ~4 g/s at 11 bar)
        net.connect(boiler, 1, sink, 0, 1.07e-14);
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE, fed = 0, pumped = 0;
        int steps = 4000, last = 400;
        for (int t = 0; t < steps; t++) {
            double back = pipe.pressureAt(0.727) - vessel.pressureAt(0);
            pump.open = speed * curve.lineShare(back);
            pump.boost = curve.boost(back);
            boiler.heat = 10.6e3;
            net.step(0.05);
            if (t >= steps - last) {
                lo = Math.min(lo, pipe.P());
                hi = Math.max(hi, pipe.P());
                fed += feed.flow / last;
                pumped += pump.flow / last;
            }
        }
        String what = String.format("pipe %.3f..%.3f bar, boiler %.3f bar, pumped %.5f, fed %.5f kg/s", lo / 1e5, hi / 1e5, boiler.P() / 1e5, pumped, fed);
        assertTrue(hi - lo < 0.2e5, "swings: " + what);
        assertTrue(fed > 0 && Math.abs(pumped - fed) < 0.02 * pumped, "flow builds up: " + what);
        System.out.println("boiler feed: " + what);
    }
}
