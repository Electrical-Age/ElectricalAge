package mods.eln.sixnode.electricaldatalogger;

import mods.eln.gl.GL11;

import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.sim.PhysicalConstant;
import net.minecraft.nbt.CompoundTag;

public class DataLogs implements INBTTReady {

    byte[] log;
    int sizeMax, size;

    float samplingPeriod = 0.5f;
    float maxValue = 100f, minValue = 0f;
    byte unitType = percentType;
    static final byte voltageType = 0, currentType = 1, powerType = 2, celsiusType = 3, percentType = 4, energyType = 5;

    public DataLogs(int sizeMax) {
        log = new byte[sizeMax];
        this.sizeMax = sizeMax;
        size = 0;
    }

    void write(byte data) {
        int idx;
        if (size != sizeMax) {
            size++;
        }
        if (size != sizeMax)
            idx = size;
        else
            idx = size - 1;

        while (idx > 0) {
            log[idx] = log[idx - 1];
            idx--;
        }
        log[0] = data;
    }

    void reset() {
        size = 0;
    }

    int size() {
        return size;
    }

    byte read(int idx) {
        return log[idx];
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        byte[] cpy = nbt.getByteArray(str + "log").orElse(new byte[0]);
        Utils.println("Datalog readnbt " + cpy.length);
        for (int idx = 0; idx < cpy.length; idx++) {
            write(cpy[cpy.length - 1 - idx]);
        }

        samplingPeriod = nbt.getFloatOr(str + "samplingPeriod", 0f);
        maxValue = nbt.getFloatOr(str + "maxValue", 0f);
        minValue = nbt.getFloatOr(str + "minValue", 0f);
        unitType = nbt.getByteOr(str + "unitType", (byte) 0);
        Utils.println("Datalog readnbt done");
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putByteArray(str + "log", copyLog());
        nbt.putFloat(str + "samplingPeriod", samplingPeriod);
        nbt.putFloat(str + "maxValue", maxValue);
        nbt.putFloat(str + "minValue", minValue);
        nbt.putByte(str + "unitType", unitType);
    }

    public byte[] copyLog() {
        byte[] cpy = new byte[size];
        for (int idx = 0; idx < size; idx++) {
            cpy[idx] = read(idx);
        }
        return cpy;
    }

    @Override
    public String toString() {
        String str = "";
        for (int idx = 0; idx < size; idx++) {
            str += ((int) read(idx) + 128) + " ";
        }
        return str;
    }
// 26.x port: draw() removed (client rendering; redone in P3)
    // 26.x port: draw() removed (client rendering; redone in P3)
    public static String getYstring(float factor, float maxValue, float minValue, byte unitType) {
        String str = "";

        switch (unitType) {
            case celsiusType:
                str = Utils.plotCelsius("", factor * (maxValue - minValue) + minValue - PhysicalConstant.Tamb);
                break;
            case voltageType:
                str = Utils.plotVolt("", factor * (maxValue - minValue) + minValue);
                break;
            case currentType:
                str = Utils.plotAmpere("", factor * (maxValue - minValue) + minValue);
                break;
            case powerType:
                str = Utils.plotPower("", factor * (maxValue - minValue) + minValue);
                break;
            case percentType:
                str = Utils.plotPercent("", (factor * (maxValue - minValue) + minValue) * 0.01);
                break;
            case energyType:
                str = Utils.plotEnergy("", (factor * (maxValue - minValue) + minValue));
                break;
        }
        return str;
    }
    // 26.x port: draw() removed (client rendering; redone in P3)
}
