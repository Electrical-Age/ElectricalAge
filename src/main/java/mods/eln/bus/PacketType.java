package mods.eln.bus;

/**
 * The bus's packet types (Baughn: an RS485 bus). Each type's payload has a fixed layout, so players can't get lengths
 * wrong: an index byte (a button, register, fault code or sensor), a float, or both. Types are added here, never by
 * players; a packet of a type this version doesn't know is dropped when decoded.
 */
public enum PacketType {
    /** To a colour channel (target 1..16): set its level, 0..1. */
    SIGNAL(1, false, true),
    /** A button was pressed: its index (0 for a single button), broadcast with its address as source. */
    PRESS(2, true, false),
    /** Set a device's output level, 0..1 (a light's brightness). */
    POWER(3, false, true),
    /** Set a device register: register, value. */
    WRITE(4, true, true),
    /** Ask for a register; the device sends REPLY to the source. */
    READ(5, true, false),
    /** The answer to READ: register, value. */
    REPLY(6, true, true),
    /** A device reports a fault: its code, broadcast. */
    FAULT(7, true, false),
    /** A floor sensor saw the car arrive: its index, broadcast. */
    ARRIVE(8, true, false);

    public final int id;
    public final boolean hasIndex, hasValue;

    PacketType(int id, boolean hasIndex, boolean hasValue) {
        this.id = id;
        this.hasIndex = hasIndex;
        this.hasValue = hasValue;
    }

    /** Payload bytes: 1 for the index, 4 for the float. */
    public int payloadLength() {
        return (hasIndex ? 1 : 0) + (hasValue ? 4 : 0);
    }

    /** Its name as programs write it (press). */
    public String label() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    private static final PacketType[] BY_ID = new PacketType[256];

    static {
        for (PacketType t : values()) BY_ID[t.id] = t;
    }

    /** The type with this id, or null (unknown: dropped). */
    public static PacketType byId(int id) {
        return id >= 0 && id < 256 ? BY_ID[id] : null;
    }
}
