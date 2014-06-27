package mods.eln.sim.mna.misc;

import java.util.ArrayList;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.state.State;



public class GenericDestructor implements IDestructor {

	public GenericDestructor(RootSystem root, Resistor component) {
		this.root = root;
		this.component = component;
	}

	RootSystem root;
	public Resistor component;
	public ArrayList<SubSystem> removeSubSystemDestructor = new ArrayList<SubSystem>();
	public ArrayList<Component> removeComponent = new ArrayList<Component>();
	public ArrayList<State> removeState = new ArrayList<State>();
	public  ArrayList<IRootSystemPreStepProcess> preProcess = new ArrayList<IRootSystemPreStepProcess>();

	@Override
	public void destruct(boolean withSubSystem) {
		component.usedAsInterSystem = false;
		for(SubSystem c : removeSubSystemDestructor) {
			c.breakDestructor.remove(this);
		}
		root.componentDestructor.remove(component);
		
		for(Component c : removeComponent) {
			//if(c.getSubSystem() != null)
				c.getSubSystem().removeComponent(c);
				c.breakConnection();
		}
		for(State s : removeState) {
			//if(s.getSubSystem() != null)
				s.getSubSystem().removeState(s);
		}

		for(IRootSystemPreStepProcess p : preProcess){
			root.removeProcess(p);
		}
		
		//root.addComponent(component);
		component.returnToRootSystem(root);
		
		if(withSubSystem){
			for(SubSystem c : removeSubSystemDestructor) {
				root.breakSystem(c);
			}
		}
		
		if(component instanceof ISubSystemProcessFlush){
			root.removeProcess((ISubSystemProcessFlush) component);
		}
		
		

		
	}

}
