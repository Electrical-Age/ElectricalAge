package mods.eln.api.impl;

import mods.eln.Eln;
import mods.eln.api.ElementMachine;
import mods.eln.api.MachineBus;
import mods.eln.api.MachineSpec;
import mods.eln.api.MachineState;
import mods.eln.api.MachineView;
import mods.eln.api.RelativeFace;
import mods.eln.api.TripCause;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A machine on an element (see Machines.forElement): a MachineCore and its MnaMachine on a power terminal of its
 * own, plus a gate node per signal, all added to the element's lists. Two processes drive it: the step's plan
 * (beforeStep) in the slow pre-processes, and the step's books (afterStep) first in the slow processes, so the
 * element's own slow processes, the owner's work, run between them, as MachineCore wants.
 * <p>
 * Saved under NBT_PREFIX by the afterStep process (an INBTTReady entry of the element's slow processes): the buffer,
 * temperature (with a heat model), a trip in progress and the last trip. A loaded state is applied at the first
 * tick, after the element has set its ratings (respec) from its own loaded state.
 * <p>
 * A burn (burnsOnOvervoltage) goes through the element's failure path (ElementSite.burn). When the element is back in
 * the simulation unburnt (repaired, or the failure was called off for a worse one nearby) the core is repaired.
 */
public final class ElementHost implements ElementMachine {
    public static final String NBT_PREFIX = MnaMachine.NBT_PREFIX;

    private final MachineCore core;
    private final MnaMachine mna;
    private final ElementSite site;
    private final NbtElectricalLoad power;
    private final NbtElectricalLoad[] inputs, outputLoads;
    private final NbtElectricalGateOutputProcess[] outputs;
    private final MachineView view;
    private final AfterStep afterStep = new AfterStep();
    private final IProcess beforeStep = dt -> {
        applySaved();
        planStep();
    };
    private MachineCore.Saved saved;

    /**
     * Builds the machine and adds its parts: loads and components to the element's lists, afterStep first in slow,
     * beforeStep to slowPre. Throws IllegalStateException if slow already holds a machine (one per element).
     */
    public ElementHost(MachineSpec spec, ElementSite site, List<? super NbtElectricalLoad> loads,
                       List<? super Component> components, List<IProcess> slow, List<IProcess> slowPre) {
        this.site = Objects.requireNonNull(site, "site");
        for (IProcess p : slow)
            if (p instanceof AfterStep) throw new IllegalStateException("one machine per element: this one already has " + ((AfterStep) p).host().spec());
        core = new MachineCore(Objects.requireNonNull(spec, "spec"));
        view = new ReadOnlyView(core);
        power = new NbtElectricalLoad(NBT_PREFIX + "power");
        power.setRs(site.powerRs());
        mna = new MnaMachine(core, power);
        inputs = new NbtElectricalLoad[spec.signalsIn().size()];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = new NbtElectricalLoad(NBT_PREFIX + "in" + i);
            inputs[i].setRs(site.signalRs());
        }
        outputLoads = new NbtElectricalLoad[spec.signalsOut().size()];
        outputs = new NbtElectricalGateOutputProcess[outputLoads.length];
        for (int i = 0; i < outputs.length; i++) {
            outputLoads[i] = new NbtElectricalLoad(NBT_PREFIX + "out" + i);
            outputLoads[i].setRs(site.signalRs());
            outputs[i] = new NbtElectricalGateOutputProcess(NBT_PREFIX + "drive" + i, outputLoads[i]);
            outputs[i].setHighImpedance(true); // outputs start floating
        }
        core.onTrip(cause -> {
            // severity as EA's voltage watchdogs rank it: the overflow per budget of a quarter of nominal volts
            if (cause.burns()) site.burn(cause, (cause.measured() - cause.limit()) / (0.25 * core.spec().nominalVolts()));
        });

        loads.add(power);
        for (NbtElectricalLoad l : inputs) loads.add(l);
        for (NbtElectricalLoad l : outputLoads) loads.add(l);
        loads.addAll(mna.loads());
        components.addAll(mna.components());
        for (NbtElectricalGateOutputProcess o : outputs) components.add(o);
        slow.add(0, afterStep);
        slowPre.add(beforeStep);
    }

    /** The core, for tests and the host's own views. */
    public MachineCore core() {
        return core;
    }

    /** The process that plans each step (in the element's slow pre-processes). */
    public IProcess beforeStepProcess() {
        return beforeStep;
    }

    /** The process that books each step (first of the element's slow processes; it also saves the machine). */
    public IProcess afterStepProcess() {
        return afterStep;
    }

    private void applySaved() {
        if (saved == null) return;
        if (core.ticks() == 0) core.restore(saved);
        saved = null;
    }

    private void planStep() {
        mna.applyBeforeStep();
    }

    /** The afterStep process; also the machine's NBT. */
    private final class AfterStep implements IProcess, INBTTReady {
        ElementHost host() {
            return ElementHost.this;
        }

        @Override
        public void process(double time) {
            applySaved();
            if (core.burnt() && !site.burnt()) core.repair(); // repaired, or the burn was called off
            mna.readAfterStep();
        }

        @Override
        public void readFromNBT(CompoundTag nbt, String str) {
            String k = str + NBT_PREFIX;
            if (!nbt.contains(k + "bufferJ") || core.ticks() > 0) return;
            TripCause last = null;
            MachineState lastState = state(nbt.getStringOr(k + "lastTrip", ""));
            if (lastState != null && lastState.tripped())
                last = new TripCause(lastState, nbt.getDoubleOr(k + "lastTripMeasured", 0), nbt.getDoubleOr(k + "lastTripLimit", 0),
                    nbt.getBooleanOr(k + "lastTripBurns", false));
            saved = new MachineCore.Saved(nbt.getDoubleOr(k + "bufferJ", 0), nbt.getDoubleOr(k + "celsius", MachineSpec.AMBIENT_CELSIUS),
                state(nbt.getStringOr(k + "trip", "")), nbt.getIntOr(k + "tripTicks", 0), last);
        }

        @Override
        public void writeToNBT(CompoundTag nbt, String str) {
            String k = str + NBT_PREFIX;
            MachineCore.Saved s = saved != null ? saved : core.snapshot();
            nbt.putDouble(k + "bufferJ", s.bufferJoules());
            if (core.spec().thermal().isPresent()) nbt.putDouble(k + "celsius", s.celsius());
            if (s.state() != null && s.state().tripped()) {
                nbt.putString(k + "trip", s.state().name());
                nbt.putInt(k + "tripTicks", s.tripTicks());
            }
            TripCause t = s.lastTrip();
            if (t != null) {
                nbt.putString(k + "lastTrip", t.state().name());
                nbt.putDouble(k + "lastTripMeasured", t.measured());
                nbt.putDouble(k + "lastTripLimit", t.limit());
                nbt.putBoolean(k + "lastTripBurns", t.burns());
            }
        }
    }

    private static MachineState state(String name) {
        for (MachineState s : MachineState.values()) if (s.name().equals(name)) return s;
        return null;
    }

    // ---- faces

    @Override
    public Optional<RelativeFace> relativeFace(Direction side) {
        Direction front = site.front();
        if (front == null || side == null) return Optional.empty();
        return Optional.of(Faces.relative(side, front));
    }

    @Override
    public ElectricalLoad electricalLoad(Direction side) {
        RelativeFace f = relativeFace(side).orElse(null);
        if (f == null) return null;
        MachineSpec spec = core.spec();
        if (spec.powerFaces().contains(f)) return power;
        for (int i = 0; i < inputs.length; i++) if (spec.signalsIn().get(i).face() == f) return inputs[i];
        for (int i = 0; i < outputLoads.length; i++) if (spec.signalsOut().get(i).face() == f) return outputLoads[i];
        return null;
    }

    @Override
    public int connectionMask(Direction side) {
        RelativeFace f = relativeFace(side).orElse(null);
        if (f == null) return 0;
        MachineSpec spec = core.spec();
        if (spec.powerFaces().contains(f)) return NodeBase.maskElectricalPower;
        for (MachineSpec.SignalIn s : spec.signalsIn()) if (s.face() == f) return NodeBase.maskElectricalInputGate;
        for (MachineSpec.SignalOut s : spec.signalsOut()) if (s.face() == f) return NodeBase.maskElectricalOutputGate;
        return 0;
    }

    @Override
    public ElectricalLoad powerLoad() {
        return power;
    }

    @Override
    public void addWaila(Map<String, String> info) {
        info.put(I18N.tr("Voltage"), Utils.plotVolt("", core.volts()));
        boolean producer = core.spec().kind() == MachineSpec.Kind.PRODUCER;
        info.put(I18N.tr(producer ? "Power output" : "Power consumption"), Utils.plotPower("", core.watts()));
        if (core.state() != MachineState.RUNNING)
            info.put(I18N.tr("State"), I18N.tr(core.state().name().toLowerCase(Locale.ROOT).replace('_', ' ')));
    }

    // ---- Machine

    @Override
    public boolean tryTakeJoules(double joules) {
        site.checkThread();
        return core.tryTakeJoules(joules);
    }

    @Override
    public double takeJoules(double joules) {
        site.checkThread();
        return core.takeJoules(joules);
    }

    @Override
    public double giveJoules(double joules) {
        site.checkThread();
        return core.giveJoules(joules);
    }

    @Override
    public double ratedJoulesPerTick() {
        return core.ratedJoulesPerTick();
    }

    @Override
    public double signal(String name) {
        site.checkThread();
        List<MachineSpec.SignalIn> in = core.spec().signalsIn();
        for (int i = 0; i < in.size(); i++) {
            if (!in.get(i).name().equals(name)) continue;
            Direction front = site.front();
            if (front == null || !site.connected(Faces.resolve(in.get(i).face(), front))) return in.get(i).defaultValue();
            return Math.max(0, Math.min(1, inputs[i].getU() / Eln.SVU));
        }
        throw new IllegalArgumentException(core.spec() + " has no signal input '" + name + "'"
            + (outputIndex(name) >= 0 ? " (it is an output)" : ""));
    }

    @Override
    public void setSignal(String name, double level) {
        site.checkThread();
        NbtElectricalGateOutputProcess o = output(name);
        if (!(level >= 0 && level <= 1))
            throw new IllegalArgumentException("a signal level is 0..1 (not volts), got " + level);
        if (o.isHighImpedance()) o.setHighImpedance(false);
        o.setOutputNormalized(level);
    }

    @Override
    public void releaseSignal(String name) {
        site.checkThread();
        NbtElectricalGateOutputProcess o = output(name);
        if (!o.isHighImpedance()) o.setHighImpedance(true);
    }

    private int outputIndex(String name) {
        List<MachineSpec.SignalOut> out = core.spec().signalsOut();
        for (int i = 0; i < out.size(); i++) if (out.get(i).name().equals(name)) return i;
        return -1;
    }

    private NbtElectricalGateOutputProcess output(String name) {
        int i = outputIndex(name);
        if (i < 0) throw new IllegalArgumentException(core.spec() + " has no signal output '" + name + "'"
            + (core.spec().signalsIn().stream().anyMatch(s -> s.name().equals(name)) ? " (it is an input)" : ""));
        return outputs[i];
    }

    /** Signal output name's level as driven, or empty while it floats (for tests and tools). */
    public java.util.OptionalDouble outputLevel(String name) {
        NbtElectricalGateOutputProcess o = output(name);
        return o.isHighImpedance() ? java.util.OptionalDouble.empty() : java.util.OptionalDouble.of(o.getOutputNormalized());
    }

    @Override
    public MachineBus bus() {
        site.checkThread();
        throw new IllegalStateException(core.spec() + " has no bus port here: an element machine has none yet");
    }

    @Override
    public void heatJoules(double joules) {
        site.checkThread();
        core.heatJoules(joules);
    }

    @Override
    public void onTrip(Consumer<TripCause> listener) {
        site.checkThread();
        core.onTrip(listener);
    }

    @Override
    public void respec(MachineSpec spec) {
        site.checkThread();
        core.respec(spec);
    }

    @Override
    public MachineView view() {
        return view;
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
}
