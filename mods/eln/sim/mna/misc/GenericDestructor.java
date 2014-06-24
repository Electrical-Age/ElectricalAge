package mods.eln.sim.mna.misc;

import java.util.ArrayList;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.state.State;



public class GenericDestructor implements IDestructor {

	public GenericDestructor(RootSystem root, Component component) {
		this.root = root;
		this.component = component;
	}

	RootSystem root;
	Component component;
	public ArrayList<SubSystem> removeSubSystemDestructor = new ArrayList<SubSystem>();
	public ArrayList<Component> removeComponent = new ArrayList<Component>();
	public ArrayList<State> removeState = new ArrayList<State>();

	@Override
	public void destruct() {
		for(SubSystem c : removeSubSystemDestructor) {
			c.breakDestructor.remove(this);
		}
		root.componentDestructor.remove(component);
		
		for(Component c : removeComponent) {
			//if(c.getSubSystem() != null)
				c.getSubSystem().removeComponent(c);
		}
		for(State s : removeState) {
			//if(s.getSubSystem() != null)
				s.getSubSystem().removeState(s);
		}

		root.addComponent(component);
	}

}
