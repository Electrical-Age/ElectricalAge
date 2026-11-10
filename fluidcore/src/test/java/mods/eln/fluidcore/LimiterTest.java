package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The end of a step (Limiter): what spec/ proves over exact numbers, checked here in doubles, and the vectors for it. */
class LimiterTest {
    static double[] mv(int from, int to, int k, double amt) {
        return new double[]{from, to, k, amt};
    }

    /** One mass and the energy per node; the masses after the given moves from the given starts. */
    static double[] masses(String name, double[] m0, List<double[]> moves) throws Exception {
        double[] y0 = new double[2 * m0.length];
        for (int i = 0; i < m0.length; i++) y0[2 * i] = m0[i];
        double[] y, m = new double[m0.length];
        try (LimiterTap tap = LimiterTap.open(name)) {
            y = Limiter.apply(2, y0, moves);
        }
        for (int i = 0; i < m0.length; i++) m[i] = y[2 * i];
        return m;
    }

    /** Two empty nodes swapping 10 each way while one leaks 1 to a third: four passes of scaling left it at -0.83. */
    @Test
    void aRingOfEmptyNodesSendsNothing() throws Exception {
        double[] m = masses("ring", new double[]{0, 0, 0}, List.of(mv(0, 1, 0, 10), mv(1, 0, 0, 10), mv(1, 2, 0, 1)));
        for (double x : m) assertEquals(0, x, 0, java.util.Arrays.toString(m));
    }

    /**
     * The same ring with something in it: nothing is lost, and no node ends below zero by more than rounding. (A node
     * that sends all it has ends on the difference of two sums, which in doubles can be a few 1e-16 of what passed
     * through it either side of zero.)
     */
    @Test
    void aLeakingRingKeepsWhatItHas() throws Exception {
        double[] m = masses("ring", new double[]{3, 0.5, 0}, List.of(mv(0, 1, 0, 10), mv(1, 0, 0, 10), mv(1, 2, 0, 1)));
        for (double x : m) assertTrue(x >= -1e-14, java.util.Arrays.toString(m));
        assertEquals(3.5, m[0] + m[1] + m[2], 1e-12, "conserved");
        assertTrue(m[2] > 0.1, "and the leak leaked: " + m[2]);
    }

    /** A chain of empty transit nodes each sending 1 % more than it gets, fed 5 at one end. */
    static List<double[]> greedyChain(int n) {
        List<double[]> moves = new ArrayList<>();
        moves.add(mv(-1, 0, 0, 5));
        for (int i = 0; i + 1 < n; i++) moves.add(mv(i, i + 1, 0, 5 * (1 + 0.01 * (i + 1))));
        return moves;
    }

    /** Four passes left the fourth node of such a chain at -0.05; now all of it arrives, also down a chain too long to settle by scaling. */
    @Test
    void aLongChainOfOverSendersSettles() throws Exception {
        for (int n : new int[]{12, 60}) {
            double[] m = masses("chain", new double[n], greedyChain(n));
            double total = 0;
            for (double x : m) {
                assertTrue(x >= -1e-14, java.util.Arrays.toString(m));
                total += x;
            }
            assertEquals(5, total, 1e-12, "what the fixed node sent is all there");
            assertEquals(5, m[n - 1], 1e-12, "and it all reached the end of " + n);
        }
    }

    /** Past the raising passes' reach the flow stops where it got to: nothing lost, nothing negative. */
    @Test
    void aChainTooLongToRaiseStopsShort() throws Exception {
        int n = 200;
        double[] m = masses("chain", new double[n], greedyChain(n));
        double total = 0;
        for (double x : m) {
            assertTrue(x >= -1e-14, java.util.Arrays.toString(m));
            total += x;
        }
        assertEquals(5, total, 1e-12, "what the fixed node sent is all there");
    }

    /** A transit chain with nothing over-sent passes everything on untouched, however long, also beside a ring that never settles. */
    @Test
    void transitIsNotCut() throws Exception {
        int n = 100;
        List<double[]> moves = new ArrayList<>();
        moves.add(mv(-1, 0, 0, 5));
        for (int i = 0; i + 1 < n; i++) moves.add(mv(i, i + 1, 0, 5));
        assertEquals(5, masses("transit", new double[n], moves)[n - 1], 0, "all of it at the end");
        // nodes n, n+1, n+2: the leaking ring
        moves.add(mv(n, n + 1, 0, 10));
        moves.add(mv(n + 1, n, 0, 10));
        moves.add(mv(n + 1, n + 2, 0, 1));
        Limiter.histogram();
        double[] m = masses("transit", new double[n + 3], moves);
        assertTrue(Limiter.histogram().contains("silenced: 1"), "the ring didn't settle");
        assertEquals(5, m[n - 1], 0, "all of it at the end");
    }

