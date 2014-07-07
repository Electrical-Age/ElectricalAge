package mods.eln.sim.mna.state;

import java.util.ArrayList;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.IAbstractor;


public class State {
	private int id = -1;
	
	public int getId(){
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public double state;
	
	SubSystem subSystem;
	public void addedTo(SubSystem s){
		this.subSystem = s;
	}
	public SubSystem getSubSystem(){
		if(isAbstracted()) return abstractedBy.getAbstractorSubSystem();
		return subSystem;
	}	
	public void quitSubSystem() {
		subSystem = null;
	}

	
	ArrayList<Component> components = new ArrayList<Component>();
	
	public ArrayList<Component> getConnectedComponents(){
		return components;
	}
	
	public void add(Component c) {
		components.add(c);
		//System.out.println("ADD " + c + " To " +  this);
	}
	public void remove(Component c) {
		components.remove(c);
	}
	


	public boolean canBeSimplifiedByLine(){ return false; }
	
	boolean isPrivateSubSystem = false;
	public State setAsPrivate(){
		isPrivateSubSystem = true;
		return this;
	}
	
	public boolean isPrivateSubSystem() { return isPrivateSubSystem;}
	
	public void returnToRootSystem(RootSystem root) {
		root.addStates.add(this);
	}
	
	
	public IAbstractor abstractedBy;
	public boolean isAbstracted() {
		return abstractedBy != null;
	}	
	
	
	public boolean isNotSimulated() {
		
		return subSystem == null && abstractedBy == null;
	}
}
