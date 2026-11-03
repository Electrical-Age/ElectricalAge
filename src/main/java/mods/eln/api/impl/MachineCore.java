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
 * most a quarter of a tick's rated work (held back a second when the line can't keep up), but never more than the buffer's
 * room, and none without demand; all capped at rated x (U/U_nom)^2 and at rated. An idle machine draws its idle
 * watts only. A Brownout.SLOW consumer on a soft line plans against the line as it is (see LINE_MODEL_SAG): never
 * past the line's maximum power, so it can't pull its own line down to its rated resistance. Delivered energy pays the idle watts first; the rest goes to the buffer, and what doesn't fit (the
 * line rose) is heat. joulesIn() == joulesOut() + bufferJoules() + joulesLost() at all times (for a producer too).
 * <p>
 * Producer: the buffer is the queue of given energy, at most two ticks of rated output. The source is on while the
 * queue holds energy (and SOURCE_HOLD_TICKS after, feeding nothing), behind the droop resistance 0.05 U_nom^2 / rated (1.05 U_nom open, rated watts at U_nom);
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
    /** UNDERVOLTAGE ends this far (share of nominal) above the floor, or halfway from the floor to nominal if nearer... */
    public static final double UNDERVOLTAGE_BAND = 0.05;
    /**
     * ...held for this many ticks; coming from UNPOWERED, the floor itself held as long. So a line that sags under
     * the machine's own load, or flickers, doesn't flip it every tick.
     */
    public static final int UNDERVOLTAGE_RECOVERY_TICKS = 10;
    /** The buffer refill, as a share of rated work power. */
    public static final double REFILL_SHARE = 0.25;
    /**
     * The refill stops for REFILL_HOLDOFF_TICKS when two steps in a row deliver less than this share of the work
     * they planned: on a source that can't keep up (a starved producer) it waits instead of pulling the voltage
     * down further, which would collapse it. One short step is only a resistive line sagging under a new load (the
     * next plan, made at the sagged volts, is met).
     */
    public static final double REFILL_KEPT_UP = 0.95;
    public static final int REFILL_HOLDOFF_TICKS = 20;
    /**
     * Brownout.SLOW consumers learn their line's Thevenin resistance from two steps whose current differs by more
     * than LINE_LEARN_SHARE (the rest of the circuit unchanged in between: R = -dU/dI, and none if that isn't
     * positive). Planning on the last volts alone (R = U^2 / P) assumes a stiff line: asked for more than a weak line
     * can carry, it lowers its resistance past the line's maximum power and the line collapses to its rated
     * resistance. When the learnt line would sag over LINE_MODEL_SAG at the planned power, the plan is capped at the
     * line's maximum power E^2 / 4R and its resistance solved on the line's stiff side; below that the old rule
     * stands. Brownout.STOP machines keep the old rule: a source that can't carry their demand stops them (Baughn: the
     * starved-supply behaviour is the spec's choice).
     */
    public static final double LINE_LEARN_SHARE = 0.02, LINE_MODEL_SAG = 0.01;
    /** A producer's open-circuit voltage, share of nominal. */
    public static final double DROOP_OPEN_RATIO = 1.05;
    /**
     * A producer's source stays connected this many ticks after its queue empties, at zero current, so an owner
     * that gives every few ticks doesn't switch the circuit (and rebuild its matrix) each time.
     */
    public static final int SOURCE_HOLD_TICKS = 20;
    /** The buffer, in ticks of rated energy. */
    public static final double BUFFER_TICKS = 2;
    /** Above this Thevenin resistance (ohm) nothing is connected to a source. */
    static final double OPEN_OHMS = 1e18;

    private final boolean producer;
    // from the spec (respec changes them)
    private MachineSpec spec;
    private double uNom, ratedJoulesPerTick, capacity, workWatts, floor, recoveryRatio;
    private int cooldownTicks;
    private MachineSpec.Thermal thermal;
    private final List<Consumer<TripCause>> listeners = new ArrayList<>();

    private double volts, amps, watts, buffer, heatPending;
    private double celsius = MachineSpec.AMBIENT_CELSIUS, ambientCelsius = MachineSpec.AMBIENT_CELSIUS;
    private MachineState state = MachineState.UNPOWERED;
    private TripCause lastTrip;
    private boolean burnt;
    private int tripTimer, upTimer;
    private long ticks;

    // consumer
    private double demand, failedTry, previousDemand, idleCommand, workCommand, loadOhms = Double.POSITIVE_INFINITY;
    private double lineOhms, lineVolts, prevVolts = Double.NaN, prevAmps = Double.NaN;
    private int keptUpTicks = REFILL_HOLDOFF_TICKS, shortSteps;
    // producer
    private boolean sourceOn;
    private int emptyTicks = SOURCE_HOLD_TICKS; // a producer never given anything starts open
    private double sourceVolts;
    // books: consumer: delivered in / taken out / idle and spilled; producer: given in / fed out (source side)
    private double joulesIn, joulesOut, joulesLost;

    public MachineCore(MachineSpec spec) {
        producer = spec.kind() == MachineSpec.Kind.PRODUCER;
        derive(spec);
    }

    private void derive(MachineSpec spec) {
        this.spec = spec;
        uNom = spec.nominalVolts();
        workWatts = spec.ratedWatts() - spec.idleWatts();
        ratedJoulesPerTick = workWatts * TICK_SECONDS;
        capacity = BUFFER_TICKS * ratedJoulesPerTick;
        cooldownTicks = (int) Math.ceil(spec.tripCooldownSeconds() / TICK_SECONDS - 1e-9);
        floor = Math.max(UNPOWERED_RATIO, spec.minVoltageRatio()); // Brownout.SLOW: none above UNPOWERED
        recoveryRatio = Math.min(floor + UNDERVOLTAGE_BAND, (1 + floor) / 2);
        thermal = spec.thermal().orElse(null);
    }

    /**
     * Swaps in a spec with other ratings (see Machine.respec): same kind, faces, signals, bus and burn, else
     * IllegalArgumentException. State, books and heat carry over; a buffer over the new capacity is cut to it and
     * the excess booked as lost (a consumer's as heat).
     */
    public void respec(MachineSpec next) {
        java.util.Objects.requireNonNull(next, "spec");
        List<String> differs = new ArrayList<>();
        if (next.kind() != spec.kind()) differs.add("kind");
        if (!next.powerFaces().equals(spec.powerFaces())) differs.add("power faces");
        if (!next.signalsIn().equals(spec.signalsIn())) differs.add("signal inputs");
        if (!next.signalsOut().equals(spec.signalsOut())) differs.add("signal outputs");
        if (!next.busFace().equals(spec.busFace())) differs.add("bus face");
        // (a host checks the burn when it is made: EA's burn-out can't come or go after; a handler may be a new object)
        if (next.burnsOnOvervoltage() != spec.burnsOnOvervoltage() || ElementBurn.of(next) != ElementBurn.of(spec))
            differs.add("overvoltage burn");
        if (!differs.isEmpty())
            throw new IllegalArgumentException("respec changes ratings only; " + next + " differs from " + spec
                + " in its " + String.join(", ", differs));
        boolean hadHeat = thermal != null;
        derive(next);
        if (thermal == null || !hadHeat) celsius = ambientCelsius;
        if (state == MachineState.OVERHEAT_TRIPPED && thermal == null) { // no heat model left to cool down
            state = MachineState.UNPOWERED;
            tripTimer = 0;
        }
        if (buffer > capacity) {
            double excess = buffer - capacity;
            buffer = capacity;
            joulesLost += excess;
            if (!producer) heatPending += excess;
        }
    }

    /**
     * What a host saves of a machine (see snapshot()); state is only kept when it is a trip or RUNNING. burnt: it
     * burnt (burnsOnOvervoltage) and was never repaired.
     */
    public record Saved(double bufferJoules, double celsius, MachineState state, int tripTicks, TripCause lastTrip, boolean burnt) {
        /** Not burnt. */
        public Saved(double bufferJoules, double celsius, MachineState state, int tripTicks, TripCause lastTrip) {
            this(bufferJoules, celsius, state, tripTicks, lastTrip, false);
        }
    }

    /**
     * The machine's lasting state: its buffer, temperature, a trip in progress or that it was running, the last
     * trip, and whether it burnt. A burn is not kept as a trip: restored, it is burnt again, and repaired it starts
     * UNPOWERED as repair() leaves it.
     */
    public Saved snapshot() {
        boolean trip = state.tripped() && !burnt;
        MachineState kept = trip || state == MachineState.RUNNING && !burnt ? state : MachineState.UNPOWERED;
        return new Saved(buffer, celsius, kept, trip ? tripTimer : 0, lastTrip, burnt);
    }

    /**
     * On a fresh core (no step yet, else IllegalStateException): takes back a snapshot. The buffer is clamped to the
     * spec's capacity and booked as delivered (or given), so the books stay exact; a trip resumes its cooldown; a
     * machine saved running runs on at once if its first step finds the line in range (no power-up hold: a chunk or
     * world reload doesn't stop it for half a second). A machine saved burnt is burnt again (OVERVOLTAGE_TRIPPED for
     * good), if its spec still burns.
     */
    public void restore(Saved saved) {
        if (ticks > 0) throw new IllegalStateException("restore on a core that has already stepped");
        double b = Double.isFinite(saved.bufferJoules()) ? Math.min(saved.bufferJoules(), capacity) : 0;
        buffer = producer ? b : Math.max(0, b);
        joulesIn += buffer;
        if (thermal != null && Double.isFinite(saved.celsius())) celsius = saved.celsius();
        if (saved.state() != null && saved.state().tripped()
            && (saved.state() != MachineState.OVERHEAT_TRIPPED || thermal != null)) {
            state = saved.state();
            tripTimer = Math.max(0, Math.min(saved.tripTicks(), cooldownTicks));
        } else if (saved.state() == MachineState.RUNNING) state = MachineState.RUNNING;
        lastTrip = saved.lastTrip();
        if (saved.burnt() && spec.burnsOnOvervoltage()) {   // (a spec that no longer burns starts afresh)
            burnt = true;
            state = MachineState.OVERVOLTAGE_TRIPPED;
            tripTimer = 0;
        }
    }

    /**
     * The host repaired a burnt machine, or called its burn off (another failure in the circuit went first): it is
     * no longer burnt and starts again UNPOWERED. lastTrip is kept.
     */
    public void repair() {
        burnt = false;
        state = MachineState.UNPOWERED;
        tripTimer = 0;
        upTimer = 0;
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
            shortSteps = work >= REFILL_KEPT_UP * workCommand * TICK_SECONDS ? 0 : shortSteps + 1;
            keptUpTicks = shortSteps >= 2 ? 0 : Math.min(keptUpTicks + 1, REFILL_HOLDOFF_TICKS);
            if (spec.brownout() == mods.eln.api.Brownout.SLOW) learnLine(a, amps);
            double stored = Math.max(0, Math.min(work, capacity - buffer));
            buffer += stored;
            joulesLost += joules - stored;
            heat += joules - stored;
        }
        if (thermal != null) {
            double k = thermal.heatLossWPerK();
            double eq = ambientCelsius + heat / TICK_SECONDS / k;
            celsius = eq + (celsius - eq) * Math.exp(-k * TICK_SECONDS / thermal.heatCapacityJPerK());
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
            emptyTicks = buffer > 0 ? 0 : Math.min(emptyTicks + 1, SOURCE_HOLD_TICKS);
            sourceOn = !state.tripped() && emptyTicks < SOURCE_HOLD_TICKS;
            return sourceOn ? Math.min(buffer / TICK_SECONDS, DROOP_OPEN_RATIO * spec.ratedWatts()) : 0;
        }
        double idle = 0, work = 0;
        if (!state.tripped() && state != MachineState.UNPOWERED) {
            double r = Math.abs(volts) / uNom;
            double cap = spec.ratedWatts() * r * r;
            idle = Math.min(spec.idleWatts(), cap);
            if (state == MachineState.RUNNING) {
                double asked = Math.min(ratedJoulesPerTick, demand + failedTry);
                double smoothed = Math.max(asked, (asked + previousDemand) / 2);
                double refill = keptUpTicks >= REFILL_HOLDOFF_TICKS ? REFILL_SHARE * ratedJoulesPerTick : 0;
                double joules = smoothed > 0 ? Math.min(capacity - buffer, smoothed + refill) : 0;
                work = Math.max(0, Math.min(joules / TICK_SECONDS, Math.min(workWatts, cap - idle)));
            }
        }
        previousDemand = Math.min(ratedJoulesPerTick, demand + failedTry);
        demand = 0;
        failedTry = 0;
        double p = idle + work;
        lineVolts = Math.abs(volts) + amps * lineOhms;
        if (p > 0 && lineOhms > 0 && p * lineOhms > LINE_MODEL_SAG * lineVolts * lineVolts) {
            // a soft line: no more than its maximum power, the resistance that draws p on its stiff side
            double e = lineVolts, r = lineOhms;
            p = Math.min(p, e * e / (4 * r));
            double ohms = Math.max((e * e - 2 * p * r + e * Math.sqrt(Math.max(0, e * e - 4 * p * r))) / (2 * p),
                uNom * uNom / spec.ratedWatts());
            p = e * e * ohms / ((ohms + r) * (ohms + r));
            idle = Math.min(idle, p);
            work = p - idle;
            loadOhms = ohms;
        } else loadOhms = p > 0 ? volts * volts / p : Double.POSITIVE_INFINITY;
        idleCommand = idle;
        workCommand = work;
        return p;
    }

    /** The line's resistance as last learnt (see LINE_LEARN_SHARE), from a step at u volts drawing i amps. */
    private void learnLine(double u, double i) {
        double di = i - prevAmps;
        if (Double.isFinite(prevVolts) && Math.abs(di) > LINE_LEARN_SHARE * Math.max(i, prevAmps)) {
            double r = (prevVolts - u) / di;
            lineOhms = r > 0 && Double.isFinite(r) ? r : 0;
        }
        prevVolts = u;
        prevAmps = i;
    }

    /**
     * The most the learnt line can deliver, E^2 / 4R, W, while the plan uses it (a Brownout.SLOW consumer on a soft
     * line); infinite otherwise.
     */
    public double lineWattsLimit() {
        boolean soft = spec.brownout() == mods.eln.api.Brownout.SLOW && !producer && lineOhms > 0;
        return soft ? lineVolts * lineVolts / (4 * lineOhms) : Double.POSITIVE_INFINITY;
    }

    /** Consumer: the resistance that draws beforeStep()'s watts at the last line volts, ohm; infinite for none. */
    public double loadOhms() {
        return loadOhms;
    }

    /** Producer: is the source connected for the next step? (With an empty queue, sourceVolts makes it feed nothing.) */
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
        if (!Double.isFinite(e)) e = Double.isFinite(theveninVolts) ? theveninVolts : open; // no current, never 0 V
        sourceVolts = e;
        return e;
    }

    /**
     * Sets the temperature the heat model cools to (the world's, at the machine), degrees C; before the first step
     * the machine starts at it too.
     */
    public void setAmbientCelsius(double c) {
        if (!Double.isFinite(c)) throw new IllegalArgumentException("ambient must be finite, got " + c);
        ambientCelsius = c;
        if (ticks == 0) celsius = c;
    }

    // ---- the owner side (see Machine for the contract)

    public boolean tryTakeJoules(double joules) {
        consumerOnly("tryTakeJoules");
        checkAmount("joules", joules);
        if (joules > capacity)
            throw new IllegalArgumentException("tryTakeJoules(" + joules + ") can never succeed: " + spec
                + " buffers at most " + capacity + " J (two ticks at rated power); split the step or raise ratedWatts");
        // up to the buffer's size is a fair step (no warning); the load is sized for at most a tick's rated work
        if (state != MachineState.RUNNING || buffer < joules * (1 - 1e-9)) {
            failedTry = Math.max(failedTry, Math.min(joules, ratedJoulesPerTick)); // a retry in the same tick isn't more demand
            return false;
        }
        demand = Math.min(ratedJoulesPerTick, demand + Math.min(joules, ratedJoulesPerTick));
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
        return thermal != null ? celsius : ambientCelsius;
    }

    public double ratedJoulesPerTick() {
        return ratedJoulesPerTick;
    }

    @Override
    public double bufferCapacityJoules() {
        return capacity;
    }

    @Override
    public double joulesDelivered() {
        return producer ? joulesOut : joulesIn;
    }

    @Override
    public double joulesTaken() {
        return producer ? 0 : joulesOut;
    }

    @Override
    public double joulesGiven() {
        return producer ? joulesIn : 0;
    }

    /** Consumer: joules delivered by the line; producer: joules given by the owner. */
    public double joulesIn() {
        return joulesIn;
    }

    /** Consumer: joules taken by the owner; producer: joules fed out of the source. */
    public double joulesOut() {
        return joulesOut;
    }

    /**
     * Consumer: delivered joules that never reached the buffer (idle watts, what didn't fit, what a respec cut), J.
     * Producer: given joules a respec cut from the queue.
     */
    @Override
    public double joulesLost() {
        return joulesLost;
    }

    /** Overvoltage burnt it (burnsOnOvervoltage): it never resets; only its host's repair() brings it back. */
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
                // 10 K under the trip point, or halfway down to ambient if that is nearer (a hot place still resets)
                default -> celsius < thermal.overheatCelsius() - Math.min(thermal.overheatResetKelvin(),
                    (thermal.overheatCelsius() - ambientCelsius) / 2);
            };
            tripTimer = clear ? tripTimer + 1 : 0;
            if (tripTimer < cooldownTicks) return;
            tripTimer = 0;
            // the cooldown proved the line: classified at once, no power-up hold
            state = r < UNPOWERED_RATIO ? MachineState.UNPOWERED
                : r < floor ? MachineState.UNDERVOLTAGE : MachineState.RUNNING;
            upTimer = 0;
        }
        if (r > OVERVOLTAGE_RATIO) {
            trip(new TripCause(MachineState.OVERVOLTAGE_TRIPPED, Math.abs(volts), OVERVOLTAGE_RATIO * uNom,
                spec.burnsOnOvervoltage()));
        } else if (thermal != null && celsius > thermal.overheatCelsius()) {
            trip(new TripCause(MachineState.OVERHEAT_TRIPPED, celsius, thermal.overheatCelsius(), false));
        } else if (r < UNPOWERED_RATIO) {
            state = MachineState.UNPOWERED;
            upTimer = 0;
        } else if (state == MachineState.RUNNING) {
            if (r < floor) state = MachineState.UNDERVOLTAGE;
        } else {
            // UNPOWERED or UNDERVOLTAGE: runs once the line has held up for UNDERVOLTAGE_RECOVERY_TICKS
            double needed = state == MachineState.UNPOWERED ? floor : recoveryRatio;
            upTimer = r >= needed ? upTimer + 1 : 0;
            if (r < floor) state = MachineState.UNDERVOLTAGE;
            if (upTimer >= UNDERVOLTAGE_RECOVERY_TICKS) {
                state = MachineState.RUNNING;
                upTimer = 0;
            }
        }
    }

    private void trip(TripCause cause) {
        state = cause.state();
        lastTrip = cause;
        tripTimer = 0;
        upTimer = 0;
        if (cause.burns()) burnt = true;
        for (Consumer<TripCause> l : List.copyOf(listeners)) {
            try {
                l.accept(cause);
            } catch (RuntimeException e) { // one bad listener mustn't skip the others, or the other machines
                LOG.error("Machine {}: a trip listener threw on {}", spec, cause, e);
            }
        }
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
