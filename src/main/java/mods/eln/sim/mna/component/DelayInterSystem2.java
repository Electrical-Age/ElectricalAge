package mods.eln.sim.mna.component;

import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;

public class DelayInterSystem2 extends VoltageSource {

    private DelayInterSystem2 other;

    public double Rth;
    public double Uth;

    public boolean thevnaCalc = false;

	public DelayInterSystem2() {
		super(null);
	}

	public void set(DelayInterSystem2 other) {
		this.other = other;
	}

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

			double U = (a.Uth - b.Uth) * b.Rth / (a.Rth + b.Rth) + b.Uth;
			if (Double.isNaN(U)) {
				U = 0;
			}
			a.setU(U);
			b.setU(U);
		}

		void doJobFor(DelayInterSystem2 d) {
			double originalU = d.getU();

			double aU = 10;
			d.setU(aU);
			double aI = d.getSubSystem().solve(d.getCurrentState());

			double bU = 5;
			d.setU(bU);
			double bI = d.getSubSystem().solve(d.getCurrentState());

			d.Rth = (aU - bU) / (bI - aI);
			//if(Double.isInfinite(d.Rth)) d.Rth = Double.MAX_VALUE;
 			if (d.Rth > 10000000000000000000.0) {
				d.Uth = 0;
				d.Rth = 10000000000000000000.0;
			} else {
				d.Uth = aU + d.Rth * aI;
			}
			d.setU(originalU);
		}
	}
}
