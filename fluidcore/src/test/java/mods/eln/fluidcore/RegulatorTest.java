package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The REGULATOR component: a reducing valve holds its outlet, a relief valve its inlet, at the set point; slopes. */
class RegulatorTest {
    static final double DT = 0.05;

    static Network.Edge regulator(Network net, Network.Node a, Network.Node b, double set, boolean relief) {
        Network.Edge e = net.connect(a, 0.5, b, 0.5, 2e-7);
        e.kind = Network.Kind.REGULATOR;
        e.setPa = set;
        e.relief = relief;
        return e;
    }

    static Network.Node air(Network net, double V, boolean fixed, double bar) {
        return net.add(new Network.Node(V, 0, 1, fixed).fill(0, bar * 1e5 * V / (287.05 * 300), 300));
    }

    /** 10 bar -> reducing valve at 3 bar -> a vessel that leaks to 1 atm: the vessel holds near 3 bar. */
    @Test
    void aReducingValveHoldsItsOutlet() {
        Network net = new Network();
        var src = air(net, 1, true, 10);
        var v = air(net, 0.01, false, 1);
        var atm = air(net, 1, true, 1.01325);
        regulator(net, src, v, 3e5, false);
        net.connect(v, 0.5, atm, 0.5, 2e-9);
        for (int s = 0; s < 2000; s++) net.step(DT);
        System.out.printf("reducing valve: vessel %.3f bar%n", v.P() / 1e5);
        assertEquals(3.0, v.P() / 1e5, 0.1, "held near its set point");
        assertEquals(0, net.stats.failures, "failed sub-steps");
    }

    /** A vessel fed from 10 bar through a pipe, a relief valve at 5 bar to 1 atm: the vessel holds near 5 bar. */
    @Test
    void aReliefValveHoldsItsInlet() {
        Network net = new Network();
        var src = air(net, 1, true, 10);
        var v = air(net, 0.01, false, 1);
        var atm = air(net, 1, true, 1.01325);
        net.connect(src, 0.5, v, 0.5, 2e-9);
        regulator(net, v, atm, 5e5, true);
        for (int s = 0; s < 2000; s++) net.step(DT);
        System.out.printf("relief valve: vessel %.3f bar%n", v.P() / 1e5);
        assertEquals(5.0, v.P() / 1e5, 0.1, "held near its set point");
        // (nothing flows back through it)
        assertTrue(net.edges.get(1).flow >= -1e-12, "forward only");
        assertEquals(0, net.stats.failures, "failed sub-steps");
    }

    /** Its slopes match finite differences on the way to and at its set point. */
    @Test
    void slopesMatchFiniteDifferences() {
        for (boolean relief : new boolean[]{false, true}) {
            Network net = new Network();
            var src = air(net, 0.05, false, 10);
            var v = air(net, 0.01, false, 1);
            var atm = air(net, 1, true, 1.01325);
            if (relief) {
                net.connect(src, 0.5, v, 0.5, 2e-9);
                regulator(net, v, atm, 5e5, true);
            } else {
                regulator(net, src, v, 3e5, false);
                net.connect(v, 0.5, atm, 0.5, 2e-9);
            }
            for (int s = 0; s < 60; s++) {
                String r = net.checkJacobian(DT);
                double worst = Double.parseDouble(r.substring(0, r.indexOf(' ')));
                assertTrue(worst < 1e-3, (relief ? "relief" : "reducing") + " step " + s + ": " + r);
                net.step(DT);
            }
        }
    }
}
