package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.state.CurrentState;
import mods.eln.sim.mna.state.State;

public class Transformer extends Bipole implements ISubSystemProcessI {

	public Transformer() {
	}
    public CurrentState aCurrentState = new CurrentState();
    public CurrentState bCurrentState = new CurrentState();
	public Transformer(State aPin, State bPin) {
		super(aPin, bPin);
	}

    double ratio = 1;
    public void setRatio(double ratio){
        this.ratio = ratio;
    }
	//public SubSystem interSystemA, interSystemB;

/*	public Line line = null;
	public boolean lineReversDir;
	public boolean isInLine() {
		
		return line != null;
	}*/
	
	private double r = MnaConst.highImpedance, rInv = 1 / MnaConst.highImpedance;

    @Override
    public void quitSubSystem() {
        subSystem.states.remove(aCurrentState);
        subSystem.states.remove(bCurrentState);
        subSystem.removeProcess(this);
        super.quitSubSystem();
    }

    @Override
    public void addedTo(SubSystem s) {
        super.addedTo(s);
        s.addState(aCurrentState);
        s.addState(bCurrentState);
        s.addProcess(this);
    }

    @Override
    public void applyTo(SubSystem s) {
       /* s.addToA(bPin, bCurrentState, 1.0);
        s.addToA(bCurrentState, bPin, 1.0);
        s.addToA(bCurrentState, aPin, 2.0);

        s.addToA(aPin, aCurrentState, 1.0);
        s.addToA(aCurrentState, aPin, 1.0);
        s.addToA(aCurrentState, bPin, 0.5);

        s.addToA(aCurrentState, aCurrentState, 1.0);
        s.addToA(aCurrentState, bCurrentState,2.0);
        s.addToA(bCurrentState, aCurrentState, 1.0);
        s.addToA(bCurrentState, bCurrentState,2.0);*/


        s.addToA(bPin, bCurrentState, 1.0);
        s.addToA(bCurrentState, bPin, 1.0);
        s.addToA(bCurrentState, aPin, -ratio);

        s.addToA(aPin, aCurrentState, 1.0);
        s.addToA(aCurrentState, aPin, 1.0);
        s.addToA(aCurrentState, bPin, -1/ratio);

        s.addToA(aCurrentState, aCurrentState, 1.0);
        s.addToA(aCurrentState, bCurrentState,ratio);
        s.addToA(bCurrentState, aCurrentState, 1.0);
        s.addToA(bCurrentState, bCurrentState,ratio);


        //s.addToA(bPin, aCurrentState, 0.5);
       // s.addToA(aPin, bCurrentState, -2.0);
    }
    @Override
    public void simProcessI(SubSystem s) {
       // s.addToI(bCurrentState, u);
    }

/*
	@Override
	public void applyTo(SubSystem s) {
		s.addToA(aPin, aPin, 1);
		s.addToA(aPin, bPin, -4);
		s.addToA(bPin, bPin, -1);
		s.addToA(bPin, aPin, 4);
	}*/

	@Override
	public double getCurrent() {
		return 0;

	}
}
