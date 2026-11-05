package mods.eln.bus;

/**
 * A field bus: the ports one connected run of bus cable reaches, and its 16 channels (BusChannel). Values are 0..1 (a
 * signal's share of its 50 V). Double-buffered: what devices write in a simulator tick is read in the next one, so the
 * order devices run in doesn't matter; several writers on one channel: the highest wins (a wired OR). Nothing here is
 * in the circuit solver.
 * <p>
 * Packets (Packet): what ports send in a tick is delivered at the start of the next, ordered by source address, then by
 * order of sending, at most CAP a tick (the rest are dropped and counted for Jade). A packet goes to every listening
 * port it is for except its sender: a broadcast to all, a colour channel's to the ports claiming that channel, a
 * device's to the port with that address.
 */
public final class Bus {
    /** Packets delivered per bus per tick at most (Baughn: a cap). */
    public static final int CAP = 64;

    /** A packet waiting for the next tick: its sender and its place in the order of sending. */
    record Queued(Packet packet, BusPort from, long seq) {
    }

    /** The order of sending across every bus (a merge keeps it). */
    static long sequence;

    java.util.List<Queued> outbox = new java.util.ArrayList<>();
    int delivered, dropped;
    long droppedTotal;

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
        deliver();
    }

    void send(BusPort from, Packet p) {
        outbox.add(new Queued(p, from, sequence++));
    }

    /** Last tick's packets to their ports, in order, up to CAP. */
    void deliver() {
        if (outbox.isEmpty()) {
            delivered = dropped = 0;
            return;
        }
        java.util.List<Queued> q = outbox;
        outbox = new java.util.ArrayList<>();
        q.sort(java.util.Comparator.comparingInt((Queued e) -> e.packet().source()).thenComparingLong(Queued::seq));
        delivered = Math.min(q.size(), CAP);
        dropped = q.size() - delivered;
        droppedTotal += dropped;
        for (int i = 0; i < delivered; i++) {
            Queued e = q.get(i);
            for (BusPort p : ports) if (p != e.from() && p.wants(e.packet())) p.receive(e.packet());
        }
    }

    /** Packets delivered at the start of this tick. */
    public int delivered() {
        return delivered;
    }

    /** Packets dropped at the start of this tick (over CAP). */
    public int dropped() {
        return dropped;
    }

    /** Packets dropped since this bus was formed (a rebuild keeps the count). */
    public long droppedTotal() {
        return droppedTotal;
    }

    /** Packets waiting for the next tick. */
    public int queued() {
        return outbox.size();
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
