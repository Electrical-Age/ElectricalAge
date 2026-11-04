package mods.eln.api.lowlevel;

/**
 * A voltage source behind its internal resistance, from ground to its load: open-circuit it holds volts(); loaded,
 * its terminal sags by amps() x ohms(). Never ideal, so sources in parallel share their load instead of making the
 * circuit unsolvable.
 */
public interface VoltageSource extends Branch {
    /** The load it feeds. */
    Load out();

    /** Its EMF (open-circuit voltage), V. */
    double volts();

    /** Sets its EMF from the next step (finite). */
    void setVolts(double volts);

    /** Its internal resistance, ohm. */
    double ohms();

    /** Sets its internal resistance from the next step (positive and finite). */
    void setOhms(double ohms);
}
