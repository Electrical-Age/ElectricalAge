package mods.eln.api.impl;

import mods.eln.api.MachineSpec;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.Simulator;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.nbt.NbtElectricalLoad;

import java.util.List;

/**
 * Puts a MachineCore into the circuit at one terminal (an ElectricalLoad the host owns and wires to its cables).
 * Consumer: a resistor from the terminal to ground, set to the core's loadOhms() (high impedance for 0 W). Producer:
 * a voltage source on a private node behind the droop resistor to the terminal; the resistor goes high impedance
 * while the source is off (see MachineCore.sourceOn), so an idle producer is no load on the line, and during the step (a root pre-step
 * process, registered by the source as EA's PowerSource does) the source voltage comes from the core and the circuit's Thevenin equivalent.
 * <p>
 * The host calls applyBeforeStep() before the electrical step (a slow pre-process in game) and readAfterStep()
 * after it (outside the step: trip listeners run there). Needs one electrical step per tick (EA's default): addTo
 * and checkSimulator refuse a simulator that steps otherwise, since the energy books count one step per tick. A host
 * that joins the simulator through lists of its own (an element) adds loads() and components().
 * <p>
 * Every resistance change rebuilds the circuit's matrix, so a consumer's resistor keeps its value while that draws
 * at most OHMS_DEADBAND less than planned (never more, but for 1 ppm of rounding); the books read the resistor as it is, so they stay exact.
 */
public final class MnaMachine implements IRootSystemPreStepProcess {
    /** A consumer's resistor is left alone while it is at most this share above the planned resistance. */
    public static final double OHMS_DEADBAND = 0.01;

    /** The prefix of the saved keys of a producer's parts (its source's volts and current). */
    public static final String NBT_PREFIX = "api.machine.";

    private final MachineCore core;
    private final ElectricalLoad terminal;
    private final Resistor resistor;
    private final VoltageSource source;
    private final NbtElectricalLoad emf;

    public MnaMachine(MachineCore core, ElectricalLoad terminal) {
        this.core = core;
        this.terminal = terminal;
        if (core.spec().kind() == MachineSpec.Kind.PRODUCER) {
            emf = new NbtElectricalLoad(NBT_PREFIX + "emf");
            source = new Source(NBT_PREFIX + "emf", emf);
            resistor = new Resistor(emf, terminal);
        } else {
            emf = null;
            source = null;
            resistor = new Resistor(terminal, null);
        }
        resistor.highImpedance();
    }

    public MachineCore core() {
        return core;
    }

    /** The machine's own nodes (not the terminal): a producer's source node, else none. */
    public List<NbtElectricalLoad> loads() {
        return emf == null ? List.of() : List.of(emf);
    }

    /** The machine's components: the resistor, and a producer's source. */
    public List<Component> components() {
        return source == null ? List.of(resistor) : List.of(source, resistor);
    }

    /** Throws IllegalStateException unless sim steps once per 50 ms tick. */
    public static void checkSimulator(Simulator sim) {
        double tick = MachineCore.TICK_SECONDS;
        if (Math.abs(sim.callPeriod - tick) > 1e-9 || Math.abs(sim.electricalPeriod - tick) > 1e-9)
            throw new IllegalStateException("machines need one electrical step per " + tick + " s tick; this simulator is called every "
                + sim.callPeriod + " s and steps every " + sim.electricalPeriod + " s");
    }

    /** Adds the machine's parts (not the terminal) to the simulator; throws unless it steps once per 50 ms tick. */
    public void addTo(Simulator sim) {
        checkSimulator(sim);
        for (State s : loads()) sim.addElectricalLoad(s);
        for (Component c : components()) sim.addElectricalComponent(c);
    }

    /** Removes what addTo added. */
    public void removeFrom(Simulator sim) {
        for (Component c : components()) sim.removeElectricalComponent(c);
        for (State s : loads()) sim.removeElectricalLoad(s);
    }

    /** A producer's source: in the circuit, it runs the machine's pre-step (as EA's PowerSource registers itself). */
    private final class Source extends VoltageSource {
        Source(String name, State pin) {
            super(name, pin, null);
        }

        @Override
        public void addedTo(SubSystem s) {
            super.addedTo(s);
            s.getRoot().addProcess(MnaMachine.this);
        }

        @Override
        public void quitSubSystem() {
            getSubSystem().getRoot().removeProcess(MnaMachine.this);
            super.quitSubSystem();
        }
    }

    /** Before the step: the core plans it, and the resistor follows (the only circuit change, made outside the step). */
    public void applyBeforeStep() {
        core.beforeStep();
        if (source == null) {
            double r = core.loadOhms(), now = resistor.getR();
            if (!Double.isFinite(r)) resistor.highImpedance();
            else if (!(now >= r * (1 - 1e-6) && now <= r * (1 + OHMS_DEADBAND))) resistor.setR(r); // (1 ppm: rounding)
        } else if (core.sourceOn()) {
            resistor.setR(core.sourceOhms());
        } else {
            resistor.highImpedance();
            source.setU(terminal.getU()); // no leak through the open resistor
        }
    }

    /** The machine's resistor now (the load, or the droop resistance), ohm; MnaConst.highImpedance when open. */
    public double ohms() {
        return resistor.getR();
    }

    /** Producer, during the step: the source voltage for the circuit as it is now. */
    @Override
    public void rootSystemPreStepProcess() {
        if (!core.sourceOn()) return;
        SubSystem s = emf.getSubSystem();
        if (s == null) return;
        SubSystem.Th th = s.getTh(emf, source); // (leaves the source at the node's voltage: set it right after)
        source.setU(core.sourceVolts(th.U, th.R));
    }

    /**
     * The circuit's resistance at the terminal without the machine's own resistor, ohm, as of the step just run: from
     * the driving-point resistance z (which includes it, conductance g) as z / (1 - z g). Exact, from the solver's
     * matrix, so the core never has to guess its line (Baughn 2026-11-04); NaN when the matrix isn't at hand.
     */
    double theveninOhms() {
        SubSystem s = terminal.getSubSystem();
        double z = s == null ? Double.NaN : s.drivingPointOhms(terminal), g = resistor.getRInv();
        if (!(z > 0) || !Double.isFinite(z)) return Double.NaN;
        double d = 1 - z * g;
        return d > 1e-9 ? z / d : Double.POSITIVE_INFINITY;
    }

    /**
     * After the step: hands the core the terminal's volts and the energy moved. Producer: source volts x current
     * out x dt, 0 if the current ran backwards or the source was off (leakage through the open resistor is ignored).
     */
    public void readAfterStep() {
        double u = terminal.getU(), dt = MachineCore.TICK_SECONDS;
        if (source == null) {
            if (core.spec().brownout() == mods.eln.api.Brownout.SLOW) core.lineSeen(theveninOhms());
            core.afterStep(u, u * u * resistor.getRInv() * dt);
        } else {
            double joules = core.sourceOn() ? source.getU() * Math.max(0, resistor.getCurrent()) * dt : 0;
            core.afterStep(u, joules);
        }
    }
}
