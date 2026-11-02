package mods.eln.api.impl;

import mods.eln.api.MachineSpec;
import mods.eln.api.MachineState;
import mods.eln.api.MachineView;
import mods.eln.api.TripCause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * One machine's controller, in plain numbers (no circuit types): its energy buffer, state machine, heat model and
 * what it asks of the circuit. A host calls, once per tick and in this order:
 * <ol>
 * <li>afterStep(lineVolts, joules) after the electrical step (outside it: trip listeners run here);</li>
 * <li>the owner's takes or gives, in the owner's tick;</li>
 * <li>beforeStep() before the next electrical step, then applies loadOhms() (consumer) or sourceOn() (producer);
 * a producer's source voltage comes from sourceVolts(theveninVolts, theveninOhms) during the step.</li>
 * </ol>
 * Consumer: the buffer holds at most two ticks of rated work energy. The next step's load is idle watts plus work:
 * the demand smoothed over two ticks (fast attack: a rise counts at once, a fall halves first) plus a refill of at
 * most a quarter of a tick's rated work, but never more than the buffer's room, and none without demand; all
 * capped at rated x (U/U_nom)^2 and at rated. An idle machine draws its idle watts only. Delivered energy pays the
 * idle watts first; the rest goes to the buffer, and what doesn't fit (the line rose) is heat.
 * <p>
 * Producer: the buffer is the queue of given energy, at most two ticks of rated output. The source is on while the
 * queue holds energy, behind the droop resistance 0.05 U_nom^2 / rated (1.05 U_nom open, rated watts at U_nom);
 * sourceVolts lowers its voltage so the step takes no more than the queue and the current stays under rated /
 * U_nom (EA's PowerSource rule). Several sources in one circuit can still overdraw a little within one step; the
 * overdraw is carried as a negative queue that later gives repay.
 * <p>
 * Not thread-safe: hosts check the calling thread before delegating; this class does not.
 */
public final class MachineCore implements MachineView {
    private static final Logger LOG = LoggerFactory.getLogger("eln");
    /** Specs whose owner already got the "asked more than a tick" warning (once per spec, not per machine). */
    private static final Set<MachineSpec> WARNED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    /** One server tick, the electrical step, s. */
    public static final double TICK_SECONDS = 0.05;
    /** Under this share of nominal volts the line counts as dead. */
    public static final double UNPOWERED_RATIO = 0.02;
    /** Over this share of nominal volts the machine trips. */
    public static final double OVERVOLTAGE_RATIO = 1.3;
    /** The overvoltage cooldown only counts below this share of nominal volts. */
    public static final double OVERVOLTAGE_RESET_RATIO = 1.1;
    /** UNDERVOLTAGE ends this far (share of nominal) above the floor... */
    public static final double UNDERVOLTAGE_BAND = 0.05;
    /** ...held for this many ticks, so a line that sags under the machine's own load doesn't flip it every tick. */
    public static final int UNDERVOLTAGE_RECOVERY_TICKS = 10;
    /** Overpower compares a running average, avg += ALPHA (P - avg) per tick, so one tick's surge doesn't trip it. */
    public static final double POWER_AVERAGE_ALPHA = 0.25;
    /** The buffer refill, as a share of rated work power. */
    public static final double REFILL_SHARE = 0.25;
    /** A producer's open-circuit voltage, share of nominal. */
    public static final double DROOP_OPEN_RATIO = 1.05;
    /** The buffer, in ticks of rated energy. */
    public static final double BUFFER_TICKS = 2;
    /** Above this Thevenin resistance (ohm) nothing is connected to a source. */
    static final double OPEN_OHMS = 1e18;

    private final MachineSpec spec;
    private final boolean producer;
    private final double uNom, ratedJoulesPerTick, capacity, workWatts;
    private final int cooldownTicks;
    private final MachineSpec.Thermal thermal;
    private final List<Consumer<TripCause>> listeners = new ArrayList<>();

    private double volts, amps, watts, buffer, powerAverage, heatPending;
    private double celsius = MachineSpec.AMBIENT_CELSIUS, ambientCelsius = MachineSpec.AMBIENT_CELSIUS;
    private MachineState state = MachineState.UNPOWERED;
    private TripCause lastTrip;
    private boolean burnt;
    private int tripTimer, undervoltageTimer;
    private long ticks;

    // consumer
    private double demand, previousDemand, idleCommand, loadOhms = Double.POSITIVE_INFINITY;
    // producer
    private boolean sourceOn;
    private double sourceVolts;
    // books: consumer: delivered in / taken out; producer: given in / fed out (source side)
    private double joulesIn, joulesOut;

    public MachineCore(MachineSpec spec) {
        this.spec = spec;
        producer = spec.kind() == MachineSpec.Kind.PRODUCER;
        uNom = spec.nominalVolts();
        workWatts = spec.ratedWatts() - spec.idleWatts();
        ratedJoulesPerTick = workWatts * TICK_SECONDS;
        capacity = BUFFER_TICKS * ratedJoulesPerTick;
        cooldownTicks = (int) Math.ceil(spec.tripCooldownSeconds() / TICK_SECONDS - 1e-9);
        thermal = spec.thermal().orElse(null);
    }

    // ---- the circuit side

    /**
     * After the electrical step: lineVolts at the machine, and joules the step moved (consumer: drawn from the
     * line; producer: out of its source, droop losses included; a host clamps a reverse flow to 0). Updates the
     * buffer, heat and state; trip listeners run here.
     */
    public void afterStep(double lineVolts, double joules) {
        if (!Double.isFinite(lineVolts)) throw new IllegalArgumentException("lineVolts must be finite, got " + lineVolts);
        checkAmount("joules", joules);
        ticks++;
        volts = lineVolts;
        double a = Math.abs(lineVolts), heat = heatPending;
        heatPending = 0;
        if (producer) {
            buffer -= joules;
            joulesOut += joules;
            amps = sourceVolts > 0 ? joules / (sourceVolts * TICK_SECONDS) : 0;
            watts = a * amps;
            heat += Math.max(0, joules - watts * TICK_SECONDS); // the droop resistance's loss
        } else {
            joulesIn += joules;
            watts = joules / TICK_SECONDS;
            amps = a > 0 ? watts / a : 0;
            double idle = Math.min(joules, idleCommand * TICK_SECONDS);
            double work = joules - idle;
            double stored = Math.max(0, Math.min(work, capacity - buffer));
            buffer += stored;
            heat += idle + work - stored;
        }
        powerAverage += POWER_AVERAGE_ALPHA * (watts - powerAverage);
        if (thermal != null) {
            double k = thermal.lossWperK();
            double eq = ambientCelsius + heat / TICK_SECONDS / k;
            celsius = eq + (celsius - eq) * Math.exp(-k * TICK_SECONDS / thermal.heatCapacityJperK());
        }
        updateState();
    }

    /**
     * Before the next electrical step: plans it. Consumer: returns the watts to draw (loadOhms() is the matching
     * resistance at the last line volts). Producer: switches the source on or off (sourceOn()) and returns the
     * most it may feed, W.
     */
    public double beforeStep() {
        if (producer) {
            sourceOn = !state.tripped() && buffer > 0;
            return sourceOn ? Math.min(buffer / TICK_SECONDS, DROOP_OPEN_RATIO * spec.ratedWatts()) : 0;
        }
        double idle = 0, work = 0;
        if (!state.tripped() && state != MachineState.UNPOWERED) {
            double r = Math.abs(volts) / uNom;
            double cap = spec.ratedWatts() * r * r;
            idle = Math.min(spec.idleWatts(), cap);
            if (state == MachineState.RUNNING) {
                double smoothed = Math.max(demand, (demand + previousDemand) / 2);
                double joules = smoothed > 0
                    ? Math.min(capacity - buffer, smoothed + REFILL_SHARE * ratedJoulesPerTick) : 0;
                work = Math.max(0, Math.min(joules / TICK_SECONDS, Math.min(workWatts, cap - idle)));
            }
        }
        previousDemand = demand;
        demand = 0;
        idleCommand = idle;
        double p = idle + work;
        loadOhms = p > 0 ? volts * volts / p : Double.POSITIVE_INFINITY;
        return p;
    }

    /** Consumer: the resistance that draws beforeStep()'s watts at the last line volts, ohm; infinite for none. */
    public double loadOhms() {
        return loadOhms;
    }

    /** Producer: is the source connected for the next step? */
    public boolean sourceOn() {
        return sourceOn;
    }

    /** Producer: the droop (internal) resistance, 0.05 U_nom^2 / rated, ohm. */
    public double sourceOhms() {
        return (DROOP_OPEN_RATIO - 1) * uNom * uNom / spec.ratedWatts();
    }

    /**
     * Producer, during the step: the source voltage for the circuit seen from the source, theveninVolts behind
     * theveninOhms (droop resistance included): the lowest of the open-circuit 1.05 U_nom, the current limit
     * rated / U_nom and the power the queue holds for one tick; never under theveninVolts (no reverse current).
     */
    public double sourceVolts(double theveninVolts, double theveninOhms) {
        double open = DROOP_OPEN_RATIO * uNom;
        double e;
        if (!sourceOn) e = theveninVolts;
        else if (!(theveninOhms > 0 && theveninOhms < OPEN_OHMS) || !Double.isFinite(theveninVolts)) e = open;
        else {
            double p = Math.max(0, buffer) / TICK_SECONDS;
            double byPower = (theveninVolts + Math.sqrt(theveninVolts * theveninVolts + 4 * p * theveninOhms)) / 2;
            double byCurrent = theveninVolts + theveninOhms * spec.ratedWatts() / uNom;
            e = Math.max(theveninVolts, Math.min(open, Math.min(byCurrent, byPower)));
        }
        if (!Double.isFinite(e)) e = 0;
        sourceVolts = e;
        return e;
    }

    /** Sets the temperature the heat model cools to (the world's, at the machine), degrees C. */
    public void setAmbientCelsius(double c) {
        if (!Double.isFinite(c)) throw new IllegalArgumentException("ambient must be finite, got " + c);
        ambientCelsius = c;
    }

    // ---- the owner side (see Machine for the contract)

    public boolean tryTakeJoules(double joules) {
        consumerOnly("tryTakeJoules");
        checkAmount("joules", joules);
        if (joules > capacity)
            throw new IllegalArgumentException("tryTakeJoules(" + joules + ") can never succeed: " + spec
                + " buffers at most " + capacity + " J (two ticks at rated power); split the step or raise ratedWatts");
        demand = Math.min(ratedJoulesPerTick, demand + capAsk(joules));
        if (state != MachineState.RUNNING || buffer < joules * (1 - 1e-9)) return false;
        double got = Math.min(joules, buffer);
        buffer -= got;
        joulesOut += got;
        return true;
    }

    public double takeJoules(double joules) {
        consumerOnly("takeJoules");
        checkAmount("joules", joules);
        double ask = capAsk(joules);
        demand = Math.min(ratedJoulesPerTick, demand + ask);
        if (state != MachineState.RUNNING) return 0;
        double got = Math.min(ask, buffer);
        buffer -= got;
        joulesOut += got;
        return got;
    }

    public double giveJoules(double joules) {
        if (!producer) throw new IllegalStateException("giveJoules on a consumer (" + spec + "): use takeJoules");
        checkAmount("joules", joules);
        double accepted = Math.min(joules, Math.max(0, capacity - buffer));
        buffer += accepted;
        joulesIn += accepted;
        return accepted;
    }

    public void heatJoules(double joules) {
        checkAmount("joules", joules);
        if (thermal != null) heatPending += joules;
    }

    public void onTrip(Consumer<TripCause> listener) {
        listeners.add(java.util.Objects.requireNonNull(listener, "listener"));
    }

    // ---- reading

    @Override
    public MachineSpec spec() {
        return spec;
    }

    @Override
    public MachineState state() {
        return state;
    }

    @Override
    public Optional<TripCause> lastTrip() {
        return Optional.ofNullable(lastTrip);
    }

    @Override
    public double volts() {
        return volts;
    }

    @Override
    public double voltageRatio() {
        boolean on = producer ? !state.tripped() && state != MachineState.UNPOWERED : state == MachineState.RUNNING;
        return on ? Math.abs(volts) / uNom : 0;
    }

    @Override
    public double amps() {
        return amps;
    }

    @Override
    public double watts() {
        return watts;
    }

    @Override
    public double bufferJoules() {
        return buffer;
    }

    @Override
    public double celsius() {
        return celsius;
    }

    public double ratedJoulesPerTick() {
        return ratedJoulesPerTick;
    }

    public double bufferCapacityJoules() {
        return capacity;
    }

    /** Consumer: joules delivered by the line; producer: joules given by the owner. */
    public double joulesIn() {
        return joulesIn;
    }

    /** Consumer: joules taken by the owner; producer: joules fed out of the source. */
    public double joulesOut() {
        return joulesOut;
    }

    /** Overvoltage destroyed it (burnsOnOvervoltage): the host removes the machine; it never resets. */
    public boolean burnt() {
        return burnt;
    }

    /** afterStep calls so far. */
    public long ticks() {
        return ticks;
    }

    // ---- internals

    private void updateState() {
        if (burnt) return;
        double r = Math.abs(volts) / uNom;
        if (state.tripped()) {
            boolean clear = switch (state) {
                case OVERVOLTAGE_TRIPPED -> r < OVERVOLTAGE_RESET_RATIO;
                case OVERHEAT_TRIPPED -> celsius < thermal.tripCelsius() - MachineSpec.OVERHEAT_RESET_KELVIN;
                default -> true; // overpower: it draws nothing while tripped
            };
            tripTimer = clear ? tripTimer + 1 : 0;
            if (tripTimer < cooldownTicks) return;
            tripTimer = 0;
            state = MachineState.UNPOWERED; // re-classified below as if power had just come
        }
        if (r > OVERVOLTAGE_RATIO) {
            trip(new TripCause(MachineState.OVERVOLTAGE_TRIPPED, Math.abs(volts), OVERVOLTAGE_RATIO * uNom,
                spec.burnsOnOvervoltage()));
        } else if (powerAverage > spec.maxWatts()) {
            trip(new TripCause(MachineState.OVERPOWER_TRIPPED, powerAverage, spec.maxWatts(), false));
        } else if (thermal != null && celsius > thermal.tripCelsius()) {
            trip(new TripCause(MachineState.OVERHEAT_TRIPPED, celsius, thermal.tripCelsius(), false));
        } else if (r < UNPOWERED_RATIO) {
            state = MachineState.UNPOWERED;
            undervoltageTimer = 0;
        } else if (state == MachineState.UNDERVOLTAGE) {
            undervoltageTimer = r >= spec.minVoltageRatio() + UNDERVOLTAGE_BAND ? undervoltageTimer + 1 : 0;
            if (undervoltageTimer >= UNDERVOLTAGE_RECOVERY_TICKS) {
                state = MachineState.RUNNING;
                undervoltageTimer = 0;
            }
        } else {
            state = r < spec.minVoltageRatio() ? MachineState.UNDERVOLTAGE : MachineState.RUNNING;
        }
    }

    private void trip(TripCause cause) {
        state = cause.state();
        lastTrip = cause;
        tripTimer = 0;
        undervoltageTimer = 0;
        if (cause.burns()) burnt = true;
        for (Consumer<TripCause> l : List.copyOf(listeners)) l.accept(cause);
    }

    private double capAsk(double joules) {
        if (joules <= ratedJoulesPerTick) return joules;
        if (WARNED.add(spec))
            LOG.warn("Machine {}: asked {} J in one tick, more than the {} J it gets per tick at rated power; "
                + "capped (see Machine.ratedJoulesPerTick)", spec, joules, ratedJoulesPerTick);
        return ratedJoulesPerTick;
    }

    private void consumerOnly(String what) {
        if (producer) throw new IllegalStateException(what + " on a producer (" + spec + "): use giveJoules");
    }

    private static void checkAmount(String what, double v) {
        if (!(Double.isFinite(v) && v >= 0))
            throw new IllegalArgumentException(what + " must be finite and not negative, got " + v);
    }
}
