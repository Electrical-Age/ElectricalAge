package mods.eln.misc;

import net.minecraft.nbt.NBTTagCompound;

public interface INBTTReady {
    public abstract void readFromNBT(NBTTagCompound nbt, String str);

    public abstract void writeToNBT(NBTTagCompound nbt, String str);
}
