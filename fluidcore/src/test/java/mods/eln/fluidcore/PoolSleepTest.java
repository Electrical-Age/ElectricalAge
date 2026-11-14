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

    static int stepsToSleep(Network net, int most) {
        int s = 0;
        while (!net.asleep() && s < most) {
            net.step(0.05);
            s++;
        }
        return s;
    }

    /** A dead end behind a shut check valve keeps its contents and sleeps (the softplus's tail drained it for an hour). */
    @Test
    void deadEndBehindCheckValveSleeps() {
        Network net = new Network();
        var stub = net.add(new Network.Node(2e-5, 0, 0.1, false).fill(0, 2.4e-5, 293.15));
        var vessel = net.add(new Network.Node(0.016, 0, 1, false).fill(0, 0.0193, 293.15));
        net.connect(stub, 0.5, vessel, 0.5, 8.5e-7).kind = Network.Kind.CHECK;
        double air = stub.m[1];
        assertTrue(stepsToSleep(net, 400) < 400, "it sleeps: restless " + net.restless);
        assertEquals(air, stub.m[1], 1e-6 * air, "and the stub kept its air");
    }

    /** A network asleep round a stopped pump or a shut turbine wakes when the device starts. */
    @Test
    void pumpStartingWakesIt() {
        Network net = new Network();
        var a = net.add(new Network.Node(0.1, 0, 1, false).fill(40, 0.07, 300));
        var b = net.add(new Network.Node(0.1, 0, 1, false).fill(40, 0.07, 300));
        var pump = PumpTest.pump(net, a, b, 1, 1e5);
        pump.speed = 0;
        assertTrue(stepsToSleep(net, 2000) < 2000, "at rest it sleeps");
        double m = b.m[0];
        pump.speed = 1;
        net.step(0.05);
        assertFalse(net.asleep(), "the pump's speed wakes it");
        assertTrue(b.m[0] > m, "and it pumps");
    }

    /** A conductance a rounding apart (a device set it from the state the last step left) is the same input; a real change is not. */
    @Test
    void roundingDoesNotWakeIt() {
        Network net = new Network();
        var a = net.add(new Network.Node(0.1, 0, 1, false).fill(40, 0.12, 300));
        var b = net.add(new Network.Node(0.1, 0, 1, false).fill(40, 0.12, 300));
        var e = net.connect(a, 0, b, 0, 1e-6);
        assertTrue(stepsToSleep(net, 2000) < 2000, "at rest it sleeps");
        e.g *= 1 + 1e-12;
        net.step(0.05);
        assertTrue(net.asleep(), "a rounding's difference leaves it asleep");
        e.g *= 1 + 1e-6;
        net.step(0.05);
        assertFalse(net.asleep(), "a change wakes it");
    }
}