    /** Random transfer lists, rings and empty senders included. {C, y0, moves}. */
    static Object[] randomCase(Random r) {
        int n = 1 + r.nextInt(8), C = 2 + r.nextInt(3), S = C - 1;
        double[] y0 = new double[n * C];
        for (int i = 0; i < n * C; i++) {
            double mag = Math.pow(10, -12 + 14 * r.nextDouble());
            y0[i] = i % C == S ? (r.nextDouble() - 0.3) * 1e5 * mag : r.nextInt(3) == 0 ? (r.nextInt(4) == 0 ? -0.0 : 0) : r.nextInt(12) == 0 ? -1e-18 * mag : mag;
        }
        List<double[]> moves = new ArrayList<>();
        int count = r.nextInt(4 * n + 2);
        double base = Math.pow(10, -12 + 14 * r.nextDouble());
        for (int j = 0; j < count; j++) {
            int from = r.nextInt(n + 1) - 1, to = r.nextInt(n + 1) - 1;
            if (from == to) continue;
            // (often of one size, so that rings nearly balance and the scales settle slowly)
            double amt = r.nextInt(3) == 0 ? base * (1 + 0.1 * r.nextDouble()) : Math.pow(10, -12 + 14 * r.nextDouble());
            moves.add(mv(from, to, r.nextInt(C), amt));
        }
        return new Object[]{C, y0, moves};
    }

    @Test
    @SuppressWarnings("unchecked")
    void randomListsConserveAndStayPositive() throws Exception {
        Random r = new Random(5);
        Limiter.histogram();
        try (LimiterTap tap = LimiterTap.open("random")) {
            for (int c = 0; c < 20000; c++) {
                Object[] cs = randomCase(r);
                int C = (int) cs[0], S = C - 1;
                double[] y0 = (double[]) cs[1];
                List<double[]> moves = (List<double[]>) cs[2];
                double[][] seen = new double[1][];
                Limiter.TAP = (cc, a, b, passes, scale, y) -> {
                    seen[0] = scale;
                    tap.call(cc, a, b, passes, scale, y);
                };
                double[] y = Limiter.apply(C, y0, moves), scale = seen[0];
                double[] in = new double[y0.length], boundary = new double[C], size = new double[C];
                for (double[] m : moves) {
                    int k = (int) m[2];
                    double amt = m[3] * (m[0] >= 0 && k < S ? scale[C * (int) m[0] + k] : 1);
                    assertTrue(amt >= 0 && amt <= m[3], "case " + c + " moved " + amt + " of " + m[3]);
                    if (m[1] >= 0) in[C * (int) m[1] + k] += amt;
                    if (m[0] < 0) boundary[k] += amt;
                    if (m[1] < 0) boundary[k] -= amt;
                    size[k] += amt;
                }
                // conservation, per component: the free nodes' total moves by what fixed nodes sent less what they got
                for (int k = 0; k < C; k++) {
                    double before = 0, after = 0;
                    for (int i = k; i < y0.length; i += C) {
                        before += y0[i];
                        after += y[i];
                        size[k] += Math.abs(y0[i]);
                    }
                    assertEquals(before + boundary[k], after, 1e-12 * size[k], "case " + c + " component " + k);
                }
                // positivity: no mass lower than it started, except within rounding of what it had
                for (int i = 0; i < y0.length; i++) {
                    if (i % C == S) continue;
                    assertTrue(scale[i] >= 0 && scale[i] <= 1, "case " + c + " scale " + scale[i]);
                    assertTrue(y[i] >= Math.min(y0[i], 0) - 1e-13 * (Math.abs(y0[i]) + in[i]), "case " + c + " cell " + i + ": " + y[i] + " from " + y0[i] + " + " + in[i]);
                }
            }
        }
        System.out.println("LimiterTest random lists: " + Limiter.histogram());
    }

    /** Where no node over-sends, the scales stay 1 and the step ends on exactly Newton's fluxes: result = iterate - residual. */
    @Test
    void anUnlimitedStepEndsOnTheResidualsFluxes() {
        double dt = 0.05;
        int checked = 0;
        for (long seed = 1; seed <= 30; seed++) {
            for (boolean mix : new boolean[]{false, true}) {
                Network net = mix ? MixNetworkTest.level(seed) : NetworkTest.level(seed);
                for (var e : net.edges) e.conductance = 50;
                for (int s = 0; s < 5; s++) net.step(dt);
                for (var n : net.nodes) n.heat = 100;
                List<Network.Node> free = new ArrayList<>();
                for (var n : net.nodes) if (!n.fixed) free.add(n);
                Map<Network.Node, Integer> idx = new IdentityHashMap<>();
                for (int i = 0; i < free.size(); i++) idx.put(free.get(i), i);
                double[] y0 = net.pack(free);
                // an "iterate" a little off the start (no mass moved by more than 1e-6 of itself: nothing over-sends)
                Random r = new Random(seed);
                double[] it = y0.clone();
                for (int i = 0; i < it.length; i++) it[i] *= 1 + 1e-6 * (r.nextDouble() - 0.5);
                net.set(free, it);
                double[] res = net.residual(free, idx, y0, dt);
                int[] passes = {-1};
                Limiter.TAP = (C, a, b, p, scale, y) -> passes[0] = p;
                try {
                    net.finish(free, idx, y0, dt);
                } finally {
                    Limiter.TAP = null;
                }
                if (passes[0] != 0) continue;
                checked++;
                double[] y = net.pack(free);
                for (int i = 0; i < y.length; i++)
                    assertEquals(it[i] - res[i], y[i], 1e-12 * Math.max(Math.abs(y[i]), Math.abs(y0[i])) + 1e-300, "seed " + seed + (mix ? " mixed" : "") + " row " + i);
            }
        }
        assertTrue(checked >= 40, "unlimited cases checked: " + checked);
    }
}
