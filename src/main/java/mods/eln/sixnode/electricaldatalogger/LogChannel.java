package mods.eln.sixnode.electricaldatalogger;

import net.minecraft.nbt.CompoundTag;

/**
 * How a logger channel is shown (board #45): the signal's 0..1 spans min..max in a unit. Only the drawing uses it, so
 * changing it re-scales the history instead of clearing it.
 */
public final class LogChannel {
    public float min = 0, max = 100;
    public byte unit = DataLogs.percentType;

    public LogChannel() {
    }

    public LogChannel(float min, float max, byte unit) {
        this.min = min;
        this.max = max;
        this.unit = unit;
    }

    /** The shown value of a signal fraction. */
    public float value(float fraction) {
        return min + fraction * (max - min);
    }

    /** Do both read on one axis (the same unit and range)? */
    public boolean sameScale(LogChannel o) {
        return unit == o.unit && min == o.min && max == o.max;
    }

    public void writeToNBT(CompoundTag nbt, String prefix) {
        nbt.putFloat(prefix + "min", min);
        nbt.putFloat(prefix + "max", max);
        nbt.putByte(prefix + "unit", unit);
    }

    public void readFromNBT(CompoundTag nbt, String prefix) {
        min = nbt.getFloatOr(prefix + "min", 0f);
        max = nbt.getFloatOr(prefix + "max", 100f);
        unit = nbt.getByteOr(prefix + "unit", DataLogs.percentType);
    }

    public void write(java.io.DataOutputStream o) throws java.io.IOException {
        o.writeByte(unit);
        o.writeFloat(min);
        o.writeFloat(max);
    }

    public void read(java.io.DataInputStream i) throws java.io.IOException {
        unit = i.readByte();
        min = i.readFloat();
        max = i.readFloat();
    }
}
