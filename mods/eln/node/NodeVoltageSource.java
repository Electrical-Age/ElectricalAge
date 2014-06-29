package mods.eln.node;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.INBTTReady;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.State;

public class NodeVoltageSource extends VoltageSource implements INBTTReady{
	String name;

	public NodeVoltageSource(String name)
	{
		super();
		this.name = name;
	}
	public NodeVoltageSource(State aPin,State bPin,String name) {
		super(aPin, bPin);
		this.name = name;
	}
	
		
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		setU(nbt.getDouble(str + "U"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setDouble(str + "U",getU());
	}

}
