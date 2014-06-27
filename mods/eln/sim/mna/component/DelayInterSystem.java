package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.state.State;

public class DelayInterSystem extends Component implements ISubSystemProcessI {

	private DelayInterSystem other;
	public State pin;

	public void set(State pin,DelayInterSystem other){
		this.other = other;
		this.pin = pin;
	}
	
	public DelayInterSystem set(double impedance) {
		this.impedance = impedance;
		this.conductance = 1 / impedance;
	
		return this;
	}

	
	@Override
	public void quitSubSystem() {
		subSystem.removeProcess(this);

		super.quitSubSystem();	
	}
	
	double impedance, conductance;

	@Override
	public void addedTo(SubSystem s) {
		super.addedTo(s);
		s.addProcess(this);
	}

	@Override
	public void applyTo(SubSystem s) {
		s.addToA(pin, pin, conductance);
	}

	public double[] oldIother = new double[] { 0, 0 };
	int doubleBuffer = 0;

	@Override
	public void simProcessI(SubSystem s) {
		double pinI = 2 * other.getSubSystem().getX(other.pin) * conductance + oldIother[doubleBuffer];		
		//pinI = (pinI*1 - other.oldIother[doubleBuffer]*0);
		s.addToI(pin, pinI);
		
		doubleBuffer = (doubleBuffer + 1 ) & 1;
		other.oldIother[doubleBuffer]= -pinI;
	}
	/*@Override
	public void simProcessI(SubSystem s) {
		double iA = pin.state*conductance + oldIother[doubleBuffer];
		double iB = other.pin.state*conductance + other.oldIother[doubleBuffer];
		double iTarget = (iA - iB)/2;
		
		double aPinI = iTarget - (pin.state + other.pin.state)*0.5*conductance;
		
		s.addToI(pin, -aPinI);
		
		
		doubleBuffer = (doubleBuffer + 1 ) & 1;
		oldIother[doubleBuffer]= aPinI;
	}*/
	
	/*
	@Override
	public void simProcessI(SubSystem s) {
		double iThis = pin.state*conductance + oldIother[doubleBuffer];
		double iOther = other.pin.state*conductance + other.oldIother[doubleBuffer];
		double iTarget = (iThis - iOther)/2;
		
		double pinI = 2 * other.getSubSystem().getX(other.pin) * conductance + oldIother[doubleBuffer];		
		//pinI = (pinI*1 - other.oldIother[doubleBuffer]*0);
		s.addToI(pin, pinI);
		
		doubleBuffer = (doubleBuffer + 1 ) & 1;
		other.oldIother[doubleBuffer]= -pinI;
	}*/

	@Override
	public State[] getConnectedStates() {
		// TODO Auto-generated method stub
		return new State[]{};
	}

	public void setInitialCurrent(double i) {
		oldIother[doubleBuffer] = i;
	}

}
