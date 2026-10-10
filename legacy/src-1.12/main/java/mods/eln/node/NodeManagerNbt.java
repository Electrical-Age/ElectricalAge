package mods.eln.node;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.storage.WorldSavedData;

public class NodeManagerNbt extends WorldSavedData {
    public NodeManagerNbt(String par1Str) {
        super(par1Str);
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        NodeManager.instance.loadFromNbt(nbt);
    }

    @Override
    public CompoundTag writeToNBT(CompoundTag nbt) {
        //NodeManager.instance.saveToNbt(nbt, Integer.MIN_VALUE);
        return nbt;
    }
}
