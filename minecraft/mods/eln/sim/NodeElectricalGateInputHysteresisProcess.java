package mods.eln.sim;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.node.NodeElectricalGateInput;

public abstract class NodeElectricalGateInputHysteresisProcess implements IProcess,INBTTReady{
	NodeElectricalGateInput gate;
	String name;

	
	public NodeElectricalGateInputHysteresisProcess(String name,NodeElectricalGateInput gate) {
		this.gate = gate;
		this.name = name;
	}
	
	protected abstract void setOutput(boolean value);
	@Override
	public void process(double time) {
		if(state)
		{
			if(gate.Uc < Eln.instance.SVU*0.2) 
			{
				state = false;
				setOutput(false);
			}
			else setOutput(true);
		}
		else
		{
			if(gate.Uc > Eln.instance.SVU*0.6) 
			{
				state = true;
				setOutput(true);
			}
			else setOutput(false);
				
		}
	}
	
	boolean state = false;
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		state = nbt.getBoolean(str + name +  "state");
	}
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		nbt.setBoolean(str + name +  "state",state);
	}

}
