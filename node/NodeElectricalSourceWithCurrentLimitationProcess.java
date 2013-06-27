package mods.eln.node;

import mods.eln.INBTTReady;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalSourceWithCurrentLimitationProcess;
import net.minecraft.nbt.NBTTagCompound;

public class NodeElectricalSourceWithCurrentLimitationProcess extends ElectricalSourceWithCurrentLimitationProcess  implements INBTTReady{ 

	public NodeElectricalSourceWithCurrentLimitationProcess(
			String name,
			ElectricalLoad positiveLoad, ElectricalLoad negativeLoad, double U,
			double Imax) {
		super(positiveLoad, negativeLoad, U, Imax);
		this.name = name;
	}
	String name;
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		this.U = nbt.getFloat(str + name + "U");
		this.Imax = nbt.getFloat(str + name + "Imax");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		nbt.setFloat(str + name + "U", (float) U);
		nbt.setFloat(str + name + "Imax", (float) Imax);
	}

}
