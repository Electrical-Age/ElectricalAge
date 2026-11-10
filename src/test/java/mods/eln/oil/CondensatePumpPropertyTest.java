package mods.eln.oil;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property test (Baughn 2026-11-03: "check against physics"): a condensate pump stepped as the game steps it, into a
 * random boiler pressure through a random run of liquid-full pipe and a random feed conductance, must settle where the
 * physics puts it: one steady pressure (no swing from step to step), the pump's flow on its curve at the back-pressure
 * it settles against, that flow all going into the boiler (nothing builds up in a full pipe), and no flow at or past the
 * shut-off head. The swing Baughn saw on live (a condenser's outlet pipe at 10.5..12.5 bar) fails the first; each case's
 * seed is in the failure message.
 */
public class CondensatePumpPropertyTest {
    static final int CASES = Integer.getInteger("fluid.cases", 200);

    @org.junit.jupiter.api.Disabled("the pump law runs outside the solve; returns with the fluid core's PUMP component (end-state P1)")
    @Test
    void settlesOnItsCurve() {
        for (long seed = 1; seed <= CASES; seed++) {
            Random r = new Random(seed);
            double h0 = 3e5 + 47e5 * r.nextDouble();                       // shut-off 3 .. 50 bar
            double q0 = Math.pow(10, -3.5 + 2 * r.nextDouble());          // free flow 0.3 g/s .. 30 g/s
            CondensatePump curve = new CondensatePump(h0, 0.75 * h0, q0);
            double speed = 0.2 + r.nextDouble();
            double boilerPa = (0.2 + 0.9 * r.nextDouble()) * h0;        // up to past the shut-off
            double k = Math.pow(10, -16 + 6 * r.nextDouble());            // a tight valve .. a short wide pipe
            int pipes = 1 + r.nextInt(6);
            double vol = Math.pow(10, -5 + 2.3 * r.nextDouble());         // 0.01 .. 2 L a node
            PumpLoop.Result res = PumpLoop.run(curve, speed, boilerPa, k, pipes, vol);
            String what = String.format("seed %d: H0 %.1f bar, free %.4f kg/s, speed %.2f, boiler %.2f bar, k %.1e, %d x %.5f m3 -> pipe %.4f..%.4f bar, pump %.6f, feed %.6f, back %.3f bar, %d steps",
                seed, h0 / 1e5, q0, speed, boilerPa / 1e5, k, pipes, vol, res.pLo() / 1e5, res.pHi() / 1e5, res.pump(), res.feed(), res.back() / 1e5, res.steps());
            double tol = 1e-3 * q0;
            assertTrue(res.steps() < PumpLoop.MAX_STEPS, "never settles: " + what);
            assertTrue(res.pHi() - res.pLo() < Math.max(1e-4 * h0, 100), "swings: " + what);
            assertTrue(Math.abs(res.pump() - speed * curve.kgPerS(res.back())) < 1e-2 * speed * q0 + tol, "off its curve: " + what);
            assertTrue(Math.abs(res.pump() - res.feed()) < 1e-2 * Math.abs(res.pump()) + tol, "flow builds up in the pipe: " + what);
            assertTrue(res.back() < h0 * (1 + 1e-3) || res.pump() < tol, "pumps past its shut-off: " + what);
        }
    }
}
