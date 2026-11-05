package mods.eln.bus;

import java.nio.ByteBuffer;

/**
 * One bus packet, [target][source][type][payload] (Baughn: an RS485 bus). Target and source are bytes: target 0 is a
 * broadcast, 1..16 the colour channels (dye ids 0..15), 17..255 devices; source is the sender's address, which devices
 * fill in themselves (a computer's raw API may set any). The payload is an index byte and/or a float, as the type says
 * (PacketType); the other field is 0.
 */
public record Packet(int target, int source, PacketType type, int index, float value) {
    public static final int BROADCAST = 0;
    /** The lowest device address (1..16 are the colour channels). */
    public static final int FIRST_DEVICE = BusChannel.COUNT + 1;
    public static final int MAX_ADDRESS = 255;

    public Packet {
        if (type == null) throw new IllegalArgumentException("no packet type");
        target &= 0xFF;
        source &= 0xFF;
        index = type.hasIndex ? index & 0xFF : 0;
        if (!type.hasValue || !Float.isFinite(value)) value = 0;
        else if (type == PacketType.SIGNAL || type == PacketType.POWER) value = Math.max(0, Math.min(1, value));
    }

    /** The target for colour channel c (dye id 0..15). */
    public static int channelTarget(int c) {
        return c + 1;
    }

    /** The colour channel this packet goes to, or BusChannel.UNUSED. */
    public int channel() {
        return target >= 1 && target <= BusChannel.COUNT ? target - 1 : BusChannel.UNUSED;
    }

    /** Is this a valid device address (17..255)? */
    public static boolean deviceAddress(int a) {
        return a >= FIRST_DEVICE && a <= MAX_ADDRESS;
    }

    public static Packet signal(int source, int channel, double level) {
        return new Packet(channelTarget(channel), source, PacketType.SIGNAL, 0, (float) level);
    }

    /** The wire form: target, source, type, then the index byte and the big-endian float, as the type has them. */
    public byte[] encode() {
        ByteBuffer b = ByteBuffer.allocate(3 + type.payloadLength());
        b.put((byte) target).put((byte) source).put((byte) type.id);
        if (type.hasIndex) b.put((byte) index);
        if (type.hasValue) b.putFloat(value);
        return b.array();
    }

    /** A packet from its wire form, or null: too short, an unknown type, or the wrong length for its type. */
    public static Packet decode(byte[] raw) {
        if (raw == null || raw.length < 3) return null;
        PacketType t = PacketType.byId(raw[2] & 0xFF);
        if (t == null || raw.length != 3 + t.payloadLength()) return null;
        ByteBuffer b = ByteBuffer.wrap(raw, 3, raw.length - 3);
        int index = t.hasIndex ? b.get() & 0xFF : 0;
        float value = t.hasValue ? b.getFloat() : 0;
        return new Packet(raw[0] & 0xFF, raw[1] & 0xFF, t, index, value);
    }

    @Override
    public String toString() {
        return target + "<-" + source + " " + type.label() + (type.hasIndex ? " " + index : "") + (type.hasValue ? " " + value : "");
    }
}
