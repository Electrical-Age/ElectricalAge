package mods.eln.api.impl.lowlevel;

import mods.eln.Eln;
import mods.eln.api.impl.ApiTrace;
import mods.eln.api.impl.ServerThread;
import mods.eln.api.lowlevel.Branch;
import mods.eln.api.lowlevel.Circuit;
import mods.eln.api.lowlevel.FaceConnection;
import mods.eln.api.lowlevel.HostedNode;
import mods.eln.api.lowlevel.Load;
import mods.eln.api.lowlevel.PortLink;
import mods.eln.api.lowlevel.PortRef;
import mods.eln.api.lowlevel.ResistorLoad;
import mods.eln.api.lowlevel.SignalLoad;
import mods.eln.api.lowlevel.SignalSource;
import mods.eln.api.lowlevel.VoltageSource;
import mods.eln.api.lowlevel.Watchdog;
import mods.eln.api.lowlevel.WatchdogTrip;
import mods.eln.api.lowlevel.Watchdogs;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.Simulator;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

/**
 * Circuit.create: the parts, and their joining and leaving a simulator together (attach, detach). Two processes run
 * while attached: before the step (a slow pre-process) the port links resolve; after it (a slow process) the
 * watchdogs check their limits. A part's state is an EA ElectricalLoad whose series resistance (what an
 * ElectricalConnection to it adds) comes from the site at attach: a cable's node for power, a signal cable's for
 * signals.
 */
public final class CircuitImpl implements Circuit {
    private static final Logger LOG = LoggerFactory.getLogger("eln");
    /** One step, s. */
    static final double STEP_SECONDS = 0.05;

    /** Where a circuit runs: a game node at a block entity, or a test bench. */
    public interface Site {
        Simulator simulator();

        /** Throws IllegalStateException off the site's thread. */
        void checkThread();

        /** Series resistance of a power load, ohm (a cable node's). */
        double powerRs();

        /** Series resistance of a signal load, ohm. */
        double signalRs();

        /** The level port links resolve in, or null (a bench). */
        ServerLevel level();

        /** Where it is, for messages. */
        String where();
    }

    private final String name;
    private final List<LoadImpl> loads = new ArrayList<>();
    private final List<ElectricalLoad> privateStates = new ArrayList<>();
    private final List<Component> components = new ArrayList<>();
    private final List<PortLinkImpl> links = new ArrayList<>();
    private final List<WatchdogImpl> watchdogs = new ArrayList<>();
    private final IProcess pre = dt -> beforeStep(), post = dt -> afterStep();
    private final Watchdogs watchdogFactory = new WatchdogFactory();
    private Site site;
    private boolean hosting, retired;

    public CircuitImpl(String name) {
        this.name = requireName(name);
    }

