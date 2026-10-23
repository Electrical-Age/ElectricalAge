package mods.eln.sim.process.destruct;

/** Why a device failed; picks the failure outcome (see Failure). */
public enum FailureCause {
    OVERHEAT("overheated"),
    OVERVOLTAGE("overvoltage"),
    OVERSPEED("overspeed"),
    OVERPRESSURE("burst"),
    /** Burned by a voltage of the wrong sign (Baughn: a machine fed negative showed "overvoltage", exactly backwards).
     *  Appended: causes are saved by ordinal. */
    REVERSE_VOLTAGE("reverse voltage");

    public final String text;

    FailureCause(String text) {
        this.text = text;
    }

    /** A voltage fault, either sign (same outcomes and repair as overvoltage). */
    public boolean electrical() {
        return this == OVERVOLTAGE || this == REVERSE_VOLTAGE;
    }

    public static FailureCause fromOrdinal(int i) {
        FailureCause[] v = values();
        return i >= 0 && i < v.length ? v[i] : OVERHEAT;
    }
}
