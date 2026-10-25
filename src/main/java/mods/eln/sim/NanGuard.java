package mods.eln.sim;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Where a non-finite value (NaN, infinity) would enter the simulation, it is replaced (an open circuit, no heat) and
 * reported here: the first few with a stack trace, so the device that produced it can be found. One NaN resistance
 * used to turn a whole network NaN, silently, and watchdogs can't see NaN (AGE's history found the spots).
 */
public final class NanGuard {
    private NanGuard() {
    }

    static final int TRACES = 10;
    private static final AtomicInteger count = new AtomicInteger();

    public static void report(String where, double value) {
        int n = count.incrementAndGet();
        if (n <= TRACES)
            mods.eln.Eln.LOG.warn("Electrical Age: {} got {} (replaced){}", where, value,
                n == TRACES ? "; further ones are counted, not logged" : "", new Throwable("from"));
    }

    /** How many were replaced since start (tests, diagnostics). */
    public static int count() {
        return count.get();
    }
}
