package mods.eln.sim.mna.component;

import mods.eln.sim.mna.RootSystem;


public class InterSystem extends Resistor{

	public static class InterSystemDestructor{
		boolean done = false;
		
	}
	
	@Override
	public boolean canBeReplacedByInterSystem() {
		// TODO Auto-generated method stub
		return true;
	}
	
	
}
