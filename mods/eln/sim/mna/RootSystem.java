package mods.eln.sim.mna;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.DelayInterSystem;
import mods.eln.sim.mna.component.InterSystem;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.GenericDestructor;
import mods.eln.sim.mna.misc.IDestructor;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;



public class RootSystem {
	
	public RootSystem(double dt){
		this.dt = dt;
	}
	
	double dt;
	

	ArrayList<SubSystem> systems = new ArrayList<SubSystem>();
	
	public HashMap<Component, IDestructor> componentDestructor = new HashMap<Component, IDestructor>();
	
	HashSet<Component> addComponents = new HashSet<Component>();
	HashSet<State> addStates = new HashSet<State>();
	
//	boolean isValid = false;
	
	public void invalidate(){
		//isValid = false;
	}
	
	public void addState(State s){
		addStates.add(s);
		invalidate();
	}
	
	public void addComponent(Component c){
		addComponents.add(c);
		invalidate();
		c.onAddToRootSystem();
	}

	public void removeComponent(Component c){
		c.onRemovefromRootSystem();
		if(c.getSubSystem() != null){
			breakSystem(c.getSubSystem());
		}else{
			IDestructor d = componentDestructor.get(c);
			if(d != null) d.destruct();
		}
		addComponents.remove(c);
		invalidate();
	}
	
	public void removeState(State s){
		if(s.getSubSystem() != null){
			breakSystem(s.getSubSystem());
		}
		addStates.remove(s);		
		invalidate();
	}	
	
	public void generate(){
		if(addComponents.size() != 0 || addStates.size() != 0){
			generateBreak();
			generateLine();
			generateSystems();
			generateInterSystems();
		}
	}
	
	private void generateLine(){
		
	}
	
	private void generateBreak(){
		for(Component c : (HashSet<Component>)addComponents.clone()){
			SubSystem system = null;
			for(State s : c.getConnectedStates()){				
				system = findSubSystemWith(s);
				if(system != null) {
					breakSystem(system);
				}
			}
		}
	}
	
	private void generateSystems(){
		while(true){

			
			State root = null;
			for(Component c : addComponents){
				if(c.isInterSystem()) continue;
				for(State s : c.getConnectedStates()){
					if(s != null){
						root = s;
						break;
					}
				}
				if(root != null) break;
			}
			if(root == null){
				break;
			}
			
			buildSubSystem(root,false);

		}
		
		while(addStates.isEmpty() == false){
			State root = addStates.iterator().next();
			buildSubSystem(root,true);
		}
		
	}
	
	
	public void generateInterSystems(){
		Iterator<Component> ic = addComponents.iterator();
		while(ic.hasNext()){
			Component c = ic.next();
			if(!c.isInterSystem()){
				System.out.println("ELN generateInterSystems ERROR");
			}
			InterSystem interSystem = (InterSystem)c;
			{
				VoltageState aState = interSystem.aPin;
				VoltageState bState = interSystem.bPin;
				SubSystem aSystem = aState.getSubSystem();
				SubSystem bSystem = bState.getSubSystem();
				
				VoltageState aNewState = new VoltageState();
				Resistor aNewResistor = new Resistor();
				DelayInterSystem aNewDelay = new DelayInterSystem();
				VoltageState bNewState = new VoltageState();
				Resistor bNewResistor = new Resistor();
				DelayInterSystem bNewDelay = new DelayInterSystem();
				
				double u = (aState.state + bState.state)/2;
				aNewState.state = u;
				bNewState.state = u;
				double i = (aState.state - bState.state)*interSystem.getRInv(); 				
				aNewDelay.setInitialCurrent(-u/interSystem.getR() - i);
				bNewDelay.setInitialCurrent(-u/interSystem.getR() + i);
				
				aNewResistor.setR(interSystem.getR()/2).connectGhostTo(aState, aNewState);
				aNewDelay.set(interSystem.getR()).set(aNewState, bNewDelay);
				bNewResistor.setR(interSystem.getR()/2).connectGhostTo(bState, bNewState);
				bNewDelay.set(interSystem.getR()).set(bNewState, aNewDelay);
				
				aSystem.addComponent(aNewResistor);
				aSystem.addState(aNewState);
				aSystem.addComponent(aNewDelay);
				bSystem.addComponent(bNewResistor);
				bSystem.addState(bNewState);
				bSystem.addComponent(bNewDelay);
				
				GenericDestructor destructor = new GenericDestructor(this,interSystem);
				destructor.removeComponent.add(aNewResistor);
				destructor.removeComponent.add(bNewResistor);
				destructor.removeComponent.add(aNewDelay);
				destructor.removeComponent.add(bNewDelay);
				destructor.removeState.add(aNewState);
				destructor.removeState.add(bNewState);
				destructor.removeSubSystemDestructor.add(aSystem);
				destructor.removeSubSystemDestructor.add(bSystem);
				
				aSystem.breakDestructor.add(destructor);
				bSystem.breakDestructor.add(destructor);
				componentDestructor.put(interSystem, destructor);
			}
			ic.remove();
		}
	}
	
