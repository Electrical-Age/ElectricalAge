package mods.eln.sim;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.state.VoltageState;
import net.minecraft.nbt.CompoundTag;

public class NodeVoltageState extends VoltageState implements INBTTReady {

    String name;

    public NodeVoltageState(String name) {
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
