package mods.eln.api.testkit;

import mods.eln.api.Machine;
import mods.eln.api.MachineBus;
import mods.eln.api.MachineSpec;
import mods.eln.api.MachineState;
import mods.eln.api.MachineView;
import mods.eln.api.TripCause;
import mods.eln.api.impl.MachineCore;
import mods.eln.api.impl.MnaMachine;
import mods.eln.api.impl.ReadOnlyView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

/**
 * A machine on a Bench: the Machine an owner's code gets in game, plus the test's side of it (owner code to run
 * every tick, signal inputs to drive, outputs to read). Calls from a thread other than the bench's throw, as in game.
 */
public final class BenchMachine implements Machine {
    private final Bench bench;
    private final MachineCore core;
    final MnaMachine mna;
    private Consumer<Machine> owner = m -> {
    };
    private final Map<String, Double> inputs = new HashMap<>(), outputs = new HashMap<>();
    private final MachineBus bus;

    BenchMachine(Bench bench, MachineCore core, MnaMachine mna) {
        this.bench = bench;
        this.core = core;
        this.mna = mna;
        bus = new MachineBus() {
            @Override
            public double read(int channel) {
                bench.checkThread();
                return bench.busRead(channel);
            }

            @Override
            public void write(int channel, double level) {
                bench.checkThread();
                bench.busWrite(channel, level);
            }
        };
    }

    /** The owner's code, run every tick after the electrical step (as a block entity's tick). Replaces the last. */
    public BenchMachine owner(Consumer<Machine> owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
        return this;
    }

    void runOwner() {
        owner.accept(this);
    }

    /** Drives signal input name at level 0..1 (what a cable would bring). */
    public BenchMachine input(String name, double level) {
        inputSpec(name);
        checkLevel(level);
        inputs.put(name, level);
        return this;
    }

    /** Stops driving signal input name: it reads its default again. */
    public BenchMachine releaseInput(String name) {
        inputSpec(name);
        inputs.remove(name);
        return this;
    }

    /** Signal output name's level, or empty while it floats (high impedance). */
    public OptionalDouble output(String name) {
        outputSpec(name);
        Double v = outputs.get(name);
        return v == null ? OptionalDouble.empty() : OptionalDouble.of(v);
    }

    // ---- Machine

    @Override
    public boolean tryTakeJoules(double joules) {
        bench.checkThread();
        return core.tryTakeJoules(joules);
    }

    @Override
    public double takeJoules(double joules) {
        bench.checkThread();
        return core.takeJoules(joules);
    }

    @Override
    public double giveJoules(double joules) {
        bench.checkThread();
        return core.giveJoules(joules);
    }

    @Override
    public double ratedJoulesPerTick() {
        return core.ratedJoulesPerTick();
    }

    @Override
    public double signal(String name) {
        bench.checkThread();
        MachineSpec.SignalIn s = inputSpec(name);
        return inputs.getOrDefault(name, s.defaultValue());
    }

    /** On the bench an input is connected while the bench drives it (input(name, level)). */
    @Override
    public boolean signalConnected(String name) {
        bench.checkThread();
        inputSpec(name);
        return inputs.containsKey(name);
    }

    @Override
    public void setSignal(String name, double level) {
        bench.checkThread();
        outputSpec(name);
        checkLevel(level);
        outputs.put(name, level);
    }

    @Override
    public void releaseSignal(String name) {
        bench.checkThread();
        outputSpec(name);
        outputs.remove(name);
    }

    @Override
    public MachineBus bus() {
        bench.checkThread();
        if (core.spec().busFace().isEmpty())
            throw new IllegalStateException(core.spec() + " has no bus face: add .bus(face) to its spec");
        return bus;
    }

    @Override
    public void heatJoules(double joules) {
        bench.checkThread();
        core.heatJoules(joules);
    }

    @Override
    public void onTrip(Consumer<TripCause> listener) {
        bench.checkThread();
        core.onTrip(listener);
    }

    @Override
    public void respec(MachineSpec spec) {
        bench.checkThread();
        core.respec(spec);
    }

    @Override
    public MachineView view() {
        return new ReadOnlyView(core);
    }

    // ---- MachineView

    @Override
    public MachineSpec spec() {
        return core.spec();
    }

    @Override
    public MachineState state() {
        return core.state();
    }

    @Override
    public Optional<TripCause> lastTrip() {
        return core.lastTrip();
    }

    @Override
    public double volts() {
        return core.volts();
    }

    @Override
    public double voltageRatio() {
        return core.voltageRatio();
    }

    @Override
    public double amps() {
        return core.amps();
    }

    @Override
    public double watts() {
        return core.watts();
    }

    @Override
    public double bufferJoules() {
        return core.bufferJoules();
    }

    @Override
    public double celsius() {
        return core.celsius();
    }

    /** Consumer: joules the line delivered so far; producer: joules the owner gave so far. */
    public double joulesIn() {
        return core.joulesIn();
    }

    /** Consumer: joules the owner took so far; producer: joules its source fed so far (droop losses included). */
    public double joulesOut() {
        return core.joulesOut();
    }

    private MachineSpec.SignalIn inputSpec(String name) {
        for (MachineSpec.SignalIn s : core.spec().signalsIn()) if (s.name().equals(name)) return s;
        throw new IllegalArgumentException(core.spec() + " has no signal input '" + name + "'"
            + (core.spec().signalsOut().stream().anyMatch(s -> s.name().equals(name)) ? " (it is an output)" : ""));
    }

    private void outputSpec(String name) {
        if (core.spec().signalsOut().stream().noneMatch(s -> s.name().equals(name)))
            throw new IllegalArgumentException(core.spec() + " has no signal output '" + name + "'"
                + (core.spec().signalsIn().stream().anyMatch(s -> s.name().equals(name)) ? " (it is an input)" : ""));
    }

    static void checkLevel(double level) {
        if (!(level >= 0 && level <= 1))
            throw new IllegalArgumentException("a signal level is 0..1 (not volts), got " + level);
    }
}
