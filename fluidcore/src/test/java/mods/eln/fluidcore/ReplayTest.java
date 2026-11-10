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
