package mods.eln.sim.nbt;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;

public class NbtThermalLoad extends ThermalLoad implements INBTTReady {

    String name;

    public NbtThermalLoad(String name, double Tc, double Rp, double Rs, double C) {
        super(Tc, Rp, Rs, C);
        this.name = name;
    }

    public NbtThermalLoad(String name) {
        super();
        this.name = name;
    }

    public void readFromNBT(CompoundTag nbttagcompound, String str) {
        Tc = nbttagcompound.getFloatOr(str + name + "Tc", 0f);
        if (Double.isNaN(Tc)) Tc = 0;
        if (Tc == Float.NEGATIVE_INFINITY) Tc = 0;
        if (Tc == Float.POSITIVE_INFINITY) Tc = 0;
    }

    public void writeToNBT(CompoundTag nbttagcompound, String str) {
        nbttagcompound.putFloat(str + name + "Tc", (float) Tc);
    }
}
