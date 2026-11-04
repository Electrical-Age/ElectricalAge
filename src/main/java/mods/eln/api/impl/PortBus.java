package mods.eln.api.impl;

import mods.eln.api.MachineBus;
import mods.eln.bus.BusChannel;
import mods.eln.bus.BusPort;

import java.util.Objects;

/** A MachineBus on an EA bus port: checks channel and level, and claims each channel written (for Jade's list). */
public final class PortBus implements MachineBus {
    private final BusPort port;

    public PortBus(BusPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    @Override
    public double read(int channel) {
        check(channel);
        return port.read(channel);
    }

    @Override
    public void write(int channel, double level) {
        check(channel);
        if (!(level >= 0 && level <= 1)) throw new IllegalArgumentException("a bus level is 0..1, got " + level);
        if ((port.claims() & (1 << channel)) == 0) port.claim(port.claims() | 1 << channel);
        port.write(channel, level);
    }

    private static void check(int channel) {
        if (!BusChannel.valid(channel))
            throw new IllegalArgumentException("bus channels are 0..15 (dye order), got " + channel);
    }
}
