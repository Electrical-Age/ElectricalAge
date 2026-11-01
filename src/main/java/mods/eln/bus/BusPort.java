package mods.eln.bus;

/**
 * One member of a field bus: a bus cable conductor or a device's bus pin. Ports are joined by links (BusNet.link) when
 * their nodes connect, and the ports a link reaches form one Bus. A device's port also claims the channels it uses, a
 * bit each, so Jade can list a bus's channels in use; every channel is carried whether claimed or not.
 */
public final class BusPort {
    final java.util.Set<BusPort> links = new java.util.LinkedHashSet<>();
    int claims;
    Bus bus;

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
}
