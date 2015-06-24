package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.primitives.Conductance;
import mods.eln.sim.mna.primitives.Current;
import mods.eln.sim.mna.primitives.Resistance;
import mods.eln.sim.mna.primitives.Voltage;

public class Delay extends Bipole implements ISubSystemProcessI {
	Resistance impedance;
	Conductance conductance;

    Current oldIa, oldIb;

	public Delay set(Resistance impedance) {
		this.impedance = impedance;
		this.conductance = impedance.invert();
		return this;
	}

	@Override
	public void addedTo(SubSystem s) {
		super.addedTo(s);
		s.addProcess(this);
	}
	
	@Override
	public void applyTo(SubSystem s) {
		s.addToA(aPin, aPin, conductance.getValue());
		s.addToA(bPin, bPin, conductance.getValue());
	}

	/*@Override
	public void simProcessI(SubSystem s) {
		double aPinI = 2 * s.getX(bPin) * conductance + oldIb;
		double bPinI = 2 * s.getX(aPin) * conductance + oldIa;
		
		s.addToI(aPin, aPinI);
		s.addToI(bPin, bPinI);
		
		oldIa = -aPinI;
		oldIb = -bPinI;
	}*/

	@Override
	public void simProcessI(SubSystem s) {
		Current iA = new Voltage(aPin.state).multiply(conductance).add(oldIa);
		Current iB = new Voltage(bPin.state).multiply(conductance).add(oldIb);
		Current iTarget = iA.substract(iB).multiply(0.5);
		
		Current aPinI = iTarget.substract(new Voltage(aPin.state).add(
				new Voltage(bPin.state)).multiply(0.5).multiply(conductance));
		Current bPinI = iTarget.add(new Voltage(aPin.state).add(new Voltage(bPin.state)
		        ).multiply(0.5).multiply(conductance)).opposite();
		
		s.addToI(aPin, -aPinI.getValue());
		s.addToI(bPin, -bPinI.getValue());
		
		oldIa = aPinI;
		oldIb = bPinI;
	}	

	@Override
	public Current getCurrent() {
		return oldIa.substract(oldIb);
	}
}
