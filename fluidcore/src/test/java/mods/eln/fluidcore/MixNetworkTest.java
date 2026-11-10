package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static mods.eln.fluidcore.MixFluidTest.DIESEL;
import static mods.eln.fluidcore.MixFluidTest.GASOLINE;
import static mods.eln.fluidcore.MixFluidTest.NATURAL_GAS;
import static mods.eln.fluidcore.MixFluidTest.RESIDUE;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Networks carrying several species (10-species-plan.md test 7): conservation per species, positivity, convergence. */
class MixNetworkTest {
    static final Species[] SP = {Species.WATER, GASOLINE, DIESEL, RESIDUE, Species.AIR, NATURAL_GAS};
    static final double DT = 0.05;

    /** NetworkTest.level's shapes, each node holding a random selection of the six species (often none of some). */
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
            double T = 280 + 150 * r.nextDouble();
            double[] m = new double[SP.length];
            double liquidRoom = 0.9 * r.nextDouble();
            for (int k = 0; k < SP.length; k++) {
                if (r.nextDouble() < 0.5) continue;
                m[k] = SP[k].liquidCapable() ? r.nextDouble() * liquidRoom / 4 * V * SP[k].rho : (0.5 + 10 * r.nextDouble()) * V * (SP[k] == Species.AIR ? 1 : 0.5);
            }
            ns.add(net.add(new Network.Node(V, z[i], h[i], false, SP).fillMasses(m, T)));
        }
        for (int i = 1; i < n; i++) {
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
        double[] t = new double[SP.length + 1];
        for (var n : net.nodes) {
            for (int k = 0; k < SP.length; k++) t[k] += n.m[k];
            t[SP.length] += n.U;
        }
        return t;
    }

    @Test
    void randomMixedNetworksConserveAndStayPositive() throws Exception {
        int cases = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_CASES"), "20"));
        int steps = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUIDCORE_STEPS"), "300"));
        List<String> bad = new ArrayList<>(), failed = new ArrayList<>();
        long failures = 0, substeps = 0, iters = 0, total = 0;
        for (long seed = 1; seed <= cases; seed++) {
            Network net = level(seed);
            double[] t0 = totals(net);
            for (int s = 0; s < steps; s++) {
                net.step(DT);
                for (var n : net.nodes)
                    for (int k = 0; k < SP.length; k++)
                        if (!(n.m[k] >= -1e-12 * Math.max(1e-3, n.mass()))) {
                            bad.add(seed + "@" + s + ":" + SP[k]);
                            break;
                        }
            }
            double[] t1 = totals(net);
            for (int k = 0; k < SP.length; k++)
                if (Math.abs(t0[k] - t1[k]) > 1e-11 * Math.max(1e-3, t0[k])) bad.add("conservation " + seed + " " + SP[k]);
            if (Math.abs(t0[SP.length] - t1[SP.length]) > 1e-9 * Math.max(1e3, Math.abs(t0[SP.length]))) bad.add("energy " + seed);
            if (net.stats.failures > 0) failed.add(seed + "x" + net.stats.failures);
            failures += net.stats.failures;
            substeps += net.stats.substeps;
            iters += net.stats.iterations;
            total += net.stats.steps;
        }
        String log = String.format("mixed: cases %d steps %d: failures %d %s, substeps %d, iterations/step %.2f, bad %d %s%n",
            cases, steps, failures, failed, substeps, iters / (double) total, bad.size(), bad.stream().limit(10).toList());
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/fluidcore-mix.txt"), log);
        assertTrue(failures == 0 && bad.isEmpty(), log);
    }
}
