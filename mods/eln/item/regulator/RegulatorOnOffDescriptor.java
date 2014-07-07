package mods.eln.item.regulator;

import mods.eln.sim.RegulatorProcess;
import mods.eln.sim.RegulatorType;

public class RegulatorOnOffDescriptor extends IRegulatorDescriptor{
	double hysteresis;

	public RegulatorOnOffDescriptor(
			String name,String iconName,
			double hysteresis
			) {
		super( name);
		changeDefaultIcon(iconName);
		this.hysteresis = hysteresis;
	}

	@Override
	public RegulatorType getType() {
		
		return RegulatorType.onOff;
	}

	@Override
	public void applyTo(RegulatorProcess regulator,double workingPoint,double P,double I,double D) {
		
		regulator.setOnOff(hysteresis,workingPoint);
	}

}
