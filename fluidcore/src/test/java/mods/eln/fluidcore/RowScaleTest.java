package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The throughput-aware energy rows of the stop tests (Network.THROUGHPUT). A gas turbine's exhaust, hot air at 19 g/s
 * from a FLOW source, runs through four 0.07 L slices of an exchanger against a boiling pool: each slice holds 0.06 g
 * with a step's 0.95 g passing through, and judged on its contents alone its energy row stalled just over STALL_OK.
 * Judged on its through-flow it stops in fewer iterations at the same end state, and the books stay exact. A quiet
 * network, where no node passes more than it holds in a step, solves bit for bit as before.
 */
class RowScaleTest {
    static final double DT = 0.05;

    record Run(Network net, Network.Node[] a, double massIn, double massOut, double energyIn, double energyOut, double m0, double u0) {
        double mass() {
            double m = 0;
            for (Network.Node n : a) m += n.m[0] + n.m[1];
            return m;
        }

        double energy() {
            double u = 0;
            for (Network.Node n : a) u += n.U;
            return u;
        }
    }

    /** The exhaust exchanger: steps of DT with the stop tests on throughput or not. */
    static Run exchanger(boolean throughput, int steps, int maxIters) {
        boolean keep = Network.THROUGHPUT;
        int keepIters = Network.MAX_ITERS;
        Network.THROUGHPUT = throughput;
        double keepStall = Network.STALL_OK, keepCrawl = Network.CRAWL_OK;
        Network.MAX_ITERS = maxIters;
        if (maxIters > keepIters) {
            // (a tight solve: more iterations, and the precision-floor acceptances 100 times stricter)
            Network.STALL_OK = keepStall / 100;
            Network.CRAWL_OK = keepCrawl / 100;
        }
        try {
            Network net = new Network();
            int n = 4;
            double V = 7e-5, rate = 0.019;
            var hot = net.add(new Network.Node(1, 0, 1, true).fill(0, 1.8e5 / (287.05 * 773), 773));
            var atm = net.add(new Network.Node(10, 0, 1, true).fill(0, 1.01325e5 * 10 / (287.05 * 300), 300));
            Network.Node[] a = new Network.Node[n], b = new Network.Node[n];
            for (int i = 0; i < n; i++) {
                a[i] = net.add(new Network.Node(V, 0, 1, false).fill(0, 1.2e5 * V / (287.05 * 450), 450));
                b[i] = net.add(new Network.Node(1, 0, 1, true).fill(500, 1e-4, 450));   // (a boiling pool at 14 bar)
            }
            Network.Edge src = net.connect(hot, 0.5, a[0], 0.5, 0);
            src.kind = Network.Kind.FLOW;
            src.rate = rate;
            double g = 1e-6;
            for (int i = 0; i + 1 < n; i++) net.connect(a[i], 0.5, a[i + 1], 0.5, n * g);
            Network.Edge out = net.connect(a[n - 1], 0.5, atm, 0.5, 2 * n * g);
            Network.Edge[] c = new Network.Edge[n];
            for (int i = 0; i < n; i++) {
                c[i] = net.connect(a[i], 0.5, b[i], 0.5, 0);
                c[i].conductance = 50.0 / n;
            }
            double m0 = 0, u0 = 0;
            for (Network.Node x : a) {
                m0 += x.m[0] + x.m[1];
                u0 += x.U;
            }
            double mIn = 0, mOut = 0, eIn = 0, eOut = 0;
            for (int s = 0; s < steps; s++) {
                net.step(DT);
                mIn += src.moved[0] + src.moved[1];
                eIn += src.moved[2];
                mOut += out.moved[0] + out.moved[1];
                eOut += out.moved[2];
                for (Network.Edge e : c) eOut += e.moved[2];
            }
            return new Run(net, a, mIn, mOut, eIn, eOut, m0, u0);
        } finally {
            Network.THROUGHPUT = keep;
            Network.MAX_ITERS = keepIters;
            Network.STALL_OK = keepStall;
            Network.CRAWL_OK = keepCrawl;
        }
    }

    @Test
    void aSmallNodeWithALargeThroughflowStopsSoonerAtTheSameState() {
        int steps = 400;
        Run off = exchanger(false, steps, Network.MAX_ITERS), on = exchanger(true, steps, Network.MAX_ITERS);
        Run tight = exchanger(false, steps, 200);
        for (Run r : new Run[]{off, on, tight}) {
            Network.Stats st = r.net().stats;
            System.out.printf("exchanger %s: %.2f iterations %.3f sub-steps a step, stalls %d crawls %d failures %d; out %.4f g/s at %.2f K, slices %s K%n",
                r == off ? "today" : r == on ? "throughput" : "tight", (double) st.iterations / st.steps, (double) st.substeps / st.steps, st.stalls,
                st.crawls, st.failures, r.net().edges.get(4).flow * 1e3, r.a()[3].T(), java.util.Arrays.toString(java.util.Arrays.stream(r.a()).mapToDouble(Network.Node::T).toArray()));
        }
        // the same end state as a tight solve (to well under what the slices' temperatures and pressures show)
        for (int i = 0; i < on.a().length; i++) {
            assertEquals(tight.a()[i].T(), on.a()[i].T(), 1e-3, "slice " + i + " temperature");
            assertEquals(tight.a()[i].P(), on.a()[i].P(), 1e-3 * tight.a()[i].P() * 1e-2, "slice " + i + " pressure");
        }
        // fewer iterations than today, none failing
        assertTrue(on.net().stats.iterations <= off.net().stats.iterations, "iterations " + on.net().stats.iterations + " vs today " + off.net().stats.iterations);
        assertEquals(0, on.net().stats.failures, "failures");
        // the books: the slices' masses and energy change by exactly what crossed their boundary
        for (Run r : new Run[]{off, on}) {
            assertEquals(r.m0() + r.massIn() - r.massOut(), r.mass(), 1e-15, "mass conserved");
            assertEquals(r.u0() + r.energyIn() - r.energyOut(), r.energy(), 1e-12 * Math.abs(r.energyIn()), "energy conserved");
        }
    }

