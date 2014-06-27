package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.state.State;

public class DelayInterSystem2 extends VoltageSource {

	private DelayInterSystem2 other;


	public void set(DelayInterSystem2 other) {
		this.other = other;

	}


	public double Rth;
	public double Uth;


	public boolean thevnaCalc = false;
	public double thenvaU;



	public static class ThevnaCalculator implements IRootSystemPreStepProcess {
		DelayInterSystem2 a, b;

		public ThevnaCalculator(DelayInterSystem2 a, DelayInterSystem2 b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public void rootSystemPreStepProcess() {
			doJobFor(a);
			doJobFor(b);
			
			double U = (a.Uth-b.Uth)/(a.Rth+b.Rth)*b.Rth+b.Uth;
			a.setU(U);
			b.setU(U);
		}

		void doJobFor(DelayInterSystem2 d) {
			d.thevnaCalc = true;

			d.thenvaU = 2;
			double aU = 10;
			double aI = d.getSubSystem().solve(d.currentState);

			d.thenvaU = 1;
			double bU = 5;
			double bI = d.getSubSystem().solve(d.currentState);

			//double aC = -(aU * d.conductance + aIs);
			//double bC = -(bU * d.conductance + bIs);

			d.Rth = (aU - bU) / (bI - aI);
			d.Uth = aU + d.Rth * aI;

			d.thevnaCalc = false;
		}

	}
}
