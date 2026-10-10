package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.state.State;

public class DelayInterSystem extends Component implements ISubSystemProcessI {

    private DelayInterSystem other;
    public State pin;

    double impedance, conductance;

    public double[] oldIother = new double[]{0, 0};
    int doubleBuffer = 0;
    public boolean thevnaCalc = false;
    public double thenvaCurrent;
    public double Rth;
    public double Uth;

    double iTarget;

    public void set(State pin, DelayInterSystem other) {
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

    @Override
    public void addedTo(SubSystem s) {
        super.addedTo(s);
        s.addProcess(this);
    }

    @Override
    public void applyTo(SubSystem s) {
        s.addToA(pin, pin, conductance);
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
        return new State[]{};
    }

    public void setInitialCurrent(double i) {
        oldIother[doubleBuffer] = i;
    }

    @Override
    public void simProcessI(SubSystem s) {
        if (!thevnaCalc) {
            //Thevna delay line

            if (Math.abs(Rth) < 1000000.0) {
                double uTarget = Uth - Rth * iTarget;
                double aPinI = iTarget - uTarget * conductance;
                s.addToI(pin, -aPinI);
            } else {
                double uTarget = other.pin.state * 0.5 + pin.state * 0.5;
                //uTarget = 0;
                double aPinI = iTarget - uTarget * conductance;
                s.addToI(pin, -aPinI);
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
            double iTarget = (a.Uth - b.Uth) / (a.Rth + b.Rth);
            a.iTarget = iTarget;
            b.iTarget = -iTarget;
        }

        void doJobFor(DelayInterSystem d) {
            d.thevnaCalc = true;

            d.thenvaCurrent = 2;
            double aIs = 2;
            double aU = d.getSubSystem().solve(d.pin);

            d.thenvaCurrent = 1;
            double bIs = 1;
            double bU = d.getSubSystem().solve(d.pin);

            double aC = -(aU * d.conductance + aIs);
            double bC = -(bU * d.conductance + bIs);

            d.Rth = (aU - bU) / (aC - bC);
            d.Uth = aU - d.Rth * aC;

            d.thevnaCalc = false;
        }
    }
}
