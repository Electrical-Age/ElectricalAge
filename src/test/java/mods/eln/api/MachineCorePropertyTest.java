package mods.eln.api;

import mods.eln.api.impl.MachineCore;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Properties of MachineCore over random specs and histories, with no circuit: a consumer's line delivers what its
 * planned resistance draws at the next tick's volts (which wander through every threshold), a producer's source
 * runs into a random Thevenin network. Energy is conserved, the buffer stays in bounds, the draw stays under its
 * caps, trips latch for their cooldown, undervoltage doesn't chatter, and an ask sizes the very next step.
 */
public class MachineCorePropertyTest {
    static final double DT = MachineCore.TICK_SECONDS;

    /** One tick of a consumer history: line volts as a share of nominal, the owner's action and amount (share of a tick's rated energy). */
    record Tick(double ratio, int action, double share) {
    }

    @Provide
    Arbitrary<MachineSpec> consumers() {
        return Combinators.combine(Arbitraries.of(Tier.values()), Arbitraries.doubles().between(10, 20000),
                Arbitraries.doubles().between(0, 0.5), Arbitraries.doubles().between(0.5, 0.9),
                Arbitraries.doubles().between(0.5, 4))
            .as((t, rated, idle, floor, cool) -> MachineSpec.consumer(t).ratedWatts(rated).idleWatts(idle * rated)
                .minVoltageRatio(floor).tripCooldown(cool).build());
    }

    /** Volts that dwell near every threshold: 0, the floor, nominal, 1.1 and 1.3. */
    static Arbitrary<Double> ratios() {
        return Arbitraries.frequencyOf(
            net.jqwik.api.Tuple.of(1, Arbitraries.just(0.0)),
            net.jqwik.api.Tuple.of(4, Arbitraries.doubles().between(0.9, 1.1)),
            net.jqwik.api.Tuple.of(2, Arbitraries.doubles().between(0.6, 0.95)),
            net.jqwik.api.Tuple.of(2, Arbitraries.doubles().between(1.05, 1.15)),
            net.jqwik.api.Tuple.of(2, Arbitraries.doubles().between(1.25, 1.35)),
            net.jqwik.api.Tuple.of(1, Arbitraries.doubles().between(0, 1.6)));
    }

    @Provide
    Arbitrary<List<Tick>> histories() {
        Arbitrary<Tick> tick = Combinators.combine(ratios(), Arbitraries.integers().between(0, 2),
            Arbitraries.doubles().between(0, 2)).as(Tick::new);
        // runs of equal volts, so states get time to settle and cooldowns to finish
        return Combinators.combine(tick, Arbitraries.integers().between(1, 80)).as((t, n) -> java.util.Collections.nCopies(n, t))
            .list().ofMinSize(1).ofMaxSize(30)
            .map(runs -> runs.stream().flatMap(List::stream).toList());
    }

    /** The owner's action: 0 nothing, 1 takeJoules, 2 tryTakeJoules (within the buffer's size). */
    static double act(MachineCore c, Tick t) {
        double j = t.share() * c.ratedJoulesPerTick();
        return switch (t.action()) {
            case 1 -> c.takeJoules(j);
            case 2 -> c.tryTakeJoules(Math.min(j, c.bufferCapacityJoules())) ? Math.min(j, c.bufferCapacityJoules()) : 0;
            default -> 0;
        };
    }

    /** Runs a consumer through a history; the step delivers U^2 / R for the planned R at that tick's volts. */
    static void runConsumer(MachineSpec spec, List<Tick> history, TickCheck check) {
        MachineCore c = new MachineCore(spec);
        double ohms = Double.POSITIVE_INFINITY;
        for (int i = 0; i < history.size(); i++) {
            Tick t = history.get(i);
            double u = t.ratio() * spec.nominalVolts();
            c.afterStep(u, Double.isInfinite(ohms) ? 0 : u * u / ohms * DT);
            MachineState state = c.state();
            double got = act(c, t);
            double p = c.beforeStep();
            ohms = c.loadOhms();
            check.check(i, c, state, u, got, p);
        }
    }

