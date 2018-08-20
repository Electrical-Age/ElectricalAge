package mods.eln.sim.nbt;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.ElectricalLoad;
import net.minecraft.nbt.NBTTagCompound;

public class NbtElectricalLoad extends ElectricalLoad implements INBTTReady {

    String name;

    public NbtElectricalLoad(String name) {
        super();
        this.name = name;
    }

    public void readFromNBT(NBTTagCompound nbttagcompound, String str) {
        setU(nbttagcompound.getFloat(str + name + "Uc"));
        if (Double.isNaN(getU())) setU(0);
        if (getU() == Float.NEGATIVE_INFINITY) setU(0);
        if (getU() == Float.POSITIVE_INFINITY) setU(0);
    }

    public void writeToNBT(NBTTagCompound nbttagcompound, String str) {
        nbttagcompound.setFloat(str + name + "Uc", (float) getU());
    }
}
