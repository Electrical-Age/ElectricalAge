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
        combustibleEnergy = nbttagcompound.getDoubleOr(str + name + "Q", 0.0);   // (saves before 2026-11-03 hold a float)
        setGain(nbttagcompound.getDoubleOr(str + name + "gain", 0.0));
        // (saves from before the lag: the fire burns at its setting)
        if (nbttagcompound.contains(str + name + "fire")) fire.read(nbttagcompound, str + name);
        else fire.level = getGain();
    }

    @Override
    public void writeToNBT(CompoundTag nbttagcompound, String str) {
        nbttagcompound.putDouble(str + name + "Q", combustibleEnergy);   // (a float lost the fire bed's last joules: loading topped it up again)
        nbttagcompound.putDouble(str + name + "gain", getGain());
        fire.write(nbttagcompound, str + name);
    }
}
