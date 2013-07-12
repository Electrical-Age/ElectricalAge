package mods.eln.item;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.ITemperatureWatchdogDescriptorForInventory;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalResistor;


public class ThermalIsolatorElement extends GenericItemUsingDamageDescriptor implements ITemperatureWatchdogDescriptorForInventory{
	

	
	public double conductionFactor,Tmax;

	public ThermalIsolatorElement(String name,
								  double isolationFactor,
								  double Tmax
								//double thermalNominalT,double thermalNominalP
								) {
		super(name);
		
		this.Tmax = Tmax;
		this.conductionFactor = isolationFactor;

	}

	@Override
	public double getTmax() {
		// TODO Auto-generated method stub
		return Tmax;
	}

	@Override
	public double getTmin() {
		// TODO Auto-generated method stub
		return -0.2 * Tmax;
	}

	@Override
	public double getBreakPropPerKelvinOverflow() {
		// TODO Auto-generated method stub
		return 0.2;
	}
	
	
	
/*
	public void applyTo(ThermalLoad resistor)
	{
		resistor.setRp(thermalRp);
	}*/
}
