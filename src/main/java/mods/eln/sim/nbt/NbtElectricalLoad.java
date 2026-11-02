package mods.eln.sim.nbt;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.ElectricalLoad;
import net.minecraft.nbt.CompoundTag;

public class NbtElectricalLoad extends ElectricalLoad implements INBTTReady {

    String name;
    /** The start of its NBT keys: name, unless given apart. */
    private final String nbtName;

    /** The load's name: introspect names ports and loads by it. Its NBT keys start with it unless given apart. */
    public String name() {
        return name;
    }

    public NbtElectricalLoad(String name) {
        this(name, name);
    }

    /** Named name, saved under keys that start with nbtName (stable keys behind a name that may change). */
    public NbtElectricalLoad(String name, String nbtName) {
        super();
        this.name = name;
        this.nbtName = nbtName;
    }

    public void readFromNBT(CompoundTag nbttagcompound, String str) {
        setU(nbttagcompound.getFloatOr(str + nbtName + "Uc", 0f));
        if (Double.isNaN(getU())) setU(0);
        if (getU() == Float.NEGATIVE_INFINITY) setU(0);
        if (getU() == Float.POSITIVE_INFINITY) setU(0);
    }

    public void writeToNBT(CompoundTag nbttagcompound, String str) {
        nbttagcompound.putFloat(str + nbtName + "Uc", (float) getU());
    }
}
