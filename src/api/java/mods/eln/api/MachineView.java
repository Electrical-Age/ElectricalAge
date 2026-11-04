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

    /**
     * The most the buffer holds, J: two ticks of rated energy (consumer: 2 x ratedJoulesPerTick(), the work part;
     * producer: two ticks of rated output). Changes with respec.
     */
    double bufferCapacityJoules();

    /**
     * Energy across the power terminal since the machine was built or loaded, J. Consumer: delivered by the line
     * (idle draw included); producer: fed into the line (out of its source, droop losses included). A loaded machine
     * books its restored buffer here (consumer) or as given (producer), so the books balance from the load on.
     */
    double joulesDelivered();

    /** Consumer: energy the owner took (takeJoules, tryTakeJoules) since built or loaded, J; producer: 0. */
    double joulesTaken();

    /** Producer: energy the owner gave and the machine accepted (giveJoules) since built or loaded, J; consumer: 0. */
    double joulesGiven();

    /**
     * Energy that did no work, J. Consumer: delivered but not stored for the owner (the idle draw, what the full buffer
     * couldn't take, what a respec cut). Producer: given energy a respec cut from its queue. The books, at all times:
     * consumer joulesDelivered() = joulesTaken() + bufferJoules() + joulesLost(); producer joulesGiven() =
     * joulesDelivered() + bufferJoules() + joulesLost().
     */
    double joulesLost();

    /** Temperature, degrees C; ambient when the spec has no thermal model. */
    double celsius();
}
