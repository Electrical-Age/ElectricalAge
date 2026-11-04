package mods.eln.api.lowlevel;

import java.util.Optional;
import java.util.function.Consumer;

/** One limit on one part (see Watchdogs). */
public interface Watchdog {
    /** The default hold time: how long the value must stay beyond its limit to trip, s. */
    double DEFAULT_HOLD_SECONDS = 0.25;

    /** Sets how long the value must stay beyond its limit to trip, s: at least one step (0.05), finite. */
    Watchdog holdSeconds(double seconds);

    /** Calls listener on the server thread at each trip (once per trip). */
    Watchdog onTrip(Consumer<WatchdogTrip> listener);

    /** Tripped and not reset since? */
    boolean tripped();

    /** The most recent trip, kept after reset. */
    Optional<WatchdogTrip> lastTrip();

    /** Clears a trip: it watches again from the next step, with a fresh hold time. */
    void reset();
}
