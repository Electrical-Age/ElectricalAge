package mods.eln.node;

import mods.eln.INBTTReady;
import mods.eln.misc.FunctionTable;
import mods.eln.sim.BatteryProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import net.minecraft.nbt.NBTTagCompound;

public class NodeBatteryProcess extends BatteryProcess implements INBTTReady {

	public NodeBatteryProcess(ElectricalLoad positiveLoad,ElectricalLoad negativeLoad,FunctionTable voltageFunction,double IMax ,VoltageSource voltageSource) {
		super(positiveLoad,negativeLoad,voltageFunction,IMax,voltageSource);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound, String str) {
        Q = nbttagcompound.getDouble(str + "NBP" + "Q");
        life = nbttagcompound.getDouble(str + "NBP" + "life");
        setCut(nbttagcompound.getBoolean(str + "NBP" + "cut"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound, String str) {
	       nbttagcompound.setDouble(str + "NBP" + "Q", Q);
	       nbttagcompound.setDouble(str + "NBP" + "life", life);
	       nbttagcompound.setBoolean(str + "NBP" + "cut", cut);
	}

	public void setIMax(double iMax) {
		this.IMax = iMax;
	}

}
