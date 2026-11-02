package mods.eln.api.lowlevel;

import mods.eln.api.impl.ApiTrace;
import mods.eln.api.testkit.Bench;
import mods.eln.api.testkit.ElnTestKit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The low level on a Bench: loads, resistors, sources behind their resistance, signals, watchdogs, and the rules. */
public class LowLevelBenchTest {
    @Test
    void aResistorOnTheBenchLineDrawsByOhmsLaw() {
        Bench b = ElnTestKit.bench().supply(50, 0.01);
        Circuit c = Circuit.create("heater");
        Load in = c.load("in");
        ResistorLoad r = c.resistorToGround("element", in, 10);
        b.attach(c).connect(c, in).ticks(3);
        double amps = 50 / (10 + 0.01 + Bench.POWER_RS);
        assertEquals(amps, r.amps(), 1e-6);
        assertEquals(amps * amps * 10, r.watts(), 1e-4);
        assertEquals(10 * amps, in.volts(), 1e-6);
        assertEquals(in.volts(), r.volts(), 1e-12);
        assertTrue(c.attached());
        r.setOhms(Double.POSITIVE_INFINITY);
        b.tick();
        assertEquals(Double.POSITIVE_INFINITY, r.ohms());
        assertEquals(0, r.amps(), 1e-6, "open: no current");
    }

    @Test
    void aSourceSagsBehindItsResistanceAndSourcesShareInParallel() {
        Bench b = ElnTestKit.bench();
        Circuit c = Circuit.create("dynamo");
        Load out = c.load("out");
        VoltageSource s = c.voltageSource("emf", out, 100, 1);
        ResistorLoad load = c.resistorToGround("load", out, 9);
        b.attach(c).ticks(2);
        assertEquals(90, out.volts(), 1e-6);
        assertEquals(10, s.amps(), 1e-6);
        assertEquals(900, s.watts(), 1e-4);
        load.setOhms(Double.POSITIVE_INFINITY);
        b.tick();
        assertEquals(100, out.volts(), 1e-3, "open circuit: the EMF");
        // a second source in parallel, 10 V higher: solvable (never ideal), and they share by their resistances
        load.setOhms(10);
        VoltageSource s2 = c.voltageSource("emf2", out, 110, 1);
        b.ticks(2);
        assertEquals(100, out.volts(), 1e-6);
        assertEquals(0, s.amps(), 1e-6);
        assertEquals(10, s2.amps(), 1e-6);
        s2.setVolts(100);
        s2.setOhms(2);
        b.tick();
        assertEquals(100 * 10 / (10 + 2.0 / 3), out.volts(), 1e-6);
        assertEquals(2 * s2.amps(), s.amps(), 1e-6);
    }

    @Test
    void aSignalSourceFloatsUntilSetAndDrivesALevel() {
        Bench b = ElnTestKit.bench();
        Circuit c = Circuit.create("sensor");
        SignalSource out = c.signalSource("out");
        SignalLoad in = c.signalLoad("in");
        c.resistor("wire", out, in, 10);
        c.resistorToGround("pull-down", in, 1e5);
        b.attach(c).ticks(5);
        assertTrue(out.driven().isEmpty(), "floats until first set");
        assertEquals(0, in.level(), 1e-3);
        out.setLevel(0.6);
        b.ticks(5);
        assertEquals(0.6, out.driven().getAsDouble(), 1e-12);
        assertEquals(0.6, out.level(), 0.01);
        assertEquals(0.6, in.level(), 0.01);
        out.release();
        b.ticks(200);
        assertTrue(out.driven().isEmpty());
        assertTrue(in.level() < 0.05, "released, the pull-down takes it: " + in.level());
        assertThrows(IllegalArgumentException.class, () -> out.setLevel(1.5), "a level, not volts");
        assertThrows(IllegalArgumentException.class, () -> out.setLevel(Double.NaN));
    }

    @Test
    void aWatchdogTripsOnceAfterItsHoldAndStaysTrippedUntilReset() {
        Bench b = ElnTestKit.bench().supply(50, 0.01);
        Circuit c = Circuit.create("guarded");
        Load in = c.load("in");
        ResistorLoad r = c.resistorToGround("element", in, 10);
        List<WatchdogTrip> trips = new ArrayList<>();
        Watchdog volts = c.watchdogs().volts(in, 0, 40).onTrip(trips::add);
        Watchdog watts = c.watchdogs().watts(r, 1000);   // 250 W: never
        b.attach(c).connect(c, in);
        b.ticks(4);
        assertFalse(volts.tripped(), "held 4 steps of the 5 of 0.25 s");
        b.tick();
        assertTrue(volts.tripped());
        assertEquals(1, trips.size());
        WatchdogTrip t = trips.get(0);
        assertEquals("in", t.part());
        assertEquals("V", t.unit());
        assertEquals(40, t.limit());
        assertTrue(t.measured() > 49.9, t.toString());
        assertEquals("in: 49.9 V over 40.0 V", t.toString());
        b.ticks(20);
        assertEquals(1, trips.size(), "once per trip");
        assertFalse(watts.tripped());
        volts.reset();
        assertFalse(volts.tripped());
        b.ticks(5);
        assertEquals(2, trips.size(), "reset, it watches again with a fresh hold");
        assertEquals(t, volts.lastTrip().orElseThrow());
        Watchdog quick = c.watchdogs().amps(r, 1).holdSeconds(0.05);
        b.tick();
        assertTrue(quick.tripped(), "a one-step hold trips on the first step over");
        b.supplyVolts(0);
        Watchdog under = c.watchdogs().volts(in, 10, 60);
        b.ticks(5);
        assertTrue(under.tripped());
        assertTrue(under.lastTrip().orElseThrow().toString().contains("under 10.0 V"), under.lastTrip().toString());
    }

