package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Replays networks the game wrote out when one of their steps failed (NetworkDump; the game's fluidcore-fail/): the
 * cases kept under test resources, and with FLUIDCORE_REPLAY a directory of dumps, each stepped once as the game did
 * and once with the library's own sub-step depth, the counts printed.
 */
class ReplayTest {
    /**
     * Networks whose step failed in the game (GameTests on the core's first adapter), kept as cases: each must now step
     * without a failed sub-step with the game's settings (sub-steps to 1/256 of a tick, cut short at the floor), and
     * cover the whole step (Baughn 2026-11-10: a network making no progress for long is not workable, whatever its
     * devices do). The last two stalled a closed plant's turbine network for seconds: its condensate pump had pumped the
     * condenser's vapour out to a hard vacuum, and steam then expanded into the empty vessel. The last: the condenser's
     * unpiped makeup node drained into that vacuum to under the EOS's 120 K floor. And boiling water flooding a warm, dry
     * pump line: a 0.1 ms transient at ~1 us steps (Network.adaptive).
     */
    @Test
    void gameNetworksThatOnceFailedStep() throws Exception {
        int depth = Network.MAX_DEPTH;
        boolean cut = Network.CUT_SHORT;
        try {
            Network.MAX_DEPTH = 8;
            Network.CUT_SHORT = true;
            for (String name : new String[]{"air-pipe-first-steam", "steam-into-air-stub", "steam-into-air-stub-2", "fill-near-full-from-50-bar", "flood-pipe-from-6-bar", "boiler-flash-into-stub",
                "condenser-pumped-to-vacuum", "steam-into-evacuated-condenser", "makeup-drained-below-the-eos-floor",
                "boiling-water-floods-a-warm-dry-line"}) {
                String text;
                try (var in = ReplayTest.class.getResourceAsStream("/replay/" + name + ".txt")) {
                    text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
                double[] dt = new double[1];
                Network net = NetworkDump.read(text, dt);
                net.step(dt[0]);
                System.out.printf("%s: substeps %d, iterations %d%n", name, net.stats.substeps, net.stats.iterations);
                assertEquals(0, net.stats.failures, name + ": failed sub-steps");
                assertEquals(dt[0], net.done, 1e-12, name + ": the whole step covered");
            }
        } finally {
            Network.MAX_DEPTH = depth;
            Network.CUT_SHORT = cut;
        }
    }

    /**
     * Baughn's workshop plant (2026-11-15; /elnlab fluiddump of a 48-node network: boiler, turbine, condenser, an 8-slice
     * counterflow exchanger full of water at 11 bar, a flare): it cost 22-43 ms a step on the server, never asleep. Every
     * linear solve fell back to the dense one: the sparse LU's pivot test took the energy row of a liquid-full node
     * (dP/dm ~1e12 J/kg beside its diagonal of 1) for a vanishing pivot; on that path the whole step also stalled and
     * sub-stepped about half the time (the dense, unscaled elimination's directions, presumably). Weighed by the unknowns' scales, no solve falls back and a step takes
     * a handful of iterations; the states agree with the old path's to the solve's precision.
     */
    @Test
    void workshopPlantSolvesSparse() throws Exception {
        int depth = Network.MAX_DEPTH;
        boolean cut = Network.CUT_SHORT;
        try {
            Network.MAX_DEPTH = 8;
            Network.CUT_SHORT = true;
            String text;
            try (var in = ReplayTest.class.getResourceAsStream("/replay/baughn-workshop-stuck.txt")) {
                text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            double[] dt = new double[1];
            Network net = NetworkDump.read(text, dt);
            long fallbacks = Sparse.FALLBACKS;
            int steps = 40;
            for (int i = 0; i < steps; i++) {
                net.step(dt[0]);
                assertEquals(dt[0], net.done, 1e-12, "step " + i + " covered");
            }
            System.out.printf("workshop: %d steps, iterations %d, solves %d, substeps %d, dense fallbacks %d%n", steps, net.stats.iterations,
                net.stats.solves, net.stats.substeps, Sparse.FALLBACKS - fallbacks);
            assertEquals(0, net.stats.failures, "failed sub-steps");
            assertEquals(0, Sparse.FALLBACKS - fallbacks, "solves that fell back to the dense solve");
            // (before: 11-13 iterations, ~75 solves and 1-2 sub-steps a step)
            org.junit.jupiter.api.Assertions.assertTrue(net.stats.iterations <= 8 * steps, "iterations: " + net.stats.iterations);
            org.junit.jupiter.api.Assertions.assertTrue(net.stats.substeps <= steps / 4, "sub-steps: " + net.stats.substeps);
        } finally {
            Network.MAX_DEPTH = depth;
            Network.CUT_SHORT = cut;
        }
    }

    /**
     * Baughn's workshop network with his counterflow exchanger back in (2026-11-16, clone of the live server; /elnlab
     * fluiddump of its 35 nodes: boiler, turbine, condenser, a lone exchanger block of two slices a side, its feed water at
     * 12.4 bar, the furnaces' flue gas on the other side). It cost 16-20 ms a step on the server, 15 iterations and ~0.4
     * sub-steps, never settling. Newton reached its tolerance's neighbourhood in a few iterations and then crawled or
     * stalled on the flash's own noise (MixFluid's polish tolerances: 1e-9 J in a 14 mg flue pipe, 1e-13 of a liquid-full
     * pipe's volume): 12 iterations, 24 solves and 0.4 sub-steps a step in this replay; 6, 12 and none with the tighter
     * polish. The end state is a tight solve's (stall and crawl acceptances 100 times stricter, 200 iterations).
     */
    @Test
    void workshopExchangerSettlesInAFewIterations() throws Exception {
        int depth = Network.MAX_DEPTH;
        boolean cut = Network.CUT_SHORT;
        double keepStall = Network.STALL_OK, keepCrawl = Network.CRAWL_OK;
        int keepIters = Network.MAX_ITERS;
        try {
            Network.MAX_DEPTH = 8;
            Network.CUT_SHORT = true;
            String text;
            try (var in = ReplayTest.class.getResourceAsStream("/replay/baughn-workshop-hx.txt")) {
                text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            double[] dt = new double[1];
            int steps = 40;
            Network net = NetworkDump.read(text, dt);
            if (System.getenv("HX_OLD") != null) MixFluid.OLD_TOL = true;
            for (int i = 0; i < steps; i++) {
                net.step(dt[0]);
                assertEquals(dt[0], net.done, 1e-12, "step " + i + " covered");
            }
            MixFluid.OLD_TOL = false;
            System.out.printf("workshop exchanger: %d steps, iterations %d, solves %d, substeps %d%n", steps, net.stats.iterations, net.stats.solves,
                net.stats.substeps);
            boolean old = System.getenv("HX_OLD") != null;
            if (!old) {
                assertEquals(0, net.stats.failures, "failed sub-steps");
                assertEquals(0, net.stats.substeps, "sub-steps");
                // (before: 12 iterations a step)
                org.junit.jupiter.api.Assertions.assertTrue(net.stats.iterations <= 8 * steps, "iterations: " + net.stats.iterations);
            }
            Network.STALL_OK = keepStall / 100;
            Network.CRAWL_OK = keepCrawl / 100;
            Network.MAX_ITERS = 200;
            Network tight = NetworkDump.read(text, dt);
            for (int i = 0; i < steps; i++) tight.step(dt[0]);
            // (a node with a gas space; the liquid-full feed pipe only printed: 1.5e10 Pa/kg there, so the residual a step ends
            // on is a pressure, ~20 Pa against the old polish's 80, and noisy; the old polish was also 0.009 K and 1.2 Pa off
            // elsewhere. HX_OLD=1: the old polish's numbers, unchecked)
            double worstT = 0, worstP = 0, worstFull = 0;
            for (int i = 0; i < net.nodes.size(); i++) {
                Network.Node a = net.nodes.get(i), b = tight.nodes.get(i);
                if (a.fixed || a.mass() < 1e-6) continue;
                worstT = Math.max(worstT, Math.abs(a.T() - b.T()));
                if (a.level() > 0.999) worstFull = Math.max(worstFull, Math.abs(a.P() - b.P()));
                else worstP = Math.max(worstP, Math.abs(a.P() - b.P()));
            }
            System.out.printf("  against a tight solve: worst %.3g K, %.3g Pa (liquid-full: %.3g Pa)%n", worstT, worstP, worstFull);
            if (!old) {
                org.junit.jupiter.api.Assertions.assertTrue(worstT < 1e-4, "temperature off a tight solve by " + worstT);
                org.junit.jupiter.api.Assertions.assertTrue(worstP < 0.5, "pressure off a tight solve by " + worstP);
            }
        } finally {
            Network.MAX_DEPTH = depth;
            Network.CUT_SHORT = cut;
            Network.STALL_OK = keepStall;
            Network.CRAWL_OK = keepCrawl;
            Network.MAX_ITERS = keepIters;
        }
    }

    /**
     * The same network at 20:20, its feed at 4.1 bar: three liquid-full feed pipes with almost no gas (bubbles of 1e-9
     * to 1e-6 of their volume). Their vapour's mass was the difference of two numbers 1e8 times its size, and the noise
     * kept the flash's polish off its tolerance: every update fell back to the bracketed solve, 43 a step, 78 % of the
     * flash's evaluations, 18 ms a step and 7.4 iterations. Now none, 4-5 iterations, and the end state is a tight
     * solve's, the liquid-full pipes' pressures included.
     */
    @Test
    void workshopLiquidFullFeedNeverBrackets() throws Exception {
        int depth = Network.MAX_DEPTH;
        boolean cut = Network.CUT_SHORT, count = MixFluid.COUNT;
        double keepStall = Network.STALL_OK, keepCrawl = Network.CRAWL_OK;
        int keepIters = Network.MAX_ITERS;
        try {
            Network.MAX_DEPTH = 8;
            Network.CUT_SHORT = true;
            String text;
            try (var in = ReplayTest.class.getResourceAsStream("/replay/baughn-workshop-hx-4bar.txt")) {
                text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            double[] dt = new double[1];
            int steps = 40;
            Network net = NetworkDump.read(text, dt);
            MixFluid.COUNT = true;
            long b0 = MixFluid.BRACKETS;
            for (int i = 0; i < steps; i++) net.step(dt[0]);
            long brackets = MixFluid.BRACKETS - b0;
            MixFluid.COUNT = count;
            System.out.printf("workshop at 4 bar: %d steps, iterations %d, substeps %d, bracketed flashes %d%n", steps, net.stats.iterations,
                net.stats.substeps, brackets);
            assertEquals(0, net.stats.substeps, "sub-steps");
            org.junit.jupiter.api.Assertions.assertTrue(brackets <= steps / 10, "bracketed flashes: " + brackets + " (43 a step before)");
            org.junit.jupiter.api.Assertions.assertTrue(net.stats.iterations <= 6 * steps, "iterations: " + net.stats.iterations);
            Network.STALL_OK = keepStall / 100;
            Network.CRAWL_OK = keepCrawl / 100;
            Network.MAX_ITERS = 200;
            Network tight = NetworkDump.read(text, dt);
            for (int i = 0; i < steps; i++) tight.step(dt[0]);
            double worstT = 0, worstP = 0;
            for (int i = 0; i < net.nodes.size(); i++) {
                Network.Node a = net.nodes.get(i), b = tight.nodes.get(i);
                if (a.fixed || a.mass() < 1e-6) continue;
                worstT = Math.max(worstT, Math.abs(a.T() - b.T()));
                worstP = Math.max(worstP, Math.abs(a.P() - b.P()));
            }
            System.out.printf("  against a tight solve: worst %.3g K, %.3g Pa (liquid-full pipes included)%n", worstT, worstP);
            org.junit.jupiter.api.Assertions.assertTrue(worstT < 1e-4, "temperature off a tight solve by " + worstT);
            org.junit.jupiter.api.Assertions.assertTrue(worstP < 1, "pressure off a tight solve by " + worstP);
        } finally {
            Network.MAX_DEPTH = depth;
            Network.CUT_SHORT = cut;
            MixFluid.COUNT = count;
            Network.STALL_OK = keepStall;
            Network.CRAWL_OK = keepCrawl;
            Network.MAX_ITERS = keepIters;
        }
    }

    @Test
    void replayDumps() throws Exception {
        String dir = System.getenv("FLUIDCORE_REPLAY");
        if (dir == null) return;
        List<Path> files;
        try (Stream<Path> s = Files.list(Path.of(dir))) {
            files = s.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
        }
        int depth = Network.MAX_DEPTH;
        for (Path f : files) {
            String text = Files.readString(f);
            double[] dt = new double[1];
            for (int d : new int[]{5, depth}) {
                Network.MAX_DEPTH = d;
                Network net = NetworkDump.read(text, dt);
                double mass = 0;
                for (Network.Node n : net.nodes) if (!n.fixed) mass += n.mass();
                long t0 = System.nanoTime();
                net.step(dt[0]);
                double after = 0;
                for (Network.Node n : net.nodes) if (!n.fixed) after += n.mass();
                System.out.printf("%s depth %d: %d nodes %d edges, dt %.3f: substeps %d failures %d iterations %d, %.1f ms; free mass %.6g -> %.6g%n",
                    f.getFileName(), d, net.nodes.size(), net.edges.size(), dt[0], net.stats.substeps, net.stats.failures, net.stats.iterations,
                    (System.nanoTime() - t0) / 1e6, mass, after);
            }
        }
        Network.MAX_DEPTH = depth;
        assertEquals(depth, Network.MAX_DEPTH);
    }
}
