package mods.eln.api;

/**
 * A machine's nominal voltage: one of EA's cable tiers, so no machine is built for a voltage no cable carries. A
 * machine connects to cable of any tier; running it on the wrong one is the player's problem (undervoltage or an
 * overvoltage trip, see MachineState).
 */
public enum Tier {
    /** Low voltage, 50 V. */
    V50(50),
    /** Medium voltage, 200 V. */
    V200(200),
    /** High voltage, 800 V. */
    V800(800);

    private final double volts;

    Tier(double volts) {
        this.volts = volts;
    }

    /** The nominal voltage, V. */
    public double volts() {
        return volts;
    }
}
