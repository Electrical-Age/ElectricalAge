package mods.eln.sim.mna.component;

import mods.eln.sim.mna.RootSystem;
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
	public boolean canBeReplacedByInterSystem(){ return false; }
	public void breakConnection(){};
//	public void fromSubSystemToRoot(RootSystem root){root.addComponents.add(this);}
	public void returnToRootSystem(RootSystem root){
		root.addComponent(this);
	} 

	
	public void dirty(){
		/*if(abstractedBy != null){
			abstractedBy.dirty(this);
		} else */if(getSubSystem() != null){
			getSubSystem().invalidate();
		}
	}
	
	public void quitSubSystem() {
		subSystem = null;
	}
	
	
	public IComponentAbstractor abstractedBy;
	
	
	public void onAddToRootSystem(){}
	public void onRemovefromRootSystem(){}
}
