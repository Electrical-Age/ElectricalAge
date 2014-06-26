package mods.eln.sim.mna.component;

import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;

public abstract class Bipole extends Component {
	public VoltageState aPin,bPin;
	
	public Bipole connectTo(VoltageState aPin,VoltageState bPin){
		breakConnection();
		
		this.aPin = aPin;
		this.bPin = bPin;
		
		if(aPin != null) aPin.add(this);
		if(bPin != null) bPin.add(this);
		return this;
	}

	public Bipole connectGhostTo(VoltageState aPin,VoltageState bPin){
		breakConnection();
		
		this.aPin = aPin;
		this.bPin = bPin;
		return this;
	}
	
	@Override
	public void breakConnection() {	
		if(aPin != null) aPin.remove(this);
		if(bPin != null) bPin.remove(this);
	}
	
	@Override
	public State[] getConnectedStates() {
		// TODO Auto-generated method stub
		return new State[]{aPin,bPin};
	}
	

	
}
