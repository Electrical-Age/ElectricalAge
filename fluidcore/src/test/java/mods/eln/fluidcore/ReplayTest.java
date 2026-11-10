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
     * without a failure within the game's sub-step depth (5).
     */
    @Test
    void gameNetworksThatOnceFailedStep() throws Exception {
        int depth = Network.MAX_DEPTH;
        try {
            Network.MAX_DEPTH = 5;
            for (String name : new String[]{"air-pipe-first-steam", "steam-into-air-stub", "steam-into-air-stub-2", "fill-near-full-from-50-bar"}) {
                String text;
                try (var in = ReplayTest.class.getResourceAsStream("/replay/" + name + ".txt")) {
                    text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
                double[] dt = new double[1];
                Network net = NetworkDump.read(text, dt);
                net.step(dt[0]);
                System.out.printf("%s: substeps %d, iterations %d%n", name, net.stats.substeps, net.stats.iterations);
                assertEquals(0, net.stats.failures, name + ": failed sub-steps");
            }
        } finally {
            Network.MAX_DEPTH = depth;
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
