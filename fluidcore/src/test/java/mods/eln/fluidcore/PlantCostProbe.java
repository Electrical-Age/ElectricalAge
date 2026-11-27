package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

/**
 * Probe (with PLANT_PROBE set; not a test): what a step of a big running steam plant costs and what it is made of.
 * The dumps are Incognito's 193- and 69-node plants of 2026-11-27 (/elnlab fluiddump: boilers, furnaces with their
 * flue ducting, steam generators, condensers). Prints per round the time a step, the solver's counts, and a hash of
 * every node's energy and masses, bit for bit (a change that should cost nothing but time leaves it as it was), with
 * the plant's energy and highest pressure in full for changes that may move the last digits.
 * StuckProbe is the tool for looking into one dump; this one weighs a change.
 */
class PlantCostProbe {
    @Test
    void probe() throws Exception {
        if (System.getenv("PLANT_PROBE") == null) return;
        for (String name : new String[]{"plant193", "plant69"}) {
            String text;
            try (var in = PlantCostProbe.class.getResourceAsStream("/replay/" + name + ".txt")) {
                text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            double[] dt = new double[1];
            Network net = NetworkDump.read(text, dt);
            int depth = Network.MAX_DEPTH;
            boolean cut = Network.CUT_SHORT;
            Network.MAX_DEPTH = 8;
            Network.CUT_SHORT = true;
            MixFluid.COUNT = true;
            try {
                for (int round = 0; round < 6; round++) {
                    long[] s0 = snapshot(net);
                    long e0 = MixFluid.EVALS, b0 = MixFluid.BRACKETS;
                    long t0 = System.nanoTime();
                    int n = 100;
                    for (int k = 0; k < n; k++) net.step(dt[0]);
                    long t = System.nanoTime() - t0;
                    long[] s1 = snapshot(net);
                    long hash = 1;
                    for (Network.Node x : net.nodes) {
                        hash = 31 * hash + Double.doubleToRawLongBits(x.U);
                        for (double mk : x.m) hash = 31 * hash + Double.doubleToRawLongBits(mk);
                    }
                    // (and for a change that may move the last digits: the free nodes' energy and the highest pressure, in full)
                    double sumU = 0, maxP = 0;
                    for (Network.Node x : net.nodes) if (!x.fixed) {
                        sumU += x.U;
                        maxP = Math.max(maxP, x.P());
                    }
                    System.out.printf("PROBE %s round %d: %.3f ms/step; per step: iterations %.1f solves %.1f substeps %.2f crawls %.2f stalls %.2f failures %.2f; "
                            + "flash evaluations %.1f brackets %.2f; energy %.12g J, highest %.10g Pa; state %s%n",
                        name, round, t / 1e6 / n, (s1[0] - s0[0]) / (double) n, (s1[1] - s0[1]) / (double) n, (s1[2] - s0[2]) / (double) n,
                        (s1[3] - s0[3]) / (double) n, (s1[4] - s0[4]) / (double) n, (s1[5] - s0[5]) / (double) n,
                        (MixFluid.EVALS - e0) / (double) n, (MixFluid.BRACKETS - b0) / (double) n, sumU, maxP, Long.toHexString(hash));
                }
            } finally {
                MixFluid.COUNT = false;
                Network.MAX_DEPTH = depth;
                Network.CUT_SHORT = cut;
            }
        }
    }

    static long[] snapshot(Network net) {
        var s = net.stats;
        return new long[]{s.iterations, s.solves, s.substeps, s.crawls, s.stalls, s.failures};
    }
}
