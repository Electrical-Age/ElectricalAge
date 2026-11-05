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

    /** The level of a channel, 0..1 (0: no bus, no such channel, or nobody wrote it last tick). */
    public double read(int c) {
        return bus == null ? 0 : bus.read(c);
    }

    /** Write a channel for the next tick (the highest of this tick's writes wins). */
    public void write(int c, double v) {
        if (bus != null) bus.write(c, v);
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

    /** Send a packet, delivered next tick; false: this port is on no bus. */
    public boolean send(Packet p) {
        if (bus == null || p == null) return false;
        bus.send(this, p);
        return true;
    }

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
