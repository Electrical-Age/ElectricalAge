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
		s.addToI(pin, pinI);
		doubleBuffer = (doubleBuffer + 1 ) & 1;
		other.oldIother[doubleBuffer]= -pinI;
	}

	@Override
	public State[] getConnectedStates() {
		// TODO Auto-generated method stub
		return new State[]{};
	}

	public void setInitialCurrent(double i) {
		oldIother[doubleBuffer] = i;
	}

}
