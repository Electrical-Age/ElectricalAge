package mods.eln.sim.process.heater;

import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;

public class DiodeHeatThermalLoad implements IProcess {

	Resistor r;
	ThermalLoad load;
	double lastR;
	
	public DiodeHeatThermalLoad(Resistor r, ThermalLoad load) {
		this.r = r;
		this.load = load;
		lastR = r.getR();
	}

	@Override 
	public void process(double time) {
		if(r.getR() == lastR){
			load.movePowerTo(r.getP());
		}else{
			lastR = r.getR();
		}
	}
}
