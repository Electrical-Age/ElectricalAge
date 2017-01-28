package mods.eln.sim.process.heater;

import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;

public class ElectricalLoadHeatThermalLoad implements IProcess {

    ElectricalLoad r;
    ThermalLoad load;

    public ElectricalLoadHeatThermalLoad(ElectricalLoad r, ThermalLoad load) {
        this.r = r;
        this.load = load;
    }

    @Override
    public void process(double time) {
        if (r.isNotSimulated()) return;
        double I = r.getI();
        load.movePowerTo(I * I * r.getRs() * 2);
    }

	/*double powerMax = 100000;
    public void setDeltaTPerSecondMax(double deltaTPerSecondMax) {
		powerMax = deltaTPerSecondMax*load.C;
	}*/
}
