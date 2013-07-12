package mods.eln.thermaldissipatorpassive;

import mods.eln.Eln;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;

public class ThermalDissipatorPassiveDescriptor extends TransparentNodeDescriptor implements ITemperatureWatchdogDescriptor ,IThermalDestructorDescriptor{
	public double thermalRs,thermalRp,thermalC;
	public ThermalDissipatorPassiveDescriptor(
			String name,
			double warmLimit,double coolLimit,
			double nominalP,double nominalT,
			double nominalTao,double nominalConnectionDrop
			) {
		super(name, ThermalDissipatorPassiveElement.class, ThermalDissipatorPassiveRender.class);
		thermalC = nominalP * nominalTao / nominalT;
		thermalRp = nominalT / nominalP;
		thermalRs = nominalConnectionDrop / nominalP;
		this.coolLimit = coolLimit;
		this.warmLimit = warmLimit;
		Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC);
	}
	double warmLimit,coolLimit;
	
	public void applyTo(ThermalLoad load)
	{
		load.set(thermalRs, thermalRp, thermalC);
	}


	@Override
	public double getThermalDestructionMax() {
		// TODO Auto-generated method stub
		return 2;
	}


	@Override
	public double getThermalDestructionStart() {
		// TODO Auto-generated method stub
		return 1;
	}


	@Override
	public double getThermalDestructionPerOverflow() {
		// TODO Auto-generated method stub
		return 0.1;
	}


	@Override
	public double getThermalDestructionProbabilityPerOverflow() {
		// TODO Auto-generated method stub
		return 0.05;
	}


	@Override
	public double getTmax() {
		// TODO Auto-generated method stub
		return warmLimit;
	}


	@Override
	public double getTmin() {
		// TODO Auto-generated method stub
		return coolLimit;
	}
}