    /** Two vessels equalising through a narrow pipe with a little heat between them: nothing passes more than it holds. */
    static Network quiet(boolean throughput, int steps) {
        boolean keep = Network.THROUGHPUT;
        Network.THROUGHPUT = throughput;
        try {
            Network net = new Network();
            var a = net.add(new Network.Node(0.01, 0, 1, false).fill(0, 1.5e5 * 0.01 / (287.05 * 400), 400));
            var b = net.add(new Network.Node(0.01, 0, 1, false).fill(0.5, 1.0e5 * 0.01 / (287.05 * 300), 300));
            var c = net.add(new Network.Node(0.005, 0, 1, false).fill(0, 1.0e5 * 0.005 / (287.05 * 300), 300));
            net.connect(a, 0.5, b, 0.5, 1e-7);
            net.connect(b, 0.5, c, 0.5, 1e-8).conductance = 0.5;
            for (int s = 0; s < steps; s++) net.step(DT);
            return net;
        } finally {
            Network.THROUGHPUT = keep;
        }
    }

    @Test
    void aQuietNetworkSolvesExactlyAsBefore() {
        Network off = quiet(false, 600), on = quiet(true, 600);
        assertEquals(off.stats.iterations, on.stats.iterations, "iterations");
        assertEquals(off.stats.substeps, on.stats.substeps, "sub-steps");
        for (int i = 0; i < off.nodes.size(); i++) {
            Network.Node x = off.nodes.get(i), y = on.nodes.get(i);
            for (int k = 0; k < x.m.length; k++) assertEquals(Double.doubleToLongBits(x.m[k]), Double.doubleToLongBits(y.m[k]), "node " + i + " mass " + k);
            assertEquals(Double.doubleToLongBits(x.U), Double.doubleToLongBits(y.U), "node " + i + " energy");
        }
        System.out.printf("quiet: %d iterations in %d steps either way%n", on.stats.iterations, on.stats.steps);
    }

    /** Water at 350 K through a liquid-full 10 mL node at 300 K, 0.5 kg/s (25 g a step): its pressure turns a mass residual into bar. */
    static Network liquid(boolean throughput, int steps, int maxIters) {
        boolean keep = Network.THROUGHPUT;
        int keepIters = Network.MAX_ITERS;
        Network.THROUGHPUT = throughput;
        double keepStall = Network.STALL_OK, keepCrawl = Network.CRAWL_OK;
        Network.MAX_ITERS = maxIters;
        if (maxIters > keepIters) {
            // (a tight solve: more iterations, and the precision-floor acceptances 100 times stricter)
            Network.STALL_OK = keepStall / 100;
            Network.CRAWL_OK = keepCrawl / 100;
        }
        try {
            Network net = new Network();
            var hi = net.add(new Network.Node(1, 0, 1, true).fill(500, 3e5 * 0.5 / (287.05 * 350), 350));
            var lo = net.add(new Network.Node(1, 0, 1, true).fill(500, 1e5 * 0.5 / (287.05 * 300), 300));
            var x = net.add(new Network.Node(1e-5, 0, 1, false).fill(9.9e-3, 1e-9, 300));
            net.connect(hi, 0, x, 0.5, 5e-6);
            net.connect(x, 0.5, lo, 0, 5e-6);
            for (int s = 0; s < steps; s++) net.step(DT);
            return net;
        } finally {
            Network.THROUGHPUT = keep;
            Network.MAX_ITERS = keepIters;
            Network.STALL_OK = keepStall;
            Network.CRAWL_OK = keepCrawl;
        }
    }

    @Test
    void aLiquidFullNodeWithALargeThroughflowKeepsItsPressure() {
        Network off = liquid(false, 200, Network.MAX_ITERS), on = liquid(true, 200, Network.MAX_ITERS), tight = liquid(false, 200, 200);
        for (Network n : new Network[]{off, on, tight}) {
            Network.Node x = n.nodes.get(2);
            System.out.printf("liquid %s: %.3f iterations %.3f sub-steps a step, stalls %d crawls %d; node %.6f bar %.5f g %.4f K, flow %.6f kg/s%n",
                n == off ? "today" : n == on ? "throughput" : "tight", (double) n.stats.iterations / n.stats.steps, (double) n.stats.substeps / n.stats.steps,
                n.stats.stalls, n.stats.crawls, x.P() / 1e5, (x.m[0] + x.m[1]) * 1e3, x.T(), n.edges.get(0).flow);
        }
        Network.Node a = on.nodes.get(2), b = tight.nodes.get(2);
        // (a liquid-full node isn't loosened: the same solve as today's, which ends 6 Pa off the tight one)
        assertEquals(off.nodes.get(2).P(), a.P(), 0, "pressure as today's");
        assertEquals(b.P(), a.P(), 10, "pressure (Pa)");
        assertEquals(b.T(), a.T(), 1e-4, "temperature");
        assertEquals(off.edges.get(0).flow, on.edges.get(0).flow, 0, "flow as today's");
        assertEquals(tight.edges.get(0).flow, on.edges.get(0).flow, 1e-4 * tight.edges.get(0).flow, "flow");
        assertTrue(on.stats.iterations <= off.stats.iterations, "iterations " + on.stats.iterations + " vs today " + off.stats.iterations);
    }
}
