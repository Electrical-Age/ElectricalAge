package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.state.State;

public class Resistor extends Bipole {

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

    private double r = MnaConst.highImpedance, rInv = 1 / MnaConst.highImpedance;

    //public boolean usedAsInterSystem = false;

    public double getRInv() {
        return rInv;
    }

    public double getR() {
        return r;
    }

    public double getI() {
        return getCurrent();
    }

    public double getP() {
        return getU() * getCurrent();
    }

    public double getU() {
        return (aPin == null ? 0 : aPin.state) - (bPin == null ? 0 : bPin.state);
    }

    public Resistor setR(double r) {
        if (this.r != r) {
            this.r = r;
            this.rInv = 1 / r;
            dirty();
        }
        return this;
    }

    public void highImpedance() {
        setR(MnaConst.highImpedance);
    }

    public void ultraImpedance() {
        setR(MnaConst.ultraImpedance);
    }

    public Resistor pullDown() {
        setR(MnaConst.pullDown);
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
        s.addToA(aPin, aPin, rInv);
        s.addToA(aPin, bPin, -rInv);
        s.addToA(bPin, bPin, rInv);
        s.addToA(bPin, aPin, -rInv);
    }

    @Override
    public double getCurrent() {
        return getU() * rInv;
		/*if(line == null)
			return getU() * rInv;
		else if (lineReversDir)
			return -line.getCurrent();
		else
			return line.getCurrent();*/
    }
}
