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

    /** Power comes on at volts: UNPOWERED for the hold, then RUNNING. */
    static void powerUp(MachineCore c, double volts, double[] ohms) {
        for (int i = 0; i < MachineCore.UNDERVOLTAGE_RECOVERY_TICKS; i++) {
            assertEquals(MachineState.UNPOWERED, c.state());
            step(c, volts, ohms);
            plan(c, ohms);
        }
        assertEquals(MachineState.RUNNING, c.state());
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
        powerUp(c, 200, ohms);
        assertEquals(0, c.joulesIn()); // nothing drawn while powering up
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
        powerUp(c, 200, ohms);
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
        powerUp(c, 200, ohms);
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

    @Test
    void overheatTripsAndCools() {
        MachineCore c = new MachineCore(MachineSpec.consumer(Tier.V200).ratedWatts(1000).heatCapacityJPerK(100).heatLossWPerK(1).overheatCelsius(60).build());
        for (int i = 0; i < MachineCore.UNDERVOLTAGE_RECOVERY_TICKS; i++) c.afterStep(200, 0);
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
        MachineCore d = new MachineCore(MachineSpec.consumer(Tier.V200).ratedWatts(1000).heatCapacityJPerK(100).heatLossWPerK(1).overheatCelsius(60).build());
        d.heatJoules(1000); // 20 kW for one tick: +10 K in the limit of a short pulse
        d.afterStep(200, 0);
        assertEquals(10, d.celsius() - 20, 0.01);
    }

    /** Brownout.SLOW: a 200 V machine on a 100 V line runs, on a quarter of its rated power. */
    @Test
    void slowRunsOnWhatArrives() {
        MachineCore c = new MachineCore(MachineSpec.consumer(Tier.V200).ratedWatts(2000).idleWatts(20)
            .onBrownout(Brownout.SLOW).build());
        double[] ohms = {Double.POSITIVE_INFINITY};
        powerUp(c, 100, ohms);
        double got = 0;
        for (int i = 0; i < 40; i++) {
            got = c.takeJoules(c.ratedJoulesPerTick());
            assertTrue(plan(c, ohms) <= 500 + 1e-9);
            step(c, 100, ohms);
            assertEquals(MachineState.RUNNING, c.state());
        }
        assertEquals((500 - 20) * DT, got, 1e-6);
        assertEquals(0.5, c.voltageRatio(), 1e-12);
        step(c, 1, ohms);
        assertEquals(MachineState.UNPOWERED, c.state());
    }

    /** Even with the highest floor, a sagged machine runs again once the line is back at nominal. */
    @Test
    void undervoltageRecoversWithAHighFloor() {
        MachineCore c = new MachineCore(MachineSpec.consumer(Tier.V200).ratedWatts(2000).minVoltageRatio(0.95).build());
        double[] ohms = {Double.POSITIVE_INFINITY};
        powerUp(c, 200, ohms);
        step(c, 185, ohms);
        assertEquals(MachineState.UNDERVOLTAGE, c.state());
        for (int i = 1; i < MachineCore.UNDERVOLTAGE_RECOVERY_TICKS; i++) step(c, 196, ohms); // 0.98: halfway to nominal is 0.975
        assertEquals(MachineState.UNDERVOLTAGE, c.state());
        step(c, 196, ohms);
        assertEquals(MachineState.RUNNING, c.state());
    }

    /** A line flickering on and off never starts the machine. */
    @Test
    void flickerDoesNotStartIt() {
        MachineCore c = new MachineCore(MILL);
        double[] ohms = {Double.POSITIVE_INFINITY};
        for (int i = 0; i < 100; i++) {
            step(c, i % 5 == 4 ? 0 : 200, ohms);
            assertEquals(MachineState.UNPOWERED, c.state(), "tick " + i);
            assertEquals(0, plan(c, ohms));
        }
    }

    /** Failed tryTakes retried in one tick count once; a step up to the buffer's size is sized at a tick's rated work. */
    @Test
    void retriedTryTakeIsOneAsk() {
        MachineCore c = new MachineCore(MILL);
        double[] ohms = {Double.POSITIVE_INFINITY};
        powerUp(c, 200, ohms);
        double refill = MachineCore.REFILL_SHARE * c.ratedJoulesPerTick();
        assertFalse(c.tryTakeJoules(40));
        assertFalse(c.tryTakeJoules(40));
        assertFalse(c.tryTakeJoules(30));
        assertEquals(20 + (40 + refill) / DT, plan(c, ohms), 1e-9);
        step(c, 200, ohms);
        assertFalse(c.tryTakeJoules(150)); // more than a tick, within the buffer: allowed, sized at a tick
        assertEquals(2000, plan(c, ohms), 1e-9);
    }

    /** In a place hotter than 10 K under the trip point, the overheat trip resets halfway down to ambient. */
    @Test
    void overheatResetsInAHotPlace() {
        MachineCore c = new MachineCore(MachineSpec.consumer(Tier.V200).ratedWatts(1000)
            .heatCapacityJPerK(10).heatLossWPerK(1).overheatCelsius(60).build());
        c.setAmbientCelsius(55);
        assertEquals(55, c.celsius());
        for (int i = 0; i < MachineCore.UNDERVOLTAGE_RECOVERY_TICKS; i++) c.afterStep(200, 0);
        c.heatJoules(100);
        c.afterStep(200, 0);
        assertEquals(MachineState.OVERHEAT_TRIPPED, c.state());
        int n = 0;
        while (c.state().tripped() && n++ < 10000) c.afterStep(200, 0);
        assertEquals(MachineState.RUNNING, c.state());
        assertTrue(c.celsius() < 57.5);
        // no heat model: celsius() is the ambient set
        MachineCore plain = new MachineCore(MILL);
        plain.setAmbientCelsius(-5);
        assertEquals(-5, plain.celsius());
    }

    /** A listener that throws is logged; the others still hear of the trip. */
    @Test
    void badListenerDoesNotSilenceTheOthers() {
        MachineCore c = new MachineCore(MILL);
        List<TripCause> heard = new ArrayList<>();
        c.onTrip(t -> {
            throw new RuntimeException("listener bug (expected in this test)");
        });
        c.onTrip(heard::add);
        double[] ohms = {Double.POSITIVE_INFINITY};
        powerUp(c, 200, ohms);
        step(c, 300, ohms);
        assertEquals(1, heard.size());
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

    /** respec changes ratings only: a booster doubles the draw from the next plan; a buffer over the new size is cut and booked lost. */
    @Test
    void respecChangesRatingsOnly() {
        MachineSpec small = MachineSpec.consumer(Tier.V50).ratedWatts(200).onBrownout(Brownout.SLOW).power(RelativeFace.LEFT).build();
        MachineSpec big = MachineSpec.consumer(Tier.V50).ratedWatts(400).onBrownout(Brownout.SLOW).power(RelativeFace.LEFT).build();
        MachineCore c = new MachineCore(big);
        double[] ohms = {Double.POSITIVE_INFINITY};
        powerUp(c, 50, ohms);
        for (int i = 0; i < 20; i++) {
            step(c, 50, ohms);
            c.takeJoules(5);    // a quarter of a tick: the buffer fills
            plan(c, ohms);
        }
        assertTrue(c.bufferJoules() > small.ratedWatts() * 2 * DT, "buffer " + c.bufferJoules());
        c.respec(small);
        assertEquals(small.ratedWatts() * 2 * DT, c.bufferJoules(), 1e-9);
        assertEquals(c.joulesIn(), c.joulesOut() + c.bufferJoules() + c.joulesLost(), 1e-9);
        assertEquals(10, c.ratedJoulesPerTick(), 1e-12);
        c.respec(big);
        c.takeJoules(c.ratedJoulesPerTick());
        assertEquals(400, plan(c, ohms), 1e-6);
        // other faces, signals or kind: a different machine, not a respec
        MachineSpec moved = MachineSpec.consumer(Tier.V50).ratedWatts(400).onBrownout(Brownout.SLOW).power(RelativeFace.RIGHT).build();
        String m = assertThrows(IllegalArgumentException.class, () -> c.respec(moved)).getMessage();
        assertTrue(m.contains("power faces"), m);
        assertThrows(IllegalArgumentException.class, () -> c.respec(MachineSpec.producer(Tier.V50).ratedWatts(400).power(RelativeFace.LEFT).build()));
    }

    /** A snapshot taken back on a fresh core: buffer (booked as delivered), heat, a trip in its cooldown, the last trip. */
    @Test
    void snapshotRestores() {
        MachineSpec hot = MachineSpec.consumer(Tier.V50).ratedWatts(100).heatCapacityJPerK(10).heatLossWPerK(1).overheatCelsius(60).build();
        MachineCore c = new MachineCore(hot);
        double[] ohms = {Double.POSITIVE_INFINITY};
        powerUp(c, 50, ohms);
        c.takeJoules(5);
        plan(c, ohms);
        step(c, 50, ohms);
        step(c, 70, ohms);  // overvoltage trip
        MachineCore.Saved s = c.snapshot();
        assertEquals(MachineState.OVERVOLTAGE_TRIPPED, s.state());
        MachineCore d = new MachineCore(hot);
        d.restore(s);
        assertEquals(s, d.snapshot());
        assertEquals(MachineState.OVERVOLTAGE_TRIPPED, d.state());
        assertEquals(d.joulesIn(), d.joulesOut() + d.bufferJoules() + d.joulesLost(), 1e-12);
        d.afterStep(50, 0);
        assertThrows(IllegalStateException.class, () -> d.restore(s));
    }

    /**
     * A machine saved running runs on at once after a load when its first step finds the line in range (no power-up
     * hold); on a dead or sagging line it is classified as it finds it.
     */
    @Test
    void savedRunningRunsOn() {
        MachineSpec spec = MachineSpec.consumer(Tier.V50).ratedWatts(100).build();
        MachineCore c = new MachineCore(spec);
        double[] ohms = {Double.POSITIVE_INFINITY};
        powerUp(c, 50, ohms);
        MachineCore.Saved s = c.snapshot();
        assertEquals(MachineState.RUNNING, s.state());
        MachineCore d = new MachineCore(spec);
        d.restore(s);
        d.afterStep(50, 0);
        assertEquals(MachineState.RUNNING, d.state(), "no power-up hold after a load");
        MachineCore dead = new MachineCore(spec), weak = new MachineCore(spec);
        dead.restore(s);
        dead.afterStep(0, 0);
        assertEquals(MachineState.UNPOWERED, dead.state());
        weak.restore(s);
        weak.afterStep(30, 0);
        assertEquals(MachineState.UNDERVOLTAGE, weak.state());
    }

    /** A burnt core repaired by its host starts again UNPOWERED and runs once the line holds. */
    @Test
    void repairUnburns() {
        MachineCore c = new MachineCore(MachineSpec.consumer(Tier.V50).ratedWatts(100).burnsOnOvervoltage().build());
        double[] ohms = {Double.POSITIVE_INFINITY};
        step(c, 70, ohms);
        assertTrue(c.burnt());
        assertEquals(MachineState.UNPOWERED, c.snapshot().state(), "a burn is the host's to keep, not a trip to wait out");
        c.repair();
        assertFalse(c.burnt());
        powerUp(c, 50, ohms);
        assertTrue(c.lastTrip().orElseThrow().burns(), "the last trip is kept");
    }
}
