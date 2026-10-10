package mods.eln.sim.nbt;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.ElectricalLoad;
import net.minecraft.nbt.CompoundTag;

public class NbtElectricalLoad extends ElectricalLoad implements INBTTReady {

    String name;

    public NbtElectricalLoad(String name) {
        super();
        this.name = name;
    }

    public void readFromNBT(CompoundTag nbttagcompound, String str) {
        setU(nbttagcompound.getFloatOr(str + name + "Uc", 0f));
        if (Double.isNaN(getU())) setU(0);
        if (getU() == Float.NEGATIVE_INFINITY) setU(0);
        if (getU() == Float.POSITIVE_INFINITY) setU(0);
    }

    public void writeToNBT(CompoundTag nbttagcompound, String str) {
        nbttagcompound.putFloat(str + name + "Uc", (float) getU());
    }
}