	public void step(){	
		generate();
		
		for(SubSystem s : systems){
			s.stepCalc();
		}
		for(SubSystem s : systems){
			s.stepFlush();
		}
	}
	
	private void buildSubSystem(State root,boolean withInterSystem){

		Set<Component> componentSet = new HashSet<Component>();
		Set<State> stateSet = new HashSet<State>();
		
		buildSubSystem(root,withInterSystem,componentSet,stateSet);
		
		addComponents.removeAll(componentSet);
		addStates.removeAll(stateSet);
		
		SubSystem subSystem = new SubSystem(dt);
		subSystem.addState(stateSet);
		subSystem.addComponent(componentSet);
		
		systems.add(subSystem);
	}
	
	private void buildSubSystem(State root,boolean withInterSystem, Set<Component> componentSet,Set<State> stateSet) {
		if(stateSet.contains(root) || findSubSystemWith(root) != null) return;
		stateSet.add(root);
		for(Component c : root.getConnectedComponents()){
			if(withInterSystem == false && c.isInterSystem()) continue;
			if(componentSet.contains(c)) continue;
			boolean noGo = false;
			for(State s : c.getConnectedStates()){
				if(s == null) continue;
				if(s.getSubSystem() != null){
					noGo = true;
					break;
				}
			}
			if(noGo) continue;
			componentSet.add(c);
			for(State s : c.getConnectedStates()){
				if(s == null) continue;
				buildSubSystem(s,withInterSystem, componentSet, stateSet);
			}
			
		}
	}

	private SubSystem findSubSystemWith(State state){
		for(SubSystem s : systems){
			if(s.containe(state)) return s;
		}
		
		return null;
	}
	
	
	private void breakSystem(SubSystem s){
		s.breakSystem(this);
		systems.remove(s);
		invalidate();
	}
	
	
	
	public static void main(String[] args) {
		RootSystem s = new RootSystem(0.1);
		
		VoltageState n1,n2;
		VoltageSource u1;
		Resistor r1,r2;
		
		s.addState(n1 = new VoltageState());
		s.addState(n2 = new VoltageState());
				
		s.addComponent((u1 = new VoltageSource()).setU(1).connectTo(n1, null));
		
		s.addComponent((r1 = new Resistor()).setR(10).connectTo(n1,n2));
		s.addComponent((r2 = new Resistor()).setR(20).connectTo(n2,null));
		
		
		VoltageState n11,n12;
		VoltageSource u11;
		Resistor r11,r12,r13;
		
		s.addState(n11 = new VoltageState());
		s.addState(n12 = new VoltageState());
				
		s.addComponent((u11 = new VoltageSource()).setU(1).connectTo(n11, null));
		
		s.addComponent((r11 = new Resistor()).setR(10).connectTo(n11,n12));
		s.addComponent((r12 = new Resistor()).setR(30).connectTo(n12,null));
		
		
		
		InterSystem i01;
		
		s.addComponent((i01 = new InterSystem()).setR(10).connectTo(n2, n12));

		for (int i = 0; i < 50; i++) {
			s.step();
		}
		
		
		s.addComponent((r13 = new Resistor()).setR(30).connectTo(n12,null));

		for (int i = 0; i < 50; i++) {
			s.step();
		}
		
		
		s.step();


	}

	public int getSubSystemCount() {
		// TODO Auto-generated method stub
		return systems.size();
	}
	
}

//Ttodo garbadge collector
//Ghost suprresion
