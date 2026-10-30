package mods.eln.sixnode.electricaldatalogger;

import net.minecraft.nbt.CompoundTag;

/**
 * How a logger channel is shown (board #45): the signal's 0..1 spans min..max in a unit, free text (Baughn 2026-10-30;
 * 1.12 had six fixed units). Only the drawing uses it, so changing it re-scales the history instead of clearing it.
 */
public final class LogChannel {
    public float min = 0, max = 100;
    /** What the device calls the channel (A, B, C, out), for legends; set by the device, not saved. */
    public String name = "";
    public String unit = "%";
    /** A unit's longest. */
    public static final int UNIT_CHARS = 10;
    /** 1.12's units, by their number: voltage, current, power, temperature, percent, energy. */
    static final String[] OLD_UNITS = {"V", "A", "W", "\u00B0C", "%", "J"};

    public LogChannel() {
    }

    public LogChannel(float min, float max, String unit) {
        this.min = min;
        this.max = max;
        this.unit = sanitize(unit);
    }

    /** A 1.12 unit number as text. */
    public static String oldUnit(byte type) {
        return type >= 0 && type < OLD_UNITS.length ? OLD_UNITS[type] : "";
    }

    /** A unit as typed, made safe to keep and draw: printable, no formatting codes, trimmed, at most UNIT_CHARS. */
    public static String sanitize(String u) {
        if (u == null) return "";
        StringBuilder b = new StringBuilder();
        u.codePoints().filter(c -> c >= 0x20 && c != 0x7F && c != 0xA7 && !Character.isISOControl(c)).forEach(b::appendCodePoint);
        String t = b.toString().strip();
        return t.length() > UNIT_CHARS ? t.substring(0, t.offsetByCodePoints(0, Math.min(UNIT_CHARS, t.codePointCount(0, t.length())))) : t;
    }

    /** The shown value of a signal fraction. */
    public float value(float fraction) {
        return min + fraction * (max - min);
    }

    /** Do both read on one axis (the same unit and range)? */
    public boolean sameScale(LogChannel o) {
        return unit.equals(o.unit) && min == o.min && max == o.max;
    }

    public void writeToNBT(CompoundTag nbt, String prefix) {
        nbt.putFloat(prefix + "min", min);
        nbt.putFloat(prefix + "max", max);
        nbt.putString(prefix + "unitName", unit);
    }

    public void readFromNBT(CompoundTag nbt, String prefix) {
        min = nbt.getFloatOr(prefix + "min", 0f);
        max = nbt.getFloatOr(prefix + "max", 100f);
        // (a save from before free-text units has the number of one of 1.12's six)
        unit = nbt.contains(prefix + "unitName") ? sanitize(nbt.getStringOr(prefix + "unitName", "")) : oldUnit(nbt.getByteOr(prefix + "unit", DataLogs.percentType));
    }

    public void write(java.io.DataOutputStream o) throws java.io.IOException {
        o.writeUTF(unit);
        o.writeFloat(min);
        o.writeFloat(max);
    }

    public void read(java.io.DataInputStream i) throws java.io.IOException {
        unit = sanitize(i.readUTF());
        min = i.readFloat();
        max = i.readFloat();
    }
}
