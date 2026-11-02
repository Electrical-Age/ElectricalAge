package mods.eln.api.lowlevel;

/**
 * A node of a circuit: a voltage against ground. Cables, resistors, sources and port links join at it, each through
 * EA's small series resistance of a cable's node.
 */
public interface Load {
    String name();

    /** Its voltage after the last step, V. */
    double volts();
}
