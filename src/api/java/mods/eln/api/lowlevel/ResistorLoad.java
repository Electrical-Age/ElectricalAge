package mods.eln.api.lowlevel;

import java.util.Optional;

/** A resistor between two loads, or a load and ground. */
public interface ResistorLoad extends Branch {
    Load a();

    /** The other end, empty for ground. */
    Optional<Load> b();

    /** Its resistance, ohm; infinite when open. */
    double ohms();

    /** Sets its resistance from the next step: positive, or Double.POSITIVE_INFINITY to open it. */
    void setOhms(double ohms);

    /** Voltage across it in the last step, a minus b, V. */
    double volts();
}
