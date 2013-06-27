package mods.eln.item.regulator;

import mods.eln.sim.RegulatorProcess;
import mods.eln.sim.RegulatorType;

public class RegulatorOnOffDescriptor extends IRegulatorDescriptor{
	double hysteresis;

	public RegulatorOnOffDescriptor(
			String name,
			double hysteresis
			) {
		super( name);
		this.hysteresis = hysteresis;
	}

	@Override
	public RegulatorType getType() {
		// TODO Auto-generated method stub
		return RegulatorType.onOff;
	}

	@Override
	public void applyTo(RegulatorProcess regulator,double workingPoint) {
		// TODO Auto-generated method stub
		regulator.setOnOff(hysteresis,workingPoint);
	}

}
