package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/** Fluid A/B measurements (review #61; manual, with FLUID_EXP=1 and FLUID_CONFIGS, FLUID_CASES, FLUID_SEED, FLUID_NEWTON; writes notes/fluid-review): the closed-network settle tally and limiter activity per experiment set. */
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "FLUID_EXP", matches = "1")
class FluidExperimentTest {
    /** FLUID_LEVEL=1: networks whose edges join ports at one height (level()). */
    static final boolean LEVEL = "1".equals(System.getenv("FLUID_LEVEL"));
    static final int N = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUID_CASES"), "600"));

    /** For the strict-restless seeds of the base config: is it slow settling (moving falls) or a cycle (it doesn't)? */
    @Test
    void restlessKind() throws Exception {
        FluidNetwork.NEWTON = "1".equals(System.getenv("FLUID_NEWTON"));
        FluidNetwork.NEWTON_FD = "1".equals(System.getenv("FLUID_NEWTON_FD"));
        FluidNetwork.MEAN_HEAD = "1".equals(System.getenv("FLUID_MEAN_HEAD"));
        StringBuilder out = new StringBuilder("seed | moving/total at 3000, 4500, 6000, 9000 steps | flow sign flips in the last 200 steps (edges) | nodes, edges\n");
        int shown = 0;
        for (long seed = 1001; seed < 1001 + Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUID_CASES"), "300")) && shown < 25; seed++) {
            FluidPropertyTest.Case c = LEVEL ? level(seed) : FluidPropertyTest.random(seed, true);
            double total = 0;
            for (FluidNode n : c.free()) total += n.mass();
            List<Double> at = new ArrayList<>();
            double[] last = new double[c.net().edges.size()];
            int flips = 0;
            for (int s = 1; s <= 9000; s++) {
                c.net().step(FluidPropertyTest.DT);
                if (s == 3000 || s == 4500 || s == 6000 || s == 9000) {
                    double m = 0;
                    for (FluidEdge e : c.net().edges) m += Math.abs(e.flow);
                    at.add(m / total);
                    if (s == 3000 && m * 1000 < total + 1e-6) break;
                }
                if (s > 8800) for (int i = 0; i < last.length; i++) {
                    double f = c.net().edges.get(i).flow;
                    if (f * last[i] < 0) flips++;
                    last[i] = f;
                } else for (int i = 0; i < last.length; i++) last[i] = c.net().edges.get(i).flow;
            }
            if (at.size() < 4) continue;
            shown++;
            out.append(seed).append(" | ");
            for (double v : at) out.append(String.format("%.2e ", v));
            double tLo = 1e9, tHi = 0;
            for (FluidNode n : c.free()) if (n.mass() > 1e-9) {
                tLo = Math.min(tLo, n.T());
                tHi = Math.max(tHi, n.T());
            }
            out.append("| ").append(flips).append(" | ").append(c.free().size()).append(", ").append(c.net().edges.size())
                .append(String.format(" | T spread %.2f K", tHi - tLo)).append("\n");
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/restless-kind-" + (FluidNetwork.NEWTON ? "newton" : "base") + ".txt"), out.toString());
    }

    /** Seed (fluid.seed, default 1129): its nodes and edges, and steps 2990-2999 in detail. */
    @Test
    void traceSeed() throws Exception {
        long seed = Long.parseLong(java.util.Objects.requireNonNullElse(System.getenv("FLUID_SEED"), "1129"));
        FluidNetwork.NEWTON = "1".equals(System.getenv("FLUID_NEWTON"));
        FluidNetwork.NEWTON_FD = "1".equals(System.getenv("FLUID_NEWTON_FD"));
        FluidNetwork.MEAN_HEAD = "1".equals(System.getenv("FLUID_MEAN_HEAD"));
        FluidPropertyTest.Case c = FluidPropertyTest.random(seed, true);
        StringBuilder out = new StringBuilder("seed " + seed + "\n");
        var net = c.net();
        for (FluidNode n : c.free()) out.append(String.format("node V=%.4g z=%.2f h=%.2f mW=%.4g mA=%.4g T=%.1f P=%.4g level=%.3f vL=%.4g%n", n.volume, n.z, n.height, n.mWater, n.mAir, n.T(), n.P(), n.level(), n.liquidVolume()));
        for (FluidEdge e : net.edges) out.append(String.format("edge %d(%.2f) -> %d(%.2f) k=%.3g kind=%s open=%.2f%n", c.free().indexOf(e.a), e.portA, c.free().indexOf(e.b), e.portB, e.k, e.kind, e.open));
        FluidNetwork.Stats.reset();
        for (int s = 0; s < 3000; s++) {
            long it0 = FluidNetwork.Stats.newtonIters, f0 = FluidNetwork.Stats.newtonFailed;
            net.step(FluidPropertyTest.DT);
            if (s >= 2990) {
                out.append("step ").append(s).append(" (newton ").append(FluidNetwork.Stats.newtonIters - it0).append(FluidNetwork.Stats.newtonFailed > f0 ? " FAILED" : "").append(" ").append(FluidNetwork.Stats.lastNorms.stream().map(v -> String.format("%.3g", v)).toList()).append("):");
                for (FluidNode n : c.free()) out.append(String.format(" [P=%.5g T=%.2f lvl=%.4f mW=%.6g mA=%.4g]", n.P(), n.T(), n.level(), n.mWater, n.mAir));
                out.append("\n   ");
                for (FluidEdge e : net.edges) out.append(String.format(" flow=%.4g %s;", e.flow, e.debug()));
                out.append("\n");
            }
        }
        out.append("totals: steps " + FluidNetwork.Stats.steps + " newton iters " + FluidNetwork.Stats.newtonIters + " failed " + FluidNetwork.Stats.newtonFailed + "\n");
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/trace-" + seed + (FluidNetwork.NEWTON_FD ? "-fd" : FluidNetwork.NEWTON ? "-newton" : "") + (FluidNetwork.MEAN_HEAD ? "-mean" : "") + ".txt"), out.toString());
    }

    /**
     * As FluidPropertyTest.random(closed), but every edge joins its two ports at one height, as in the game (a pipe run's
     * port meets a tank's at their shared face; a vertical run is one node, its height inside). The random networks join
     * ports up to 15 m apart through zero-volume edges, whose head has no pipe content to weigh.
     */
    static FluidPropertyTest.Case level(long seed) {
        java.util.Random r = new java.util.Random(seed);
        FluidNetwork net = new FluidNetwork();
        List<FluidNode> free = new ArrayList<>();
        int n = 2 + r.nextInt(7);
        double[] Vs = new double[n], hs = new double[n], Ts = new double[n], liq = new double[n], ps = new double[n];
        for (int i = 0; i < n; i++) {
            Vs[i] = Math.pow(10, -4 + 2.5 * r.nextDouble());
            hs[i] = 0.2 + r.nextDouble();
            Ts[i] = 280 + 200 * r.nextDouble();
            liq[i] = r.nextDouble() < 0.6 ? r.nextDouble() * 0.9 * Vs[i] * Water.RHO_L : 0;
            ps[i] = r.nextDouble() < 0.3 ? 0 : 0.5e5 + 15e5 * r.nextDouble();
        }
        double[] z = new double[n];
        int[] parent = new int[n];
        double[] pa = new double[n], pb = new double[n];
        z[0] = r.nextInt(12) - 4;
        for (int i = 1; i < n; i++) {
            parent[i] = r.nextInt(i);
            pa[i] = r.nextInt(2);
            pb[i] = r.nextInt(2);
            double meet = z[parent[i]] + pa[i] * hs[parent[i]];
            z[i] = meet - pb[i] * hs[i];
        }
        for (int i = 0; i < n; i++) {
            FluidNode node = net.add(new FluidNode(Vs[i], z[i], hs[i], false).fill(liq[i], ps[i], Ts[i]));
            free.add(node);
        }
        for (int i = 1; i < n; i++) {
            FluidEdge e = net.connect(free.get(parent[i]), pa[i], free.get(i), pb[i], Math.pow(10, -13 + 3 * r.nextDouble()));
            if (r.nextDouble() < 0.15) e.kind(FluidEdge.Kind.CHECK);
            if (r.nextDouble() < 0.15) e.open = r.nextDouble();
        }
        for (int extra = r.nextInt(3); extra > 0; extra--) {
            int a = r.nextInt(n), b = r.nextInt(n);
            if (a == b) continue;
            double fa = r.nextInt(2), za = z[a] + fa * hs[a];
            double fb = (za - z[b]) / hs[b];
            if (fb < 0 || fb > 1) continue;   // (no shared height: no edge)
            net.connect(free.get(a), fa, free.get(b), fb, Math.pow(10, -13 + 3 * r.nextDouble()));
        }
        return new FluidPropertyTest.Case(net, free, List.of(), List.of(), new double[n]);
    }

    /**
     * A synthetic plant of n lumped nodes, ports joined at one height: a chain of pipe-run nodes with branches (a tree)
     * and a few loops, every tenth a tank part full of water, some pumps (boost) and check valves. Seeded.
     */
    static FluidNetwork plant(int n, long seed) {
        java.util.Random r = new java.util.Random(seed);
        FluidNetwork net = new FluidNetwork();
        List<FluidNode> ns = new ArrayList<>();
        double[] z = new double[n], h = new double[n];
        for (int i = 0; i < n; i++) {
            boolean tank = i % 10 == 0;
            h[i] = tank ? 2 + r.nextDouble() : 0.5 + r.nextDouble();
            double V = tank ? 1 + 4 * r.nextDouble() : 0.002 + 0.02 * r.nextDouble();
            int parent = i == 0 ? -1 : Math.max(0, i - 1 - (r.nextDouble() < 0.3 ? r.nextInt(Math.min(i, 20)) : 0));
            z[i] = parent < 0 ? 0 : z[parent] + (r.nextBoolean() ? 0 : h[parent]) - (r.nextBoolean() ? 0 : h[i]);
            double liquid = tank ? (0.2 + 0.6 * r.nextDouble()) * V * Water.RHO_L : (r.nextDouble() < 0.5 ? 0.9 * V * Water.RHO_L : 0);
            FluidNode node = net.add(new FluidNode(V, z[i], h[i], false).fill(liquid, 1e5 + 2e5 * r.nextDouble(), 293 + 60 * r.nextDouble()));
            ns.add(node);
            if (parent >= 0) {
                double meet = Math.max(z[parent], z[i]);
                FluidEdge e = net.connect(ns.get(parent), (meet - z[parent]) / h[parent], node, (meet - z[i]) / h[i], 1e-9 + 1e-8 * r.nextDouble());
                if (r.nextDouble() < 0.05) e.kind(FluidEdge.Kind.CHECK);
                if (r.nextDouble() < 0.05) e.boost = 1e5 * r.nextDouble();
            }
        }
        for (int extra = 0; extra < n / 20; extra++) {
            int a = r.nextInt(n), b = r.nextInt(n);
            if (a == b) continue;
            double lo = Math.max(z[a], z[b]), hi = Math.min(z[a] + h[a], z[b] + h[b]);
            if (hi < lo) continue;
            net.connect(ns.get(a), (lo - z[a]) / h[a], ns.get(b), (lo - z[b]) / h[b], 1e-9);
        }
        return net;
    }

    /** Step time against network size (scale plan step 1). */
    @Test
    void bench() throws Exception {
        StringBuilder out = new StringBuilder("nodes | edges | steps timed | ms per step (avg) | newton iters\n");
        for (int n : new int[]{25, 100, 300, 1000, 2000}) {
            FluidNetwork net = plant(n, 42);
            int warm = n >= 1000 ? 1 : 20, steps = n >= 2000 ? 2 : n >= 1000 ? 5 : 100;
            for (int s = 0; s < warm; s++) net.step(FluidPropertyTest.DT);
            FluidNetwork.Stats.reset();
            long t0 = System.nanoTime();
            for (int s = 0; s < steps; s++) net.step(FluidPropertyTest.DT);
            double ms = (System.nanoTime() - t0) / 1e6 / steps;
            out.append(String.format("%d | %d | %d | %.3f | %d | solves/step %.1f | phases ms/step %s%n", n, net.edges.size(), steps, ms, FluidNetwork.Stats.newtonIters, FluidNetwork.Stats.solves / (double) steps,
                java.util.Arrays.stream(FluidNetwork.Stats.phaseNanos).mapToObj(v -> String.format("%.2f", v / 1e6 / steps)).toList()));
            java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/bench.txt"), out.toString());
        }
    }

    @Test
    void experiments() throws Exception {
        StringBuilder out = new StringBuilder("config | restless lax | restless strict | steps | newton iters | newton failed | ms\n");
        String[] configs = java.util.Objects.requireNonNullElse(System.getenv("FLUID_CONFIGS"), "base,N").split(",");
        for (String cfg : configs) {
            FluidNetwork.NEWTON = cfg.contains("N");
            FluidNetwork.NEWTON_FD = cfg.contains("J");
            FluidNetwork.MEAN_HEAD = cfg.contains("M");
            Flash.BRACKETED = !cfg.contains("F");   // (F: the old bisection)
            FluidNetwork.Stats.reset();
            List<Long> lax = new ArrayList<>(), strict = new ArrayList<>();
            long t0 = System.nanoTime();
            for (long seed = 1001; seed < 1001 + N; seed++) {
                FluidPropertyTest.Case c = LEVEL ? level(seed) : FluidPropertyTest.random(seed, true);
                for (int s = 0; s < 3000; s++) c.net().step(FluidPropertyTest.DT);
                double moving = 0, total = 0;
                for (FluidNode n : c.free()) total += n.mass();
                for (FluidEdge e : c.net().edges) moving += Math.abs(e.flow);
                if (moving * 10 >= total + 1e-6) lax.add(seed);
                if (moving * 1000 >= total + 1e-6) strict.add(seed);
            }
            long ms = (System.nanoTime() - t0) / 1_000_000;
            out.append(String.format("%s | %d | %d | %d | %d | %d | %d%n", cfg, lax.size(), strict.size(), FluidNetwork.Stats.steps, FluidNetwork.Stats.newtonIters, FluidNetwork.Stats.newtonFailed, ms));
            out.append("  lax seeds ").append(lax).append("\n");
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/experiments-unit" + (LEVEL ? "-level" : "") + ".txt"), out.toString());
    }
}
