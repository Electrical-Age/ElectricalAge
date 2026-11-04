package mods.eln.api.lowlevel;

/** A part current flows through: a ResistorLoad or a VoltageSource (what Watchdogs.amps and watts watch). */
public interface Branch {
    String name();

    /** Current in the last step, A (a resistor: from a to b; a source: out of it into its load). */
    double amps();

    /** Power in the last step, W (a resistor: dissipated; a source: delivered at its terminal). */
    double watts();
}
