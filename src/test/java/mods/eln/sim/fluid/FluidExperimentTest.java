package mods.eln.sim.fluid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/** Fluid A/B measurements (review #61; manual, with FLUID_EXP=1 and FLUID_CONFIGS, FLUID_CASES, FLUID_SEED, FLUID_NEWTON; writes notes/fluid-review): the closed-network settle tally and limiter activity per experiment set. */
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "FLUID_EXP", matches = "1")
class FluidExperimentTest {
    static final int N = Integer.parseInt(java.util.Objects.requireNonNullElse(System.getenv("FLUID_CASES"), "600"));

    /** For the strict-restless seeds of the base config: is it slow settling (moving falls) or a cycle (it doesn't)? */
    @Test
    void restlessKind() throws Exception {
        FluidNetwork.NEWTON = "1".equals(System.getenv("FLUID_NEWTON"));
        StringBuilder out = new StringBuilder("seed | moving/total at 3000, 4500, 6000, 9000 steps | flow sign flips in the last 200 steps (edges) | nodes, edges\n");
        int shown = 0;
        for (long seed = 1001; seed < 1001 + 300 && shown < 25; seed++) {
            FluidPropertyTest.Case c = FluidPropertyTest.random(seed, true);
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
            out.append("| ").append(flips).append(" | ").append(c.free().size()).append(", ").append(c.net().edges.size()).append("\n");
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/restless-kind-" + (FluidNetwork.NEWTON ? "newton" : "base") + ".txt"), out.toString());
    }

    /** Seed (fluid.seed, default 1129): its nodes and edges, and steps 2990-2999 in detail. */
    @Test
    void traceSeed() throws Exception {
        long seed = Long.parseLong(java.util.Objects.requireNonNullElse(System.getenv("FLUID_SEED"), "1129"));
        FluidNetwork.NEWTON = "1".equals(System.getenv("FLUID_NEWTON"));
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
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/trace-" + seed + (FluidNetwork.NEWTON ? "-newton" : "") + ".txt"), out.toString());
    }

    @Test
    void experiments() throws Exception {
        StringBuilder out = new StringBuilder("config | restless lax | restless strict | steps | newton iters | newton failed | ms\n");
        String[] configs = java.util.Objects.requireNonNullElse(System.getenv("FLUID_CONFIGS"), "base,N").split(",");
        for (String cfg : configs) {
            FluidNetwork.NEWTON = cfg.contains("N");
            Flash.BRACKETED = !cfg.contains("F");   // (F: the old bisection)
            FluidNetwork.Stats.reset();
            List<Long> lax = new ArrayList<>(), strict = new ArrayList<>();
            long t0 = System.nanoTime();
            for (long seed = 1001; seed < 1001 + N; seed++) {
                FluidPropertyTest.Case c = FluidPropertyTest.random(seed, true);
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
        java.nio.file.Files.writeString(java.nio.file.Path.of("/var/lib/mclab/agent/notes/fluid-review/experiments-unit.txt"), out.toString());
    }
}
