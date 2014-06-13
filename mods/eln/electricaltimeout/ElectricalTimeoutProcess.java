package mods.eln.electricaltimeout;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.sim.IProcess;
import mods.eln.sound.SoundCommand;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalTimeoutProcess implements IProcess, INBTTReady {
	ElectricalTimeoutElement element;
	boolean inputState = false;
	public ElectricalTimeoutProcess(ElectricalTimeoutElement element) {
		this.element = element;
	}
	
	int tickCounter = 0;
	@Override
	public void process(double time) {
		boolean oldInputState = inputState;
		if(inputState) {
			if(element.inputGate.stateLow()) inputState = false;
		}
		else {
			if(element.inputGate.stateHigh()) inputState = true;
		}
		
		if(inputState) {
			element.timeOutCounter = element.timeOutValue;
		}
		
		if(element.timeOutCounter != 0.0) {
			element.outputGateProcess.state(true);
			if(inputState == false) element.timeOutCounter -= time;
			if(element.timeOutCounter < 0.0) element.timeOutCounter = 0.0;
			
			if (inputState == false && ++tickCounter % 200 == 0) 
				element.play( new SoundCommand(element.descriptor.tickSound).mulVolume(element.descriptor.tickVolume, 1f));
		}
		else {
			element.outputGateProcess.state(false);
		}
		
		if(inputState != oldInputState) {
			element.needPublish();
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		inputState = nbt.getBoolean(str + "SProcinputState");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setBoolean(str + "SProcinputState",inputState);
	}
}
