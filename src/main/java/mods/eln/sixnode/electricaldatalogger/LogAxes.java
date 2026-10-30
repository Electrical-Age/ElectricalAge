package mods.eln.sixnode.electricaldatalogger;

import java.util.ArrayList;
import java.util.List;

/**
 * The Y axes of a logger graph (board #45). Channels with the same unit and range share an axis. The first axis is on
 * the left, the second on the right; any more go on the right too, each in its own tick column further out.
 */
public final class LogAxes {
    public enum Side {LEFT, RIGHT}

    /** An axis: where it goes, its column on that side (0 = next to the graph), its scale, and the channels on it. */
    public record Axis(Side side, int column, LogChannel scale, List<Integer> channels) {
    }

    private LogAxes() {
    }

    public static List<Axis> layout(List<LogChannel> channels) {
        List<Axis> out = new ArrayList<>();
        for (int c = 0; c < channels.size(); c++) {
            LogChannel ch = channels.get(c);
            Axis found = null;
            for (Axis a : out) if (a.scale().sameScale(ch)) found = a;
            if (found != null) {
                found.channels().add(c);
                continue;
            }
            int n = out.size();
            out.add(new Axis(n == 0 ? Side.LEFT : Side.RIGHT, n == 0 ? 0 : n - 1, ch, new ArrayList<>(List.of(c))));
        }
        return out;
    }

    /**
     * Tick values between lo and hi (either order) at a "nice" step (1, 2 or 5 times a power of ten), at most max of
     * them and at least two (the ends, when no nice step fits).
     */
    public static double[] ticks(double lo, double hi, int max) {
        double a = Math.min(lo, hi), b = Math.max(lo, hi), span = b - a;
        if (!(span > 0) || Double.isInfinite(span) || max < 2) return new double[]{a, b};
        double step = Math.pow(10, Math.floor(Math.log10(span / max)));
        for (double m : new double[]{1, 2, 5, 10, 20, 50}) {
            double s = step * m;
            double first = Math.ceil(a / s - 1e-9) * s;
            int n = (int) Math.floor((b - first) / s + 1e-9) + 1;
            if (n <= max) {
                if (n < 2) break;
                double[] t = new double[n];
                for (int i = 0; i < n; i++) t[i] = clean(first + i * s, s);
                return t;
            }
        }
        return new double[]{a, b};
    }

    /** Rounds off float noise below the step (0.30000000000000004 -> 0.3). */
    static double clean(double v, double step) {
        double q = Math.pow(10, Math.floor(Math.log10(step)) - 3);
        double r = Math.round(v / q) * q;
        return r == 0 ? 0 : r;
    }
}
