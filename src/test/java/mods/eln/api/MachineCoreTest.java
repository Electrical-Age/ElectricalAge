package mods.eln.api;

import mods.eln.api.impl.MachineCore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** MachineCore by example: misuse, the one-tick lag, the refill, and each trip with its cooldown. */
public class MachineCoreTest {
    static final double DT = MachineCore.TICK_SECONDS;
    static final MachineSpec MILL = MachineSpec.consumer(Tier.V200).ratedWatts(2000).idleWatts(20).build();
    static final MachineSpec DYNAMO = MachineSpec.producer(Tier.V200).ratedWatts(1000).build();

    /** One tick on an ideal line at volts: the step delivers what the last plan draws at those volts. */
    static double step(MachineCore c, double volts, double[] ohms) {
        c.afterStep(volts, Double.isInfinite(ohms[0]) ? 0 : volts * volts / ohms[0] * DT);
        return volts;
    }

    static double plan(MachineCore c, double[] ohms) {
        double p = c.beforeStep();
        ohms[0] = c.loadOhms();
        return p;
    }

    @Test
    void misuseThrows() {
        MachineCore c = new MachineCore(MILL);
        for (double bad : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> c.takeJoules(bad));
            assertThrows(IllegalArgumentException.class, () -> c.tryTakeJoules(bad));
            assertThrows(IllegalArgumentException.class, () -> c.heatJoules(bad));
            assertThrows(IllegalArgumentException.class, () -> c.afterStep(200, bad));
        }
        assertThrows(IllegalArgumentException.class, () -> c.afterStep(Double.NaN, 0));
        assertThrows(IllegalStateException.class, () -> c.giveJoules(1));
        MachineCore p = new MachineCore(DYNAMO);
        assertThrows(IllegalStateException.class, () -> p.takeJoules(1));
        assertThrows(IllegalStateException.class, () -> p.tryTakeJoules(1));
        assertThrows(IllegalArgumentException.class, () -> p.giveJoules(-1));
        // a step bigger than the buffer could never succeed
        String m = assertThrows(IllegalArgumentException.class, () -> c.tryTakeJoules(500)).getMessage();
        assertTrue(m.contains("can never succeed"), m);
    }

    @Test
    void ratedJoulesPerTickIsWorkOnly() {
        MachineCore c = new MachineCore(MILL);
        assertEquals((2000 - 20) * DT, c.ratedJoulesPerTick(), 1e-12);
        assertEquals(2 * c.ratedJoulesPerTick(), c.bufferCapacityJoules(), 1e-12);
        assertEquals(1000 * DT, new MachineCore(DYNAMO).ratedJoulesPerTick(), 1e-12);
    }

    /** Idle draws idle watts only; an ask shows in the very next plan; the buffer then refills by a quarter of rated work per tick. */
    @Test
    void askSizesTheNextTick() {
        MachineCore c = new MachineCore(MILL);
        double[] ohms = {Double.POSITIVE_INFINITY};
        step(c, 200, ohms);
        assertEquals(MachineState.RUNNING, c.state());
        assertEquals(20, plan(c, ohms), 1e-9);
        step(c, 200, ohms);
        assertEquals(20 * DT, c.joulesIn(), 1e-9);
        assertEquals(0, c.bufferJoules(), 1e-12);
        assertFalse(c.tryTakeJoules(40)); // nothing buffered yet, but the ask counts
        double refill = MachineCore.REFILL_SHARE * c.ratedJoulesPerTick();
        assertEquals(20 + (40 + refill) / DT, plan(c, ohms), 1e-9);
        step(c, 200, ohms);
        assertEquals(40 + refill, c.bufferJoules(), 1e-9);
        assertTrue(c.tryTakeJoules(40));
        // steady asks: the buffer fills to its capacity and the draw settles at the ask
        for (int i = 0; i < 20; i++) {
            plan(c, ohms);
            step(c, 200, ohms);
            assertTrue(c.tryTakeJoules(40));
        }
        assertEquals(20 + 40 / DT, plan(c, ohms), 1e-6);
        assertEquals(c.bufferCapacityJoules() - 40, c.bufferJoules(), 1e-6);
        // stop asking: the buffer is full, so idle only at once (the smoothed tail would only be heat)
        step(c, 200, ohms);
        assertEquals(20, plan(c, ohms), 1e-6);
        // a drained buffer with a falling ask: half the last ask, plus the refill
        step(c, 200, ohms);
        c.takeJoules(c.bufferCapacityJoules() / 2);
        c.takeJoules(c.bufferCapacityJoules() / 2);
        assertEquals(0, c.bufferJoules(), 1e-9);
        plan(c, ohms);
        step(c, 200, ohms);
        assertEquals(20 + (c.ratedJoulesPerTick() / 2 + refill) / DT, plan(c, ohms), 1e-6);
    }

    @Test
    void bigAskIsCapped() {
        MachineCore c = new MachineCore(MILL);
        double[] ohms = {Double.POSITIVE_INFINITY};
        step(c, 200, ohms);
        c.takeJoules(1e6);
        assertEquals(2000, plan(c, ohms), 1e-9);
    }

    @Test
    void undervoltageTakesNothing() {
        MachineCore c = new MachineCore(MILL);
        double[] ohms = {Double.POSITIVE_INFINITY};
        step(c, 100, ohms);
        assertEquals(MachineState.UNDERVOLTAGE, c.state());
        assertEquals(0, c.voltageRatio());
        assertEquals(0, c.takeJoules(10));
        assertEquals(20, plan(c, ohms), 1e-9);
        step(c, 1, ohms);
        assertEquals(MachineState.UNPOWERED, c.state());
        assertEquals(0, plan(c, ohms));
    }

    @Test
    void overvoltageTripsOnceAndCoolsDown() {
        MachineCore c = new MachineCore(MILL);
        List<TripCause> trips = new ArrayList<>();
        c.onTrip(trips::add);
        double[] ohms = {Double.POSITIVE_INFINITY};
        step(c, 200, ohms);
        step(c, 270, ohms);
        assertEquals(MachineState.OVERVOLTAGE_TRIPPED, c.state());
        assertEquals(0, plan(c, ohms));
        for (int i = 0; i < 10; i++) step(c, 270, ohms);
        assertEquals(1, trips.size());
        assertEquals(270, trips.get(0).measured());
        // 59 ticks under 1.1x, one over (restarts the count), then 60 under
        for (int i = 0; i < 59; i++) step(c, 200, ohms);
        step(c, 225, ohms);
        for (int i = 0; i < 59; i++) step(c, 200, ohms);
        assertEquals(MachineState.OVERVOLTAGE_TRIPPED, c.state());
        step(c, 200, ohms);
        assertEquals(MachineState.RUNNING, c.state());
        assertEquals(1, trips.size());
        assertTrue(c.lastTrip().isPresent());
    }

    @Test
    void burnsOnOvervoltageNeverResets() {
        MachineCore c = new MachineCore(MachineSpec.consumer(Tier.V50).ratedWatts(100).burnsOnOvervoltage().build());
        double[] ohms = {Double.POSITIVE_INFINITY};
        step(c, 70, ohms);
        assertTrue(c.burnt());
        assertTrue(c.lastTrip().orElseThrow().burns());
        for (int i = 0; i < 200; i++) step(c, 50, ohms);
        assertEquals(MachineState.OVERVOLTAGE_TRIPPED, c.state());
    }

    /** One tick's surge doesn't trip; a sustained one does, within a few ticks. */
    @Test
    void overpowerAveragesTheSurge() {
        MachineCore c = new MachineCore(MILL);
        c.afterStep(200, 2000 * DT);
        c.afterStep(200, 3000 * DT);
        c.afterStep(200, 2000 * DT);
        assertEquals(MachineState.RUNNING, c.state());
        int n = 0;
        while (c.state() == MachineState.RUNNING && n < 20) {
            c.afterStep(200, 3000 * DT);
            n++;
        }
        assertEquals(MachineState.OVERPOWER_TRIPPED, c.state());
        assertTrue(n >= 2 && n <= 5, "tripped after " + n);
        for (int i = 1; i < 60; i++) c.afterStep(200, 0);
        assertEquals(MachineState.OVERPOWER_TRIPPED, c.state());
        c.afterStep(200, 0);
        assertEquals(MachineState.RUNNING, c.state());
    }

    @Test
    void overheatTripsAndCools() {
        MachineCore c = new MachineCore(MachineSpec.consumer(Tier.V200).ratedWatts(1000).thermal(100, 1, 60).build());
        c.afterStep(200, 0);
        assertEquals(MachineSpec.AMBIENT_CELSIUS, c.celsius(), 1e-9);
        int n = 0;
        while (c.state() == MachineState.RUNNING && n < 1000) {
            c.heatJoules(100);
            c.afterStep(200, 0);
            n++;
        }
        assertEquals(MachineState.OVERHEAT_TRIPPED, c.state());
        assertTrue(c.celsius() > 60);
        int cool = 0;
        while (c.state() == MachineState.OVERHEAT_TRIPPED && cool < 100000) {
            c.afterStep(200, 0);
            cool++;
        }
        assertEquals(MachineState.RUNNING, c.state());
        assertTrue(c.celsius() < 50);
        // exact first-order decay: tau = 100 s
        MachineCore d = new MachineCore(MachineSpec.consumer(Tier.V200).ratedWatts(1000).thermal(100, 1, 60).build());
        d.heatJoules(1000); // 20 kW for one tick: +10 K in the limit of a short pulse
        d.afterStep(200, 0);
        assertEquals(10, d.celsius() - 20, 0.01);
    }

    @Test
    void producerQueuesAndFeeds() {
        MachineCore c = new MachineCore(DYNAMO);
        assertEquals(100, c.giveJoules(1000), 1e-9); // room for two ticks of rated output
        assertEquals(0, c.giveJoules(5));
        c.beforeStep();
        assertTrue(c.sourceOn());
        // nothing connected: open-circuit volts
        assertEquals(210, c.sourceVolts(0, 1e19), 1e-9);
        // a short: current-limited at rated / nominal
        double rInt = c.sourceOhms();
        assertEquals(2, rInt, 1e-12);
        double e = c.sourceVolts(0, rInt);
        assertEquals(5, e / rInt, 1e-9);
        // a load wanting more than the queue: limited to the queue's energy this tick
        c.afterStep(0, 0);
        c.giveJoules(100);
        c.beforeStep();
        // queue 100 J (would be 200 W... ) : drain it at 10 ohm load to ground
        e = c.sourceVolts(0, rInt + 10);
        double i = e / (rInt + 10);
        assertTrue(e * i * DT <= c.bufferJoules() + 1e-9);
        c.afterStep(e - i * rInt, e * i * DT);
        assertTrue(c.bufferJoules() >= -1e-9);
        assertEquals(MachineState.UNDERVOLTAGE, c.state()); // 50 V on a 200 V line: described, but it still feeds
        assertEquals(5, c.amps(), 1e-9);
        // empty: off
        c.afterStep(0, 0);
        c.beforeStep();
        assertEquals(c.bufferJoules() > 0, c.sourceOn());
    }
}
