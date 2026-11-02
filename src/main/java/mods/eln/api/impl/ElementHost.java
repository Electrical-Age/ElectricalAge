package mods.eln.api.impl;

import mods.eln.Eln;
import mods.eln.api.CablePosition;
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
import mods.eln.misc.LRDU;
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
 * A machine on an element (see Machines.forElement), or on the node MachineNode puts at another mod's block entity: a MachineCore and its MnaMachine on a power terminal of its
 * own, plus a gate node per signal, all added to the element's lists. Two processes drive it: the step's plan
 * (beforeStep) in the slow pre-processes, and the step's books (afterStep) first in the slow processes, so the
 * element's own slow processes, the owner's work, run between them, as MachineCore wants.
 * <p>
 * Saved under NBT_PREFIX by the afterStep process (an INBTTReady entry of the element's slow processes): the buffer,
 * temperature (with a heat model), a trip in progress or that it was running, and the last trip. A loaded state is applied at the first
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
    private final MachineBus bus;
    private final AfterStep afterStep = new AfterStep();
    private final IProcess beforeStep = dt -> {
        applySaved();
        planStep();
    };
    private double ambient = Double.NaN;
    private MachineCore.Saved saved;

    /**
     * Builds the machine and adds its parts: loads and components to the element's lists, afterStep first in slow,
     * beforeStep to slowPre. Throws IllegalStateException if slow already holds a machine (one per element). No bus
     * port: bus() throws.
     */
    public ElementHost(MachineSpec spec, ElementSite site, List<? super NbtElectricalLoad> loads,
                       List<? super Component> components, List<IProcess> slow, List<IProcess> slowPre) {
        this(spec, site, null, loads, components, slow, slowPre);
    }

    /** As above, with busPort as the machine's bus (used when the spec has a bus face; null: none). */
    public ElementHost(MachineSpec spec, ElementSite site, mods.eln.bus.BusPort busPort, List<? super NbtElectricalLoad> loads,
                       List<? super Component> components, List<IProcess> slow, List<IProcess> slowPre) {
        this.site = Objects.requireNonNull(site, "site");
        bus = busPort != null && spec.busFace().isPresent() ? new PortBus(busPort) : null;
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
            if (ApiTrace.enabled()) ApiTrace.log("trip machine %s at %s: %s (line %.3f V, %.3f A, resistor %.4g ohm)", core.spec(),
                site.where(), cause, core.volts(), core.amps(), mna.ohms());
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

    /** The machine in slow (an element's slow processes), or null if it has none. */
    public static ElementHost find(List<IProcess> slow) {
        for (IProcess p : slow) if (p instanceof AfterStep a) return a.host();
        return null;
    }

    /** Writes the machine's saved state into nbt under NBT_PREFIX (as the element's NBT has it). */
    public void saveState(CompoundTag nbt) {
        afterStep.writeToNBT(nbt, "");
    }

    /** Reads a state saveState wrote; applied at the first step, as a loaded element's is. Ignored once running. */
    public void loadState(CompoundTag nbt) {
        afterStep.readFromNBT(nbt, "");
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

    /** Follows the element's ambient (it is set as the element joins the simulation), then takes back a loaded state. */
    private void applySaved() {
        double a = site.ambientCelsius();
        if (a != ambient && Double.isFinite(a)) {
            ambient = a;
            core.setAmbientCelsius(a);
        }
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
            MachineState st = state(nbt.getStringOr(k + "trip", ""));
            if (st == null && nbt.getBooleanOr(k + "running", false)) st = MachineState.RUNNING;
            saved = new MachineCore.Saved(nbt.getDoubleOr(k + "bufferJ", 0), nbt.getDoubleOr(k + "celsius", MachineSpec.AMBIENT_CELSIUS),
                st, nbt.getIntOr(k + "tripTicks", 0), last);
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
            if (s.state() == MachineState.RUNNING) nbt.putBoolean(k + "running", true);
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
        return load(side, null);
    }

    @Override
    public ElectricalLoad electricalLoad(Direction side, LRDU lrdu) {
        return load(side, Objects.requireNonNull(lrdu, "lrdu"));
    }

    /** The load at side and lrdu; lrdu null: the whole face's (none for signals at a position). */
    private ElectricalLoad load(Direction side, LRDU lrdu) {
        RelativeFace f = relativeFace(side).orElse(null);
        if (f == null) return null;
        MachineSpec spec = core.spec();
        if (spec.powerFaces().contains(f)) return power;
        for (int i = 0; i < inputs.length; i++) {
            MachineSpec.SignalIn s = spec.signalsIn().get(i);
            if (s.face() == f && meets(s.position(), lrdu)) return inputs[i];
        }
        for (int i = 0; i < outputLoads.length; i++) {
            MachineSpec.SignalOut s = spec.signalsOut().get(i);
            if (s.face() == f && meets(s.position(), lrdu)) return outputLoads[i];
        }
        return null;
    }

    /** A cable at lrdu (null: the face as a whole) is at position. */
    private static boolean meets(CablePosition position, LRDU lrdu) {
        return lrdu == null ? position == CablePosition.ANY : Faces.at(position, lrdu);
    }

    @Override
    public int connectionMask(Direction side) {
        return mask(side, null);
    }

    @Override
    public int connectionMask(Direction side, LRDU lrdu) {
        return mask(side, Objects.requireNonNull(lrdu, "lrdu"));
    }

    private int mask(Direction side, LRDU lrdu) {
        RelativeFace f = relativeFace(side).orElse(null);
        if (f == null) return 0;
        MachineSpec spec = core.spec();
        if (spec.powerFaces().contains(f)) return NodeBase.maskElectricalPower;
        for (MachineSpec.SignalIn s : spec.signalsIn())
            if (s.face() == f && meets(s.position(), lrdu)) return NodeBase.maskElectricalInputGate;
        for (MachineSpec.SignalOut s : spec.signalsOut())
            if (s.face() == f && meets(s.position(), lrdu)) return NodeBase.maskElectricalOutputGate;
        return 0;
    }

    @Override
    public ElectricalLoad powerLoad() {
        return power;
    }

    @Override
    public Map<String, String> wailaLines() {
        Map<String, String> info = new java.util.LinkedHashMap<>();
        info.put(I18N.tr("Voltage"), Utils.plotVolt("", core.volts()));
        boolean producer = core.spec().kind() == MachineSpec.Kind.PRODUCER;
        info.put(I18N.tr(producer ? "Power output" : "Power consumption"), Utils.plotPower("", core.watts()));
        if (core.state() != MachineState.RUNNING)
            info.put(I18N.tr("Machine"), I18N.tr(core.state().name().toLowerCase(Locale.ROOT).replace('_', ' ')));
        return info;
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
        int i = inputIndex(name);
        if (!connected(i)) return core.spec().signalsIn().get(i).defaultValue();
        return Math.max(0, Math.min(1, inputs[i].getU() / Eln.SVU));
    }

    @Override
    public boolean signalConnected(String name) {
        site.checkThread();
        return connected(inputIndex(name));
    }

    /** Is a cable connected to input i at its face and position? */
    private boolean connected(int i) {
        MachineSpec.SignalIn s = core.spec().signalsIn().get(i);
        Direction front = site.front();
        if (front == null) return false;
        Direction side = Faces.resolve(s.face(), front);
        LRDU at = Faces.lrdu(s.position());
        return at == null ? site.connected(side) : site.connected(side, at);
    }

    private int inputIndex(String name) {
        List<MachineSpec.SignalIn> in = core.spec().signalsIn();
        for (int i = 0; i < in.size(); i++) if (in.get(i).name().equals(name)) return i;
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
        if (bus != null) return bus;
        if (core.spec().busFace().isEmpty())
            throw new IllegalStateException(core.spec() + " has no bus face: add .bus(face) to its spec");
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

    @Override
    public double bufferCapacityJoules() {
        return core.bufferCapacityJoules();
    }

    @Override
    public double joulesDelivered() {
        return core.joulesDelivered();
    }

    @Override
    public double joulesTaken() {
        return core.joulesTaken();
    }

    @Override
    public double joulesGiven() {
        return core.joulesGiven();
    }

    @Override
    public double joulesLost() {
        return core.joulesLost();
    }
}
