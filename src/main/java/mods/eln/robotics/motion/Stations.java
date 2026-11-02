package mods.eln.robotics.motion;

import java.util.Arrays;
import java.util.TreeSet;

/**
 * The stops of a servo axis, in whole blocks from its start (0..travel): the station markers' places, or, with none, up
 * to eight stops spread evenly over the travel. Pure numbers (the device scans the markers).
 */
public final class Stations {
    private Stations() {
    }

    /** Bands of the 0..1 setpoint in station mode: band k selects the k-th station from the bottom. */
    public static final int BANDS = 8;

    /** The stops, ascending and distinct: the markers within 0..travel, else evenly spread ones (0 and travel included). */
    public static int[] of(int[] markers, int travel) {
        TreeSet<Integer> s = new TreeSet<>();
        for (int m : markers) if (m >= 0 && m <= travel) s.add(m);
        if (s.isEmpty() && travel > 0) {
            int n = Math.min(BANDS, travel + 1);
            for (int k = 0; k < n; k++) s.add((int) Math.round(k * (double) travel / (n - 1)));
        }
        return s.stream().mapToInt(Integer::intValue).toArray();
    }

    /** The station a normalised setpoint (0..1) selects in station mode; the top band and above take the highest. */
    public static int band(double normalized, int[] stations) {
        if (stations.length == 0) return 0;
        int k = (int) Math.floor(Math.max(0, Math.min(1, normalized)) * BANDS);
        return stations[Math.min(Math.min(k, BANDS - 1), stations.length - 1)];
    }

    /** The stop a normalised setpoint (0..1) asks for in proportional mode: the travel, to the nearest block. */
    public static int proportional(double normalized, int travel) {
        return (int) Math.round(Math.max(0, Math.min(1, normalized)) * travel);
    }

    /** The next station above position p (a stop it stands at doesn't count), or -1 at the top. */
    public static int above(double p, int[] stations) {
        for (int s : stations) if (s > p + 1e-3) return s;
        return -1;
    }

    /** The next station below position p (a stop it stands at doesn't count), or -1 at the bottom. */
    public static int below(double p, int[] stations) {
        int[] r = stations.clone();
        Arrays.sort(r);
        for (int i = r.length - 1; i >= 0; i--) if (r[i] < p - 1e-3) return r[i];
        return -1;
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
