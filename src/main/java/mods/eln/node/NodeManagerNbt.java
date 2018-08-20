package mods.eln.node;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

public class NodeManagerNbt extends WorldSavedData {
    public NodeManagerNbt(String par1Str) {
        super(par1Str);
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        NodeManager.instance.loadFromNbt(nbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        //NodeManager.instance.saveToNbt(nbt, Integer.MIN_VALUE);
    }
}