    interface TickCheck {
        void check(int tick, MachineCore c, MachineState state, double volts, double got, double plannedWatts);
    }

    @Property
    void consumerConservesEnergy(@ForAll("consumers") MachineSpec spec, @ForAll("histories") List<Tick> history) {
        runConsumer(spec, history, (i, c, state, u, got, p) -> {
            double eps = 1e-9 * (1 + c.joulesIn());
            assertTrue(c.joulesOut() + c.bufferJoules() <= c.joulesIn() + eps,
                "tick " + i + ": taken " + c.joulesOut() + " + buffered " + c.bufferJoules() + " > delivered " + c.joulesIn());
            assertTrue(c.bufferJoules() >= 0 && c.bufferJoules() <= c.bufferCapacityJoules() * (1 + 1e-12),
                "tick " + i + ": buffer " + c.bufferJoules() + " outside 0.." + c.bufferCapacityJoules());
            if (state != MachineState.RUNNING) assertEquals(0, got, "tick " + i + ": took while " + state);
        });
    }

    @Property
    void consumerDrawStaysUnderCaps(@ForAll("consumers") MachineSpec spec, @ForAll("histories") List<Tick> history) {
        runConsumer(spec, history, (i, c, state, u, got, p) -> {
            double r = Math.abs(u) / spec.nominalVolts(), eps = 1e-9 * spec.ratedWatts();
            assertTrue(p <= spec.ratedWatts() * r * r + eps, "tick " + i + ": " + p + " W over rated (U/Unom)^2 at " + u + " V");
            assertTrue(p <= spec.ratedWatts() + eps, "tick " + i + ": " + p + " W over rated");
            if (r < spec.minVoltageRatio() || state != MachineState.RUNNING)
                assertTrue(p <= spec.idleWatts() + eps, "tick " + i + ": " + p + " W, more than idle while " + state + " at " + u + " V");
            if (state.tripped()) assertEquals(0, p, "tick " + i + ": draws while " + state);
        });
    }

    /** A trip lasts at least its cooldown; an overvoltage trip ends only after a cooldown spent under 1.1x. */
    @Property
    void tripsLatchForTheirCooldown(@ForAll("consumers") MachineSpec spec, @ForAll("histories") List<Tick> history) {
        int cooldown = (int) Math.ceil(spec.tripCooldownSeconds() / DT - 1e-9);
        int[] since = {-1};
        MachineState[] prev = {MachineState.UNPOWERED};
        double[] ratios = history.stream().mapToDouble(Tick::ratio).toArray();
        runConsumer(spec, history, (i, c, state, u, got, p) -> {
            if (state.tripped() && !prev[0].tripped()) since[0] = i;
            if (prev[0].tripped() && !state.tripped()) {
                assertTrue(i - since[0] >= cooldown, "tick " + i + ": " + prev[0] + " reset after " + (i - since[0]) + " < " + cooldown + " ticks");
                if (prev[0] == MachineState.OVERVOLTAGE_TRIPPED)
                    for (int k = i - cooldown + 1; k <= i; k++)
                        assertTrue(ratios[k] < MachineCore.OVERVOLTAGE_RESET_RATIO, "tick " + i + ": reset though tick " + k + " was at " + ratios[k]);
            }
            prev[0] = state;
        });
    }

