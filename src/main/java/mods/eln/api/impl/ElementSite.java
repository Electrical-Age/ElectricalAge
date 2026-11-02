package mods.eln.api.impl;

import mods.eln.api.TripCause;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;

/** What an ElementHost needs of the element it is on (an adapter per element kind; a stub in unit tests). */
public interface ElementSite {
    /** The element's front, or null before it is placed or loaded. */
    Direction front();

    /** Something is connected to the element on side at cable position lrdu (a cable or a device). */
    boolean connected(Direction side, LRDU lrdu);

    /** Something is connected to the element on side, at any position. */
    default boolean connected(Direction side) {
        for (LRDU l : LRDU.values()) if (connected(side, l)) return true;
        return false;
    }

    /** Throws IllegalStateException off the server thread. */
    void checkThread();

    /** The element is burnt out (out of the simulation until repaired). */
    boolean burnt();

    /** Queues the element's burn-out for cause (EA's failure path: it runs at the end of the tick). */
    void burn(TripCause cause, double severity);

    /** The series resistance of the power terminal, ohm (what a cable's node has). */
    double powerRs();

    /** The series resistance of a signal gate, ohm. */
    double signalRs();
}
