package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.primitives.Conductance;
import mods.eln.sim.mna.primitives.Current;
import mods.eln.sim.mna.primitives.Resistance;
import mods.eln.sim.mna.primitives.Voltage;
import mods.eln.sim.mna.state.State;

public class DelayInterSystem extends Component implements ISubSystemProcessI {

	private DelayInterSystem other;
	public State pin;

    Resistance impedance;
    Conductance conductance;

    public Current[] oldIother = new Current[]{new Current(), new Current()};
    int doubleBuffer = 0;
    public boolean thevnaCalc = false;
    public double thenvaCurrent;
    public Resistance Rth;
    public Voltage Uth;

    Current iTarget;

	public void set(State pin, DelayInterSystem other) {
		this.other = other;
		this.pin = pin;
	}

	public DelayInterSystem set(Resistance impedance) {
		this.impedance = impedance;
		this.conductance = impedance.invert();

		return this;
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

	@Override
	public void applyTo(SubSystem s) {
		s.addToA(pin, pin, conductance.getValue());
	}

	/*@Override
	public void simProcessI(SubSystem s) {
		if(thevnaCalc == false) {
			double pinI = 2 * other.getSubSystem().getX(other.pin) * conductance + oldIother[doubleBuffer];
			//pinI = (pinI * 1 - other.oldIother[doubleBuffer] * 0);
			s.addToI(pin, pinI);

			doubleBuffer = (doubleBuffer + 1) & 1;
			other.oldIother[doubleBuffer] = -pinI;
		} else {
			s.addToI(pin, -thenvaCurrent);
		}
	}*/

	/*@Override
	public void simProcessI(SubSystem s) {
		if (thevnaCalc == false) {
			double iA = pin.state*conductance + oldIother[doubleBuffer];
			double iB = other.pin.state*conductance + other.oldIother[doubleBuffer];
			double iTarget = (iA - iB) / 2;
			
			double aPinI = iTarget - (pin.state + other.pin.state) * 0.5 * conductance;
			
			s.addToI(pin, -aPinI);
			
			
			doubleBuffer = (doubleBuffer + 1 ) & 1;
			oldIother[doubleBuffer]= aPinI;
		} else {
			s.addToI(pin, -thenvaCurrent);
		}
	}*/

	/*
	@Override
	public void simProcessI(SubSystem s) {
		double iThis = pin.state * conductance + oldIother[doubleBuffer];
		double iOther = other.pin.state * conductance + other.oldIother[doubleBuffer];
		double iTarget = (iThis - iOther) / 2;
		
		double pinI = 2 * other.getSubSystem().getX(other.pin) * conductance + oldIother[doubleBuffer];		
		//pinI = (pinI * 1 - other.oldIother[doubleBuffer] * 0);
		s.addToI(pin, pinI);
		
		doubleBuffer = (doubleBuffer + 1 ) & 1;
		other.oldIother[doubleBuffer]= -pinI;
	}*/

	@Override
	public State[] getConnectedStates() {
		return new State[] {};
	}

	public void setInitialCurrent(Current i) {
		oldIother[doubleBuffer] = i;
	}

	@Override
	public void simProcessI(SubSystem s) {
		if (!thevnaCalc) {
			//Thevna delay line
			
			if (Math.abs(Rth.getValue()) < 1000000.0) {
				Voltage uTarget = Uth.substract(Rth.multiply(iTarget));
				Current aPinI = iTarget.substract(uTarget.multiply(conductance));
				s.addToI(pin, -aPinI.getValue());
			} else {
				Voltage uTarget = new Voltage(other.pin.state * 0.5 + pin.state * 0.5);
				//uTarget = 0;
				Current aPinI = iTarget.substract(uTarget.multiply(conductance));
				s.addToI(pin, -aPinI.getValue());
			}
			
			/*
			//STD delay line
			double pinI = 2 * other.getSubSystem().getX(other.pin) * conductance + oldIother[doubleBuffer];
			s.addToI(pin, pinI);
			
			doubleBuffer = (doubleBuffer + 1) & 1;
			other.oldIother[doubleBuffer] = -pinI;*/

		} else {
			s.addToI(pin, -thenvaCurrent);
		}
	}

	public static class ThevnaCalculator implements IRootSystemPreStepProcess {
		DelayInterSystem a, b;

		public ThevnaCalculator(DelayInterSystem a, DelayInterSystem b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public void rootSystemPreStepProcess() {
			doJobFor(a);
			doJobFor(b);
			Current iTarget = a.Uth.substract(b.Uth).divide(a.Rth.add(b.Rth));
			a.iTarget = iTarget;
			b.iTarget = iTarget.opposite();
		}

		void doJobFor(DelayInterSystem d) {
			d.thevnaCalc = true;

			d.thenvaCurrent = 2;
			Current aIs = new Current(2);
			Voltage aU = new Voltage(d.getSubSystem().solve(d.pin));

			d.thenvaCurrent = 1;
			Current bIs = new Current(1);
			Voltage bU = new Voltage(d.getSubSystem().solve(d.pin));

			Current aC = aU.multiply(d.conductance).add(aIs).opposite();
			Current bC = bU.multiply(d.conductance).add(bIs).opposite();

			d.Rth = aU.substract(bU).divide(aC.substract(bC));
			d.Uth = aU.substract(d.Rth.multiply(aC));

			d.thevnaCalc = false;
		}
	}
}
