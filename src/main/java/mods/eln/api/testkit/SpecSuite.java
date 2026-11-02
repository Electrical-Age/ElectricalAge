package mods.eln.api.testkit;

import mods.eln.api.Brownout;
import mods.eln.api.Machine;
import mods.eln.api.MachineSpec;
import mods.eln.api.MachineState;
import mods.eln.api.TripCause;
import mods.eln.api.impl.MachineCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ElnTestKit.verifySpec's checks: each scenario runs a fresh Bench with one machine and checks what the API promises
 * for the spec (Machine, MachineView, MachineState, Brownout), not how the core gets there. On every tick of every
 * scenario: the energy books balance, the buffer stays within its bounds, a consumer draws at most
 * rated x (U/U_nom)^2 (and the circuit's current agrees with it), a producer's current stays under its limit.
 * Then per scenario: a voltage sweep 0..2 x nominal (states, work rate), rated power at nominal, step loads (the
 * one-tick rise, the two-tick fall, idle draw), tryTakeJoules all or nothing, brownout (STOP: stops and recovers
 * without chatter; SLOW: runs slower), a weak line, the overvoltage trip and its cooldown (or the burn), overheat
 * and its reset (with a heat model), and a producer's droop, current limit and queue.
 * <p>
 * The checks read the spec under test; the bench runs onBench, which is the same spec except in this package's
 * own test of the suite (a machine that doesn't behave as its spec says must fail).
 */
final class SpecSuite {
    /** Sweep points, share of nominal volts. */
    static final double[] RATIOS = {0, 0.01, 0.1, 0.25, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.25, 1.35, 1.5, 1.75, 2.0};
    /** A test longer than this many ticks (an hour) is skipped: a heat model too slow to cool on a bench. */
    static final int MAX_TICKS = 72_000;
    static final double DT = MachineCore.TICK_SECONDS;

    private final MachineSpec spec, onBench;
    private final boolean producer;
    private final double un, rated, idle, ratedJ, cap, floor, stiffOhms, ilim;
    private final int cooldownTicks;
    private final List<SpecReport.Check> checks = new ArrayList<>();

    private SpecSuite(MachineSpec spec, MachineSpec onBench) {
        this.spec = spec;
        this.onBench = onBench;
        producer = spec.kind() == MachineSpec.Kind.PRODUCER;
        un = spec.nominalVolts();
        rated = spec.ratedWatts();
        idle = spec.idleWatts();
        ratedJ = (rated - idle) * DT;
        cap = MachineCore.BUFFER_TICKS * ratedJ;
        floor = spec.brownout() == Brownout.SLOW ? MachineCore.UNPOWERED_RATIO : spec.minVoltageRatio();
        stiffOhms = 1e-3 * un * un / rated;
        ilim = rated / un;
        cooldownTicks = (int) Math.ceil(spec.tripCooldownSeconds() / DT - 1e-9);
    }

    static SpecReport run(MachineSpec spec, MachineSpec onBench) {
        if (spec.kind() != onBench.kind()) throw new IllegalArgumentException("onBench must be of the same kind");
        SpecSuite s = new SpecSuite(spec, onBench);
        s.sweep();
        if (s.producer) s.droop();
        else {
            s.stepLoads();
            s.atomicTakes();
            s.brownout();
            s.weakLine();
        }
        s.overvoltage();
        s.overheat();
        return new SpecReport(spec, s.checks);
    }

    // ---- one scenario

    /** A fresh bench with the machine, its owner, and the per-tick invariants. */
    private final class Run {
        final Bench bench = new Bench();
        final BenchMachine m;
        final List<MachineState> states = new ArrayList<>();
        final List<Double> watts = new ArrayList<>(), got = new ArrayList<>();
        final List<TripCause> trips = new ArrayList<>();
        final boolean supplied;
        boolean loaded;
        String violation;
        int ticks;
        /** The owner's ask per tick (consumer) or give (producer), J; NaN: fill the queue (producer). */
        double ask;
        /** Heat added per tick, J. */
        double heat;

        Run(double supplyRatio) {
            supplied = !Double.isNaN(supplyRatio);
            if (supplied) bench.supply(supplyRatio * un, stiffOhms);
            m = bench.add(onBench);
            m.onTrip(trips::add);
            ask = producer ? Double.NaN : ratedJ;
            m.owner(this::own);
        }

        private void own(Machine machine) {
            if (heat > 0) machine.heatJoules(heat);
            if (producer) {
                machine.giveJoules(Double.isNaN(ask) ? machine.bufferCapacityJoules() : ask);
                got.add(0.0);
            } else got.add(ask > 0 ? machine.takeJoules(ask) : 0.0);
        }

        Run load(double ohms) {
            bench.load(ohms);
            loaded = true;
            return this;
        }

        void supply(double ratio) {
            bench.supplyVolts(ratio * un);
        }

        void run(int n) {
            for (int i = 0; i < n; i++) {
                bench.tick();
                ticks++;
                states.add(m.state());
                watts.add(m.watts());
                if (violation == null) violation = invariants();
            }
        }

        /** The first broken invariant this tick, or null. */
        private String invariants() {
            double v = Math.abs(m.volts());
            if (producer) {
                double lhs = m.joulesGiven(), rhs = m.joulesDelivered() + m.bufferJoules() + m.joulesLost();
                if (Math.abs(lhs - rhs) > 1e-9 * Math.max(1, lhs))
                    return at("books: given %.6g J != delivered + buffer + lost %.6g J", lhs, rhs);
                if (m.bufferJoules() > cap * (1 + 1e-9) + 1e-12 || m.bufferJoules() < -0.01 * ratedJ - 1e-9)
                    return at("buffer %.6g J outside -1%% of a tick .. %.6g J", m.bufferJoules(), cap);
                if (m.amps() > ilim * (1 + 1e-3) + 1e-9) return at("current %.6g A over the limit %.6g A", m.amps(), ilim);
            } else {
                double lhs = m.joulesDelivered(), rhs = m.joulesTaken() + m.bufferJoules() + m.joulesLost();
                if (Math.abs(lhs - rhs) > 1e-9 * Math.max(1, lhs))
                    return at("books: delivered %.6g J != taken + buffer + lost %.6g J", lhs, rhs);
                if (m.bufferJoules() < -1e-12 || m.bufferJoules() > cap * (1 + 1e-9) + 1e-12)
                    return at("buffer %.6g J outside 0..%.6g J", m.bufferJoules(), cap);
                double max = rated * (v / un) * (v / un);
                if (m.watts() > max * (1 + 1e-5) + 1e-9) return at("draws %.6g W, over rated x (U/U_nom)^2 = %.6g W", m.watts(), max);
                if (supplied && !loaded) {
                    double circuit = v * bench.supplyAmps(), slack = 1e-6 * rated + 2 * v * v / 1e9 + 1e-9;
                    if (Math.abs(m.watts() - circuit) > slack)
                        return at("books %.6g W but the circuit carries %.6g W", m.watts(), circuit);
                }
            }
            return null;
        }

        private String at(String format, Object... args) {
            return "tick " + ticks + ": " + String.format(Locale.ROOT, format, args);
        }

        /** Where a failure's trace is shown: around this reading's tick (-1: the last ticks). */
        int focus = -1;

        /** Twelve lines of the bench's trace: around focus, or the last. */
        String trace() {
            String[] lines = bench.trace().split("\n");
            int from = focus < 0 ? lines.length - 12 : focus - 4;
            from = Math.max(0, Math.min(from, lines.length - 12));
            StringBuilder b = new StringBuilder();
            for (int i = from; i < Math.min(lines.length, from + 12); i++) b.append(lines[i]).append('\n');
            return b.toString().stripTrailing();
        }

        double avgGot(int from, int to) {
            double s = 0;
            for (int i = from; i < to; i++) s += got.get(i);
            return s / (to - from);
        }

        /** The first tick index at or after from where the state is s, or -1. */
        int first(MachineState s, int from) {
            for (int i = from; i < states.size(); i++) if (states.get(i) == s) return i;
            return -1;
        }

        int changes(int from) {
            int n = 0;
            for (int i = Math.max(1, from); i < states.size(); i++) if (states.get(i) != states.get(i - 1)) n++;
            return n;
        }
    }

    /** Records a check: PASS when ok and the run kept its invariants, else FAIL with the trace's last ticks. */
    private void check(String name, Run r, boolean ok, String format, Object... args) {
        String detail = String.format(Locale.ROOT, format, args);
        boolean pass = ok && (r == null || r.violation == null);
        if (r != null && r.violation != null) detail += "; invariant broken at " + r.violation;
        checks.add(new SpecReport.Check(name, pass ? SpecReport.Result.PASS : SpecReport.Result.FAIL, detail,
            pass || r == null ? "" : r.trace()));
    }

    private void skip(String name, String why) {
        checks.add(new SpecReport.Check(name, SpecReport.Result.SKIP, why, ""));
    }

    private static boolean near(double a, double b, double rel) {
        return Math.abs(a - b) <= rel * Math.max(Math.abs(b), 1e-9);
    }

    // ---- the scenarios

    /** The state a fresh machine settles in at ratio x nominal, or null near a threshold (not checked there). */
    private MachineState expected(double r) {
        if (Math.abs(r - MachineCore.UNPOWERED_RATIO) < 0.01 || Math.abs(r - MachineCore.OVERVOLTAGE_RATIO) < 0.03) return null;
        if (r < MachineCore.UNPOWERED_RATIO) return MachineState.UNPOWERED;
        if (r > MachineCore.OVERVOLTAGE_RATIO) return MachineState.OVERVOLTAGE_TRIPPED;
        if (spec.brownout() == Brownout.STOP && Math.abs(r - floor) < 0.02) return null;
        return r < floor ? MachineState.UNDERVOLTAGE : MachineState.RUNNING;
    }

    private void sweep() {
        for (double r : RATIOS) {
            String name = String.format(Locale.ROOT, "sweep %.2f x nominal", r);
            MachineState want = expected(r);
            Run run = new Run(r);
            run.run(60);
            BenchMachine m = run.m;
            if (want == null) {
                check(name, run, true, "%s (at a threshold: invariants only)", m.state());
                continue;
            }
            boolean ok = m.state() == want;
            String what = m.state() + (ok ? "" : ", expected " + want);
            switch (want) {
                case RUNNING -> {
                    if (producer) what += String.format(Locale.ROOT, ", feeds %.4g W", m.watts());
                    else {
                        double u = Math.abs(m.volts()) / un;
                        double work = Math.min(ratedJ, Math.max(0, rated * u * u - idle) * DT), got = run.avgGot(40, 60);
                        ok &= got >= 0.98 * work - 1e-12 && got <= work * (1 + 1e-4) + 1e-12;
                        what += String.format(Locale.ROOT, ", work %.4g J/tick (expected %.4g)", got, work);
                        if (r == 1.0) {
                            ok &= near(m.watts(), rated, 0.01);
                            what += String.format(Locale.ROOT, ", draws %.4g W of rated %.4g W", m.watts(), rated);
                        }
                    }
                }
                case UNDERVOLTAGE, UNPOWERED -> {
                    double u = Math.abs(m.volts()) / un;
                    double allowed = (want == MachineState.UNPOWERED ? 0 : Math.min(idle, rated * u * u)) * 1.01 + 1e-6 * rated + leakW(m.volts());
                    if (producer) what += String.format(Locale.ROOT, ", feeds %.4g W (only a trip stops a producer's output)", m.watts());
                    else {
                        ok &= run.avgGot(40, 60) == 0 && m.watts() <= allowed;
                        what += String.format(Locale.ROOT, ", takes 0, draws %.4g W (at most %.4g)", m.watts(), allowed);
                    }
                }
                case OVERVOLTAGE_TRIPPED -> {
                    TripCause t = m.lastTrip().orElse(null);
                    ok &= t != null && t.state() == want && t.burns() == spec.burnsOnOvervoltage() && run.trips.size() == 1
                        && Math.abs(m.amps() * m.volts()) <= 1e-6 * rated + leakW(m.volts());
                    what += ", " + t + ", " + run.trips.size() + " trip(s), " + String.format(Locale.ROOT, "%.3g A", m.amps());
                }
                default -> {
                }
            }
            check(name, run, ok, "%s", what);
        }
    }

    /** Asks every tick, none for 20 ticks, then again: the rise one tick after the ask, the fall within two, idle in between. */
    private void stepLoads() {
        Run run = new Run(1);
        run.run(30);
        run.ask = 0;
        run.run(20);
        run.ask = ratedJ;
        run.run(20);
        // readings: index i is after tick i+1's step; the owner's ask in that tick sizes the next step
        // (asks stop in the owner's tick of index 30: the step of index 31 is planned on half the last ask and the
        // refill, the next on none; they resume in the owner's tick of index 50, which sizes the step of index 51)
        double idleMax = idle * 1.01 + 1e-6 * rated + leakW(un);
        boolean idles = true;
        for (int i = 32; i <= 50; i++) idles &= run.watts.get(i) <= idleMax;
        boolean rises = run.watts.get(51) >= idle + 0.99 * (rated - idle);
        run.focus = idles ? 51 : 32;
        check("step loads", run, run.states.get(29) == MachineState.RUNNING && idles && rises,
            "asks stop: %.4g W, then %.4g W (idle %.4g W); asks resume: %.4g W in the tick of the ask, %.4g W the next (rated %.4g W)",
            run.watts.get(31), run.watts.get(32), idle, run.watts.get(50), run.watts.get(51), rated);
    }

    /** tryTakeJoules takes all or nothing, and a step bigger than the buffer throws. */
    private void atomicTakes() {
        Run run = new Run(1);
        int[] ok = {0};
        run.m.owner(m -> {
            if (m.tryTakeJoules(ratedJ)) ok[0]++;
        });
        run.run(60);
        boolean whole = near(run.m.joulesTaken(), ok[0] * ratedJ, 1e-9);
        boolean throwsBig;
        try {
            run.m.tryTakeJoules(cap * 1.01);
            throwsBig = false;
        } catch (IllegalArgumentException e) {
            throwsBig = true;
        }
        check("tryTakeJoules all or nothing", run, whole && ok[0] >= 40 && throwsBig,
            "%d whole steps of %.4g J in 60 ticks, taken %.6g J; a step over the buffer throws: %b", ok[0], ratedJ,
            run.m.joulesTaken(), throwsBig);
    }

    private void brownout() {
        Run run = new Run(1);
        run.run(30);
        if (spec.brownout() == Brownout.STOP) {
            double low = Math.max(0.05, floor * 0.6);
            run.supply(low);
            run.run(30);
            run.supply(1);
            run.run(30);
            int down = run.first(MachineState.UNDERVOLTAGE, 30), up = run.first(MachineState.RUNNING, 60);
            run.focus = down < 0 || down > 31 ? 30 : 60 + MachineCore.UNDERVOLTAGE_RECOVERY_TICKS;
            boolean takesNothing = run.avgGot(32, 60) == 0;
            int hold = MachineCore.UNDERVOLTAGE_RECOVERY_TICKS;
            boolean ok = run.states.get(29) == MachineState.RUNNING && down >= 30 && down <= 31 && takesNothing
                && up >= 60 + hold - 1 && up <= 60 + hold + 2 && run.changes(30) == 2;
            check("brownout STOP", run, ok, "line to %.2f x: UNDERVOLTAGE after %d tick(s), takes %s; restored: RUNNING after %d ticks "
                    + "(hold %d); %d state changes (2 expected)", low, down - 29, takesNothing ? "nothing" : "something", up - 59, hold,
                run.changes(30));
        } else {
            run.supply(0.5);
            run.run(40);
            double u = Math.abs(run.m.volts()) / un;
            double work = Math.min(ratedJ, Math.max(0, rated * u * u - idle) * DT), got = run.avgGot(50, 70);
            boolean ok = run.first(MachineState.UNDERVOLTAGE, 0) < 0 && run.m.state() == MachineState.RUNNING
                && near(run.m.voltageRatio(), 0.5, 0.02) && (work == 0 ? got == 0 : near(got, work, 0.03));
            check("brownout SLOW", run, ok, "line at 0.5 x: %s, voltage ratio %.3f, work %.4g J/tick (expected %.4g)", run.m.state(),
                run.m.voltageRatio(), got, work);
        }
    }

    /** A line that can carry only half the rated power (its maximum power transfer). */
    private void weakLine() {
        Run run = new Run(Double.NaN);
        double r = un * un / (2 * rated);
        run.bench.supply(un, r);
        run.run(200);
        if (spec.brownout() == Brownout.SLOW) {
            double maxW = un * un / (4 * r);
            boolean ok = run.m.state() == MachineState.RUNNING && Math.abs(run.m.volts()) >= 0.49 * un
                && run.m.watts() <= maxW * 1.01 && (rated * 0.25 <= idle || run.avgGot(150, 200) > 0);
            check("weak line, SLOW", run, ok, "half the rated power available: %s at %.3f x nominal, draws %.4g W (line maximum %.4g W), "
                + "work %.4g J/tick", run.m.state(), Math.abs(run.m.volts()) / un, run.m.watts(), maxW, run.avgGot(150, 200));
        } else {
            int runs = 0;
            for (int i = 20; i < 200; i++) if (run.states.get(i) == MachineState.RUNNING && run.states.get(i - 1) != MachineState.RUNNING) runs++;
            int allowed = 180 / MachineCore.UNDERVOLTAGE_RECOVERY_TICKS + 1;
            check("weak line, STOP", run, runs <= allowed, "half the rated power available: %d restarts in 9 s (at most %d: no chatter); ends %s",
                runs, allowed, run.m.state());
        }
    }

    private void overvoltage() {
        Run run = new Run(1);
        if (producer) run.load(un * un / rated);
        run.run(30);
        run.supply(1.45);
        run.run(10);
        run.supply(1);
        run.run(cooldownTicks + 30);
        int trip = run.first(MachineState.OVERVOLTAGE_TRIPPED, 30);
        TripCause t = run.trips.isEmpty() ? null : run.trips.get(0);
        boolean once = run.trips.size() == 1 && t.state() == MachineState.OVERVOLTAGE_TRIPPED && t.burns() == spec.burnsOnOvervoltage();
        boolean quiet = true;
        for (int i = trip + 1; trip >= 0 && i < run.states.size() && run.states.get(i).tripped(); i++)
            quiet &= Math.abs(run.watts.get(i)) <= 1e-6 * rated + leakW(2 * un) && run.got.get(i) == 0;
        if (spec.burnsOnOvervoltage()) {
            boolean stays = run.m.state() == MachineState.OVERVOLTAGE_TRIPPED;
            run.focus = 30;
            check("overvoltage burns", run, trip >= 30 && trip <= 31 && once && quiet && stays,
                "1.45 x nominal: tripped after %d tick(s), %s; %s at the end (a burnt machine never resets)", trip - 29, t, run.m.state());
        } else {
            int back = run.first(MachineState.RUNNING, 40);
            boolean resets = back >= 40 + cooldownTicks - 1 && back <= 40 + cooldownTicks + 2;
            check("overvoltage trip and cooldown", run, trip >= 30 && trip <= 31 && once && quiet && resets,
                "1.45 x nominal: tripped after %d tick(s), %s, nothing %s while tripped: %b; line restored: RUNNING after %d ticks "
                    + "(cooldown %d)", trip - 29, t, producer ? "fed" : "drawn", quiet, back - 39, cooldownTicks);
        }
    }

    private void overheat() {
        var th = spec.thermal().orElse(null);
        if (th == null) {
            skip("overheat", "no heat model");
            return;
        }
        double amb = MachineSpec.AMBIENT_CELSIUS, delta = th.overheatCelsius() - amb, tau = th.heatCapacityJPerK() / th.heatLossWPerK();
        double heatW = 1.5 * th.heatLossWPerK() * delta / (1 - Math.exp(-5 / tau)); // over the trip point in about 5 s
        double resetC = th.overheatCelsius() - Math.min(th.overheatResetKelvin(), delta / 2);
        Run run = new Run(1);
        if (producer) run.load(un * un / rated);
        run.run(30);
        run.heat = heatW * DT;
        int over = -1;
        for (int i = 0; i < 400 && over < 0; i++) {
            run.run(1);
            if (run.m.celsius() > th.overheatCelsius()) over = run.ticks - 1;
        }
        run.heat = 0;
        run.run(2); // (the heat given in the tick it tripped still lands)
        run.focus = over;
        if (over < 0) {
            check("overheat trip", run, false, "%.4g W of heat for 20 s never passed %.1f C (at %.1f C)", heatW, th.overheatCelsius(), run.m.celsius());
            return;
        }
        boolean trips = run.states.get(over) == MachineState.OVERHEAT_TRIPPED && run.trips.size() == 1
            && run.trips.get(0).state() == MachineState.OVERHEAT_TRIPPED;
        double t0 = run.m.celsius();
        double coolTicks = tau * Math.log((t0 - amb) / (resetC - amb)) / DT;
        if (coolTicks + cooldownTicks > MAX_TICKS) {
            check("overheat trip", run, trips, "tripped at %.1f C (limit %.1f C), %d trip(s)", run.m.celsius(), th.overheatCelsius(), run.trips.size());
            skip("overheat reset", String.format(Locale.ROOT, "cools too slowly for a bench: %.0f s to %.1f C (time constant %.0f s)",
                coolTicks * DT, resetC, tau));
            return;
        }
        int cool = -1;
        boolean held = true;
        int limit = (int) (coolTicks * 1.05) + cooldownTicks + 200;
        for (int i = 0; i < limit && run.m.state() == MachineState.OVERHEAT_TRIPPED; i++) {
            run.run(1);
            if (cool < 0 && run.m.celsius() < resetC) cool = run.ticks - 1;
            if (cool < 0) held &= run.m.state() == MachineState.OVERHEAT_TRIPPED;
        }
        int back = run.first(MachineState.RUNNING, over);
        run.focus = cool < 0 ? -1 : back < 0 ? cool + cooldownTicks : back;
        boolean resets = cool >= 0 && back >= cool + cooldownTicks - 1 && back <= cool + cooldownTicks + 2;
        check("overheat trip and reset", run, trips && held && resets,
            "tripped at %.1f C (limit %.1f C), %d trip(s); held while over %.1f C: %b; RUNNING %d ticks after it cooled below it (cooldown %d)",
            t0, th.overheatCelsius(), run.trips.size(), resetC, held, cool < 0 || back < 0 ? -1 : back - cool + 1, cooldownTicks);
    }

    /** A producer's droop: open-circuit volts, rated power at nominal, the current limit, and output paced by its queue. */
    private void droop() {
        double rd = (MachineCore.DROOP_OPEN_RATIO - 1) * un * un / rated, rl = un * un / rated;
        Run open = new Run(Double.NaN);
        open.run(40);
        check("droop: open circuit", open, near(open.m.volts(), MachineCore.DROOP_OPEN_RATIO * un, 0.01),
            "%.4g V with nothing to feed (expected %.4g V)", open.m.volts(), MachineCore.DROOP_OPEN_RATIO * un);
        Run full = new Run(Double.NaN).load(rl);
        full.run(60);
        check("droop: rated power at nominal volts", full, near(full.m.volts(), un, 0.01) && near(full.bench.loadWatts(), rated, 0.02),
            "into %.4g ohm: %.4g V, %.4g W (rated %.4g W)", rl, full.m.volts(), full.bench.loadWatts(), rated);
        Run shorted = new Run(Double.NaN).load(rd / 100);
        shorted.run(60);
        check("droop: current limit", shorted, shorted.m.amps() <= ilim * 1.01 && shorted.m.amps() >= 0.9 * ilim,
            "into a short: %.4g A (limit %.4g A)", shorted.m.amps(), ilim);
        Run paced = new Run(Double.NaN).load(rl);
        paced.ask = 0.5 * ratedJ;
        paced.run(40);
        double before = paced.m.joulesDelivered();
        paced.run(40);
        double perTick = (paced.m.joulesDelivered() - before) / 40;
        check("droop: output paced by what is given", paced, near(perTick, 0.5 * ratedJ, 0.02),
            "given %.4g J/tick, fed %.4g J/tick", 0.5 * ratedJ, perTick);
    }

    /** What an open resistor (1 Gohm, MnaConst.highImpedance) leaks at v volts, W (twice, for margin). */
    private static double leakW(double v) {
        return 2 * v * v / 1e9;
    }
}
