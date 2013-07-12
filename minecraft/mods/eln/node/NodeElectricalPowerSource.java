package mods.eln.node;

import mods.eln.INBTTReady;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalPowerSource;
import net.minecraft.nbt.NBTTagCompound;

public class NodeElectricalPowerSource extends ElectricalPowerSource implements INBTTReady{

	String name;
	public NodeElectricalPowerSource(String name,ElectricalLoad positiveLoad,
			ElectricalLoad negativeLoad) {
		super(positiveLoad, negativeLoad);
		this.name = name;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		setP(nbt.getDouble(str + name + "P"));
		setUmax(nbt.getDouble(str + name + "Umax"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		
		nbt.setDouble(str + name + "P", this.getP());
		nbt.setDouble(str + name + "Umax", this.getUmax());
	}

	

}
