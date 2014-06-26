package mods.eln.sim.mna.state;

import java.awt.image.ComponentSampleModel;
import java.util.ArrayList;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.Component;


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
		return subSystem;
	}	
	public void disconnectFromSubSystem() {
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
	


}