    @Test
    void valuesAndWiringAreChecked() {
        Circuit c = Circuit.create("checked"), other = Circuit.create("other");
        Load a = c.load("a"), x = other.load("x");
        assertThrows(IllegalArgumentException.class, () -> c.resistorToGround("r", a, 0));
        assertThrows(IllegalArgumentException.class, () -> c.resistorToGround("r", a, -1));
        assertThrows(IllegalArgumentException.class, () -> c.resistorToGround("r", a, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> c.voltageSource("s", a, 50, 0), "never an ideal source");
        assertThrows(IllegalArgumentException.class, () -> c.voltageSource("s", a, 50, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> c.voltageSource("s", a, Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> c.resistor("r", a, x, 1), "a part of another circuit");
        assertThrows(IllegalArgumentException.class, () -> c.connect(x, PortRef.of(net.minecraft.core.BlockPos.ZERO,
            net.minecraft.core.Direction.UP, PortKind.POWER)));
        ResistorLoad foreign = other.resistorToGround("r", x, 1);
        assertThrows(IllegalArgumentException.class, () -> c.watchdogs().amps(foreign, 1));
        assertThrows(IllegalArgumentException.class, () -> c.watchdogs().volts(a, 10, 10));
        assertThrows(IllegalArgumentException.class, () -> c.watchdogs().watts(c.resistorToGround("q", a, 1), 0));
        assertThrows(IllegalArgumentException.class, () -> c.watchdogs().volts(a, 0, 1).holdSeconds(0.01));
        assertThrows(IllegalArgumentException.class, () -> c.load(" "), "parts need a name");
        assertThrows(NullPointerException.class, () -> c.resistor("r", a, null, 1));
    }

    @Test
    void benchRulesThreadsAndPortLinks() throws InterruptedException {
        Bench b = ElnTestKit.bench();
        Circuit c = Circuit.create("ruled");
        Load a = c.load("a");
        PortLink link = c.connect(a, PortRef.of(net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.UP, PortKind.POWER));
        b.attach(c);
        assertThrows(IllegalStateException.class, () -> b.attach(c), "once per circuit");
        b.ticks(2);
        assertEquals(PortStatus.NOT_HOSTED, link.status(), "a bench has no world");
        List<PortStatus> seen = new ArrayList<>();
        link.onStatus(seen::add);
        link.remove();
        assertEquals(PortStatus.REMOVED, link.status());
        assertEquals(List.of(PortStatus.REMOVED), seen);
        AtomicReference<Throwable> off = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                c.load("late");
            } catch (Throwable x) {
                off.set(x);
            }
        });
        t.start();
        t.join(5000);
        assertTrue(off.get() instanceof IllegalStateException, String.valueOf(off.get()));
    }

    @Test
    void theDebugTraceLogsAttachesLinksAndTripsWithVoltsAndOhmsOnlyWhenOn() {
        List<String> lines = new ArrayList<>();
        ApiTrace.setSink(lines::add);
        try {
            Circuit quiet = Circuit.create("quiet");
            quiet.load("q");
            ElnTestKit.bench().attach(quiet).ticks(2);
            assertTrue(lines.isEmpty(), "off by default: " + lines);
            ApiTrace.setEnabled(true);
            Bench b = ElnTestKit.bench().supply(50, 0.01);
            Circuit c = Circuit.create("traced");
            Load in = c.load("in");
            c.resistorToGround("r", in, 10);
            c.watchdogs().volts(in, 0, 40).holdSeconds(0.05);
            b.attach(c).connect(c, in).ticks(2);
            assertTrue(lines.stream().anyMatch(l -> l.startsWith("attach circuit 'traced' at the bench") && l.contains("ohm")), lines.toString());
            assertTrue(lines.stream().anyMatch(l -> l.contains("watchdog trip") && l.contains(" V over 40.0 V")), lines.toString());
        } finally {
            ApiTrace.setEnabled(false);
            ApiTrace.setSink(null);
        }
    }
}
