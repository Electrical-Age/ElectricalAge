package mods.eln.node;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.INBTTReady;
import mods.eln.sim.ElectricalInductor;
import mods.eln.sim.ElectricalLoad;

public class NodeElectricalInductor extends ElectricalInductor implements INBTTReady{

	private String name;

	public NodeElectricalInductor(ElectricalLoad positiveLoad,ElectricalLoad negativeLoad,String name) {
		super(positiveLoad, negativeLoad);
		this.name = name;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		setHenri(nbt.getDouble(str + name + "henri"));
		setCurrent(nbt.getDouble(str + name + "current"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setDouble(str + name + "henri",getHenri());
		nbt.setDouble(str + name + "current",getCurrent());
	}

}
