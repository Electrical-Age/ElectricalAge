package mods.eln.api.lowlevel;

/**
 * Limits on a circuit's parts, checked after every electrical step. A watchdog trips when its value has been beyond
 * its limit for its hold time (Watchdog.holdSeconds, default 0.25 s, so a switching transient doesn't trip it): it
 * calls its onTrip listeners once, on the server thread, and stays tripped until reset(). A trip does nothing else:
 * what it means (open a resistor, drop a source, break your block) is the listener's to do.
 */
public interface Watchdogs {
    /** Trips when load's volts leave minVolts..maxVolts (finite, min below max). */
    Watchdog volts(Load load, double minVolts, double maxVolts);

    /** Trips when branch's current, either way, goes over maxAmps (positive, finite). */
    Watchdog amps(Branch branch, double maxAmps);

    /** Trips when branch's power, either way, goes over maxWatts (positive, finite). */
    Watchdog watts(Branch branch, double maxWatts);
}
