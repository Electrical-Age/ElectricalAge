package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.primitives.Capacitance;
import mods.eln.sim.mna.primitives.Conductance;
import mods.eln.sim.mna.primitives.Current;
import mods.eln.sim.mna.primitives.Energy;
import mods.eln.sim.mna.state.State;

public class Capacitor extends Bipole  implements ISubSystemProcessI {

	private Capacitance c = new Capacitance();

	public Capacitor() {
	}
	
	public Capacitor(State aPin,State bPin) {
		connectTo(aPin, bPin);
	}

	@Override
	public Current getCurrent() {
		return new Current();
	}

	public void setC(final Capacitance c) {
		this.c = c;
		dirty();
	}

	@Override
	public void applyTo(SubSystem s) {
	    Conductance cdt = c.divide(s.getDt());
		
		s.addToA(aPin, aPin, cdt.getValue());
		s.addToA(aPin, bPin, -cdt.getValue());
		s.addToA(bPin, bPin, cdt.getValue());
		s.addToA(bPin, aPin, -cdt.getValue());
	}
	
	@Override
	public void simProcessI(SubSystem s) {
		Conductance cdt = c.divide(s.getDt());
		double add = s.getXSafe(aPin).substract(s.getXSafe(bPin)).multiply(cdt);
		s.addToI(aPin, add);
		s.addToI(bPin, -add);
	}
	
	@Override
	public void quitSubSystem() {
		subSystem.removeProcess(this);
		super.quitSubSystem();
	}

	@Override
	public void addedTo(SubSystem s) {
		super.addedTo(s);
		s.addProcess(this);
	}

	public Energy getE() {
		return new Energy(getU(), c);
	}

	public Capacitance getC() {
		return c;
	}
}
