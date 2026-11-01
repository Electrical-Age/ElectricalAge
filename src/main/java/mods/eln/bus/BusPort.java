package mods.eln.bus;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One member of a field bus: a bus cable conductor or a device's bus pin. Ports are joined by links (BusNet.link) when
 * their nodes connect, and the ports a link reaches form one Bus. A device's port also claims the channel names it uses,
 * so a channel exists while some device on the bus claims it (Baughn: named channels, 64 a bus).
 */
public final class BusPort {
    final Set<BusPort> links = new LinkedHashSet<>();
    final Set<String> claims = new LinkedHashSet<>();
    Bus bus;

    /** The bus this port is on now, or null: it has no links, or the net hasn't been rebuilt since it was joined. */
    public Bus bus() {
        return bus;
    }

    /** The names this port claims. */
    public Set<String> claims() {
        return java.util.Collections.unmodifiableSet(claims);
    }

    /** Claim exactly these names (a device's settings changed or loaded); the bus's channel list follows. */
    public void claim(java.util.Collection<String> names) {
        claims.clear();
        for (String n : names) if (Bus.validName(n)) claims.add(n);
        if (bus != null) bus.reclaim();
    }

    /** The level of a channel, 0..1 (0: no such channel, or nobody wrote it last tick). */
    public double read(String name) {
        return bus == null ? 0 : bus.read(name);
    }

    /** Write a channel for the next tick (the highest of this tick's writes wins). */
    public void write(String name, double v) {
        if (bus != null) bus.write(name, v);
    }
}
