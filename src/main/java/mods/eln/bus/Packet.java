package mods.eln.bus;

import java.nio.ByteBuffer;

/**
 * One bus packet, [target][source][type][payload] (Baughn: an RS485 bus). Target and source are bytes: target 0 is a
 * broadcast, 1..16 the colour channels (dye ids 0..15), 17..255 devices; source is the sender's address, which devices
 * fill in themselves (a computer's raw API may set any). The payload is an index byte and/or a float, as the type says
 * (PacketType); the other field is 0. A data packet carries bytes (up to MAX_DATA) instead of a float.
 */
public record Packet(int target, int source, PacketType type, int index, float value, byte[] data) {
    public static final int BROADCAST = 0;
    /** The most bytes a data packet carries. */
    public static final int MAX_DATA = 64;
    private static final byte[] NO_DATA = new byte[0];
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
        if (!type.hasData || data == null) data = NO_DATA;
        else data = java.util.Arrays.copyOf(data, Math.min(data.length, MAX_DATA));
    }

    public Packet(int target, int source, PacketType type, int index, float value) {
        this(target, source, type, index, value, null);
    }

    /** A data packet. */
    public static Packet data(int target, int source, int index, byte[] data) {
        return new Packet(target, source, PacketType.DATA, index, 0, data);
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    /** The data's length (0 unless a data packet). */
    public int dataLength() {
        return data.length;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Packet p && p.target == target && p.source == source && p.type == type && p.index == index
            && Float.compare(p.value, value) == 0 && java.util.Arrays.equals(p.data, data);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(target, source, type, index, value, java.util.Arrays.hashCode(data));
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

    /**
     * The wire form: target, source, type, then the index byte, the big-endian float, and the data's length byte and
     * bytes, as the type has them.
     */
    public byte[] encode() {
        ByteBuffer b = ByteBuffer.allocate(3 + type.payloadLength() + data.length);
        b.put((byte) target).put((byte) source).put((byte) type.id);
        if (type.hasIndex) b.put((byte) index);
        if (type.hasValue) b.putFloat(value);
        if (type.hasData) b.put((byte) data.length).put(data);
        return b.array();
    }

    /** A packet from its wire form, or null: too short, an unknown type, or the wrong length for its type. */
    public static Packet decode(byte[] raw) {
        if (raw == null || raw.length < 3) return null;
        PacketType t = PacketType.byId(raw[2] & 0xFF);
        if (t == null || raw.length < 3 + t.payloadLength()) return null;
        ByteBuffer b = ByteBuffer.wrap(raw, 3, raw.length - 3);
        int index = t.hasIndex ? b.get() & 0xFF : 0;
        float value = t.hasValue ? b.getFloat() : 0;
        byte[] data = null;
        if (t.hasData) {
            int n = b.get() & 0xFF;
            if (n > MAX_DATA || b.remaining() != n) return null;
            data = new byte[n];
            b.get(data);
        } else if (b.hasRemaining()) return null;
        return new Packet(raw[0] & 0xFF, raw[1] & 0xFF, t, index, value, data);
    }

    @Override
    public String toString() {
        return target + "<-" + source + " " + type.label() + (type.hasIndex ? " " + index : "") + (type.hasValue ? " " + value : "")
            + (type.hasData ? " " + data.length + " bytes" : "");
    }
}