    /** Volts hovering around the trip point trip it at most once per cooldown, never every tick. */
    @Property
    void noChatterAroundTheTripPoint(@ForAll("consumers") MachineSpec spec, @ForAll @DoubleRange(min = 0.01, max = 0.3) double swing,
                                     @ForAll @IntRange(min = 2, max = 7) int period) {
        int cooldown = (int) Math.ceil(spec.tripCooldownSeconds() / DT - 1e-9);
        List<Tick> history = new java.util.ArrayList<>();
        for (int i = 0; i < 600; i++)
            history.add(new Tick(1.3 + (i % period < period / 2 ? swing : -swing), 1, 0.5));
        int[] trips = {0};
        MachineState[] prev = {MachineState.UNPOWERED};
        runConsumer(spec, history, (i, c, state, u, got, p) -> {
            if (state.tripped() && !prev[0].tripped()) trips[0]++;
            prev[0] = state;
        });
        assertTrue(trips[0] <= 1 + 600 / cooldown, trips[0] + " trips in 600 ticks, cooldown " + cooldown);
    }

    /** UNDERVOLTAGE ends only after UNDERVOLTAGE_RECOVERY_TICKS ticks; a line hovering on the floor can't flip it every tick. */
    @Property
    void undervoltageDoesNotChatter(@ForAll("consumers") MachineSpec spec, @ForAll("histories") List<Tick> history) {
        int[] since = {-1};
        MachineState[] prev = {MachineState.UNPOWERED};
        runConsumer(spec, history, (i, c, state, u, got, p) -> {
            if (state == MachineState.UNDERVOLTAGE && prev[0] != MachineState.UNDERVOLTAGE) since[0] = i;
            if (prev[0] == MachineState.UNDERVOLTAGE && state == MachineState.RUNNING)
                assertTrue(i - since[0] >= MachineCore.UNDERVOLTAGE_RECOVERY_TICKS, "tick " + i + ": running again after " + (i - since[0]));
            prev[0] = state;
        });
    }

    /**
     * Two machines with the same history except a bigger ask at tick n plan the same load up to tick n - 1, and at
     * tick n (the step of tick n + 1) the bigger ask plans at least as much, more when it was served: the
     * demand-to-load lag is exactly one tick.
     */
    @Property
    void askSizesTheNextStep(@ForAll("consumers") MachineSpec spec, @ForAll("histories") List<Tick> history,
                             @ForAll @DoubleRange(min = 0.01, max = 0.5) double extra, @ForAll @IntRange(min = 0, max = 10000) int at) {
        int n = at % history.size();
        MachineCore a = new MachineCore(spec), b = new MachineCore(spec);
        double ohmsA = Double.POSITIVE_INFINITY, ohmsB = Double.POSITIVE_INFINITY;
        for (int i = 0; i <= n; i++) {
            Tick t = history.get(i);
            double u = t.ratio() * spec.nominalVolts();
            a.afterStep(u, Double.isInfinite(ohmsA) ? 0 : u * u / ohmsA * DT);
            b.afterStep(u, Double.isInfinite(ohmsB) ? 0 : u * u / ohmsB * DT);
            double gotA = a.takeJoules(t.share() / 4 * a.ratedJoulesPerTick());
            double gotB = b.takeJoules((t.share() / 4 + (i == n ? extra : 0)) * b.ratedJoulesPerTick());
            double pA = a.beforeStep(), pB = b.beforeStep();
            ohmsA = a.loadOhms();
            ohmsB = b.loadOhms();
            if (i < n) assertEquals(pA, pB, "tick " + i + " before the extra ask at " + n);
            else {
                assertTrue(pB >= pA, "the bigger ask planned less: " + pB + " < " + pA);
                double cap = spec.ratedWatts() * Math.pow(u / spec.nominalVolts(), 2);
                if (gotB > gotA && pA < Math.min(cap, spec.ratedWatts()) * (1 - 1e-9))
                    assertTrue(pB > pA, "the served bigger ask planned no more: " + pB + " = " + pA);
            }
        }
    }

    @Provide
    Arbitrary<MachineSpec> producers() {
        return Combinators.combine(Arbitraries.of(Tier.values()), Arbitraries.doubles().between(10, 20000))
            .as((t, rated) -> MachineSpec.producer(t).ratedWatts(rated).build());
    }

