package mods.eln.sim.mna.process;

import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.primitives.Current;
import mods.eln.sim.mna.primitives.Resistance;
import mods.eln.sim.mna.primitives.Voltage;
import mods.eln.sim.mna.state.State;

public class TransformerInterSystemProcess implements IRootSystemPreStepProcess {
	State aState, bState;
	VoltageSource aVoltgeSource, bVoltgeSource;

	double ratio = 1;

	public TransformerInterSystemProcess(State aState, State bState, VoltageSource aVoltgeSource, VoltageSource bVoltgeSource) {
		this.aState = aState;
		this.bState = bState;
		this.aVoltgeSource = aVoltgeSource;
		this.bVoltgeSource = bVoltgeSource;
	}

	@Override
	public void rootSystemPreStepProcess() {
		Th a = getTh(aState, aVoltgeSource);
		Th b = getTh(bState, bVoltgeSource);
		
		Voltage aU = new Voltage((a.U.getValue() * b.R.getValue() + ratio * b.U.getValue() * a.R.getValue()) / 
				(b.R.getValue() + ratio * ratio * a.R.getValue()));
		if (aU.isNaN()) {
			aU = new Voltage();
		}
		
		aVoltgeSource.setU(aU);
		bVoltgeSource.setU(aU.multiply(ratio));
	}
	
	static class Th {
		Resistance R;
		Voltage U;
	}
	
	Th getTh(State d,VoltageSource voltageSource) {
		Th th = new Th();
		Voltage originalU = new Voltage(d.state);

		Voltage aU = new Voltage(10);
		voltageSource.setU(aU);
		Current aI = new Current(d.getSubSystem().solve(voltageSource.getCurrentState()));

		Voltage bU = new Voltage(5);
		voltageSource.setU(bU);
		Current bI = new Current(d.getSubSystem().solve(voltageSource.getCurrentState()));

		Resistance Rth = aU.substract(bU).divide(bI.substract(aI));
		Voltage Uth;
		//if (Double.isInfinite(d.Rth)) d.Rth = Double.MAX_VALUE;
		if (Rth.getValue() > 10000000000000000000.0 || Rth.getValue() < 0) {
			Uth = new Voltage();
			Rth = new Resistance(10000000000000000000.0);
		} else {
			Uth = aU.add(Rth.multiply(aI));
		}
		voltageSource.setU(originalU);
		
		th.R = Rth;
		th.U = Uth;
		return th;
	}

	public void setRatio(double ratio) {
		this.ratio = ratio;
	}
}
