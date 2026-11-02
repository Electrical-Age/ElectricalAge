package mods.eln.api.lowlevel;

/** Where a PortLink stands, checked before every step: CONNECTED, or why not. */
public enum PortStatus {
    /** Linked: current flows between the port and the load. */
    CONNECTED,
    /** The circuit isn't in a world: not hosted yet (it joins at the next tick), or on a Bench, which has none. */
    NOT_HOSTED,
    /** No EA block at the position, and its chunk isn't loaded: it may be there once it loads. */
    NOT_LOADED,
    /** No EA block at the position (its chunk is loaded). */
    NO_NODE,
    /** The EA block is there but out of the simulation (frozen by another mod, or not yet connected). */
    NOT_SIMULATED,
    /** The EA block has no port on that face and position (or it burnt out: a burnt device has none). */
    NO_PORT,
    /** The port is there but of the other kind (a power link to a signal port, or the reverse). */
    WRONG_KIND,
    /** remove() was called, or the circuit left the world for good. */
    REMOVED
}
