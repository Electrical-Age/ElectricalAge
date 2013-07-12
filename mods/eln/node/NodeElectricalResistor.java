package mods.eln.node;

import mods.eln.INBTTReady;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistor;
import net.minecraft.nbt.NBTTagCompound;

public class NodeElectricalResistor extends ElectricalResistor implements INBTTReady{

	String name;
	public NodeElectricalResistor(String name,ElectricalLoad a, ElectricalLoad b) {
		super(a, b);
		this.name = name;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		setR(nbt.getDouble(str + name + "R"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setDouble(str + name + "R", R);
	}


}
