package mods.eln.node;

import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadWatchdogListener;
import mods.eln.sim.ElectricalLoadWatchdogProcess;

public class TransparentNodeElectricalLoadWatchdog extends ElectricalLoadWatchdogProcess{
	TransparentNodeElement element;
	float explosionStrength;
	public TransparentNodeElectricalLoadWatchdog(TransparentNodeElement element,ElectricalLoad load,float explosionStrength) {
		super(load,null);
		this.element = element;
		this.explosionStrength = explosionStrength;

	}
	@Override
	public void overVoltage(double time, double overflow) {
		element.node.physicalSelfDestruction(explosionStrength);
	}

	@Override
	public void underVoltage(double time, double overflow) {
		element.node.physicalSelfDestruction(explosionStrength);
	}

	
	
}
