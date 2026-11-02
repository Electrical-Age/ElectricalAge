package mods.eln.api.impl.testkit;

import mods.eln.api.MachineSpec;
import mods.eln.api.impl.MachineCore;
import mods.eln.api.impl.MnaMachine;
import mods.eln.api.impl.lowlevel.CircuitImpl;
import mods.eln.api.lowlevel.Circuit;
import mods.eln.api.lowlevel.Load;
import mods.eln.api.testkit.Bench;
import mods.eln.api.testkit.BenchMachine;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.Simulator;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** ElnTestKit.bench(): a Bench in an EA Simulator of its own, the machines on MnaMachines at its bus node. */
public final class BenchImpl implements Bench {
    private final Thread thread = Thread.currentThread();
    private final Simulator sim = new Simulator(MachineCore.TICK_SECONDS, MachineCore.TICK_SECONDS, 50, 1 / 400.0);
    private final ElectricalLoad bus = new ElectricalLoad();
    private ElectricalLoad emf;
    private VoltageSource supply;
    private Resistor feed, load;
    private double supplyOhms, lineOhms;
    private final List<BenchMachineImpl> machines = new ArrayList<>();
    private final double[] busNow = new double[16], busNext = new double[16];
    private final StringBuilder trace = new StringBuilder();
    private long ticks;

    BenchImpl() {
        sim.tickFieldBuses = false; // (the world's buses are not the bench's: it may run inside a GameTest)
        sim.init();
        sim.addElectricalLoad(bus);
        Resistor leak = new Resistor(bus, null); // 1 Gohm to ground, so a bus with nothing on it still solves
        leak.highImpedance();
        sim.addElectricalComponent(leak);
    }

    @Override
    public Bench supply(double volts, double ohms) {
        checkThread();
        if (!(Double.isFinite(volts) && volts >= 0)) throw new IllegalArgumentException("supply volts must be finite, 0 or more, got " + volts);
        if (!(Double.isFinite(ohms) && ohms > 0)) throw new IllegalArgumentException("supply ohms must be positive and finite, got " + ohms);
        if (supply == null) {
            emf = new ElectricalLoad();
            supply = new VoltageSource("supply", emf, null);
            feed = new Resistor(emf, bus);
            sim.addElectricalLoad(emf);
            sim.addElectricalComponent(supply);
            sim.addElectricalComponent(feed);
        }
        supply.setU(volts);
        supplyOhms = ohms;
        feed.setR(supplyOhms + lineOhms);
        return this;
    }

    @Override
    public Bench supplyVolts(double volts) {
        if (supply == null) throw new IllegalStateException("no supply: call supply(volts, ohms) first");
        return supply(volts, supplyOhms);
    }

    @Override
    public Bench line(double ohms) {
        checkThread();
        if (!(Double.isFinite(ohms) && ohms >= 0)) throw new IllegalArgumentException("line ohms must be finite, 0 or more, got " + ohms);
        lineOhms = ohms;
        if (feed != null) feed.setR(supplyOhms + lineOhms);
        return this;
    }

    @Override
    public Bench load(double ohms) {
        checkThread();
        if (!(ohms > 0)) throw new IllegalArgumentException("load ohms must be positive (infinite: none), got " + ohms);
        if (load == null) {
            load = new Resistor(bus, null);
            sim.addElectricalComponent(load);
        }
        if (Double.isInfinite(ohms)) load.highImpedance();
        else load.setR(ohms);
        return this;
    }

    @Override
    public Bench attach(Circuit circuit) {
        checkThread();
        CircuitImpl c = impl(circuit);
        if (c.hosting()) throw new IllegalStateException(circuit.name() + " is hosted at a block entity; a bench takes a circuit of its own");
        c.attach(new CircuitImpl.Site() {
            @Override
            public Simulator simulator() {
                return sim;
            }

            @Override
            public void checkThread() {
                BenchImpl.this.checkThread();
            }

            @Override
            public double powerRs() {
                return POWER_RS;
            }

            @Override
            public double signalRs() {
                return SIGNAL_RS;
            }

            @Override
            public net.minecraft.server.level.ServerLevel level() {
                return null;
            }

            @Override
            public String where() {
                return "the bench";
            }
        });
        return this;
    }

