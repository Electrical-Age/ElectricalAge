package mods.eln.fluidcore;

import java.util.List;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * The end of a step: the transfers between nodes that Network.finish() collected, applied in flux form (whatever leaves
 * one node arrives at another, so every mass and the energy are conserved whatever the scales), with each node's sends
 * of a mass scaled down so that it sends no more than it has: what it started with plus what it receives. A pure
 * function of its arguments; spec/ holds the same function in Lean, with the proofs of what it guarantees.
 *
 * Scales s (one per node and mass, 0..1) are feasible when for every node and mass
 * s * sends <= SLACK * max(0, start + receipts at the senders' scales); then no mass ends lower than it started, except
 * within the slack of what it had. Positivity rests on that check alone, not on how the scales were found:
 *  - all 1, then each over-sending node scaled to what it has, again while any still over-sends. Where no node
 *    over-sends this changes nothing, and the step ends on exactly the fluxes Newton solved. Chains settle in as many
 *    passes as they are long; nodes sending in a ring can go on shrinking for ever;
 *  - so after DOWN_PASSES of that, a node still over-sending sends nothing instead, again while any over-sends (each pass
 *    silences at least one more node, so this ends); then every scale is raised to what its node has at the scales so far,
 *    which keeps them feasible however often it is done.
 * The energy an edge's flow carries is scaled with the masses on that edge (their mean scale, by mass), so a node that
 * can't send the mass doesn't send its heat either. Heat conducted or supplied is never scaled.
 */
final class Limiter {
    private Limiter() {
    }

    /** How often over-sending nodes are scaled down before they are silenced, and how often the scales are raised after. */
    static int DOWN_PASSES = 16, UP_PASSES = 64;
    /** A node may over-send by this factor's excess (rounding in the sums would otherwise keep the passes going). */
    static final double SLACK = 1 + 1e-15;
    /** Tests: how many calls settled after 0, 1, ... cuts ([last]: calls that went on to silence nodes). */
    static final AtomicLongArray PASSES = new AtomicLongArray(66);
    /** Tests: the largest amount a node still over-sent when the silencing began. */
    static volatile double WORST = 0;

    /** Tests: the pass counts so far as "0: n, 1: n, ..., silenced: n (largest over-send x)", and reset. */
    static String histogram() {
        StringBuilder o = new StringBuilder();
        for (int i = 0; i < PASSES.length(); i++) {
            long n = PASSES.getAndSet(i, 0);
            if (n > 0 || i == PASSES.length() - 1) o.append(o.isEmpty() ? "" : ", ").append(i == PASSES.length() - 1 ? "silenced" : "" + i).append(": ").append(n);
        }
        o.append(String.format(" (largest over-send %.3g)", WORST));
        WORST = 0;
        return o.toString();
    }

    /** Tests: sees every call's arguments, scales and result (null: off). */
    interface Tap {
        void call(int C, double[] y0, List<double[]> moves, int passes, double[] scale, double[] y);
    }

    static volatile Tap TAP = null;

    /**
     * The contents after the transfers. Cells are node * C + component for the free nodes, the last component of each
     * the energy (never limited). y0: the contents at the step's start; moves: each {from node, to node (-1: a fixed
     * node or outside), component, amount > 0, edge (-1: none)}. An energy move with an edge is the energy that edge's
     * mass moves carry.
     */
    static double[] apply(int C, double[] y0, List<double[]> moves) {
        int[] passes = new int[1];
        double[] scale = scales(C, y0, moves, passes);
        int S = C - 1, G = 0;
        for (double[] mv : moves) G = Math.max(G, (int) mv[4] + 1);
        // each edge's masses as limited and in full
        double[] sent = new double[G], asked = new double[G];
        for (double[] mv : moves) {
            int from = (int) mv[0], k = (int) mv[2], g = (int) mv[4];
            if (g < 0 || k >= S) continue;
            sent[g] += mv[3] * (from >= 0 ? scale[C * from + k] : 1);
            asked[g] += mv[3];
        }
        double[] y = y0.clone();
        for (double[] mv : moves) {
            int from = (int) mv[0], to = (int) mv[1], k = (int) mv[2], g = (int) mv[4];
            double amt = k < S ? mv[3] * (from >= 0 ? scale[C * from + k] : 1) : mv[3] * (g >= 0 && asked[g] != 0 ? sent[g] / asked[g] : 1);
            if (from >= 0) y[C * from + k] -= amt;
            if (to >= 0) y[C * to + k] += amt;
        }
        Tap tap = TAP;
        if (tap != null) tap.call(C, y0, moves, passes[0], scale, y);
        return y;
    }

    /** The scale of each cell's sends (energy cells: 1). passes[0]: the passes that changed scales before the check held. */
    static double[] scales(int C, double[] y0, List<double[]> moves, int[] passes) {
        int S = C - 1, M = y0.length;
        double[] scale = new double[M];
        java.util.Arrays.fill(scale, 1);
        double[] in = new double[M], out = new double[M];
        int pass = 0;
        for (; ; pass++) {
            sums(C, moves, scale, in, out);
            boolean over = false;
            for (int i = 0; i < M; i++) {
                if (i % C == S || out[i] == 0) continue;
                double have = Math.max(0, y0[i] + in[i]);
                if (out[i] > have * SLACK) {
                    over = true;
                    if (pass == DOWN_PASSES) WORST = Math.max(WORST, out[i] - have);
                    scale[i] = pass < DOWN_PASSES ? scale[i] * (have / out[i]) : 0;
                }
            }
            if (!over) break;
            if (pass == DOWN_PASSES + M) {
                // (not reached: every pass since DOWN_PASSES silenced a cell that was sending, and there are at most M)
                for (int i = 0; i < M; i++) if (i % C != S) scale[i] = 0;
                break;
            }
        }
        passes[0] = pass;
        if (pass <= DOWN_PASSES) {
            PASSES.incrementAndGet(Math.min(pass, PASSES.length() - 2));
            return scale;
        }
        PASSES.incrementAndGet(PASSES.length() - 1);
        // raise: what each cell sends in all (at scale 1), and the share of it that the cell has at the scales so far
        double[] raw = new double[M], one = new double[M];
        java.util.Arrays.fill(one, 1);
        sums(C, moves, one, in, raw);
        for (pass = 0; pass < UP_PASSES; pass++) {
            sums(C, moves, scale, in, out);
            boolean raised = false;
            for (int i = 0; i < M; i++) {
                if (i % C == S || raw[i] == 0) continue;
                double s = Math.min(1, Math.max(0, y0[i] + in[i]) / raw[i]);
                if (s > scale[i]) {
                    scale[i] = s;
                    raised = true;
                }
            }
            if (!raised) break;
        }
        return scale;
    }

    /** What each cell receives and sends at the given scales. */
    private static void sums(int C, List<double[]> moves, double[] scale, double[] in, double[] out) {
        int S = C - 1;
        java.util.Arrays.fill(in, 0);
        java.util.Arrays.fill(out, 0);
        for (double[] mv : moves) {
            int from = (int) mv[0], to = (int) mv[1], k = (int) mv[2];
            double amt = mv[3] * (from >= 0 && k < S ? scale[C * from + k] : 1);
            if (from >= 0) out[C * from + k] += amt;
            if (to >= 0) in[C * to + k] += amt;
        }
    }
}
