package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Step time against network size (M3; FLUIDCORE_BENCH=1): a synthetic plant of pipe-run nodes and tanks, ports level. */
@EnabledIfEnvironmentVariable(named = "FLUIDCORE_BENCH", matches = "1")
class BenchTest {
    static {
        String v = System.getenv("FLUIDCORE_SPLIT");
        if (v != null) Network.SPLIT_DP = Double.parseDouble(v);
    }

    static Network plant(int n, long seed) {
        Random r = new Random(seed);
        Network net = new Network();
        List<Network.Node> ns = new ArrayList<>();
        double[] z = new double[n], h = new double[n];
        for (int i = 0; i < n; i++) {
            boolean tank = i % 10 == 0;
            h[i] = tank ? 2 + r.nextDouble() : 0.5 + r.nextDouble();
            double V = tank ? 1 + 4 * r.nextDouble() : 0.002 + 0.02 * r.nextDouble();
            int parent = i == 0 ? -1 : Math.max(0, i - 1 - (r.nextDouble() < 0.3 ? r.nextInt(Math.min(i, 20)) : 0));
            z[i] = parent < 0 ? 0 : z[parent] + (r.nextBoolean() ? 0 : h[parent]) - (r.nextBoolean() ? 0 : h[i]);
            double mw = tank ? (0.2 + 0.6 * r.nextDouble()) * V / WaterAir.V0 : (r.nextDouble() < 0.5 ? 0.9 * V / WaterAir.V0 : 0);
            Network.Node node = net.add(new Network.Node(V, z[i], h[i], false).fill(mw, 1.2 * V, 293 + 60 * r.nextDouble()));
            ns.add(node);
            if (parent >= 0) {
                double meet = Math.max(z[parent], z[i]);
                if (meet > z[parent] + h[parent] || meet > z[i] + h[i]) meet = Math.min(z[parent] + h[parent], z[i] + h[i]);
                Network.Edge e = net.connect(ns.get(parent), (meet - z[parent]) / h[parent], node, (meet - z[i]) / h[i], 1e-9 + 1e-8 * r.nextDouble());
                if (r.nextDouble() < 0.05) e.kind = Network.Kind.CHECK;
                if (r.nextDouble() < 0.05) e.boost = 1e5 * r.nextDouble();
            }
        }
        return net;
    }

    @Test
    void bench() throws Exception {
        StringBuilder out = new StringBuilder("nodes | steps | ms a step | iterations a step | sub-steps | failures\n");
        for (int n : new int[]{25, 100, 500, 2000}) {
            Network net = plant(n, 42);
            int warm = n >= 500 ? 2 : 20, steps = n >= 2000 ? 5 : n >= 500 ? 20 : 100;
            for (int s = 0; s < warm; s++) net.step(0.05);
            long it0 = net.stats.iterations, sub0 = net.stats.substeps, f0 = net.stats.failures;
            long t0 = System.nanoTime();
            for (int s = 0; s < steps; s++) net.step(0.05);
            double ms = (System.nanoTime() - t0) / 1e6 / steps;
            out.append(String.format("%d | %d | %.3f | %.2f | %d | %d%n", n, steps, ms, (net.stats.iterations - it0) / (double) steps,
                net.stats.substeps - sub0, net.stats.failures - f0));
            java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/fluidcore-bench.txt"), out.toString());
        }
    }
}
