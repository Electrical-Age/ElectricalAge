package mods.eln.sim.process.destruct;

import mods.eln.sim.mna.state.VoltageState;

public class VoltageStateWatchDog extends ValueWatchdog{
	VoltageState state;
	
	
	
	
	@Override
	double getValue() {
		// TODO Auto-generated method stub
		return state.getU();
	}

	
	public VoltageStateWatchDog set(VoltageState state){
		this.state = state;
		return this;
	}
	
	
	public VoltageStateWatchDog setUNominal(double uNominal){
		this.max = uNominal*1.3;
		this.min = -uNominal*0.2;
		this.timeoutReset = uNominal*0.05*5;
		return this;
	}
	

		
}
