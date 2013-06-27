package mods.eln.electricasensor;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.sim.IProcess;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalSensorProcess implements IProcess{
	ElectricalSensorElement sensor;
	
	public ElectricalSensorProcess(ElectricalSensorElement sensor) {
		this.sensor = sensor;
	}

	@Override
	public void process(double time) {
		if(sensor.typeOfSensor == sensor.voltageType)
		{
			setOutput(sensor.electricalLoad.Uc);
		}
		else if(sensor.typeOfSensor == sensor.currantType)
		{
			setOutput(sensor.electricalLoad.getCurrent());
		}
		else if(sensor.typeOfSensor == sensor.powerType)
		{
			setOutput(sensor.electricalLoad.Irs * 0.5 * sensor.electricalLoad.Uc);
		}
		 
	}

	
	void setOutput(double physical)
	{
		double U = (physical - sensor.lowValue) / (sensor.highValue - sensor.lowValue) * Eln.SVU;
		if(U > Eln.SVU) U = Eln.SVU;
		if(U < 0) U = 0;
		sensor.outputGateProcess.U = U;
	}
}
