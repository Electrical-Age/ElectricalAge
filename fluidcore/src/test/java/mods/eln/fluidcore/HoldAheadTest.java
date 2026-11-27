package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Newton's holds ahead (Network.HOLD_AHEAD): the rows held at zero in the last iteration are held before the solve.
 * Against the old way (found by a solve each time): the same end state, about half the solves on plants where the same
 * components sit at zero every iteration; a hold that fails its sign test is undone; a component held for many steps
 * comes up in the step its inflow starts.
 */
class HoldAheadTest {
    static Network replay(String name, double[] dt) throws Exception {
        try (var in = HoldAheadTest.class.getResourceAsStream("/replay/" + name + ".txt")) {
            return NetworkDump.read(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), dt);
        }
    }

    /** Steps a dump n times with the game's settings, holds ahead or not. */
    static Network run(String name, int n, boolean ahead) throws Exception {
        int depth = Network.MAX_DEPTH;
        boolean cut = Network.CUT_SHORT, keep = Network.HOLD_AHEAD;
        try {
            Network.MAX_DEPTH = 8;
            Network.CUT_SHORT = true;
            Network.HOLD_AHEAD = ahead;
            double[] dt = new double[1];
            Network net = replay(name, dt);
            for (int i = 0; i < n; i++) net.step(dt[0]);
            return net;
        } finally {
            Network.MAX_DEPTH = depth;
            Network.CUT_SHORT = cut;
            Network.HOLD_AHEAD = keep;
        }
    }

    @Test
    void sameEndStateForFewerSolves() throws Exception {
        for (String name : new String[]{"plant69", "baughn-workshop-stuck", "still-sensible-1200"}) {
            Network old = run(name, 100, false), ahead = run(name, 100, true);
            for (int i = 0; i < old.nodes.size(); i++) {
                Network.Node a = old.nodes.get(i), b = ahead.nodes.get(i);
                assertEquals(a.U, b.U, 1e-8 * Math.max(1, Math.abs(a.U)), name + " node " + i + " energy");
                for (int k = 0; k < a.m.length; k++) assertEquals(a.m[k], b.m[k], 1e-8 * Math.max(1e-9, Math.abs(a.m[k])), name + " node " + i + " mass " + k);
            }
            assertEquals(0, ahead.stats.failures, name + ": failures");
            assertEquals(old.stats.substeps, ahead.stats.substeps, name + ": sub-steps");
            System.out.printf("%s: solves %d -> %d for %d iterations%n", name, old.stats.solves, ahead.stats.solves, ahead.stats.iterations);
            assertTrue(ahead.stats.solves <= 0.8 * old.stats.solves, name + ": solves " + ahead.stats.solves + " against " + old.stats.solves);
        }
    }

    /** A pipe flooding from 6 bar: in its first step holds from the iteration before fail their sign test and are undone. */
    @Test
    void aHoldThatFailsItsSignTestIsUndone() throws Exception {
        Network old = run("flood-pipe-from-6-bar", 1, false), ahead = run("flood-pipe-from-6-bar", 1, true);
        assertTrue(ahead.stats.holdsWrong > 0, "holds undone: " + ahead.stats.holdsWrong);
        for (int i = 0; i < old.nodes.size(); i++) {
            assertEquals(old.nodes.get(i).U, ahead.nodes.get(i).U, 0.0, "node " + i + " energy, to the bit");
            for (int k = 0; k < old.nodes.get(i).m.length; k++) assertEquals(old.nodes.get(i).m[k], ahead.nodes.get(i).m[k], 0.0, "node " + i + " mass " + k);
        }
    }

    /** Two vessels of water and steam with no air, at rest for 200 steps (air held at zero throughout); then air is put into one: it reaches the other in that step, as without holds ahead. */
    @Test
    void aComponentHeldForLongComesUpWhenItsInflowStarts() {
        double[] airInB = new double[2];
        long[] solves = new long[2], iterations = new long[2];
        boolean keep = Network.HOLD_AHEAD;
        try {
            for (int mode = 0; mode < 2; mode++) {
                Network.HOLD_AHEAD = mode == 1;
                Network net = new Network();
                var a = net.add(new Network.Node(0.05, 0, 0.5, false).fill(20, 0, 400));
                var b = net.add(new Network.Node(0.05, 0, 0.5, false).fill(10, 0, 380));
                net.connect(a, 0.9, b, 0.9, 1e-7);
                net.connect(a, 0.1, b, 0.1, 1e-9);
                for (int i = 0; i < 200; i++) {
                    a.heat = 2000;   // (kept from sleeping: steam passes every step)
                    net.step(0.05);
                }
                solves[mode] = net.stats.solves;
                iterations[mode] = net.stats.iterations;
                assertEquals(0, b.m[1], 1e-15, "no air yet");
                a.m[1] += 0.01;
                a.update();
                a.heat = 2000;
                net.step(0.05);
                airInB[mode] = b.m[1];
            }
        } finally {
            Network.HOLD_AHEAD = keep;
        }
        assertTrue(airInB[0] > 1e-7, "air reaches the other vessel in the step it arrives (old way): " + airInB[0]);
        assertEquals(airInB[0], airInB[1], 1e-6 * airInB[0], "... and the same with holds ahead");
        // (no gain here, and none claimed: with no air at all its rows are stepped up from zero each iteration, which
        // isn't held ahead; 747 -> 926 solves when this was written)
        System.out.printf("two vessels: solves %d -> %d for %d iterations%n", solves[0], solves[1], iterations[1]);
    }
}
