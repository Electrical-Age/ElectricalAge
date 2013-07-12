package mods.eln.electricalregulator;

import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.RegulatorType;

public class ElectricalRegulatorDescriptor extends SixNodeDescriptor{
	
	public ElectricalRegulatorDescriptor(String name) {
		super(name, ElectricalRegulatorElement.class, ElectricalRegulatorRender.class);
		
	}

	double outputGateUmax,outputGateUmin;
	double P,I,D,hysteresis;
	RegulatorType type;
}
