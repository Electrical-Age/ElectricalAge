package mods.eln.sim.nbt;

import mods.eln.misc.FunctionTable;
import mods.eln.misc.INBTTReady;
import mods.eln.sim.BatteryProcess;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.VoltageState;
import net.minecraft.nbt.NBTTagCompound;

public class NbtBatteryProcess extends BatteryProcess implements INBTTReady {

    public NbtBatteryProcess(VoltageState positiveLoad, VoltageState negativeLoad, FunctionTable voltageFunction, double IMax, VoltageSource voltageSource) {
        super(positiveLoad, negativeLoad, voltageFunction, IMax, voltageSource);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound, String str) {
        Q = nbttagcompound.getDouble(str + "NBP" + "Q");
        if (Double.isNaN(Q)) Q = 0;
        life = nbttagcompound.getDouble(str + "NBP" + "life");
        if (Double.isNaN(life)) life = 1;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound, String str) {
        nbttagcompound.setDouble(str + "NBP" + "Q", Q);
        nbttagcompound.setDouble(str + "NBP" + "life", life);
        return nbttagcompound;
    }

    public void setIMax(double iMax) {
        this.IMax = iMax;
    }
}
