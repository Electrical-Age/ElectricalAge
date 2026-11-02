package mods.eln.api.lowlevel;

/** A signal node: EA's signal cables run 0..50 V, read here as a level 0..1 (the API's signal unit, never volts). */
public interface SignalLoad extends Load {
    /** Its level after the last step, 0..1 (clamped). */
    double level();
}
