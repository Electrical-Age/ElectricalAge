package mods.eln.robotics.motion;

/**
 * The stops of a servo axis, in whole blocks from its start (0..travel): the setpoint wire's share of the travel, and
 * where a stop asked for while moving ends. Pure numbers. (The station markers, their eight setpoint bands and the
 * panel's up/down stepping went 2026-11-05: Baughn, the drive is a plain actuator.)
 */
public final class Stations {
    private Stations() {
    }

    /** The stop a normalised setpoint (0..1) asks for: its share of the travel, to the nearest block. */
    public static int proportional(double normalized, int travel) {
        return (int) Math.round(Math.max(0, Math.min(1, normalized)) * travel);
    }

    /**
     * Where a stop asked for while moving at v (m/tick) from p ends, braking at a (m/tick^2): the first whole block at or
     * past the braking distance, within 0..travel (every stop is block-aligned).
     */
    public static int stopAhead(double p, double v, double a, int travel) {
        double stop = p + v * Math.abs(v) / (2 * a);
        double b = v > 0 ? Math.ceil(stop - 1e-9) : v < 0 ? Math.floor(stop + 1e-9) : Math.round(stop);
        return (int) Math.max(0, Math.min(travel, b));
    }
}
