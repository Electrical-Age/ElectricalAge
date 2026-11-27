package mods.eln.robotics.motion;

/**
 * The stops of a servo axis: on a rail, whole blocks from its start (0..travel); on a laid track (track B), its list of
 * stops in metres along it. The setpoint wire's share of the travel, and where a stop asked for while moving ends. Pure numbers. (The station markers, their eight setpoint bands and the
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

    /** The stop nearest want (m) of a laid track's stops (ascending; track B: they aren't whole blocks along it). */
    public static double nearest(double want, double[] stops) {
        double best = stops[0];
        for (double s : stops) if (Math.abs(s - want) < Math.abs(best - want)) best = s;
        return best;
    }

    /** As stopAhead, on a laid track's stops: the first stop at or past the braking distance (the last one if none). */
    public static double stopAhead(double p, double v, double a, double[] stops) {
        double stop = p + v * Math.abs(v) / (2 * a);
        if (v > 0) {
            for (double s : stops) if (s >= stop - 1e-9) return s;
            return stops[stops.length - 1];
        }
        if (v < 0) {
            for (int i = stops.length - 1; i >= 0; i--) if (stops[i] <= stop + 1e-9) return stops[i];
            return stops[0];
        }
        return nearest(stop, stops);
    }

    /** Is p (m) at one of the stops? */
    public static boolean atStop(double p, double[] stops) {
        for (double s : stops) if (Math.abs(s - p) < 1e-6) return true;
        return false;
    }
}
