package mods.eln.node;

import mods.eln.INBTTReady;
import mods.eln.sim.FurnaceProcess;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.NBTTagCompound;

public class NodeFurnaceProcess extends FurnaceProcess implements INBTTReady{
	
	String name;
	public NodeFurnaceProcess(String name,ThermalLoad load) {
		super(load);
		this.name = name;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound, String str) {
		combustibleEnergy = nbttagcompound.getFloat(str + name + "Q");
		setGain(nbttagcompound.getDouble(str + name + "gain"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound, String str) {
        nbttagcompound.setFloat(str + name + "Q", (float)combustibleEnergy);
        nbttagcompound.setDouble(str + name + "gain", getGain());
	}

	
}
