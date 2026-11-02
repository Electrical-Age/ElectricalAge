package mods.eln.api.lowlevel;

import java.util.function.Consumer;

/**
 * A load's link to an existing EA port (Circuit.connect). Before every step it checks that the port's block is still
 * there, in the simulation, with the same port; if anything changed it drops the link and resolves the port again,
 * so a port broken and placed again, or loaded later, links again by itself. status() says why it isn't connected;
 * nothing is swallowed.
 */
public interface PortLink {
    PortRef port();

    Load load();

    PortStatus status();

    /** Calls listener on the server thread whenever the status changes. */
    PortLink onStatus(Consumer<PortStatus> listener);

    /** Current from the port into the load in the last step, A (0 when not connected). */
    double amps();

    /** Drops the link for good (status REMOVED). */
    void remove();
}
