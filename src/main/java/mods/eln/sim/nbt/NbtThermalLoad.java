package mods.eln.sim.nbt;

import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;

public class NbtThermalLoad extends ThermalLoad implements INBTTReady {

    String name;

    /** The name its NBT keys start with (introspect reports name loads by it). */
    public String name() {
        return name;
    }

    public NbtThermalLoad(String name, double Tc, double Rp, double Rs, double C) {
        super(Tc, Rp, Rs, C);
        this.name = name;
    }

    public NbtThermalLoad(String name) {
        super();
        this.name = name;
    }

    public void readFromNBT(CompoundTag nbttagcompound, String str) {
        Tc = Utils.finiteOr(nbttagcompound.getFloatOr(str + name + "Tc", 0f), 0);
    }

    public void writeToNBT(CompoundTag nbttagcompound, String str) {
        nbttagcompound.putFloat(str + name + "Tc", (float) Tc);
    }
}
