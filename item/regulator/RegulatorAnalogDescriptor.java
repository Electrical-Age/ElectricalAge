package mods.eln.item.regulator;

import mods.eln.sim.RegulatorProcess;
import mods.eln.sim.RegulatorType;

public class RegulatorAnalogDescriptor extends IRegulatorDescriptor{
	//double P,I,D;

	public RegulatorAnalogDescriptor(
			String name,String iconName/*,
			double P,double I,double D*/
			) {
		super(name);
		changeDefaultIcon(iconName);
		/*this.P = P;
		this.D = D;
		this.I = I;*/
	}

	@Override
	public RegulatorType getType() {
		// TODO Auto-generated method stub
		return RegulatorType.analog;
	}

	@Override
	public void applyTo(RegulatorProcess regulator,double workingPoint,double P,double I,double D) {
		// TODO Auto-generated method stub
		regulator.setAnalog(P, I, D,workingPoint);
	}

}
