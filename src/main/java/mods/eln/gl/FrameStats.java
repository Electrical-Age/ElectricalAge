package mods.eln.gl;

import java.util.Arrays;

/** Statistics over a series of times (the render benchmarks: frame times, batches of draws). No client classes. */
public final class FrameStats {
    private FrameStats() {
    }

    /** Mean, median, 95th and 99th percentile, maximum and minimum of the first n values (0 when n is 0). */
    public record Summary(int n, double mean, long p50, long p95, long p99, long max, long min) {
    }

    /** The value at percentile p (0..100) of sorted[0..n): nearest rank, so it is always one of the values. */
    public static long percentile(long[] sorted, int n, double p) {
        if (n <= 0) return 0;
        int rank = (int) Math.ceil(p / 100.0 * n);
        return sorted[Math.min(n - 1, Math.max(0, rank - 1))];
    }

    /** Summary of values[0..n); values is not changed. */
    public static Summary summary(long[] values, int n) {
        if (n <= 0) return new Summary(0, 0, 0, 0, 0, 0, 0);
        long[] s = Arrays.copyOf(values, n);
        Arrays.sort(s);
        double sum = 0;
        for (long v : s) sum += v;
        return new Summary(n, sum / n, percentile(s, n, 50), percentile(s, n, 95), percentile(s, n, 99), s[n - 1], s[0]);
    }
}
