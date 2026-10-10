package mods.eln.sim;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.state.VoltageState;
import net.minecraft.nbt.NBTTagCompound;

public class NodeVoltageState extends VoltageState implements INBTTReady {

    String name;

    public NodeVoltageState(String name) {
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
