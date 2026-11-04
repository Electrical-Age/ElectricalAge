package mods.eln.api.testkit;

import mods.eln.api.Machine;

import java.util.OptionalDouble;
import java.util.function.Consumer;

/**
 * A machine on a Bench: the Machine an owner's code gets in game, plus the test's side of it (owner code to run
 * every tick, signal inputs to drive, outputs to read). Calls from a thread other than the bench's throw, as in game.
 * On the bench an input is connected (signalConnected) while the bench drives it (input(name, level)).
 */
public interface BenchMachine extends Machine {
    /** The owner's code, run every tick after the electrical step (as a block entity's tick). Replaces the last. */
    BenchMachine owner(Consumer<Machine> owner);

    /** Drives signal input name at level 0..1 (what a cable would bring). */
    BenchMachine input(String name, double level);

    /** Stops driving signal input name: it reads its default again. */
    BenchMachine releaseInput(String name);

    /** Signal output name's level, or empty while it floats (high impedance). */
    OptionalDouble output(String name);
}
