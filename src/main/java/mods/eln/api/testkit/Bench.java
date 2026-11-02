package mods.eln.api.testkit;

import mods.eln.api.MachineSpec;
import mods.eln.api.impl.MachineCore;
import mods.eln.api.impl.MnaMachine;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.Simulator;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A headless EA circuit for machine tests: an optional supply (an ideal source behind its internal resistance),
 * the line resistance, and a bus node every machine connects to, in EA's own simulator (20 Hz, one electrical step
 * per tick, as in game). tick() runs one game tick in the game's order: the electrical step, each machine's
 * afterStep, each owner's code, each machine's beforeStep. It records a trace line per tick (trace()). Not for a
 * running game: it owns its own Simulator.
 */
public final class Bench {
    private final Thread thread = Thread.currentThread();
    private final Simulator sim = new Simulator(MachineCore.TICK_SECONDS, MachineCore.TICK_SECONDS, 50, 1 / 400.0);
    private final ElectricalLoad bus = new ElectricalLoad();
    private ElectricalLoad emf;
    private VoltageSource supply;
    private Resistor feed, load;
    private double supplyOhms, lineOhms;
    private final List<BenchMachine> machines = new ArrayList<>();
    private final double[] busNow = new double[16], busNext = new double[16];
    private final StringBuilder trace = new StringBuilder();
    private long ticks;

    Bench() {
        sim.tickFieldBuses = false; // (the world's buses are not the bench's: it may run inside a GameTest)
        sim.init();
        sim.addElectricalLoad(bus);
        Resistor leak = new Resistor(bus, null); // 1 Gohm to ground, so a bus with nothing on it still solves
        leak.highImpedance();
        sim.addElectricalComponent(leak);
    }

    /** Adds a supply of volts behind ohms (more than 0), or sets the one there is. */
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

    /** Changes the supply's voltage from the next tick. */
    public Bench supplyVolts(double volts) {
        if (supply == null) throw new IllegalStateException("no supply: call supply(volts, ohms) first");
        return supply(volts, supplyOhms);
    }

    /** The line between the supply and the machines, ohms (0 or more). */
    public Bench line(double ohms) {
        checkThread();
        if (!(Double.isFinite(ohms) && ohms >= 0)) throw new IllegalArgumentException("line ohms must be finite, 0 or more, got " + ohms);
        lineOhms = ohms;
        if (feed != null) feed.setR(supplyOhms + lineOhms);
        return this;
    }

    /**
     * A resistive load of ohms (more than 0) from the machines' node to ground, for producers to feed; infinite
     * removes it. Sets the one there is.
     */
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

    /** Power into the load of load(ohms) in the last step, W (0 without one). */
    public double loadWatts() {
        return load == null ? 0 : load.getP();
    }

    /**
     * The ambient the machines' heat models cool to, degrees C (default MachineSpec.AMBIENT_CELSIUS), for those there
     * and those added later; a machine not yet stepped starts at it.
     */
    public Bench ambientCelsius(double celsius) {
        checkThread();
        if (!Double.isFinite(celsius)) throw new IllegalArgumentException("ambient must be finite, got " + celsius);
        ambientCelsius = celsius;
        for (BenchMachine m : machines) m.core.setAmbientCelsius(celsius);
        return this;
    }

    private double ambientCelsius = MachineSpec.AMBIENT_CELSIUS;

    /** Connects a machine to the bus. */
    public BenchMachine add(MachineSpec spec) {
        checkThread();
        MachineCore core = new MachineCore(spec);
        core.setAmbientCelsius(ambientCelsius);
        MnaMachine mna = new MnaMachine(core, bus);
        mna.addTo(sim);
        mna.applyBeforeStep();
        BenchMachine m = new BenchMachine(this, core, mna);
        machines.add(m);
        return m;
    }

    public List<BenchMachine> machines() {
        return Collections.unmodifiableList(machines);
    }

    /** One game tick. */
    public void tick() {
        checkThread();
        System.arraycopy(busNext, 0, busNow, 0, 16);
        Arrays.fill(busNext, 0);
        sim.tick();
        for (BenchMachine m : machines) m.mna.readAfterStep();
        for (BenchMachine m : machines) m.runOwner();
        for (BenchMachine m : machines) m.mna.applyBeforeStep();
        ticks++;
        trace.append(String.format(Locale.ROOT, "%d %.3f", ticks, volts()));
        for (BenchMachine m : machines)
            trace.append(String.format(Locale.ROOT, " | %.4f %.3f %s", m.amps(), m.bufferJoules(), m.state()));
        trace.append('\n');
    }

    public void ticks(int n) {
        for (int i = 0; i < n; i++) tick();
    }

    /** Ticks done. */
    public long tickCount() {
        return ticks;
    }

    /** The machines' bus node, V. */
    public double volts() {
        return bus.getU();
    }

    /** Current out of the supply in the last step, A (0 without one). */
    public double supplyAmps() {
        return feed == null ? 0 : feed.getCurrent();
    }

    /**
     * One line per tick: "tick volts", then per machine (in the order added) " | amps buffer state"; amps to 4
     * decimals, volts and joules to 3.
     */
    public String trace() {
        return trace.toString();
    }

    /** Channel c of the bench's field bus as written last tick (every machine with a bus face shares it). */
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
        BenchMachine.checkLevel(level);
        busNext[channel] = Math.max(busNext[channel], level);
    }

    private static void checkChannel(int c) {
        if (c < 0 || c >= 16) throw new IllegalArgumentException("bus channel must be 0..15, got " + c);
    }
}
