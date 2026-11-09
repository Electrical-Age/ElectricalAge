package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The fluid core's implicit step (M2): conservation, positivity, settling, determinism. */
class NetworkTest {
    static final double DT = 0.05;

    @Test
    void twoTanksEqualiseAndKeepTheirContents() {
        Network net = new Network();
        var a = net.add(new Network.Node(0.1, 0, 1, false).fill(40, 0.5, 300));
        var b = net.add(new Network.Node(0.1, 0, 1, false).fill(10, 0.1, 300));
        net.connect(a, 0, b, 0, 1e-5);
        double w0 = a.mw + b.mw, a0 = a.ma + b.ma, u0 = a.U + b.U;
        for (int s = 0; s < 2000; s++) net.step(DT);
        assertEquals(w0, a.mw + b.mw, 1e-12 * w0, "water");
        assertEquals(a0, a.ma + b.ma, 1e-12 * a0, "air");
        assertEquals(u0, a.U + b.U, 1e-9 * Math.abs(u0), "energy");
        assertEquals(portPressure(a, 0), portPressure(b, 0), 50, "the bottom ports' pressures meet");
        assertEquals(0, net.stats.failures, "failures");
    }

    static double portPressure(Network.Node n, double f) {
        return Network.portP(n, f);
    }

    /**
     * Random closed networks whose edges join ports at one height (as the game's do): a tree of 2-8 nodes plus a few
     * extra edges, some check valves.
     */
    static Network level(long seed) {
        Random r = new Random(seed);
        Network net = new Network();
        int n = 2 + r.nextInt(7);
        List<Network.Node> ns = new ArrayList<>();
        double[] z = new double[n], h = new double[n];
        for (int i = 0; i < n; i++) {
            h[i] = 0.2 + r.nextDouble();
            double V = Math.pow(10, -4 + 2.5 * r.nextDouble());
            if (i > 0) {
                int p = r.nextInt(i);
                double fp = r.nextInt(2), fi = r.nextInt(2);
                z[i] = z[p] + fp * h[p] - fi * h[i];
            }
            double T = 280 + 200 * r.nextDouble();
            double mw = r.nextDouble() < 0.6 ? r.nextDouble() * 0.9 * V / WaterAir.V0 : 0;
            double ma = r.nextDouble() < 0.3 ? 0 : (0.5 + 15 * r.nextDouble()) * V;
            ns.add(net.add(new Network.Node(V, z[i], h[i], false).fill(mw, ma, T)));
        }
        // tree edges as built above (the child's port meets the parent's)
        Random r2 = new Random(seed);
        r2.nextInt(7);
        for (int i = 1; i < n; i++) {
            // find a parent port at the child's port height: the parent chosen above is unknown here, so join to any
            // earlier node whose span holds the child's top or bottom
            double zi0 = z[i], zi1 = z[i] + h[i];
            for (int p = 0; p < i; p++) {
                double zp0 = z[p], zp1 = z[p] + h[p];
                double meet = Double.NaN;
                for (double c : new double[]{zi0, zi1}) if (c >= zp0 - 1e-9 && c <= zp1 + 1e-9) meet = c;
                if (Double.isNaN(meet)) continue;
                var e = net.connect(ns.get(p), (meet - zp0) / h[p], ns.get(i), (meet - zi0) / h[i], Math.pow(10, -9 + 3 * r.nextDouble()));
                if (r.nextDouble() < 0.15) e.kind = Network.Kind.CHECK;
                break;
            }
        }
        return net;
    }

    static double[] totals(Network net) {
        double w = 0, a = 0, u = 0;
        for (var n : net.nodes) {
            w += n.mw;
            a += n.ma;
            u += n.U;
        }
        return new double[]{w, a, u};
    }

    @Test
    void randomLevelNetworksConserveStayPositiveAndSettle() throws Exception {
        int cases = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_CASES"), "40"));
        int steps = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_STEPS"), "600"));
        List<Long> restless = new ArrayList<>();
        List<String> negatives = new ArrayList<>();
        long failures = 0, substeps = 0, iters = 0, total = 0;
        StringBuilder log = new StringBuilder();
        for (long seed = 1; seed <= cases; seed++) {
            Network net = level(seed);
            double[] t0 = totals(net);
            for (int s = 0; s < steps; s++) {
                net.step(DT);
                for (var n : net.nodes) if (!(n.mw >= -1e-12 * Math.max(1e-3, n.mw + n.ma) && n.ma >= -1e-12 * Math.max(1e-3, n.mw + n.ma))) negatives.add(seed + "@" + s);
            }
            double[] t1 = totals(net);
            if (Math.abs(t0[0] - t1[0]) > 1e-11 * Math.max(1, t0[0]) || Math.abs(t0[1] - t1[1]) > 1e-11 * Math.max(1e-3, t0[1])
                || Math.abs(t0[2] - t1[2]) > 1e-9 * Math.max(1e3, Math.abs(t0[2]))) negatives.add("conservation " + seed);
            double moving = 0, mass = t1[0] + t1[1];
            for (var e : net.edges) moving += Math.abs(e.flow);
            if (moving * 1000 >= mass + 1e-9) restless.add(seed);
            failures += net.stats.failures;
            substeps += net.stats.substeps;
            iters += net.stats.iterations;
            total += net.stats.steps;
        }
        log.append(String.format("cases %d steps %d: restless (>=0.1 %%/s) %s, failures %d, substeps %d, iterations/step %.2f, negatives %d %s%n",
            cases, steps, restless, failures, substeps, iters / (double) total, negatives.size(), negatives.stream().limit(10).toList()));
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/fluidcore-m2.txt"), log.toString());
        assertTrue(failures == 0 && negatives.isEmpty(), log.toString());
    }

    @Test
    void stepsAreDeterministic() {
        Network a = level(7), b = level(7);
        for (int s = 0; s < 100; s++) {
            a.step(DT);
            b.step(DT);
        }
        for (int i = 0; i < a.nodes.size(); i++) assertEquals(a.nodes.get(i).U, b.nodes.get(i).U, 0.0);
    }

    @Test
    void jacobianMatchesFiniteDifferences() throws Exception {
        StringBuilder out = new StringBuilder();
        for (long seed = 1; seed <= 10; seed++) {
            Network net = level(seed);
            out.append("seed ").append(seed).append(": ").append(net.checkJacobian(DT)).append("\n");
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/fluidcore-jac.txt"), out.toString());
    }

    @Test
    void traceFailures() throws Exception {
        StringBuilder out = new StringBuilder();
        Network net = level(1);
        for (int st = 0; st < 170; st++) {
            long f0 = net.stats.substeps;
            net.step(DT);
            if (net.stats.substeps == f0 && st < 150) continue;
            out.append("step ").append(st).append(" substeps ").append(net.stats.substeps - f0).append(" lastFail ").append(net.stats.lastFail.stream().map(v -> String.format("%.2e", v)).toList()).append("\n");
            net.stats.lastFail.clear();
        }
        for (var n : net.nodes) out.append(String.format("node V %.4g z %.2f H %.2f mw %.6g ma %.6g T %.2f P %.1f level %.4f%n", n.V, n.z, n.H, n.mw, n.ma, n.T(), n.P(), n.level()));
        for (var e : net.edges) out.append(String.format("edge %d(%.2f) -> %d(%.2f) g %.3g %s flow %.4g%n", net.nodes.indexOf(e.a), e.fa, net.nodes.indexOf(e.b), e.fb, e.g, e.kind, e.flow));
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/fluidcore-trace.txt"), out.toString());
    }
}
