package mods.eln.api;

/**
 * EA's cable voltages, a convenience for MachineSpec.consumer(Tier) and producer(Tier): a spec stores plain nominal
 * volts, and consumer(double) / producer(double) take any voltage. Baughn: voltages become continuous later (custom
 * cable designs, no fixed tiers), so nothing should depend on this enum. A machine connects to cable of any
 * voltage; running it on the wrong one is the player's problem (undervoltage or an overvoltage trip).
 */
public enum Tier {
    /** Low voltage, 50 V. */
    V50(50),
    /** Medium voltage, 200 V. */
    V200(200),
    /** High voltage, 800 V. */
    V800(800),
    /** Very high voltage, 3200 V. */
    V3200(3200);

    private final double volts;

    Tier(double volts) {
        this.volts = volts;
    }

    /** The nominal voltage, V. */
    public double volts() {
        return volts;
    }
}
