package mods.eln.item.regulator;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.GenericItemUsingDamageDescriptorUpgrade;
import mods.eln.sim.RegulatorProcess;
import mods.eln.sim.RegulatorType;

public abstract class IRegulatorDescriptor extends GenericItemUsingDamageDescriptorUpgrade{
	
	
	public IRegulatorDescriptor(String name) {
		super(name);
		
	}

	public abstract RegulatorType getType();
	public abstract void applyTo(RegulatorProcess regulator,double workingPoint,double P,double I,double D);


}
