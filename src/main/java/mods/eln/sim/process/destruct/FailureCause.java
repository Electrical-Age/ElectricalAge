package mods.eln.sim.process.destruct;

/** Why a device failed; picks the failure outcome (see Failure). */
public enum FailureCause {
    OVERHEAT("overheated"),
    OVERVOLTAGE("overvoltage"),
    OVERSPEED("overspeed"),
    OVERPRESSURE("burst"),
    /** Burned by a voltage of the wrong sign (Baughn: a machine fed negative showed "overvoltage", exactly backwards).
     *  Appended: causes are saved by ordinal. */
    REVERSE_VOLTAGE("reverse voltage"),
    /** A turning steam turbine flooded with water (Baughn: a significant quantity of water damages it). Appended. */
    WATER("water damage: water came in with its steam"),
    /** A turning steam turbine whose condenser filled with water to its exhaust (Baughn 2026-11-14: not the same as
     *  water pumped in through the steam line; say which). Appended. */
    FLOODED("water damage: its condenser flooded, its water isn't leaving");

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