    @Override
    public Bench connect(Circuit circuit, Load load) {
        checkThread();
        CircuitImpl c = impl(circuit);
        if (!c.attached()) throw new IllegalStateException(circuit.name() + " is not attached: attach(circuit) first");
        ElectricalLoad l = c.stateOf(load);
        Resistor wire = new Resistor(bus, l);
        wire.setR(l.getRs());
        sim.addElectricalComponent(wire);
        return this;
    }

    private static CircuitImpl impl(Circuit circuit) {
        if (!(java.util.Objects.requireNonNull(circuit, "circuit") instanceof CircuitImpl c))
            throw new IllegalArgumentException("not a circuit made by Circuit.create: " + circuit);
        return c;
    }

    @Override
    public double loadWatts() {
        return load == null ? 0 : load.getP();
    }

    @Override
    public Bench ambientCelsius(double celsius) {
        checkThread();
        if (!Double.isFinite(celsius)) throw new IllegalArgumentException("ambient must be finite, got " + celsius);
        ambientCelsius = celsius;
        for (BenchMachineImpl m : machines) m.core.setAmbientCelsius(celsius);
        return this;
    }

    private double ambientCelsius = MachineSpec.AMBIENT_CELSIUS;

    @Override
    public BenchMachine add(MachineSpec spec) {
        checkThread();
        MachineCore core = new MachineCore(spec);
        core.setAmbientCelsius(ambientCelsius);
        MnaMachine mna = new MnaMachine(core, bus);
        mna.addTo(sim);
        mna.applyBeforeStep();
        BenchMachineImpl m = new BenchMachineImpl(this, core, mna);
        machines.add(m);
        return m;
    }

    @Override
    public List<BenchMachine> machines() {
        return Collections.<BenchMachine>unmodifiableList(machines);
    }

    @Override
    public void tick() {
        checkThread();
        System.arraycopy(busNext, 0, busNow, 0, 16);
        Arrays.fill(busNext, 0);
        sim.tick();
        for (BenchMachineImpl m : machines) m.mna.readAfterStep();
        for (BenchMachineImpl m : machines) m.runOwner();
        for (BenchMachineImpl m : machines) m.mna.applyBeforeStep();
        ticks++;
        trace.append(String.format(Locale.ROOT, "%d %.3f", ticks, volts()));
        for (BenchMachineImpl m : machines)
            trace.append(String.format(Locale.ROOT, " | %.4f %.3f %s", m.amps(), m.bufferJoules(), m.state()));
        trace.append('\n');
    }

    @Override
    public void ticks(int n) {
        for (int i = 0; i < n; i++) tick();
    }

    @Override
    public long tickCount() {
        return ticks;
    }

    @Override
    public double volts() {
        return bus.getU();
    }

    @Override
    public double supplyAmps() {
        return feed == null ? 0 : feed.getCurrent();
    }

    @Override
    public String trace() {
        return trace.toString();
    }

    @Override
    public double busLevel(int channel) {
        checkChannel(channel);
        return busNow[channel];
    }

    void checkThread() {
        if (Thread.currentThread() != thread)
            throw new IllegalStateException("machine called from " + Thread.currentThread().getName()
                + "; only the bench's thread (in game: the server thread) may");
    }

    double busRead(int channel) {
        checkChannel(channel);
        return busNow[channel];
    }

    void busWrite(int channel, double level) {
        checkChannel(channel);
        BenchMachineImpl.checkLevel(level);
        busNext[channel] = Math.max(busNext[channel], level);
    }

    private static void checkChannel(int c) {
        if (c < 0 || c >= 16) throw new IllegalArgumentException("bus channel must be 0..15, got " + c);
    }
}
