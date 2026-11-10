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

    /** FLUIDCORE_MIX=1: the random networks through MixFluid with {water, air} instead of WaterAirFluid. */
    static final Species[] MIX = "1".equals(System.getenv("FLUIDCORE_MIX")) ? new Species[]{Species.WATER, Species.AIR} : null;

    static {
        String v = System.getenv("FLUIDCORE_SPLIT");
        if (v != null) Network.SPLIT_DP = Double.parseDouble(v);
    }

    @Test
    void twoTanksEqualiseAndKeepTheirContents() {
        Network net = new Network();
        var a = net.add(new Network.Node(0.1, 0, 1, false).fill(40, 0.5, 300));
        var b = net.add(new Network.Node(0.1, 0, 1, false).fill(10, 0.1, 300));
        net.connect(a, 0, b, 0, 1e-5);
        double w0 = a.mw() + b.mw(), a0 = a.ma() + b.ma(), u0 = a.U + b.U;
        for (int s = 0; s < 2000; s++) net.step(DT);
        assertEquals(w0, a.mw() + b.mw(), 1e-12 * w0, "water");
        assertEquals(a0, a.ma() + b.ma(), 1e-12 * a0, "air");
        assertEquals(u0, a.U + b.U, 1e-9 * Math.abs(u0), "energy");
        assertEquals(portPressure(a, 0), portPressure(b, 0), 50, "the bottom ports' pressures meet");
        assertEquals(0, net.stats.failures, "failures");
    }

    static double portPressure(Network.Node n, double f) {
        return Network.portP(n, f);
    }

    /** FLUIDCORE_LIMITER_PASSES: the limiter's pass cap for this run (measurements). */
    static void limiterPasses() {
        String v = System.getenv("FLUIDCORE_LIMITER_PASSES");
        if (v != null) Limiter.DOWN_PASSES = Integer.parseInt(v);
    }

    /** FLUIDCORE_LOOPS=1: the random networks get every edge that fits and some doubled, so rings and parallel pipes. */
    static final boolean LOOPS = "1".equals(System.getenv("FLUIDCORE_LOOPS"));

    /**
     * Random closed networks whose edges join ports at one height (as the game's do): a tree of 2-8 nodes, some
     * edges check valves.
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
            ns.add(net.add(new Network.Node(V, z[i], h[i], false, MIX).fill(mw, ma, T)));
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
                if (!LOOPS) break;
                if (r.nextInt(4) == 0) net.connect(ns.get(p), (meet - zp0) / h[p], ns.get(i), (meet - zi0) / h[i], Math.pow(10, -9 + 3 * r.nextDouble()));
            }
        }
        return net;
    }

    static double[] totals(Network net) {
        double w = 0, a = 0, u = 0;
        for (var n : net.nodes) {
            w += n.mw();
            a += n.ma();
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
        List<String> failedSeeds = new ArrayList<>();
        long failures = 0, substeps = 0, iters = 0, total = 0, stalls = 0, slept = 0, asleep = 0;
        StringBuilder log = new StringBuilder();
        limiterPasses();
        Limiter.histogram();
        double lowest = 0, lowestShare = 0;
        LimiterTap tap = LimiterTap.open("levels" + (NetworkTest.LOOPS ? "-loops" : ""));
        for (long seed = 1; seed <= cases; seed++) {
            Network net = level(seed);
            double[] t0 = totals(net);
            for (int s = 0; s < steps; s++) {
                net.step(DT);
                for (var n : net.nodes) {
                    if (!(n.mw() >= -1e-12 * Math.max(1e-3, n.mw() + n.ma()) && n.ma() >= -1e-12 * Math.max(1e-3, n.mw() + n.ma()))) negatives.add(seed + "@" + s);
                    double low = Math.min(n.mw(), n.ma());
                    lowest = Math.min(lowest, low);
                    lowestShare = Math.min(lowestShare, low / Math.max(1e-300, n.mw() + n.ma()));
                }
            }
            double[] t1 = totals(net);
            if (Math.abs(t0[0] - t1[0]) > 1e-11 * Math.max(1, t0[0]) || Math.abs(t0[1] - t1[1]) > 1e-11 * Math.max(1e-3, t0[1])
                || Math.abs(t0[2] - t1[2]) > 1e-9 * Math.max(1e3, Math.abs(t0[2]))) negatives.add("conservation " + seed);
            double moving = 0, mass = t1[0] + t1[1];
            for (var e : net.edges) moving += Math.abs(e.flow);
            if (moving * 1000 >= mass + 1e-9) restless.add(seed);
            if (net.stats.failures > 0) failedSeeds.add(seed + "x" + net.stats.failures);
            failures += net.stats.failures;
            stalls += net.stats.stalls;
            slept += net.stats.slept;
            if (net.asleep()) asleep++;
            substeps += net.stats.substeps;
            iters += net.stats.iterations;
            total += net.stats.steps;
        }
        tap.close();
        log.append(String.format("K %.3g cases %d steps %d: restless (>=0.1 %%/s) %s, failures %d %s, substeps %d, stalls %d, iterations/step %.2f, asleep at the end %d, steps slept %d, negatives %d %s%n",
            WaterAir.K, cases, steps, restless, failures, failedSeeds, substeps, stalls, iters / (double) total, asleep, slept, negatives.size(), negatives.stream().limit(10).toList()));
        log.append(String.format("limiter (scaled at most %d times): %s; lowest mass %.3g kg, lowest share of its node %.3g%n", Limiter.DOWN_PASSES, Limiter.histogram(), lowest, lowestShare));
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/fluidcore-m2.txt"), log.toString());
        assertTrue(failures == 0 && negatives.isEmpty(), log.toString());
    }

    /** Two closed volumes joined only by heat conduction: their temperatures meet, energy is conserved, no mass moves. */
    @Test
    void conductionEvensOutTemperatures() {
        Network net = new Network();
        var a = net.add(new Network.Node(1e-3, 0, 0.1, false).fill(0.5, 1e-3, 360));
        var b = net.add(new Network.Node(1e-3, 0, 0.1, false).fill(0.3, 1e-3, 300));
        var e = net.connect(a, 0, b, 0, 0);
        e.conductance = 200;   // (time constant about 4 s)
        double u0 = a.U + b.U;
        for (int s = 0; s < 1200; s++) net.step(DT);
        assertEquals(u0, a.U + b.U, 1e-9 * Math.abs(u0), "energy");
        assertEquals(0.5, a.mw(), 1e-15, "no water moves");
        assertEquals(a.T(), b.T(), 0.01, "temperatures meet");
        assertEquals(0, net.stats.failures, "failures");
    }

    /** The random networks with every edge also conducting heat: the slow thermal settling (the restless ones) goes. */
    @Test
    void randomLevelNetworksWithConductionSettle() throws Exception {
        int cases = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_CASES"), "40"));
        int steps = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_STEPS"), "600"));
        List<Long> restless = new ArrayList<>();
        long failures = 0, total = 0, iters = 0;
        List<String> failed = new ArrayList<>();
        for (long seed = 1; seed <= cases; seed++) {
            Network net = level(seed);
            for (var e : net.edges) e.conductance = 5;
            double[] t0 = totals(net);
            for (int s = 0; s < steps; s++) net.step(DT);
            double[] t1 = totals(net);
            assertEquals(t0[2], t1[2], 1e-9 * Math.max(1e3, Math.abs(t0[2])), "energy " + seed);
            assertEquals(t0[0], t1[0], 1e-11 * Math.max(1, t0[0]), "water " + seed);
            double moving = 0;
            for (var e : net.edges) moving += Math.abs(e.flow);
            if (moving * 1000 >= t1[0] + t1[1] + 1e-9) restless.add(seed);
            if (net.stats.failures > 0) failed.add(seed + "x" + net.stats.failures);
            failures += net.stats.failures;
            iters += net.stats.iterations;
            total += net.stats.steps;
        }
        String log = String.format("conduction 5 W/K: cases %d steps %d: restless %s, failures %d %s, iterations/step %.2f%n", cases, steps, restless, failures, failed, iters / (double) total);
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/fluidcore-conduction.txt"), log);
        assertEquals(0, failures, log);
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
        for (long seed = 1; seed <= 5; seed++) {
            Network net = level(seed);
            for (var e : net.edges) e.conductance = 5;
            out.append("seed ").append(seed).append(" conducting: ").append(net.checkJacobian(DT)).append("\n");
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/fluidcore-jac.txt"), out.toString());
    }

    @Test
    void traceFailures() throws Exception {
        StringBuilder out = new StringBuilder();
        long seed = Long.parseLong(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_SEED"), "12"));
        int until = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_STEPS"), "1870"));
        Network net = level(seed);
        String cond = System.getenv("FLUIDCORE_COND");
        if (cond != null) for (var e : net.edges) e.conductance = Double.parseDouble(cond);
        for (int st = 0; st < until; st++) {
            long f0 = net.stats.substeps;
            net.step(DT);
            if (net.stats.substeps == f0 && st < until - 15) continue;
            out.append("step ").append(st).append(" substeps ").append(net.stats.substeps - f0).append(" lastFail ").append(net.stats.lastFail.stream().map(v -> String.format("%.2e", v)).toList()).append("\n");
            if (net.stats.lastRows != null) out.append("  rows ").append(java.util.Arrays.stream(net.stats.lastRows).mapToObj(v -> String.format("%.1e", v)).toList()).append("\n");
            net.stats.lastRows = null;
            net.stats.lastFail.clear();
        }
        for (var n : net.nodes) out.append(String.format("node V %.4g z %.2f H %.2f mw %.6g ma %.6g T %.2f P %.1f level %.4f%n", n.V, n.z, n.H, n.mw(), n.ma(), n.T(), n.P(), n.level()));
        for (var e : net.edges) out.append(String.format("edge %d(%.2f) -> %d(%.2f) g %.3g %s flow %.4g%n", net.nodes.indexOf(e.a), e.fa, net.nodes.indexOf(e.b), e.fb, e.g, e.kind, e.flow));
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/fluidcore-trace.txt"), out.toString());
    }

    /** Every node's state for the first FLUIDCORE_STEPS steps of seed FLUIDCORE_SEED (where a negative appears). */
    @Test
    void traceStates() throws Exception {
        String seedS = System.getenv("FLUIDCORE_TRACE");
        if (seedS == null) return;
        long seed = Long.parseLong(seedS);
        int until = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_STEPS"), "10"));
        StringBuilder out = new StringBuilder();
        Network net = level(seed);
        String cond = System.getenv("FLUIDCORE_COND");
        if (cond != null) for (var e : net.edges) e.conductance = Double.parseDouble(cond);
        int debugStep = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_DEBUGSTEP"), "1"));
        int from = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_FROM"), "-1"));
        for (var n : net.nodes) out.append(String.format("node V %.4g z %.3f H %.3f%n", n.V, n.z, n.H));
        for (var e : net.edges) out.append(String.format("edge %d(%.2f) -> %d(%.2f) g %.3g %s%n", net.nodes.indexOf(e.a), e.fa, net.nodes.indexOf(e.b), e.fb, e.g, e.kind));
        for (int st = -1; st < until; st++) {
            long sub0 = net.stats.substeps, it0 = net.stats.iterations, f0 = net.stats.failures;
            if (st == debugStep) Network.DEBUG = new StringBuilder();
            if (st >= 0) net.step(DT);
            if (Network.DEBUG != null) {
                out.append(Network.DEBUG);
                Network.DEBUG = null;
            }
            if (st < from) continue;
            out.append(String.format("step %d substeps %d iters %d fail %d%n", st, net.stats.substeps - sub0, net.stats.iterations - it0, net.stats.failures - f0));
            for (var n : net.nodes) out.append(String.format("  mw %.9g ma %.9g T %.3f P %.1f level %.5g yv %.4g ml %.4g mv %.4g%n", n.mw(), n.ma(), n.T(), n.P(), n.level(), n.waterAir().out[WaterAir.YV], n.waterAir().out[WaterAir.ML], n.waterAir().out[WaterAir.MV]));
            if (st >= 0) for (var e : net.edges) out.append(String.format("  flow %.4g%n", e.flow));
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/fluidcore-states.txt"), out.toString());
    }

    /** For the given seeds (FLUIDCORE_SEEDS): slow settling, chatter or circulation? */
    @Test
    void restlessKind() throws Exception {
        String seeds = java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_SEEDS"), "7,12,29,49,63,96,98");
        StringBuilder out = new StringBuilder("seed | moving/mass at 3000, 4500, 6000, 9000 | flips in the last 200 | T spread K | nodes, edges | failures\n");
        for (String t : seeds.split(",")) {
            long seed = Long.parseLong(t.trim());
            Network net = level(seed);
            double mass = totals(net)[0] + totals(net)[1];
            List<Double> at = new ArrayList<>();
            double[] last = new double[net.edges.size()];
            int flips = 0;
            for (int s = 1; s <= 9000; s++) {
                net.step(DT);
                if (s == 3000 || s == 4500 || s == 6000 || s == 9000) {
                    double m = 0;
                    for (var e : net.edges) m += Math.abs(e.flow);
                    at.add(m / mass);
                }
                for (int i = 0; i < last.length; i++) {
                    double f = net.edges.get(i).flow;
                    if (s > 8800 && f * last[i] < 0) flips++;
                    last[i] = f;
                }
            }
            double lo = 1e9, hi = 0;
            for (var n : net.nodes) if (n.mw() + n.ma() > 1e-9) {
                lo = Math.min(lo, n.T());
                hi = Math.max(hi, n.T());
            }
            out.append(seed).append(" | ").append(at.stream().map(v -> String.format("%.2e", v)).toList()).append(" | ").append(flips)
                .append(String.format(" | %.2f | %d, %d | %d%n", hi - lo, net.nodes.size(), net.edges.size(), net.stats.failures));
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/fluidcore-restless.txt"), out.toString());
    }
}
