package mods.eln.bus;

/**
 * A field bus: the ports one connected run of bus cable reaches, and its 16 channels (BusChannel). Nothing here is in
 * the circuit solver.
 * <p>
 * Packets (Packet): what ports send in a tick is delivered at the start of the next, ordered by source address, then by
 * order of sending, at most CAP a tick (the rest are dropped and counted for Jade). A packet goes to every listening
 * port it is for except its sender: a broadcast to all, a colour channel's to the ports claiming that channel, a
 * device's to the port with that address.
 * <p>
 * Channels are held levels, 0..1 (a signal's share of its 50 V), set by signal packets: the last write wins (Baughn,
 * 2026-11-05; it was the highest of each tick's writes). A port writing a channel holds it; when a holder stops, the
 * others write theirs again, and a channel nobody holds falls to 0.
 */
public final class Bus {
    /** Packets delivered per bus per tick at most (Baughn: a cap). */
    public static final int CAP = 64;

    /** A packet waiting for the next tick: its sender and its place in the order of sending. */
    record Queued(Packet packet, BusPort from, long seq) {
    }

    /** The order of sending across every bus (a merge keeps it). */
    static long sequence;

    final java.util.List<BusPort> ports = new java.util.ArrayList<>();
    /** The channels some port uses, a bit each (Jade's list). */
    int used;
    final double[] level = new double[BusChannel.COUNT];
    java.util.List<Queued> outbox = new java.util.ArrayList<>();
    int delivered, dropped;
    long droppedTotal;

    double read(int c) {
        return BusChannel.valid(c) ? level[c] : 0;
    }

    /** The used channels again (a port's claims changed). */
    void reclaim() {
        used = 0;
        for (BusPort p : ports) used |= p.claims;
    }

    /** The start of a tick: writers that stopped let go of their channels, then last tick's packets are delivered. */
    void swap() {
        for (BusPort p : ports) {
            int stopped = p.sweep();
            for (int c = 0; stopped != 0; c++, stopped >>>= 1) if ((stopped & 1) != 0) release(c);
        }
        deliver();
    }

    /** A holder let go of channel c: the others write theirs again, or it falls to 0 if nobody holds it. */
    void release(int c) {
        boolean held = false;
        for (BusPort p : ports)
            if ((p.held >> c & 1) != 0) {
                held = true;
                p.resend(c);
            }
        if (!held) level[c] = 0;
    }

    void send(BusPort from, Packet p) {
        outbox.add(new Queued(p, from, sequence++));
    }

    /** Last tick's packets to their ports, in order, up to CAP; signals set their channels. */
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
            Packet k = e.packet();
            int c = k.type() == PacketType.SIGNAL ? k.channel() : BusChannel.UNUSED;
            if (c != BusChannel.UNUSED) {
                level[c] = k.value();
                if (e.from() != null && e.from().bus == this) e.from().held |= 1 << c;
            }
            for (BusPort p : ports) if (p != e.from() && p.wants(k)) p.receive(k);
        }
        // a dropped signal is written again at its sender's next write
        for (int i = delivered; i < q.size(); i++) {
            Queued e = q.get(i);
            int c = e.packet().type() == PacketType.SIGNAL ? e.packet().channel() : BusChannel.UNUSED;
            if (c != BusChannel.UNUSED && e.from() != null) e.from().resend(c);
        }
    }

    /** Is a channel in use: some port claims or holds it? */
    public boolean inUse(int c) {
        if (!BusChannel.valid(c)) return false;
        if ((used >> c & 1) != 0) return true;
        for (BusPort p : ports) if ((p.held >> c & 1) != 0) return true;
        return false;
    }

    /** A channel's level now (0..1). */
    public double value(int c) {
        return read(c);
    }

    /** How many ports hold a channel (two or more: a clash to show). */
    public int writers(int c) {
        if (!BusChannel.valid(c)) return 0;
        int n = 0;
        for (BusPort p : ports) n += p.held >> c & 1;
        return n;
    }

    public int portCount() {
        return ports.size();
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
}
