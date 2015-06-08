package mods.eln.sim.mna.component;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.primitives.Current;
import mods.eln.sim.mna.primitives.Inductance;
import mods.eln.sim.mna.primitives.Resistance;
import mods.eln.sim.mna.state.CurrentState;
import mods.eln.sim.mna.state.State;

public class Inductor extends Bipole implements ISubSystemProcessI, INBTTReady {

	String name;

    private Inductance l = new Inductance();
    Resistance ldt;

    private CurrentState currentState = new CurrentState();

	public Inductor(String name) {
		this.name = name;
	}
	
	public Inductor(String name, State aPin, State bPin) {
		super(aPin, bPin);
		this.name = name;
	}

	@Override
	public Current getCurrent() {
		return new Current(currentState.state);
	}

	public void setL(Inductance l) {
		this.l = l;
		dirty();
	}

	@Override
	public void applyTo(SubSystem s) {
		ldt = l.divide(s.getDt()).opposite();
		
		s.addToA(aPin, currentState, 1);
		s.addToA(bPin, currentState, -1);
		s.addToA(currentState, aPin, 1);
		s.addToA(currentState, bPin, -1);
		s.addToA(currentState, currentState, ldt.getValue());
	}
	
	@Override
	public void simProcessI(SubSystem s) {
		s.addToI(currentState, ldt.getValue() * currentState.state);
	}

	@Override
	public void quitSubSystem() {
		subSystem.states.remove(getCurrentState());
		subSystem.removeProcess(this);
		super.quitSubSystem();
	}
	
	@Override
	public void addedTo(SubSystem s) {
		super.addedTo(s);
		s.addState(getCurrentState());
		s.addProcess(this);
	}

	public CurrentState getCurrentState() {
		return currentState;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		str += name;
		currentState.state = (nbt.getDouble(str + "Istate"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		str += name;
		nbt.setDouble(str + "Istate", currentState.state);
	}

	public void resetStates() {
		currentState.state = 0;
	}
}
