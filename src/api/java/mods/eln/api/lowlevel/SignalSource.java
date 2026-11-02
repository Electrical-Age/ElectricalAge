package mods.eln.api.lowlevel;

import java.util.OptionalDouble;

/** A signal node with a gate output on it, as an EA signal source's: it drives the node at a level 0..1 or floats. */
public interface SignalSource extends SignalLoad {
    /** Drives the node at level (0..1, a level, not volts) until changed or released. */
    void setLevel(double level);

    /** Lets the node float (high impedance), as it starts. */
    void release();

    /** The level it drives, or empty while it floats. */
    OptionalDouble driven();
}
