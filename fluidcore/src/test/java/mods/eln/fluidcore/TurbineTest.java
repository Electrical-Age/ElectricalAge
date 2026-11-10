package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The TURBINE component: its nozzle law, the work it takes out of the fluid, and its slopes. */
class TurbineTest {
    static final double DT = 0.05;

    static Network.Edge turbine(Network net, Network.Node a, Network.Node b, double nozzle, double drop) {
        Network.Edge e = net.connect(a, 1, b, 1, 0);
        e.kind = Network.Kind.TURBINE;
        e.nozzle = nozzle;
        e.drop = drop;
        e.eta = 0.6;
        return e;
    }

    /** Steam at 10 bar (saturated, over water) in a big vessel: m kg of vapour in V, the rest liquid. */
    static Network.Node steam(Network net, double V, double z, boolean fixed, double T) {
        return net.add(new Network.Node(V, z, 1, fixed).fill(0.3 * V * 1000, 1e-3 * V, T));
    }

    /** Between two fixed vessels the flow follows the ellipse law, and nothing flows back. */
    @Test
    void itFollowsTheEllipseLaw() {
        for (double tb : new double[]{300, 380, 420, 453, 460}) {
            Network net = new Network();
            var in = steam(net, 1, 0, true, 453);
            var out = steam(net, 1, 0, true, tb);
            var e = turbine(net, in, out, 1e-2, 0);
            net.step(DT);
            double pa = Network.portP(in, 1), pb = Network.portP(out, 1);
            // (the law smoothed over SPLIT_DP: (p_a + p_b) S softplus((p_a - p_b) / S) for p_a^2 - p_b^2)
            double S = Network.SPLIT_DP, x = (pa - pb) / S;
            double want = 1e-2 * Math.sqrt(in.fluid.rhoGas() / pa * (pa + pb) * S * Network.spc(x));
            System.out.printf("turbine law: %.2f -> %.2f bar: %.6f kg/s, ellipse %.6f%n", pa / 1e5, pb / 1e5, e.flow, want);
            assertEquals(want, e.flow, 1e-3 * want + 1e-9, "flow at " + pb / pa);
        }
    }

    /** Between two closed vessels, the fluid loses exactly the work it does on the shaft. */
    @Test
    void theFluidLosesItsWork() {
        Network net = new Network();
        var a = steam(net, 0.02, 0, false, 453);
        var b = net.add(new Network.Node(0.02, 0, 1, false).fill(0.5, 1e-4, 320));
        var e = turbine(net, a, b, 2e-3, 4e5);
        double u0 = a.U + b.U, m0 = a.mass() + b.mass(), work = 0;
        for (int s = 0; s < 200; s++) {
            net.step(s == 0 ? 2.0 : DT);
            work += e.workJ;
        }
        double u1 = a.U + b.U;
        System.out.printf("turbine work: %.1f J to the shaft, energy fell %.1f J; %.4f kg passed%n", work, u0 - u1, b.mass() - 0.5);
        assertEquals(m0, a.mass() + b.mass(), 1e-12 * m0, "mass");
        assertTrue(work > 0, "it did work");
        assertEquals(work, u0 - u1, 1e-7 * Math.abs(u0) + 1e-6 * work, "the fluid's energy fell by the work");
        assertEquals(0, net.stats.failures, "failed sub-steps");
    }

    /** Air alone passes through it but makes no work. */
    @Test
    void airMakesNoWork() {
        Network net = new Network();
        var a = net.add(new Network.Node(0.02, 0, 1, false).fill(0, 0.2, 300));
        var b = net.add(new Network.Node(0.02, 0, 1, false).fill(0, 0.02, 300));
        var e = turbine(net, a, b, 2e-3, 4e5);
        double passed = 0, work = 0;
        for (int s = 0; s < 20; s++) {
            net.step(DT);
            passed += e.flow * DT;
            work += e.workJ;
        }
        assertTrue(passed > 1e-4, "air went through: " + passed);
        assertEquals(0, work, 1e-9, "no work from air");
    }

    @Test
    void slopesMatchFiniteDifferences() {
        Network net = new Network();
        var a = steam(net, 0.02, 0, false, 453);
        var b = net.add(new Network.Node(0.02, 0, 1, false).fill(0.5, 1e-4, 320));
        turbine(net, a, b, 2e-3, 4e5);
        net.step(DT);   // (the inlet gas's density is taken at a step's start and held: the slopes are of that step's law)
        for (int s = 0; s < 40; s++) {
            String r = net.checkJacobian(DT);
            double worst = Double.parseDouble(r.substring(0, r.indexOf(' ')));
            // (to 10 %: the energy flux's slope against the inlet's air is two terms of 1e8 that cancel to 1e7, and
            // WaterAir's pressure slope against a trace of air at saturation is ~1 % off its differences; todo #364)
            assertTrue(worst < 0.1, "step " + s + ": " + r);
            net.step(DT);
        }
    }
}
