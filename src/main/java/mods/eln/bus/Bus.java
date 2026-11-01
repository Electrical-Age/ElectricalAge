package mods.eln.bus;

/**
 * A field bus: the ports one connected run of bus cable reaches, and its 16 channels (BusChannel). Values are 0..1 (a
 * signal's share of its 50 V). Double-buffered: what devices write in a simulator tick is read in the next one, so the
 * order devices run in doesn't matter; several writers on one channel: the highest wins (a wired OR). Nothing here is
 * in the circuit solver.
 */
public final class Bus {
    final java.util.List<BusPort> ports = new java.util.ArrayList<>();
    /** The channels some port uses, a bit each (Jade's list). */
    int used;
    double[] cur = new double[BusChannel.COUNT], next = new double[BusChannel.COUNT];
    int[] writers = new int[BusChannel.COUNT], nextWriters = new int[BusChannel.COUNT];

    double read(int c) {
        return BusChannel.valid(c) ? cur[c] : 0;
    }

    void write(int c, double v) {
        if (!BusChannel.valid(c)) return;
        next[c] = Math.max(next[c], Math.max(0, Math.min(1, v)));
        nextWriters[c]++;
    }

    /** The used channels again (a port's claims changed). */
    void reclaim() {
        used = 0;
        for (BusPort p : ports) used |= p.claims;
    }

    /** Next tick's values become this tick's. */
    void swap() {
        double[] d = cur;
        cur = next;
        next = d;
        java.util.Arrays.fill(next, 0);
        int[] w = writers;
        writers = nextWriters;
        nextWriters = w;
        java.util.Arrays.fill(nextWriters, 0);
    }

    /** Is a channel in use: some port claims it, or something wrote it last tick? */
    public boolean inUse(int c) {
        return BusChannel.valid(c) && ((used >> c & 1) != 0 || writers[c] > 0);
    }

    /** A channel's level now (0..1). */
    public double value(int c) {
        return read(c);
    }

    /** How many writes a channel had last tick (two or more: a clash to show). */
    public int writers(int c) {
        return BusChannel.valid(c) ? writers[c] : 0;
    }

    public int portCount() {
        return ports.size();
    }
}
