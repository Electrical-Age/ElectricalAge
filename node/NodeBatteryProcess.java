package mods.eln.node;

import mods.eln.INBTTReady;
import mods.eln.misc.FunctionTable;
import mods.eln.sim.BatteryProcess;
import mods.eln.sim.ElectricalLoad;
import net.minecraft.nbt.NBTTagCompound;

public class NodeBatteryProcess extends BatteryProcess implements INBTTReady {

	public NodeBatteryProcess(ElectricalLoad positiveLoad,ElectricalLoad negativeLoad,FunctionTable voltageFunction ) {
		super(positiveLoad,negativeLoad,voltageFunction);
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

}
