package mods.eln.sim.process.heater;

import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;

public class ResistorHeatThermalLoad implements IProcess {

	public ResistorHeatThermalLoad(Resistor r, ThermalLoad load) {
		this.r = r;
		this.load = load;
	}
	
	Resistor r;
	ThermalLoad load;
	
	@Override
	public void process(double time) {
		load.movePowerTo(r.getP());
	}

}
