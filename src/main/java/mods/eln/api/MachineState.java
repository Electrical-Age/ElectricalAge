package mods.eln.api;

/**
 * What a machine is doing, decided after every electrical step from its line voltage, power and temperature.
 * Work energy is only available while RUNNING: in every other state takeJoules returns 0 and tryTakeJoules false.
 * A producer's state describes its line the same way, but only the trips stop its output.
 */
public enum MachineState {
    /** Line in range: takes are served from the buffer. */
    RUNNING,
    /**
     * No line voltage (under 2% of nominal), or it came back less than half a second ago (so a flickering line
     * doesn't start and stop the machine every tick): nothing is drawn.
     */
    UNPOWERED,
    /**
     * Line under the spec's minVoltageRatio (Brownout.STOP, the default; a Brownout.SLOW machine never is): idle
     * watts only, takes return 0. It runs again once the line has been
     * 5% of nominal above that floor (or halfway from it to nominal, if nearer) for half a second, so a line that
     * sags under the machine's own load doesn't flip it every tick.
     */
    UNDERVOLTAGE,
    /** Line went over 1.3x nominal: draws nothing. Resets after the trip cooldown spent below 1.1x. */
    OVERVOLTAGE_TRIPPED,
    /** Temperature went over the spec's trip point: draws nothing until the cooldown is spent 10 K below it. */
    OVERHEAT_TRIPPED;

    /** One of the trips? */
    public boolean tripped() {
        return this == OVERVOLTAGE_TRIPPED || this == OVERHEAT_TRIPPED;
    }
}
