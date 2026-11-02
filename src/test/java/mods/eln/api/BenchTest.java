package mods.eln.api;

import mods.eln.api.testkit.Bench;
import mods.eln.api.testkit.BenchMachine;
import mods.eln.api.testkit.ElnTestKit;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Machines in EA's real simulator (ElnTestKit.bench()): supplies, weak lines, wrong tiers, trips, producers. */
public class BenchTest {
    static final MachineSpec MILL = MachineSpec.consumer(Tier.V200).ratedWatts(2000).idleWatts(20).build();
    static final MachineSpec DYNAMO = MachineSpec.producer(Tier.V200).ratedWatts(1500).build();

    /** An owner that asks for joules every tick and keeps what it got. */
    static final class Taker {
        final double ask;
        final List<Double> got = new ArrayList<>();

        Taker(double ask) {
            this.ask = ask;
        }

        void tick(Machine m) {
            got.add(m.takeJoules(ask));
        }

        double last() {
            return got.get(got.size() - 1);
        }
    }

    @Test
    void stiffSupplyDrawsWhatItAsks() {
        Bench b = ElnTestKit.bench().supply(200, 0.01);
        Taker t = new Taker(50);
        BenchMachine m = b.add(MILL).owner(t::tick);
        b.ticks(40);
        assertEquals(MachineState.RUNNING, m.state());
        assertEquals(1020, m.watts(), 1020 * 1e-3); // 50 J a tick plus idle
        assertEquals(1020 / 200.0, b.supplyAmps(), 0.01);
        // half a second of power-up hold, one tick of lag, then every ask is met
        for (int i = 0; i < 10; i++) assertEquals(0, t.got.get(i), 1e-3, "tick " + i); // (the 1 Gohm open load's leak)
        for (int i = 12; i < 40; i++) assertEquals(50, t.got.get(i), 1e-6, "tick " + i);
        assertEquals(1, m.voltageRatio(), 1e-3);
    }

    @Test
    void weakLineSagsAndShares() {
        Bench b = ElnTestKit.bench().supply(200, 0.1).line(2);
        Taker ta = new Taker(50), tc = new Taker(0);
        BenchMachine a = b.add(MILL).owner(ta::tick), c = b.add(MILL).owner(tc::tick);
        b.ticks(30);
        double alone = b.volts();
        assertTrue(alone < 195, "one machine sags the line: " + alone);
        tc = new Taker(50);
        c.owner(tc::tick);
        b.ticks(40);
        double both = b.volts();
        assertTrue(both < alone - 5, "two sag it more: " + both + " vs " + alone);
        assertEquals(a.watts(), c.watts(), a.watts() * 1e-3);
        for (int i = 20; i < 40; i++) {
            assertEquals(50, tc.got.get(i), 1e-6);
            assertEquals(50, ta.got.get(30 + i), 1e-6);
        }
        assertEquals(MachineState.RUNNING, a.state());
        assertEquals(both / 200, a.voltageRatio(), 1e-9);
    }

    @Test
    void wrongTierIsUndervoltage() {
        Bench b = ElnTestKit.bench().supply(50, 0.01);
        Taker t = new Taker(50);
        BenchMachine m = b.add(MILL).owner(t::tick);
        b.ticks(20);
        assertEquals(MachineState.UNDERVOLTAGE, m.state());
        assertTrue(t.got.stream().allMatch(g -> g == 0));
        assertEquals(20, m.watts(), 0.1); // idle only
        assertEquals(0, m.voltageRatio());
    }

