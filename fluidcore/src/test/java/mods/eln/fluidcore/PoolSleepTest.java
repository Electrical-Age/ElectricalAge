package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M3: the worker pool gives the sequential result; settled networks sleep, and wake when something changes. */
class PoolSleepTest {
    @Test
    void poolMatchesSequentialSteps() {
        List<Network> a = new ArrayList<>(), b = new ArrayList<>();
        for (long seed = 1; seed <= 24; seed++) {
            a.add(NetworkTest.level(seed));
            b.add(seed % 2 == 0 ? NetworkTest.level(seed) : MixNetworkTest.level(seed));
        }
        // (b alternates in mixed networks so the pool also runs MixFluid; compare the water/air ones and the mixed ones each to their own sequential twin)
        List<Network> seqA = new ArrayList<>(), seqB = new ArrayList<>();
        for (long seed = 1; seed <= 24; seed++) {
            seqA.add(NetworkTest.level(seed));
            seqB.add(seed % 2 == 0 ? NetworkTest.level(seed) : MixNetworkTest.level(seed));
        }
        try (FluidPool pool = new FluidPool(4)) {
            for (int s = 0; s < 100; s++) {
                pool.stepAll(a, 0.05);
                pool.stepAll(b, 0.05);
            }
        }
        for (int s = 0; s < 100; s++) {
            for (Network n : seqA) n.step(0.05);
            for (Network n : seqB) n.step(0.05);
        }
        for (int i = 0; i < a.size(); i++) {
            for (int k = 0; k < a.get(i).nodes.size(); k++) assertEquals(seqA.get(i).nodes.get(k).U, a.get(i).nodes.get(k).U, 0.0, "net " + i + " node " + k);
            for (int k = 0; k < b.get(i).nodes.size(); k++) assertEquals(seqB.get(i).nodes.get(k).U, b.get(i).nodes.get(k).U, 0.0, "mixed net " + i + " node " + k);
        }
    }

    @Test
    void settledNetworkSleepsAndWakes() {
        Network net = new Network();
        var a = net.add(new Network.Node(0.1, 0, 1, false).fill(40, 0.12, 300));
        var b = net.add(new Network.Node(0.1, 0, 1, false).fill(40, 0.12, 300));
        net.connect(a, 0, b, 0, 1e-6);
        int s = 0;
        while (!net.asleep() && s < 2000) {
            net.step(0.05);
            s++;
        }
        assertTrue(net.asleep(), "a network at rest goes to sleep");
        double u = a.U;
        long slept = net.stats.slept;
        for (int k = 0; k < 50; k++) net.step(0.05);
        assertEquals(slept + 50, net.stats.slept, "asleep, its steps are skipped");
        assertEquals(u, a.U, 0.0);
        a.heat = 1000;   // (an outside change: the signature sees it)
        net.step(0.05);
        assertFalse(net.asleep(), "heat wakes it");
        assertTrue(a.U > u, "and it steps again");
    }
}
