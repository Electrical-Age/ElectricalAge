package mods.eln.api;

import java.util.function.Consumer;

/**
 * The owner's handle on a machine: take or give energy, read the line, use signals and the bus. Each tick EA steps
 * the circuit first, then the owner's tick runs; what a consumer asks for in a tick sizes its load for the next one
 * (exactly one tick later), and a machine that asks for nothing draws only its idle watts.
 * <p>
 * Server thread only: calls from another thread throw IllegalStateException. Amounts must be finite and not
 * negative, else IllegalArgumentException. Taking on a producer, or giving on a consumer, throws
 * IllegalStateException.
 */
public interface Machine extends MachineView {
    /**
     * Takes exactly joules from the buffer and returns true, or takes nothing and returns false (not RUNNING, or
     * not enough yet). Use it for steps that need all their energy, like a recipe. The ask sizes next tick's load
     * either way, capped at ratedJoulesPerTick() (a bigger ask is logged once). Asking more than the buffer can
     * ever hold (2 x ratedJoulesPerTick()) throws IllegalArgumentException, since it could never succeed.
     */
    boolean tryTakeJoules(double joules);

    /**
     * Takes up to joules from the buffer and returns what it took (0 when not RUNNING). Use it for continuous work
     * (heaters, motors): the ask sizes next tick's load, so ask for what this tick's work needs. Asks above
     * ratedJoulesPerTick() are capped to it (and logged once).
     */
    double takeJoules(double joules);

    /**
     * Producer: queues joules to feed into the line from the next tick and returns how much was accepted (up to
     * the buffer's room, two ticks of rated output). The output follows a droop: 1.05 x nominal volts open, rated
     * watts at nominal, current-limited at rated watts / nominal volts. About 5% of the energy fed at rated power
     * is lost in the droop (less at lower power).
     */
    double giveJoules(double joules);

    /** Consumer: the work energy one tick at rated power holds, (ratedWatts - idleWatts) x 0.05 s, J. Producer: ratedWatts x 0.05 s. */
    double ratedJoulesPerTick();

    /** Signal input name's level, 0..1 (its default when nothing drives it). Unknown names throw IllegalArgumentException. */
    double signal(String name);

    /** Drives signal output name at level 0..1 (a level, not volts) until changed or released. */
    void setSignal(String name, double level);

    /** Lets signal output name float (high impedance), as it starts. */
    void releaseSignal(String name);

    /** The bus port. Throws IllegalStateException when the spec has no bus face. */
    MachineBus bus();

    /** Adds joules of heat (the work's losses) to the heat model; ignored when the spec has none. */
    void heatJoules(double joules);

    /** Calls listener on the server thread whenever the machine trips, once per trip. */
    void onTrip(Consumer<TripCause> listener);

    /** A read-only view of this machine, safe to hand to other code. */
    MachineView view();
}
