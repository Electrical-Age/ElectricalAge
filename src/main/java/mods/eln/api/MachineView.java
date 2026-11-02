package mods.eln.api;

import java.util.Optional;

/**
 * A read-only look at a machine, for GUIs, tools, Jade and tests. Values are as of the last electrical step (one
 * per tick). Safe to hold; it never changes the machine.
 */
public interface MachineView {
    MachineSpec spec();

    MachineState state();

    /** The most recent trip, if the machine ever tripped (kept after the trip resets). */
    Optional<TripCause> lastTrip();

    /** Line voltage at the machine, V. */
    double volts();

    /**
     * volts() / nominal volts while RUNNING (for a producer: while not tripped and not UNPOWERED), else 0. Scale
     * speeds by it: a machine on a sagging line runs slower.
     */
    double voltageRatio();

    /** Current through the machine in the last step, A: drawn by a consumer, fed by a producer. */
    double amps();

    /** Power in the last step, W: drawn from the line by a consumer, fed into it by a producer. */
    double watts();

    /**
     * Energy waiting in the machine, J. Consumer: delivered and not yet taken (at most two ticks of rated work).
     * Producer: given and not yet fed to the line (negative while it repays a small overdraw).
     */
    double bufferJoules();

    /** Temperature, degrees C; ambient when the spec has no thermal model. */
    double celsius();
}