    @Test
    void overvoltageTripsAndCoolsDown() {
        Bench b = ElnTestKit.bench().supply(200, 0.01);
        Taker t = new Taker(50);
        BenchMachine m = b.add(MILL).owner(t::tick);
        List<TripCause> trips = new ArrayList<>();
        m.onTrip(trips::add);
        b.ticks(20);
        b.supplyVolts(280);
        b.tick();
        assertEquals(MachineState.OVERVOLTAGE_TRIPPED, m.state());
        b.ticks(10);
        assertEquals(1, trips.size());
        assertEquals(MachineState.OVERVOLTAGE_TRIPPED, trips.get(0).state());
        assertEquals(0, m.amps(), 1e-3); // open: only the 1 Gohm high-impedance leak
        assertEquals(0, t.last());
        b.supplyVolts(200);
        b.ticks(59); // the first tick at 200 V is the first of the 60-tick cooldown
        assertEquals(MachineState.OVERVOLTAGE_TRIPPED, m.state());
        b.tick();
        assertEquals(MachineState.RUNNING, m.state());
        b.ticks(3);
        assertEquals(50, t.last(), 1e-6);
        assertEquals(1, trips.size());
    }

    /** A producer feeding a consumer: what the consumer takes <= what the line delivered <= what the source fed <= what was given. */
    @Test
    void producerFeedsConsumer() {
        Bench b = ElnTestKit.bench();
        BenchMachine dynamo = b.add(DYNAMO).owner(m -> m.giveJoules(60));
        Taker t = new Taker(50);
        BenchMachine mill = b.add(MILL).owner(t::tick);
        b.ticks(200);
        // the mill's buffer refill outruns the dynamo's 180 W surplus: it backs off instead of collapsing the line
        assertFalse(b.trace().contains("UNDERVOLTAGE"), b.trace());
        assertEquals(MachineState.RUNNING, mill.state());
        for (int i = 25; i < 200; i++) assertEquals(50, t.got.get(i), 1e-6, "tick " + i);
        double taken = t.got.stream().mapToDouble(Double::doubleValue).sum();
        double delivered = mill.joulesDelivered(), fed = dynamo.joulesDelivered(), given = dynamo.joulesGiven();
        assertEquals(taken, mill.joulesTaken(), 1e-9 * taken);
        assertEquals(0, mill.joulesGiven());
        assertEquals(0, dynamo.joulesTaken());
        assertEquals(delivered, mill.joulesTaken() + mill.bufferJoules() + mill.joulesLost(), 1e-9 * delivered);
        assertEquals(given, fed + dynamo.bufferJoules() + dynamo.joulesLost(), 1e-9 * given);
        assertEquals(2 * mill.ratedJoulesPerTick(), mill.bufferCapacityJoules(), 1e-12);
        assertTrue(taken <= delivered, taken + " taken > " + delivered + " delivered");
        assertTrue(delivered <= fed, delivered + " delivered > " + fed + " fed");
        assertTrue(fed <= given + 1e-6, fed + " fed > " + given + " given");
        assertEquals(given - fed, dynamo.bufferJoules(), 1e-6);
        assertTrue(fed - delivered < 0.06 * fed, "droop losses " + (fed - delivered) + " of " + fed);
        assertTrue(b.volts() > 199 && b.volts() < 210, "line " + b.volts());
    }

    /**
     * A dynamo given 40 J a tick (800 W) feeding a mill that asks 50 J a tick (1000 W, plus 20 W idle): with
     * Brownout.STOP (the default) the line falls under the floor and the mill stops and restarts; with
     * Brownout.SLOW it settles on what the dynamo gives and takes all of it (holding the line at 160 V: it draws no
     * more than the line can give).
     */
    @Test
    void starvedProducerStopOrSlow() {
        for (Brownout mode : Brownout.values()) {
            Bench b = ElnTestKit.bench();
            BenchMachine dynamo = b.add(DYNAMO).owner(m -> m.giveJoules(40));
            Taker t = new Taker(50);
            MachineSpec.Builder spec = MachineSpec.consumer(Tier.V200).ratedWatts(2000).idleWatts(20);
            BenchMachine mill = b.add(spec.onBrownout(mode).build()).owner(t::tick);
            b.ticks(200);
            List<String> states = b.trace().lines().skip(100).map(l -> l.substring(l.lastIndexOf(' ') + 1)).toList();
            double late = t.got.subList(100, 200).stream().mapToDouble(Double::doubleValue).sum();
            if (mode == Brownout.STOP) {
                assertTrue(states.contains("UNDERVOLTAGE") && states.contains("RUNNING"), "stops and restarts: " + states);
            } else {
                assertTrue(states.stream().allMatch("RUNNING"::equals), "keeps running: " + states);
                // below nominal, but not pulled down to its rated resistance: it plans on the line it learnt
                assertTrue(b.volts() < 180, "on a sagging line: " + b.volts());
                // the dynamo's energy reaches the owner, less the idle watts and the droop loss (about 7% at this current)
                double most = (800 - 20) * 0.05 * 100;
                assertTrue(late <= most && late > 0.9 * most, "taken in the last 100 ticks: " + late + " of " + most);
            }
            assertTrue(dynamo.joulesDelivered() <= dynamo.joulesGiven() + 1e-6);
            assertTrue(mill.joulesDelivered() <= dynamo.joulesDelivered());
        }
    }

