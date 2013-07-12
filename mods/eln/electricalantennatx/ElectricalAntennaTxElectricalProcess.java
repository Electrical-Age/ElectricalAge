package mods.eln.electricalantennatx;

import mods.eln.electricalantennarx.ElectricalAntennaRxElement;
import mods.eln.sim.IProcess;

public class ElectricalAntennaTxElectricalProcess implements IProcess{

	ElectricalAntennaTxElement element;
	public ElectricalAntennaTxElectricalProcess(ElectricalAntennaTxElement element) {
		this.element = element;
	}
	
	@Override
	public void process(double time) 
	{
		ElectricalAntennaRxElement rx = element.getRxElement();

		if(rx == null)
		{
			element.signalOutProcess.setOutputNormalized(1.0);
		}
		else
		{
			double powerOut = element.powerIn.getRpPower() * element.powerEfficency;
			rx.setPowerOut(powerOut);
			element.signalOutProcess.U = rx.getSignal();
		}
		element.calculatePowerInRp();
		
	}

	
}
