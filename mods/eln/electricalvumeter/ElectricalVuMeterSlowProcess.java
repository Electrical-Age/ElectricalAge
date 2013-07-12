package mods.eln.electricalvumeter;

import mods.eln.sim.IProcess;

public class ElectricalVuMeterSlowProcess implements IProcess {
	ElectricalVuMeterElement element;
	public ElectricalVuMeterSlowProcess(ElectricalVuMeterElement element) {
		this.element = element;
	}
	double timeCounter = 0;
	static final double refreshPeriode = 0.25;
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		timeCounter += time;
		if(timeCounter >= refreshPeriode)
		{
			timeCounter -= refreshPeriode;
			element.needPublish();
		}
	}

}
