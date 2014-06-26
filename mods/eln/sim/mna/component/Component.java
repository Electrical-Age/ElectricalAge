package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.state.State;

import org.apache.commons.math3.linear.RealMatrix;


public abstract class Component {
	
	
	public Component() {
		//System.out.println("new " + this);
	}
	
	SubSystem subSystem;
	public void addedTo(SubSystem s){
		this.subSystem = s;
	}
	public SubSystem getSubSystem(){
		return subSystem;
	}
	public abstract void applyTo(SubSystem s);
	public abstract State[] getConnectedStates();
	public boolean isInterSystem(){ return false; }
	public void breakConnection(){};

	
	public void dirty(){
		if(getSubSystem() != null){
			getSubSystem().invalidate();
		}
	}
	
	public void disconnectFromSubSystem() {
		subSystem = null;
	}
	
	public void onAddToRootSystem(){}
	public void onRemovefromRootSystem(){}
}
