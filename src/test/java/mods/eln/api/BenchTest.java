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
    static final MachineSpec MILL = MachineSpec.consumer(200).ratedWatts(2000).idleWatts(20).build();
    static final MachineSpec DYNAMO = MachineSpec.producer(200).ratedWatts(1500).build();

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
            MachineSpec.Builder spec = MachineSpec.consumer(200).ratedWatts(2000).idleWatts(20);
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
        BenchMachine m = b.add(MachineSpec.consumer(200).ratedWatts(3000).idleWatts(40).onBrownout(Brownout.SLOW).build())
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

    /**
     * Several Brownout.SLOW machines of mixed ratings on one weak line, each learning the line from its own steps
     * while its neighbours start and stop (every converted macerator is SLOW, so a row of them on one line is the
     * common case). Switched on one by one, the last off and on again, the largest switching every 8 ticks, the line
     * weakened under load, then made stiff. At every tick the line is no lower than resistors at the machines'
     * ratings would hold it, and none draws less than its resistor would (no learnt line starves it); each phase settles (no oscillation), shared in proportion to the
     * ratings while no machine alone is heavier than the line; the switching leaves nothing behind; and on the stiff
     * line every machine draws its rating again. A row lighter than its line one by one but heavier together settles
     * at the machines' rated resistances, as resistors (and 1.12's machines) do: past the line's maximum power.
     */
    @Test
    void slowConsumersShareAWeakLine() {
        slowRow(new double[]{500, 1000, 2000}, 8);
        slowRow(new double[]{500, 1000, 2000, 3000}, 8);
        slowRow(new double[]{300, 800, 1500, 2000, 3000}, 5);
        slowRow(new double[]{2000, 2000, 2000, 2000}, 12);
        slowRow(new double[]{3000, 200, 400}, 20);
    }

    /**
     * Idle machines on a weak line refill their buffers without ringing (Baughn 2026-11-04): three STOP and three
     * SLOW machines powered up at once on 8 ohm, none asking, their refills together (1750 W) more than the line can
     * carry (1250 W). From power-up on, the line never drops more than 10 % in a tick (it rises when a buffer
     * is full and its refill stops, as when any load switches off), no machine leaves RUNNING
     * (no STOP machine falls under its floor), every buffer is full within 10 s, and the line is flat after.
     */
    @Test
    void idleMachinesRefillTogetherOnAWeakLine() {
        Bench b = ElnTestKit.bench().supply(200, 0.01).line(8);
        List<BenchMachine> ms = new ArrayList<>();
        for (double w : new double[]{500, 1000, 2000}) {
            ms.add(b.add(MachineSpec.consumer(200).ratedWatts(w).idleWatts(w / 100).build()));
            ms.add(b.add(MachineSpec.consumer(200).ratedWatts(w).idleWatts(w / 100).onBrownout(Brownout.SLOW).build()));
        }
        b.ticks(10);   // (the power-up hold)
        double last = b.volts();
        for (int t = 0; t < 200; t++) {
            b.tick();
            assertTrue(b.volts() > 0.9 * last, "no drop over 10 %, tick " + t + "\n" + b.trace());
            last = b.volts();
            for (int i = 0; i < ms.size(); i++)
                assertEquals(MachineState.RUNNING, ms.get(i).state(), "machine " + i + " keeps running, tick " + t + "\n" + b.trace());
        }
        for (int i = 0; i < ms.size(); i++)
            assertTrue(ms.get(i).bufferJoules() >= ms.get(i).bufferCapacityJoules() * (1 - 1e-6),
                "machine " + i + " full within 10 s\n" + b.trace());
        double u = b.volts();
        for (int t = 0; t < 40; t++) {
            b.tick();
            assertEquals(u, b.volts(), 1e-6 * u, "the line stays flat, tick " + t + "\n" + b.trace());
        }
    }

    /** A row of SLOW machines rated (W at 200 V) on a line of lineOhms; see slowConsumersShareAWeakLine. */
    private static void slowRow(double[] rated, double lineOhms) {
        SlowRow r = new SlowRow(rated, lineOhms);
        int n = rated.length, big = 0;
        r.run(60);   // (powered up a while before: their buffers refill on the weak line, then they idle)
        for (int i = 0; i < n; i++) {
            if (rated[i] > rated[big]) big = i;
            r.on[i] = true;
            r.run(60);
            r.settled("on " + (i + 1));
        }
        double[] all = r.last();
        r.on[n - 1] = false;
        r.run(80);   // (the one switched off refills its buffer first, slowly on a sagging line)
        r.settled("last off");
        r.on[n - 1] = true;
        r.run(60);
        r.settled("last on again");
        for (int t = 0; t < 120; t++) {
            if (t % 8 == 0) r.on[big] = !r.on[big];
            r.run(1);
        }
        r.on[big] = true;
        r.run(100);   // (it refills its buffer after the last off, slowly on a sagging line)
        r.settled("after the switching");
        double[] again = r.last();
        for (int i = 0; i < n; i++)
            assertEquals(all[i], again[i], 0.005 * all[i], r.what("the switching left machine " + i + " where it was"));
        r.line(lineOhms * 2.5);
        r.run(60);
        r.settled("line weakened");
        r.line(0.1);
        r.run(40);
        for (int i = 0; i < n; i++)
            assertTrue(r.ms.get(i).watts() > 0.95 * rated[i], r.what("on the stiff line machine " + i + " draws its rating"));
    }

    /** A bench with a row of SLOW machines whose owners ask a tick's rated work while on; checks every tick. */
    private static final class SlowRow {
        final double[] rated;
        final boolean[] on;
        final int[] onFor, offFor;
        final Bench b;
        final List<BenchMachine> ms = new ArrayList<>();
        int[] fullFor;
        final List<double[]> watts = new ArrayList<>();
        double lineOhms;
        /** Ticks since the line last changed: a machine plans on the line it saw, so it may take a few to follow. */
        int sinceLine = 1000;

        SlowRow(double[] rated, double lineOhms) {
            this.rated = rated;
            this.lineOhms = lineOhms;
            on = new boolean[rated.length];
            onFor = new int[rated.length];
            offFor = new int[rated.length];
            fullFor = new int[rated.length];
            b = ElnTestKit.bench().supply(200, 0.01).line(lineOhms);
            for (int i = 0; i < rated.length; i++) {
                int k = i;
                ms.add(b.add(MachineSpec.consumer(200).ratedWatts(rated[i]).idleWatts(rated[i] / 100)
                    .onBrownout(Brownout.SLOW).build()).owner(m -> m.takeJoules(on[k] ? m.ratedJoulesPerTick() : 0)));
            }
        }

        void line(double ohms) {
            lineOhms = ohms;
            sinceLine = 0;
            b.line(ohms);
        }

        /** n ticks, each checked against the resistor model: the line and every machine on for a second. */
        void run(int n) {
            for (int t = 0; t < n; t++) {
                b.tick();
                sinceLine++;
                double u = b.volts(), g = 0;
                double[] w = new double[rated.length];
                for (int i = 0; i < rated.length; i++) {
                    onFor[i] = on[i] ? onFor[i] + 1 : 0;
                    offFor[i] = on[i] ? 0 : offFor[i] + 1;
                    w[i] = ms.get(i).watts();
                    // (a machine just switched off still draws a tick or two, and one off refills its buffer until it is
                    // full (a quarter of its rating at most, slower on a sagging line): at most a resistor at its rating;
                    // then it draws its idle watts)
                    var m = ms.get(i);
                    boolean full = m.bufferJoules() >= 0.999 * m.bufferCapacityJoules();
                    fullFor[i] = full ? fullFor[i] + 1 : 0;   // (the step that filled it still drew)
                    g += offFor[i] < 5 || fullFor[i] < 2 ? rated[i] / (200 * 200) : rated[i] / 100 / (u * u);
                }
                watts.add(w);
                double ur = 200 / (1 + g * (lineOhms + 0.01));   // (the machines as resistors at their ratings)
                assertTrue(u > 0.98 * ur, what("the line held at least where resistors would: " + u + " V vs " + ur));
                double total = 0, model = 0;
                for (int i = 0; i < rated.length; i++) {
                    if (onFor[i] <= 20 || sinceLine <= 10) continue;
                    double r = ur * ur * rated[i] / (200 * 200);
                    total += w[i];
                    model += r;
                    assertTrue(w[i] > 0.99 * r, what("machine " + i + " draws at least what a resistor at its rating would: " + w[i] + " W, as a resistor " + r + " W"));
                }
                assertTrue(total >= 0.99 * model, what("together they draw at least what resistors would: " + total + " W vs " + model));
            }
        }

        /**
         * The last 40 ticks: every machine's draw flat, and, while each machine alone is lighter than the line (its rated
         * resistance over the line's), those on sharing in proportion to their ratings, as resistors would. A heavier
         * one takes the most its line gives with the others drawing (its own learnt maximum power), which is less than
         * its share but more than a resistor's (run's checks).
         */
        void settled(String phase) {
            double least = Double.POSITIVE_INFINITY, most = 0;
            boolean light = true;
            for (int i = 0; i < rated.length; i++) light &= !on[i] || 200 * 200 / rated[i] > lineOhms;
            for (int i = 0; i < rated.length; i++) {
                double lo = Double.POSITIVE_INFINITY, hi = 0;
                for (double[] w : watts.subList(watts.size() - 40, watts.size())) {
                    lo = Math.min(lo, w[i]);
                    hi = Math.max(hi, w[i]);
                }
                assertTrue(hi - lo <= 0.01 * hi + 1e-6, what(phase + ": machine " + i + " settled, " + lo + ".." + hi + " W"));
                if (!on[i]) continue;
                least = Math.min(least, hi / rated[i]);
                most = Math.max(most, hi / rated[i]);
            }
            if (light) assertTrue(most <= 1.01 * least, what(phase + ": they share in proportion to their ratings: " + least + ".." + most + " W/W"));
        }

        double[] last() {
            return watts.get(watts.size() - 1).clone();
        }

        String what(String check) {
            return java.util.Arrays.toString(rated) + " W on " + lineOhms + " ohm, tick " + b.tickCount() + ": " + check
                + "\n" + b.trace().lines().skip(Math.max(0, b.tickCount() - 30)).collect(java.util.stream.Collectors.joining("\n"));
        }
    }

    /** The bench sets the ambient its machines' heat models start at and cool to. */
    @Test
    void benchAmbient() {
        Bench b = ElnTestKit.bench().supply(200, 0.01).ambientCelsius(35);
        BenchMachine m = b.add(MachineSpec.consumer(200).ratedWatts(100).heatCapacityJPerK(10).heatLossWPerK(1).overheatRiseKelvin(60).build());
        b.ticks(5);
        assertEquals(35, m.celsius(), 1e-9);
        b.ambientCelsius(0);
        b.ticks(2000);
        assertEquals(0, m.celsius(), 0.1);
    }

    /**
     * The Linear Servo Drive's own spec in the Nether (EA's ambient there is 90 C): idle it settles 10 K over its
     * ambient, far under its trip 80 K over it (170 C); heated by its motor's losses it trips there, holds, and runs
     * again a cooldown after it is back under 140 C (its 30 K band). With a trip at a fixed 100 C it would idle at it.
     */
    @Test
    void servoDriveInTheNether() {
        Bench b = ElnTestKit.bench().supply(200, 0.01).ambientCelsius(90);
        double[] heat = {0};
        List<TripCause> trips = new ArrayList<>();
        BenchMachine m = b.add(mods.eln.robotics.servo.LinearServoDriveDescriptor.spec()).owner(x -> x.heatJoules(heat[0]));
        m.onTrip(trips::add);
        b.ticks(100_000);   // (five time constants: 4000 J/K over 4 W/K)
        assertEquals(MachineState.RUNNING, m.state());
        assertTrue(trips.isEmpty(), trips::toString);
        assertEquals(100, m.celsius(), 0.1, "idle: 40 W over 4 W/K above 90 C");
        heat[0] = 50;   // (1 kW of motor losses)
        for (int i = 0; i < 100_000 && trips.isEmpty(); i++) b.tick();
        assertEquals(1, trips.size());
        assertEquals(MachineState.OVERHEAT_TRIPPED, trips.get(0).state());
        assertEquals(170, trips.get(0).limit(), 1e-9);
        heat[0] = 0;
        int n = 0;
        double coolest = Double.POSITIVE_INFINITY;
        while (m.state() == MachineState.OVERHEAT_TRIPPED && n++ < 100_000) {
            b.tick();
            coolest = Math.min(coolest, m.celsius());
        }
        assertEquals(MachineState.RUNNING, m.state());
        assertTrue(coolest < 140 && coolest > 139, "reset under 140 C: " + coolest);
        assertEquals(1, trips.size());
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
        BenchMachine m = b.add(MachineSpec.consumer(200).ratedWatts(100)
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
        BenchMachine m = b.add(MachineSpec.consumer(50).ratedWatts(100).bus(RelativeFace.BACK).build());
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
