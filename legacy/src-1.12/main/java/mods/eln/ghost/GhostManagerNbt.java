package mods.eln.ghost;

import mods.eln.Eln;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.storage.WorldSavedData;

public class GhostManagerNbt extends WorldSavedData {
    public GhostManagerNbt(String par1Str) {
        super(par1Str);
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        Eln.ghostManager.loadFromNBT(nbt);
    }

    @Override
    public CompoundTag writeToNBT(CompoundTag nbt) {
        //Eln.ghostManager.saveToNbt(nbt, Integer.MIN_VALUE);
        return nbt;
    }
}
