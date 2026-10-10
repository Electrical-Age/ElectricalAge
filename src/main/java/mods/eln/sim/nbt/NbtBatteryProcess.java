package mods.eln.sim.nbt;

import mods.eln.misc.FunctionTable;
import mods.eln.misc.INBTTReady;
import mods.eln.sim.BatteryProcess;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.VoltageState;
import net.minecraft.nbt.CompoundTag;

public class NbtBatteryProcess extends BatteryProcess implements INBTTReady {

    public NbtBatteryProcess(VoltageState positiveLoad, VoltageState negativeLoad, FunctionTable voltageFunction, double IMax, VoltageSource voltageSource) {
        super(positiveLoad, negativeLoad, voltageFunction, IMax, voltageSource);
    }

    @Override
    public void readFromNBT(CompoundTag nbttagcompound, String str) {
        Q = nbttagcompound.getDoubleOr(str + "NBP" + "Q", 0.0);
        if (Double.isNaN(Q)) Q = 0;
        life = nbttagcompound.getDoubleOr(str + "NBP" + "life", 0.0);
        if (Double.isNaN(life)) life = 1;
    }

    @Override
    public void writeToNBT(CompoundTag nbttagcompound, String str) {
        nbttagcompound.putDouble(str + "NBP" + "Q", Q);
        nbttagcompound.putDouble(str + "NBP" + "life", life);
    }

    public void setIMax(double iMax) {
        this.IMax = iMax;
    }
}
