package mods.eln.sim.nbt;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.FurnaceProcess;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;

public class NbtFurnaceProcess extends FurnaceProcess implements INBTTReady {

    String name;

    public NbtFurnaceProcess(String name, ThermalLoad load) {
        super(load);
        this.name = name;
    }

    @Override
    public void readFromNBT(CompoundTag nbttagcompound, String str) {
        combustibleEnergy = nbttagcompound.getFloatOr(str + name + "Q", 0f);
        setGain(nbttagcompound.getDoubleOr(str + name + "gain", 0.0));
    }

    @Override
    public void writeToNBT(CompoundTag nbttagcompound, String str) {
        nbttagcompound.putFloat(str + name + "Q", (float) combustibleEnergy);
        nbttagcompound.putDouble(str + name + "gain", getGain());
    }
}
