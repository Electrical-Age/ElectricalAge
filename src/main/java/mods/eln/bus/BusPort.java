package mods.eln.bus;

/**
 * One member of a field bus: a bus cable conductor or a device's bus pin. Ports are joined by links (BusNet.link) when
 * their nodes connect, and the ports a link reaches form one Bus. A device's port also claims the channels it uses, a
 * bit each, so Jade can list a bus's channels in use; every channel is carried whether claimed or not.
 * <p>
 * A device that takes packets listens: packets for it (broadcasts, its claimed channels', its address's) wait in its
 * inbox, up to INBOX packets (older ones are dropped first), until it polls them, so a device needn't run every tick.
 * Cable ports don't listen.
 */
public final class BusPort {
    /** Packets an inbox holds at most. */
    public static final int INBOX = 64;

    final java.util.Set<BusPort> links = new java.util.LinkedHashSet<>();
    int claims;
    Bus bus;
    int address;
    /** Channels held, written through write() since the last tick, and in the tick before. */
    int held, writing, writes;
    private final float[] sent = new float[BusChannel.COUNT];
    private Bus sentOn;
    private java.util.ArrayDeque<Packet> inbox;
    private long overflow;

    /** The bus this port is on now, or null: it has no links, or the net hasn't been rebuilt since it was joined. */
    public Bus bus() {
        return bus;
    }

    /** The channels this port claims, bit c for channel c. */
    public int claims() {
        return claims;
    }

    /** Claim exactly these channels (bit c for channel c; a device's settings changed or loaded). */
    public void claim(int mask) {
        claims = mask & ((1 << BusChannel.COUNT) - 1);
        if (bus != null) bus.reclaim();
    }

    /** The level of a channel, 0..1 (0: no bus, no such channel, or nobody holds it). */
    public double read(int c) {
        return bus == null ? 0 : bus.read(c);
    }

    /**
     * Write a channel, every tick it should hold it: a signal packet goes out when the level changes (by DEADBAND or
     * more, or onto or off 0 or 1) or the bus asks again, and it shows next tick (the last write wins). A tick without a
     * write lets go of the channel.
     */
    public void write(int c, double v) {
        if (bus == null || !BusChannel.valid(c)) return;
        if (sentOn != bus) {   // a new bus: everything again
            java.util.Arrays.fill(sent, Float.NaN);
            sentOn = bus;
        }
        float f = (float) Math.max(0, Math.min(1, Double.isFinite(v) ? v : 0));
        writing |= 1 << c;
        float s = sent[c];
        if (s == s && (f == s || Math.abs(f - s) < DEADBAND && f != 0 && f != 1 && s != 0 && s != 1)) return;
        sent[c] = f;
        bus.send(this, Packet.signal(address, c, f));
    }

    /** Level changes smaller than this (5 mV of 50 V) aren't sent: an analog wire's noise would fill the bus. */
    public static final float DEADBAND = 1e-4f;

    /** The channels this port holds (its signal was the last delivered, or it writes them). */
    public int held() {
        return held;
    }

    /** The start of a tick: the channels written through write() last tick but not since, which it lets go. */
    int sweep() {
        int stopped = writes & ~writing;
        writes = writing;
        writing = 0;
        held &= ~stopped;
        for (int c = 0, m = stopped; m != 0; c++, m >>>= 1) if ((m & 1) != 0) sent[c] = Float.NaN;
        return stopped;
    }

    /** Send channel c again at the next write (a holder let go, or the packet was dropped). */
    void resend(int c) {
        sent[c] = Float.NaN;
    }

    /** Its device address (17..255), or 0: none. */
    public int address() {
        return address;
    }

    /** Take this device address; anything but 17..255 means none (1..16 are the colour channels, 0 broadcast). */
    public void setAddress(int a) {
        address = Packet.deviceAddress(a) ? a : 0;
    }

    /** Start or stop taking packets (stopping empties the inbox). */
    public void listen(boolean on) {
        if (on && inbox == null) inbox = new java.util.ArrayDeque<>();
        else if (!on) inbox = null;
    }

    public boolean listening() {
        return inbox != null;
    }

    /**
     * Send a packet, delivered next tick; false: this port is on no bus. A port linked since the last rebuild (no bus
     * yet) keeps it until the rebuild puts it on one, at the next tick's start.
     */
    public boolean send(Packet p) {
        if (p == null) return false;
        if (bus != null) bus.send(this, p);
        else if (!links.isEmpty()) {
            if (early == null) early = new java.util.ArrayList<>();
            early.add(new Bus.Queued(p, this, Bus.sequence++));
        } else return false;
        return true;
    }

    /** Packets sent before it was on a bus (send). */
    java.util.List<Bus.Queued> early;

    /** The oldest packet waiting, or null. */
    public Packet poll() {
        return inbox == null ? null : inbox.poll();
    }

    /** Packets waiting. */
    public int pending() {
        return inbox == null ? 0 : inbox.size();
    }

    /** Packets its full inbox dropped. */
    public long overflow() {
        return overflow;
    }

    boolean wants(Packet p) {
        if (inbox == null) return false;
        if (p.target() == Packet.BROADCAST) return true;
        int c = p.channel();
        if (c != BusChannel.UNUSED) return (claims >> c & 1) != 0;
        return address != 0 && p.target() == address;
    }

    void receive(Packet p) {
        if (inbox.size() >= INBOX) {
            inbox.poll();
            overflow++;
        }
        inbox.add(p);
    }
}
