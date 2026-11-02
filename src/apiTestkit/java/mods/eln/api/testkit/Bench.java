package mods.eln.api.testkit;

import mods.eln.api.MachineSpec;
import mods.eln.api.lowlevel.Circuit;
import mods.eln.api.lowlevel.Load;

import java.util.List;

/**
 * A headless EA circuit for machine tests (ElnTestKit.bench()): an optional supply (an ideal source behind its
 * internal resistance), the line resistance, and a bus node every machine connects to, in EA's own simulator (20 Hz,
 * one electrical step per tick, as in game). tick() runs one game tick in the game's order: the electrical step, each
 * machine's afterStep, each owner's code, each machine's beforeStep. It records a trace line per tick (trace()). Not
 * for a running game: it owns its own simulator. Calls from a thread other than the one that made it throw.
 */
public interface Bench {
    /**
     * The series resistance of a low-level circuit's power load on the bench, ohm (a low-voltage cable node's is of
     * this order); a signal load's is SIGNAL_RS.
     */
    double POWER_RS = 0.01, SIGNAL_RS = 100;

    /** Adds a supply of volts behind ohms (more than 0), or sets the one there is. */
    Bench supply(double volts, double ohms);

    /** Changes the supply's voltage from the next tick. */
    Bench supplyVolts(double volts);

    /** The line between the supply and the machines, ohms (0 or more). */
    Bench line(double ohms);

    /**
     * A resistive load of ohms (more than 0) from the machines' node to ground, for producers to feed; infinite
     * removes it. Sets the one there is.
     */
    Bench load(double ohms);

    /**
     * Attaches a low-level circuit (mods.eln.api.lowlevel) to the bench's simulator: its parts step with the machines,
     * and its watchdogs check after each step. Port links have no world here: they read NOT_HOSTED. Once per circuit,
     * and not one hosted at a block entity (IllegalStateException).
     */
    Bench attach(Circuit circuit);

    /**
     * Wires load (of a circuit attached here) to the machines' node, through its series resistance: the supply, line
     * and machines feed it or draw from it.
     */
    Bench connect(Circuit circuit, Load load);

    /** Power into the load of load(ohms) in the last step, W (0 without one). */
    double loadWatts();

    /**
     * The ambient the machines' heat models cool to, degrees C (default MachineSpec.AMBIENT_CELSIUS), for those there
     * and those added later; a machine not yet stepped starts at it.
     */
    Bench ambientCelsius(double celsius);

    /** Connects a machine to the bus. */
    BenchMachine add(MachineSpec spec);

    List<BenchMachine> machines();

    /** One game tick. */
    void tick();

    void ticks(int n);

    /** Ticks done. */
    long tickCount();

    /** The machines' bus node, V. */
    double volts();

    /** Current out of the supply in the last step, A (0 without one). */
    double supplyAmps();

    /**
     * One line per tick: "tick volts", then per machine (in the order added) " | amps buffer state"; amps to 4
     * decimals, volts and joules to 3.
     */
    String trace();

    /** Channel c of the bench's field bus as written last tick (every machine with a bus face shares it). */
    double busLevel(int channel);
}