    /**
     * A Brownout.SLOW consumer asking more than its line can carry (a 47 ohm line: at most 213 W at 100 V) learns the
     * line and draws its maximum power, on the stiff side; planning on the sagged volts alone, it would pull the line
     * down to its own rated resistance (13 ohm: 44 V, 145 W).
     */
    @Test
    void slowConsumerStopsAtTheLinesMaximumPower() {
        Bench b = ElnTestKit.bench().supply(200, 0.01).line(47);
        Taker t = new Taker(50); // 1000 W
        BenchMachine m = b.add(MachineSpec.consumer(Tier.V200).ratedWatts(3000).idleWatts(40).onBrownout(Brownout.SLOW).build())
            .owner(t::tick);
        b.ticks(40);
        double lowest = Double.POSITIVE_INFINITY;
        for (int i = 0; i < 100; i++) {
            b.tick();
            lowest = Math.min(lowest, m.volts());
        }
        double most = 200 * 200 / (4 * 47.01);
        assertTrue(lowest > 0.45 * 200, "the line held near half its volts: lowest " + lowest + "\n" + b.trace());
        assertEquals(most, m.watts(), 0.03 * most, "it draws the line's maximum power");
        assertEquals(MachineState.RUNNING, m.state());
    }

    /** The bench sets the ambient its machines' heat models start at and cool to. */
    @Test
    void benchAmbient() {
        Bench b = ElnTestKit.bench().supply(200, 0.01).ambientCelsius(35);
        BenchMachine m = b.add(MachineSpec.consumer(Tier.V200).ratedWatts(100).heatCapacityJPerK(10).heatLossWPerK(1).overheatCelsius(80).build());
        b.ticks(5);
        assertEquals(35, m.celsius(), 1e-9);
        b.ambientCelsius(0);
        b.ticks(2000);
        assertEquals(0, m.celsius(), 0.1);
    }

    /** Two producers in parallel (a singular matrix if they were ideal sources) share a load bigger than either's rating. */
    @Test
    void parallelProducersShare() {
        Bench b = ElnTestKit.bench();
        BenchMachine d1 = b.add(DYNAMO).owner(m -> m.giveJoules(60)), d2 = b.add(DYNAMO).owner(m -> m.giveJoules(60));
        Taker t = new Taker(80); // 1600 W, more than one dynamo's 1500
        BenchMachine mill = b.add(MILL).owner(t::tick);
        b.ticks(100);
        assertTrue(Double.isFinite(b.volts()) && b.volts() > 190, "line " + b.volts());
        assertEquals(80, t.last(), 1e-6);
        double w1 = d1.watts(), w2 = d2.watts();
        assertEquals(w1, w2, 0.05 * (w1 + w2), "share: " + w1 + " W and " + w2 + " W");
        assertEquals(mill.watts(), w1 + w2, 0.01 * mill.watts());
        assertTrue(d1.joulesDelivered() <= d1.joulesGiven() + 1e-6 && d2.joulesDelivered() <= d2.joulesGiven() + 1e-6);
    }

