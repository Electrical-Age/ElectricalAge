package mods.eln.sim.mna.component;

import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.primitives.Current;
import mods.eln.sim.mna.primitives.Resistance;
import mods.eln.sim.mna.primitives.Voltage;

public class DelayInterSystem2 extends VoltageSource {

    private DelayInterSystem2 other;

    public Resistance Rth;
    public Voltage Uth;

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

			Voltage U = a.Uth.substract(b.Uth).multiply(b.Rth.divide(a.Rth.add(b.Rth))).add(b.Uth);
			if (U.isNaN()) {
				U = new Voltage();
			}
			a.setU(U);
			b.setU(U);
		}

		void doJobFor(DelayInterSystem2 d) {
			Voltage originalU = d.getU();

			Voltage aU = new Voltage(10);
			d.setU(aU);
			Current aI = new Current(d.getSubSystem().solve(d.getCurrentState()));

		    Voltage bU = new Voltage(5);
			d.setU(bU);
			Current bI = new Current(d.getSubSystem().solve(d.getCurrentState()));

			d.Rth = aU.substract(bU).divide(bI.substract(aI));
			//if(Double.isInfinite(d.Rth)) d.Rth = Double.MAX_VALUE;
			if (d.Rth.getValue() > 10000000000000000000.0) {
				d.Uth = new Voltage();
				d.Rth = new Resistance(10000000000000000000.0);
			} else {
				d.Uth = aU.add(d.Rth.multiply(aI));
			}
			d.setU(originalU);
		}
	}
}
