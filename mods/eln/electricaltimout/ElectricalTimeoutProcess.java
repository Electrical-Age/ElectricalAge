package mods.eln.electricaltimout;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.sim.IProcess;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalTimeoutProcess implements IProcess,INBTTReady{
	ElectricalTimeoutElement element;
	boolean inputState = false;
	public ElectricalTimeoutProcess(ElectricalTimeoutElement element) {
		this.element = element;
	}
	
	@Override
	public void process(double time) {
		boolean oldInputState = inputState;
		if(inputState) 
		{
			if(element.inputGate.stateLow()) inputState = false;
		}
		else
		{
			if(element.inputGate.stateHigh()) inputState = true;
		}
		
		if(inputState) {
			element.timeOutCounter = element.timeOutValue;
		}
		
		if(element.timeOutCounter != 0.0){
			element.outputGateProcess.state(true);
			if(inputState == false) element.timeOutCounter -= time;
			if(element.timeOutCounter < 0.0) element.timeOutCounter = 0.0;
		}
		else
		{
			element.outputGateProcess.state(false);
		}
		
		if(inputState != oldInputState) {
			element.needPublish();
		}
		 
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		inputState = nbt.getBoolean(str + "SProcinputState");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		nbt.setBoolean(str + "SProcinputState",inputState);
	}

}