    /** A producer that is given nothing is no load on the line, and an empty one stops feeding. */
    @Test
    void idleProducerIsNoLoad() {
        Bench b = ElnTestKit.bench().supply(200, 0.01);
        BenchMachine d = b.add(DYNAMO);
        b.ticks(10);
        assertEquals(0, d.amps(), 1e-6);
        assertEquals(0, b.supplyAmps(), 1e-3);
        assertEquals(0, d.joulesDelivered());
    }

    @Test
    void ownerRules() {
        Bench b = ElnTestKit.bench().supply(200, 0.01);
        BenchMachine m = b.add(MachineSpec.consumer(Tier.V200).ratedWatts(100)
            .signalIn("enable", RelativeFace.UP, 1).signalOut("done", RelativeFace.DOWN).build());
        assertEquals(1, m.signal("enable"));
        assertFalse(m.signalConnected("enable"));
        m.input("enable", 0.25);
        assertEquals(0.25, m.signal("enable"));
        assertTrue(m.signalConnected("enable"));
        assertThrows(IllegalArgumentException.class, () -> m.signalConnected("done"));
        assertTrue(m.output("done").isEmpty());
        m.setSignal("done", 1);
        assertEquals(1, m.output("done").orElseThrow());
        m.releaseSignal("done");
        assertTrue(m.output("done").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> m.setSignal("done", 5));
        assertThrows(IllegalArgumentException.class, () -> m.signal("done"));
        assertThrows(IllegalArgumentException.class, () -> m.signal("nope"));
        assertThrows(IllegalStateException.class, m::bus);
        assertThrows(IllegalStateException.class, () -> m.giveJoules(1));
        assertFalse(m.view() instanceof Machine);
        assertFalse(m.view() instanceof mods.eln.api.impl.MachineCore);
        // nothing public on the view hands back what it reads
        for (java.lang.reflect.Method x : m.view().getClass().getMethods())
            assertFalse(MachineView.class.isAssignableFrom(x.getReturnType()), x.toString());
        AtomicReference<Throwable> off = new AtomicReference<>();
        Thread th = new Thread(() -> {
            try {
                m.takeJoules(1);
            } catch (Throwable e) {
                off.set(e);
            }
        });
        th.start();
        try {
            th.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertNotNull(off.get());
        assertEquals(IllegalStateException.class, off.get().getClass());
    }

    @Test
    void busIsDoubleBuffered() {
        Bench b = ElnTestKit.bench();
        BenchMachine m = b.add(MachineSpec.consumer(Tier.V50).ratedWatts(100).bus(RelativeFace.BACK).build());
        m.owner(x -> x.bus().write(3, 0.5));
        b.tick();
        assertEquals(0, m.bus().read(3));
        b.tick();
        assertEquals(0.5, m.bus().read(3));
        assertThrows(IllegalArgumentException.class, () -> m.bus().write(16, 0));
        assertThrows(IllegalArgumentException.class, () -> m.bus().write(0, 2));
    }

    /**
     * Golden trace: a mill on a weak line, idle for 10 ticks, then asking 80 J a tick for 30, then idle for 20.
     * Regenerate deliberately with -PapiTraces (writes src/test/resources/mods/eln/api/step-load.trace).
     */
    @Test
    void stepLoadTrace() throws IOException {
        Bench b = ElnTestKit.bench().supply(200, 0.1).line(1.5);
        BenchMachine m = b.add(MILL);
        m.owner(x -> {
            if (b.tickCount() >= 10 && b.tickCount() < 40) x.takeJoules(80);
        });
        b.ticks(60);
        String trace = "# tick volts | amps buffer state\n" + b.trace();
        String out = System.getProperty("eln.api.traces");
        if (out != null) {
            Files.writeString(Path.of(out, "step-load.trace"), trace);
            return;
        }
        try (InputStream in = BenchTest.class.getResourceAsStream("/mods/eln/api/step-load.trace")) {
            assertNotNull(in, "no golden trace: run the tests with -PapiTraces once");
            assertEquals(new String(in.readAllBytes(), StandardCharsets.UTF_8), trace,
                "step-load trace changed; if intended, regenerate with -PapiTraces and review the diff");
        }
    }
}
