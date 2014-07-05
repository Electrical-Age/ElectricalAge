package mods.eln.sim.process.heater;

import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;

public class ElectricalLoadHeatThermalLoad implements IProcess {

	public ElectricalLoadHeatThermalLoad(ElectricalLoad r, ThermalLoad load) {
		this.r = r;
		this.load = load;
	}
	
	ElectricalLoad r;
	ThermalLoad load;
	
	@Override
	public void process(double time) {
		if(r.isNotSimulated()) return;
		double I = r.getI();
		load.movePowerTo(I*I*r.getRs()*2);
	}

}
