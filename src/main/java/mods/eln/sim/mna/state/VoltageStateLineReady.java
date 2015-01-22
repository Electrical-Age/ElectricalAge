package mods.eln.sim.mna.state;

public class VoltageStateLineReady extends VoltageState{
	
	public void setCanBeSimplifiedByLine(boolean v){
		this.canBeSimplifiedByLine = v;
	}
	
	boolean canBeSimplifiedByLine = false;
	
	@Override
	public boolean canBeSimplifiedByLine() {
		
		return canBeSimplifiedByLine;
	}
}