    static String requireName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("a circuit part needs a name (a label for messages)");
        return name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "circuit '" + name + "'";
    }

    /** The site's thread rule; before attach the server's (no server: a unit test). */
    void check() {
        if (site != null) site.checkThread();
        else ServerThread.check();
    }

    private void checkOpen() {
        if (retired) throw new IllegalStateException(this + " has left the world (its block entity went); build a new circuit");
    }

    // ---- parts

    @Override
    public Load load(String name) {
        return addLoad(new LoadImpl(this, requireName(name), false));
    }

    @Override
    public SignalLoad signalLoad(String name) {
        return (SignalLoad) addLoad(new SignalLoadImpl(this, requireName(name)));
    }

    @Override
    public SignalSource signalSource(String name) {
        SignalSourceImpl s = new SignalSourceImpl(this, requireName(name));
        addLoad(s);
        addComponent(s.gate);
        return s;
    }

    private LoadImpl addLoad(LoadImpl l) {
        check();
        checkOpen();
        loads.add(l);
        if (site != null) {
            l.state.setRs(l.signal ? site.signalRs() : site.powerRs());
            site.simulator().addElectricalLoad(l.state);
        }
        return l;
    }

    private void addComponent(Component c) {
        components.add(c);
        if (site != null) site.simulator().addElectricalComponent(c);
    }

    private void addPrivateState(ElectricalLoad s) {
        privateStates.add(s);
        if (site != null) site.simulator().addElectricalLoad(s);
    }

    /** load's EA load, if load is a part of this circuit, else IllegalArgumentException (hosts and the bench wire it). */
    public ElectricalLoad stateOf(Load load) {
        return own(load).state;
    }

    /** load as a part of this circuit, else IllegalArgumentException. */
    LoadImpl own(Load load) {
        Objects.requireNonNull(load, "load");
        if (!(load instanceof LoadImpl l) || l.circuit != this)
            throw new IllegalArgumentException(load.name() + " is not a part of " + this + "; parts of one circuit can't be wired into another");
        return l;
    }

    @Override
    public ResistorLoad resistor(String name, Load a, Load b, double ohms) {
        check();
        checkOpen();
        ResistorImpl r = new ResistorImpl(requireName(name), own(a), own(Objects.requireNonNull(b, "b (use resistorToGround for ground)")));
        r.setOhms(ohms);
        addComponent(r.resistor);
        return r;
    }

    @Override
    public ResistorLoad resistorToGround(String name, Load a, double ohms) {
        check();
        checkOpen();
        ResistorImpl r = new ResistorImpl(requireName(name), own(a), null);
        r.setOhms(ohms);
        addComponent(r.resistor);
        return r;
    }

    @Override
    public VoltageSource voltageSource(String name, Load out, double volts, double ohms) {
        check();
        checkOpen();
        SourceImpl s = new SourceImpl(requireName(name), own(out));
        s.setVolts(volts);
        s.setOhms(ohms);
        addPrivateState(s.emf);
        addComponent(s.source);
        addComponent(s.resistor);
        return s;
    }

    @Override
    public Watchdogs watchdogs() {
        return watchdogFactory;
    }

    @Override
    public PortLink connect(Load load, PortRef port) {
        check();
        checkOpen();
        PortLinkImpl l = new PortLinkImpl(this, own(load), Objects.requireNonNull(port, "port"));
        links.add(l);
        return l;
    }

    @Override
    public HostedNode hostAt(BlockEntity be, FaceConnection... faces) {
        check();
        checkOpen();
        Objects.requireNonNull(be, "be");
        Objects.requireNonNull(faces, "faces");
        if (hosting || site != null) throw new IllegalStateException(this + " is already hosted (or on a bench)");
        for (FaceConnection f : faces) own(Objects.requireNonNull(f, "face connection").load());
        CircuitNode n = CircuitHosts.INSTANCE.host(be, this, faces);
        hosting = true;
        return n;
    }

    @Override
    public boolean attached() {
        return site != null;
    }

    // ---- joining a simulator

    /** Joins site's simulator: every part, and the two processes. Throws if attached already or retired. */
    public void attach(Site s) {
        Objects.requireNonNull(s, "site");
        checkOpen();
        if (site != null) throw new IllegalStateException(this + " is already attached at " + site.where());
        site = s;
        Simulator sim = s.simulator();
        for (LoadImpl l : loads) {
            l.state.setRs(l.signal ? s.signalRs() : s.powerRs());
            sim.addElectricalLoad(l.state);
        }
        for (ElectricalLoad p : privateStates) sim.addElectricalLoad(p);
        for (Component c : components) sim.addElectricalComponent(c);
        sim.addSlowPreProcess(pre);
        sim.addSlowProcess(post);
        if (ApiTrace.enabled()) ApiTrace.log("attach %s at %s: %s", this, s.where(), describe());
    }

    /** Leaves the simulator (links dropped, every part out); a no-op when not attached. */
    public void detach() {
        Site s = site;
        if (s == null) return;
        if (ApiTrace.enabled()) ApiTrace.log("detach %s at %s: %s", this, s.where(), describe());
        Simulator sim = s.simulator();
        for (PortLinkImpl l : links) l.drop(sim);
        sim.removeSlowPreProcess(pre);
        sim.removeSlowProcess(post);
        for (Component c : components) sim.removeElectricalComponent(c);
        for (ElectricalLoad p : privateStates) sim.removeElectricalLoad(p);
        for (LoadImpl l : loads) sim.removeElectricalLoad(l.state);
        site = null;
    }

    /** Leaves for good (the block entity went): links read REMOVED and parts can't be added. */
    public void retire() {
        detach();
        retired = true;
        hosting = false;
        for (PortLinkImpl l : links) l.removed();
    }

    /** The world stopped (its simulator is cleared with it): retired without touching the simulator. */
    public void abandon() {
        site = null;
        retired = true;
        hosting = false;
        for (PortLinkImpl l : links) l.removed();
    }

    Site site() {
        return site;
    }

    /** Each load with its volts and series resistance, for the trace. */
    String describe() {
        StringBuilder b = new StringBuilder();
        for (LoadImpl l : loads) {
            if (b.length() > 0) b.append(", ");
            b.append(String.format(java.util.Locale.ROOT, "%s %.3f V (Rs %.4g ohm)", l.name, l.state.getU(), l.state.getRs()));
        }
        return b.length() == 0 ? "no loads" : b.toString();
    }

    private void beforeStep() {
        Site s = site;
        if (s == null) return;
        for (PortLinkImpl l : List.copyOf(links)) l.resolve(s);
    }

    private void afterStep() {
        for (WatchdogImpl w : List.copyOf(watchdogs)) w.checkLimit();
    }

    void forget(PortLinkImpl l) {
        links.remove(l);
    }

    // ---- the parts

    /** A load: an ElectricalLoad of its own. */
    static class LoadImpl implements Load {
        final CircuitImpl circuit;
        final String name;
        final boolean signal;
        final ElectricalLoad state = new ElectricalLoad();

        LoadImpl(CircuitImpl circuit, String name, boolean signal) {
            this.circuit = circuit;
            this.name = name;
            this.signal = signal;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public double volts() {
            return state.getU();
        }

        /** The EA load (hosts and the bench wire it). */
        public ElectricalLoad state() {
            return state;
        }

        @Override
        public String toString() {
            return (signal ? "signal load '" : "load '") + name + "'";
        }
    }

    static class SignalLoadImpl extends LoadImpl implements SignalLoad {
        SignalLoadImpl(CircuitImpl circuit, String name) {
            super(circuit, name, true);
        }

        @Override
        public double level() {
            return Math.max(0, Math.min(1, state.getU() / Eln.SVU));
        }
    }

    static final class SignalSourceImpl extends SignalLoadImpl implements SignalSource {
        final NbtElectricalGateOutputProcess gate;

        SignalSourceImpl(CircuitImpl circuit, String name) {
            super(circuit, name);
            gate = new NbtElectricalGateOutputProcess(name, state);
            gate.setHighImpedance(true); // floats until first set
        }

        @Override
        public void setLevel(double level) {
            circuit.check();
            if (!(level >= 0 && level <= 1))
                throw new IllegalArgumentException("a signal level is 0..1 (not volts), got " + level);
            if (gate.isHighImpedance()) gate.setHighImpedance(false);
            gate.setOutputNormalized(level);
        }

        @Override
        public void release() {
            circuit.check();
            if (!gate.isHighImpedance()) gate.setHighImpedance(true);
        }

        @Override
        public OptionalDouble driven() {
            return gate.isHighImpedance() ? OptionalDouble.empty() : OptionalDouble.of(gate.getOutputNormalized());
        }
    }

    final class ResistorImpl implements ResistorLoad {
        CircuitImpl owner() {
            return CircuitImpl.this;
        }

        final String name;
        final LoadImpl a, b;
        final Resistor resistor;

        ResistorImpl(String name, LoadImpl a, LoadImpl b) {
            this.name = name;
            this.a = a;
            this.b = b;
            resistor = new Resistor(a.state, b == null ? null : b.state);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Load a() {
            return a;
        }

        @Override
        public Optional<Load> b() {
            return Optional.ofNullable(b);
        }

        @Override
        public double ohms() {
            return resistor.getR() >= MnaConst.highImpedance ? Double.POSITIVE_INFINITY : resistor.getR();
        }

        @Override
        public void setOhms(double ohms) {
            check();
            if (!(ohms > 0)) throw new IllegalArgumentException(name + ": ohms must be positive (infinite: open), got " + ohms);
            if (Double.isInfinite(ohms)) resistor.highImpedance();
            else resistor.setR(ohms);
        }

        @Override
        public double volts() {
            return resistor.getU();
        }

        @Override
        public double amps() {
            return resistor.getCurrent();
        }

        @Override
        public double watts() {
            return resistor.getP();
        }

        @Override
        public String toString() {
            return "resistor '" + name + "'";
        }
    }

    final class SourceImpl implements VoltageSource {
        CircuitImpl owner() {
            return CircuitImpl.this;
        }

        final String name;
        final LoadImpl out;
        final ElectricalLoad emf = new ElectricalLoad();
        final mods.eln.sim.mna.component.VoltageSource source;
        final Resistor resistor;
        double volts, ohms;

        SourceImpl(String name, LoadImpl out) {
            this.name = name;
            this.out = out;
            source = new mods.eln.sim.mna.component.VoltageSource(name, emf, null);
            resistor = new Resistor(emf, out.state);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Load out() {
            return out;
        }

        @Override
        public double volts() {
            return volts;
        }

        @Override
        public void setVolts(double v) {
            check();
            if (!Double.isFinite(v)) throw new IllegalArgumentException(name + ": volts must be finite, got " + v);
            volts = v;
            source.setU(v);
        }

        @Override
        public double ohms() {
            return ohms;
        }

        @Override
        public void setOhms(double r) {
            check();
            if (!(Double.isFinite(r) && r > 0))
                throw new IllegalArgumentException(name + ": a source's internal ohms must be positive and finite (never an ideal source), got " + r);
            ohms = r;
            resistor.setR(r);
        }

        @Override
        public double amps() {
            return resistor.getCurrent();
        }

        @Override
        public double watts() {
            return out.state.getU() * resistor.getCurrent();
        }

        @Override
        public String toString() {
            return "source '" + name + "'";
        }
    }

    // ---- watchdogs

    private final class WatchdogFactory implements Watchdogs {
        @Override
        public Watchdog volts(Load load, double minVolts, double maxVolts) {
            check();
            checkOpen();
            LoadImpl l = own(load);
            if (!(Double.isFinite(minVolts) && Double.isFinite(maxVolts) && minVolts < maxVolts))
                throw new IllegalArgumentException(l.name + ": volts limits must be finite with min below max, got " + minVolts + ".." + maxVolts);
            return add(new WatchdogImpl(l.name, "V", l::volts, minVolts, maxVolts));
        }

        @Override
        public Watchdog amps(Branch branch, double maxAmps) {
            Branch b = ownBranch(branch);
            checkMax(b, maxAmps, "amps");
            return add(new WatchdogImpl(b.name(), "A", () -> Math.abs(b.amps()), Double.NEGATIVE_INFINITY, maxAmps));
        }

        @Override
        public Watchdog watts(Branch branch, double maxWatts) {
            Branch b = ownBranch(branch);
            checkMax(b, maxWatts, "watts");
            return add(new WatchdogImpl(b.name(), "W", () -> Math.abs(b.watts()), Double.NEGATIVE_INFINITY, maxWatts));
        }

        private Branch ownBranch(Branch branch) {
            check();
            checkOpen();
            Objects.requireNonNull(branch, "branch");
            boolean mine = branch instanceof ResistorImpl r && r.owner() == CircuitImpl.this
                || branch instanceof SourceImpl s && s.owner() == CircuitImpl.this;
            if (!mine) throw new IllegalArgumentException(branch.name() + " is not a part of " + CircuitImpl.this);
            return branch;
        }

        private void checkMax(Branch b, double max, String what) {
            if (!(Double.isFinite(max) && max > 0))
                throw new IllegalArgumentException(b.name() + ": max " + what + " must be positive and finite, got " + max);
        }

        private Watchdog add(WatchdogImpl w) {
            watchdogs.add(w);
            return w;
        }
    }

    final class WatchdogImpl implements Watchdog {
        final String part, unit;
        final java.util.function.DoubleSupplier value;
        final double min, max;
        private int holdTicks = ticks(DEFAULT_HOLD_SECONDS), beyond;
        private boolean tripped;
        private WatchdogTrip last;
        private final List<Consumer<WatchdogTrip>> listeners = new ArrayList<>();

        WatchdogImpl(String part, String unit, java.util.function.DoubleSupplier value, double min, double max) {
            this.part = part;
            this.unit = unit;
            this.value = value;
            this.min = min;
            this.max = max;
        }

        private static int ticks(double seconds) {
            return (int) Math.ceil(seconds / STEP_SECONDS - 1e-9);
        }

        @Override
        public Watchdog holdSeconds(double seconds) {
            check();
            if (!(Double.isFinite(seconds) && seconds >= STEP_SECONDS - 1e-12))
                throw new IllegalArgumentException(part + ": hold must be at least one step (" + STEP_SECONDS + " s) and finite, got " + seconds);
            holdTicks = ticks(seconds);
            return this;
        }

        @Override
        public Watchdog onTrip(Consumer<WatchdogTrip> listener) {
            check();
            listeners.add(Objects.requireNonNull(listener, "listener"));
            return this;
        }

        @Override
        public boolean tripped() {
            return tripped;
        }

        @Override
        public Optional<WatchdogTrip> lastTrip() {
            return Optional.ofNullable(last);
        }

        @Override
        public void reset() {
            check();
            tripped = false;
            beyond = 0;
        }

        /** After a step: counts steps beyond the limit; trips once it has held. */
        void checkLimit() {
            if (tripped) return;
            double v = value.getAsDouble();
            boolean over = v > max, under = v < min;
            beyond = over || under ? beyond + 1 : 0;
            if (beyond < holdTicks) return;
            tripped = true;
            last = new WatchdogTrip(part, unit, v, over ? max : min);
            if (ApiTrace.enabled()) ApiTrace.log("watchdog trip in %s: %s", CircuitImpl.this, last);
            for (Consumer<WatchdogTrip> l : List.copyOf(listeners)) {
                try {
                    l.accept(last);
                } catch (RuntimeException e) { // one bad listener mustn't skip the others
                    LOG.error("{}: a watchdog trip listener threw on {}", CircuitImpl.this, last, e);
                }
            }
        }
    }

}
