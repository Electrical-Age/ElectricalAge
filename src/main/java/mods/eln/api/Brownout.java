package mods.eln.api;

/**
 * What a consumer does when its line sags (a weak line, or a source that can't carry the demand). Set with
 * MachineSpec.Builder.onBrownout; STOP unless a spec says otherwise. Baughn: the starved-supply behaviour is the
 * spec's choice.
 */
public enum Brownout {
    /**
     * The default: below the spec's minVoltageRatio the machine is UNDERVOLTAGE (idle watts only, takes return 0)
     * until the line recovers. An honest brownout: a 200 V mill on a 50 V line doesn't crawl.
     */
    STOP,
    /**
     * No floor: the machine keeps RUNNING on whatever arrives (down to UNPOWERED), its draw still capped at
     * rated x (U/U_nom)^2, so takes return less and voltageRatio() reads low. For loads that can run slower
     * (heaters, motors, servos).
     */
    SLOW
}
