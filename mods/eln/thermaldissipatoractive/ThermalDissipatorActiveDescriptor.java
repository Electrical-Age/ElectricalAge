package mods.eln.thermaldissipatoractive;

import mods.eln.Eln;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.thermaldissipatorpassive.ThermalDissipatorPassiveDescriptor;
import mods.eln.thermaldissipatorpassive.ThermalDissipatorPassiveElement;
import mods.eln.thermaldissipatorpassive.ThermalDissipatorPassiveRender;

public class ThermalDissipatorActiveDescriptor extends TransparentNodeDescriptor  implements ITemperatureWatchdogDescriptor ,IThermalDestructorDescriptor{
	
	
	
	public ThermalDissipatorActiveDescriptor(
			String name, 
			double nominalElectricalU,double electricalNominalP,
			double nominalElectricalCoolingPower,
			ElectricalCableDescriptor cableDescriptor,
			double warmLimit,double coolLimit, 
			double nominalP, double nominalT,
			double nominalTao, double nominalConnectionDrop
			) {
		super(name, ThermalDissipatorActiveElement.class, ThermalDissipatorActiveRender.class);
		this.cableDescriptor = cableDescriptor;
		this.electricalNominalP = electricalNominalP;
		electricalRp = nominalElectricalU*nominalElectricalU / electricalNominalP;
		electricalToThermalRp = nominalT / nominalElectricalCoolingPower;
		thermalC = (nominalP + nominalElectricalCoolingPower) * nominalTao / nominalT;
		thermalRp = nominalT / nominalP;
		thermalRs = nominalConnectionDrop / (nominalP + nominalElectricalCoolingPower);
		Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC);
		this.coolLimit = coolLimit;
		this.warmLimit = warmLimit;
	}
	double warmLimit, coolLimit;
	
	public void applyTo(ThermalLoad load)
	{
		load.set(thermalRs, thermalRp, thermalC);
	}
	
	
	public double thermalRs,thermalRp,thermalC;
	double electricalRp;
	double electricalToThermalRp;
	public double electricalNominalP;
	ElectricalCableDescriptor cableDescriptor;
	public void applyTo(ElectricalLoad load)
	{
		cableDescriptor.applyTo(load, false);
		load.setRp(electricalRp);
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
