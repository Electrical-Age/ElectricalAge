package mods.eln.node;

import net.minecraft.client.renderer.texture.TextureManager;
import mods.eln.sim.ElectricalLoadWatchdogProcess;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.ThermalLoad;


public class NodeThermalWatchdogProcessWithValue extends NodeThermalWatchdogProcess implements ITemperatureWatchdogDescriptor,IThermalDestructorDescriptor
{

	private double tMin;
	private double tMax;
	private double thermalDestructionProbabilityPerOverflow;
	private double thermalDestructionPerOverflow;
	private double termalDestructionStart;
	private double thermalDestructionMax;

	public NodeThermalWatchdogProcessWithValue(
			NodeBase node, ThermalLoad thermalLoad,
			double tMax,double strength) {
		super(node, null, null, thermalLoad);
		setParam(this,this);
		tMin = -100;

		
		
		this.tMax = tMax;
		thermalDestructionProbabilityPerOverflow = 0.1;
		thermalDestructionPerOverflow = 0;
		termalDestructionStart = strength;
		thermalDestructionMax = strength;
	}

	@Override
	public double getThermalDestructionMax() {
		// TODO Auto-generated method stub
		return thermalDestructionMax;
	}

	@Override
	public double getThermalDestructionStart() {
		// TODO Auto-generated method stub
		return termalDestructionStart;
	}

	@Override
	public double getThermalDestructionPerOverflow() {
		// TODO Auto-generated method stub
		return thermalDestructionPerOverflow;
	}

	@Override
	public double getThermalDestructionProbabilityPerOverflow() {
		// TODO Auto-generated method stub
		return thermalDestructionProbabilityPerOverflow;
	}

	@Override
	public double getTmax() {
		// TODO Auto-generated method stub
		return tMax;
	}

	@Override
	public double getTmin() {
		// TODO Auto-generated method stub
		return tMin;
	} 

	
}