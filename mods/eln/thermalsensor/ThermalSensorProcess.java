package mods.eln.thermalsensor;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.sim.IProcess;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.nbt.NBTTagCompound;

public class ThermalSensorProcess implements IProcess{
	ThermalSensorElement sensor;
	
	public ThermalSensorProcess(ThermalSensorElement sensor) {
		this.sensor = sensor;
	}

	@Override
	public void process(double time) {
		if(sensor.typeOfSensor == sensor.temperatureType)
		{
			setOutput(sensor.thermalLoad.Tc);
		}
		else if(sensor.typeOfSensor == sensor.powerType)
		{
			setOutput(sensor.thermalLoad.getPower());
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
