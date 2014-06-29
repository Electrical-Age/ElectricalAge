package mods.eln.node;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.INBTTReady;
import mods.eln.sim.mna.component.ResistorSwitch;
import mods.eln.sim.mna.state.State;

public class NodeResistorSwitch extends ResistorSwitch implements INBTTReady{
	
	String name;
	public NodeResistorSwitch(String name) {
		this.name = name;
	}
	
	public NodeResistorSwitch(State aPin,State bPin,String name) {
		super(aPin, bPin);
		this.name = name;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		setR(nbt.getDouble(str + "r"));
		setState(nbt.getBoolean(str + "state"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		nbt.setDouble(str + "r",baseR);
		nbt.setBoolean(str + "state",getState());		
	}


}
