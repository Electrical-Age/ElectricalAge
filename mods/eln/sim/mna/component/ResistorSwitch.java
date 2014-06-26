package mods.eln.sim.mna.component;

import mods.eln.sim.mna.state.State;

public class ResistorSwitch extends Resistor {
	public ResistorSwitch() {
	}
	
	public ResistorSwitch(State aPin,State bPin) {
		super(aPin, bPin);
	}
	
	boolean state = false;
	
	public void setState(boolean state){
		this.state = state;
		setR(baseR);
	}
	
	double baseR = 1;
	@Override
	public Resistor setR(double r) {
		baseR = r;
		return super.setR(state ? r : 1000000000.0);
	}
	
}
