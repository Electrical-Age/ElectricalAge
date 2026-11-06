package mods.eln.api;

/**
 * EA's cable voltages (real-world since API 1.2: 48 V, 230 V, 400 V, 11 kV; Baughn 2026-11-06), a convenience for MachineSpec.consumer(Tier) and producer(Tier): a spec stores plain nominal
 * volts, and consumer(double) / producer(double) take any voltage. Baughn: voltages become continuous later (custom
 * cable designs, no fixed tiers), so nothing should depend on this enum. A machine connects to cable of any
 * voltage; running it on the wrong one is the player's problem (undervoltage or an overvoltage trip).
 */
public enum Tier {
    /** Low voltage, 48 V. */
    V48(48),
    /** Medium voltage, 230 V. */
    V230(230),
    /** High voltage, 400 V. */
    V400(400),
    /** Transmission, 11 kV. */
    V11000(11000),
    /** The 48 V tier under its 1.12 name (it was 50 V). */
    @Deprecated V50(48),
    /** The 230 V tier under its 1.12 name (it was 200 V). */
    @Deprecated V200(230),
    /** The 400 V tier under its 1.12 name (it was 800 V). */
    @Deprecated V800(400),
    /** The 11 kV tier under its 1.12 name (it was 3200 V). */
    @Deprecated V3200(11000);

    private final double volts;

    Tier(double volts) {
        this.volts = volts;
    }

    /** The nominal voltage, V. */
    public double volts() {
        return volts;
    }
}
