package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.primitives.Conductance;
import mods.eln.sim.mna.primitives.Current;
import mods.eln.sim.mna.primitives.Power;
import mods.eln.sim.mna.primitives.Resistance;
import mods.eln.sim.mna.primitives.Voltage;
import mods.eln.sim.mna.state.State;

public class Resistor extends Bipole {
	public static final Resistance highImpedance = new Resistance(MnaConst.highImpedance);
	static final Resistance ultraImpedance = new Resistance(MnaConst.ultraImpedance);

	public Resistor() {
	}
	
	public Resistor(State aPin, State bPin) {
		super(aPin, bPin);
	}
	
	//public SubSystem interSystemA, interSystemB;

/*	public Line line = null;
	public boolean lineReversDir;
	public boolean isInLine() {
		
		return line != null;
	}*/
	
	private Resistance r = highImpedance;
	Conductance rInv = highImpedance.invert();

	//public boolean usedAsInterSystem = false;

	public Conductance getRInv() {
		return rInv;
	}
	
	public Resistance getR() {
		return r;
	}

	public Current getI() {
		return getCurrent();
	}

	public Power getP() {
		return getU().multiply(getCurrent());
	}

	public Voltage getU() {
		return new Voltage((aPin == null ? 0 : aPin.state) - (bPin == null ? 0 : bPin.state));
	}

	public Resistor setR(Resistance r) {
		if (this.r.getValue() != r.getValue()) {
			this.r = r;
			this.rInv = r.invert();
			dirty();
		}
		return this;
	}

	public void highImpedance() {
		setR(highImpedance);
	}	
	
	public void ultraImpedance() {
		setR(ultraImpedance);
	}

	public Resistor pullDown() {
		setR(new Resistance(MnaConst.pullDown));
		return this;
	}
	
	/*@Override
	public void dirty() {
		if (line != null) {
			line.recalculateR();
		}
		if (usedAsInterSystem) {
			aPin.getSubSystem().breakSystem();
			if (aPin.getSubSystem() != bPin.getSubSystem()) {
				bPin.getSubSystem().breakSystem();
			}
		}
		
		super.dirty();
	}*/
	
	boolean canBridge() {
		return false;
	}

	@Override
	public void applyTo(SubSystem s) {
		s.addToA(aPin, aPin, rInv.getValue());
		s.addToA(aPin, bPin, -rInv.getValue());
		s.addToA(bPin, bPin, rInv.getValue());
		s.addToA(bPin, aPin, -rInv.getValue());
	}

	@Override
	public Current getCurrent() {
		return getU().multiply(rInv);
		/*if(line == null)
			return getU() * rInv;
		else if (lineReversDir)
			return -line.getCurrent();
		else
			return line.getCurrent();*/
	}
}
