package mods.eln.sim.mna.component;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.IDestructor;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;

public class InterSystemAbstraction implements IAbstractor, IDestructor {

	VoltageState aNewState;
	Resistor aNewResistor;
	DelayInterSystem2 aNewDelay;
	VoltageState bNewState;
	Resistor bNewResistor;
	DelayInterSystem2 bNewDelay;
	DelayInterSystem2.ThevnaCalculator thevnaCalc;

	RootSystem root;
	Resistor interSystemResistor;

	State aState;
	State bState;
	SubSystem aSystem;
	SubSystem bSystem;

	public InterSystemAbstraction(RootSystem root, Resistor interSystemResistor) {
		this.interSystemResistor = interSystemResistor;
		this.root = root;

		aState = interSystemResistor.aPin;
		bState = interSystemResistor.bPin;
		aSystem = aState.getSubSystem();
		bSystem = bState.getSubSystem();

		aSystem.interSystemConnectivity.add(bSystem);
		bSystem.interSystemConnectivity.add(aSystem);

		aNewState = new VoltageState();
		aNewResistor = new Resistor();
		aNewDelay = new DelayInterSystem2();
		bNewState = new VoltageState();
		bNewResistor = new Resistor();
		bNewDelay = new DelayInterSystem2();

		aNewResistor.connectGhostTo(aState, aNewState);
		aNewDelay.connectTo(aNewState, null);
		bNewResistor.connectGhostTo(bState, bNewState);
		bNewDelay.connectTo(bNewState, null);

		calibrate();

		aSystem.addComponent(aNewResistor);
		aSystem.addState(aNewState);
		aSystem.addComponent(aNewDelay);
		bSystem.addComponent(bNewResistor);
		bSystem.addState(bNewState);
		bSystem.addComponent(bNewDelay);

		aSystem.breakDestructor.add(this);
		bSystem.breakDestructor.add(this);

		interSystemResistor.abstractedBy = this;

		thevnaCalc = new DelayInterSystem2.ThevnaCalculator(aNewDelay, bNewDelay);
		root.addProcess(thevnaCalc);

	}

	void calibrate() {
		double u = (aState.state + bState.state) / 2;
		aNewDelay.setU(u);
		bNewDelay.setU(u);

		double r = interSystemResistor.getR() / 2;
		aNewResistor.setR(r);
		bNewResistor.setR(r);
	}

	@Override
	public void dirty(Component component) {
		calibrate();
	}

	@Override
	public SubSystem getAbstractorSubSystem() {

		return aSystem;
	}

	@Override
	public void destruct() {
		aSystem.breakDestructor.remove(this);
		aSystem.removeComponent(aNewDelay);
		aSystem.removeComponent(aNewResistor);
		aSystem.removeState(aNewState);
		bSystem.breakDestructor.remove(this);
		bSystem.removeComponent(bNewDelay);
		bSystem.removeComponent(bNewResistor);
		bSystem.removeState(bNewState);

		root.removeProcess(thevnaCalc);
		
		interSystemResistor.abstractedBy = null;

		aSystem.component.add(interSystemResistor);
	}

}
