package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The PUMP component: its curve, its shut-off head, the work it puts into the fluid, and its slopes. */
class PumpTest {
    static final double DT = 0.05;

    static Network.Edge pump(Network net, Network.Node a, Network.Node b, double qMax, double h0) {
        Network.Edge e = net.connect(a, 0, b, 0, 0);
        e.kind = Network.Kind.PUMP;
        e.qMax = qMax;
        e.h0 = h0;
        return e;
    }

    /** Between two big fixed vessels, the flow follows speed qMax sqrt(1 - dp / h0). */
    @Test
    void itFollowsItsCurve() {
        for (double share : new double[]{0, 0.25, 0.5, 0.8}) {
            Network net = new Network();
            double h0 = 10e5;
            // (air for a gas pressure p over 0.5 m^3 of water in 1 m^3: p 0.5 / (R T))
            var in = net.add(new Network.Node(1, 0, 1, true).fill(500, 1e5 * 0.5 / (287.05 * 300), 300));
            double pOut = Network.portP(in, 0) + share * h0 - 500 * 9.81 * 0.5 / 0.5;
            var out = net.add(new Network.Node(1, 0, 1, true).fill(500, Math.max(1e3, pOut) * 0.5 / (287.05 * 300), 300));
            var e = pump(net, in, out, 0.2, h0);
            e.speed = 0.8;
            net.step(DT);
            double dp = Network.portP(out, 0) - Network.portP(in, 0);
            double want = 0.8 * 0.2 * Math.sqrt(Math.max(0, 1 - dp / h0));
            System.out.printf("pump curve: back %.3f of h0: %.5f kg/s, curve %.5f%n", dp / h0, e.flow, want);
            // (to the smoothing's few percent of the free flow near the shut-off)
            assertEquals(want, e.flow, 0.04 * 0.16, "flow at " + dp / h0 + " of the shut-off head");
        }
    }

    /** Into a closed, liquid-full vessel it stops at about its shut-off head over its inlet. */
    @Test
    void itStopsAtItsShutOffHead() {
        Network net = new Network();
        double h0 = 8e5;
        var sump = net.add(new Network.Node(1, 0, 1, true).fill(800, 0.3, 300));
        var vessel = net.add(new Network.Node(1e-3, 0, 1, false).fill(0.8, 2e-4, 300));
        System.out.printf("pump shut-off: start %.3f bar over the sump%n", (Network.portP(vessel, 0) - Network.portP(sump, 0)) / 1e5);
        var e = pump(net, sump, vessel, 0.05, h0);
        for (int s = 0; s < 400; s++) net.step(DT);
        double lift = Network.portP(vessel, 0) - Network.portP(sump, 0);
        System.out.printf("pump shut-off: lift %.4f of h0, flow %.3g kg/s, failures %d%n", lift / h0, e.flow, net.stats.failures);
        assertTrue(lift > 0.97 * h0 && lift < 1.08 * h0, "lift " + lift / h0 + " of h0");
        assertTrue(Math.abs(e.flow) < 1e-3 * 0.05, "it has stopped: " + e.flow);
        assertEquals(0, net.stats.failures, "failed sub-steps");
    }

    /** Between two closed vessels, everything it does to the fluid shows up as energy: dU = sum of work dt. */
    @Test
    void itsWorkIsTheFluidsEnergy() {
        Network net = new Network();
        var a = net.add(new Network.Node(2e-3, 0, 1, false).fill(1.5, 1e-3, 300));
        var b = net.add(new Network.Node(2e-3, 0, 1, false).fill(0.5, 1e-3, 300));
        var e = pump(net, a, b, 0.02, 3e5);
        double u0 = a.U + b.U, m0 = a.mass() + b.mass(), work = 0;
        for (int s = 0; s < 2000; s++) {
            net.step(DT);
            work += e.work * DT;
        }
        double u1 = a.U + b.U;
        System.out.printf("pump work: %.2f J done, energy rose %.2f J; b at %.3f bar over a%n", work, u1 - u0, (b.P() - a.P()) / 1e5);
        assertEquals(m0, a.mass() + b.mass(), 1e-12 * m0, "mass");
        assertTrue(work > 0, "it did work");
        assertEquals(work, u1 - u0, 1e-6 * Math.abs(u0) + 1e-3 * work, "the fluid's energy rose by the work");
    }

    @Test
    void slopesMatchFiniteDifferences() {
        Network net = new Network();
        var a = net.add(new Network.Node(2e-3, 0, 1, false).fill(1.5, 1e-3, 300));
        var b = net.add(new Network.Node(2e-3, 0.2, 1, false).fill(0.5, 1e-3, 320));
        pump(net, a, b, 0.02, 3e5);
        for (int s = 0; s < 60; s++) {
            String r = net.checkJacobian(DT);
            double worst = Double.parseDouble(r.substring(0, r.indexOf(' ')));
            assertTrue(worst < 1e-3, "step " + s + ": " + r);
            net.step(DT);
        }
    }
}
