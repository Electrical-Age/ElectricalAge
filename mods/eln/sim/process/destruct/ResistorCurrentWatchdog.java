package mods.eln.sim.process.destruct;

import mods.eln.sim.mna.component.Resistor;

public class ResistorCurrentWatchdog extends ValueWatchdog{
	Resistor resistor;

	public ResistorCurrentWatchdog set(Resistor resistor){
		this.resistor = resistor;
		return this;
	}
	
	public ResistorCurrentWatchdog setIAbsMax(double Imax){
		this.max = Imax;
		this.min = -max;
		this.timeoutReset = Imax*0.10*5;
		
		return this;
	}
	
	@Override
	double getValue() {
		
		return resistor.getI();
	}



}
