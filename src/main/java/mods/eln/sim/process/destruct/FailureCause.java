package mods.eln.sim.process.destruct;

/** Why a device failed; picks the failure outcome (see Failure). */
public enum FailureCause {
    OVERHEAT("overheated"),
    OVERVOLTAGE("overvoltage"),
    OVERSPEED("overspeed"),
    OVERPRESSURE("burst");

    public final String text;

    FailureCause(String text) {
        this.text = text;
    }

    public static FailureCause fromOrdinal(int i) {
        FailureCause[] v = values();
        return i >= 0 && i < v.length ? v[i] : OVERHEAT;
    }
}
