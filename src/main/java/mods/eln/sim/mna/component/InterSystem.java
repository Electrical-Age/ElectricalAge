package mods.eln.sim.mna.component;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;


public class InterSystem extends Resistor{

	public static class InterSystemDestructor{
		boolean done = false;
		
	}
	
	@Override
	public boolean canBeReplacedByInterSystem() {
		
		return true;
	}

	
	
}