    /** One tick of a producer history: Thevenin volts (share of nominal) and ohms (times the droop resistance), and the give (ticks of rated energy). */
    record Net(double ratio, double ohmsShare, double give) {
    }

    @Provide
    Arbitrary<List<Net>> networks() {
        Arbitrary<Double> ohms = Arbitraries.frequencyOf(
            net.jqwik.api.Tuple.of(8, Arbitraries.doubles().between(-2, 4).map(x -> Math.pow(10, x))),
            net.jqwik.api.Tuple.of(1, Arbitraries.just(1e30)));
        return Combinators.combine(Arbitraries.doubles().between(0, 1.4), ohms, Arbitraries.doubles().between(0, 2.5))
            .as(Net::new).list().ofMinSize(1).ofMaxSize(200);
    }

    /** A producer never feeds more than it was given, and its source respects the droop and the current limit. */
    @Property
    void producerNeverFeedsMoreThanGiven(@ForAll("producers") MachineSpec spec, @ForAll("networks") List<Net> history) {
        MachineCore c = new MachineCore(spec);
        double rInt = c.sourceOhms(), uNom = spec.nominalVolts(), iMax = spec.ratedWatts() / uNom;
        for (int i = 0; i < history.size(); i++) {
            Net n = history.get(i);
            double uTh = n.ratio() * uNom, rTh = rInt + n.ohmsShare() * rInt;
            double e = c.sourceVolts(uTh, rTh);
            double amps = c.sourceOn() && rTh < 1e18 ? Math.max(0, (e - uTh) / rTh) : 0;
            if (c.sourceOn() && rTh < 1e18) {
                assertTrue(e >= uTh - 1e-9 * uNom, "tick " + i + ": source " + e + " V under the line's " + uTh + " V");
                assertTrue(e <= Math.max(MachineCore.DROOP_OPEN_RATIO * uNom, uTh) * (1 + 1e-12), "tick " + i + ": source " + e + " V over open circuit");
                assertTrue(amps <= iMax * (1 + 1e-9), "tick " + i + ": " + amps + " A over the limit " + iMax);
            }
            c.afterStep(e - amps * rInt, e * amps * DT);
            assertTrue(c.joulesOut() <= c.joulesIn() + 1e-9 * (1 + c.joulesIn()),
                "tick " + i + ": fed " + c.joulesOut() + " J of " + c.joulesIn() + " given");
            c.giveJoules(n.give() * c.ratedJoulesPerTick());
            assertTrue(c.bufferJoules() <= c.bufferCapacityJoules() * (1 + 1e-12));
            c.beforeStep();
        }
    }

    /** An overdraw (another source moved the line within the step) is carried: the books always balance, and gives repay it. */
    @Property
    void producerCarriesOverdraw(@ForAll("producers") MachineSpec spec, @ForAll("networks") List<Net> history,
                                 @ForAll @DoubleRange(min = 0, max = 0.2) double over) {
        MachineCore c = new MachineCore(spec);
        double rInt = c.sourceOhms(), uNom = spec.nominalVolts();
        for (Net n : history) {
            double uTh = n.ratio() * uNom, rTh = rInt + n.ohmsShare() * rInt;
            double e = c.sourceVolts(uTh, rTh);
            double amps = c.sourceOn() && rTh < 1e18 ? Math.max(0, (e - uTh) / rTh) * (1 + over) : 0;
            c.afterStep(e - amps * rInt, e * amps * DT);
            double before = c.bufferJoules(), give = n.give() * c.ratedJoulesPerTick();
            double accepted = c.giveJoules(give);
            assertEquals(Math.min(give, Math.max(0, c.bufferCapacityJoules() - before)), accepted, 1e-9 * (1 + give));
            assertEquals(c.joulesIn() - c.joulesOut(), c.bufferJoules(), 1e-9 * (1 + c.joulesIn()));
            c.beforeStep();
        }
    }
}
