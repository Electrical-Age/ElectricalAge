package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Replays networks the game wrote out when one of their steps failed (NetworkDump; the game's fluidcore-fail/). The
 * directory comes from FLUIDCORE_REPLAY; without it there is nothing to do. Each dump is stepped once as the game
 * did, then once more with the library's own sub-step depth, and the counts printed.
 */
class ReplayTest {
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
