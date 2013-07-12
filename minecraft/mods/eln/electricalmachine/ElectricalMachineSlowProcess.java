package mods.eln.electricalmachine;

import mods.eln.sim.IProcess;

public class ElectricalMachineSlowProcess implements IProcess{
	private ElectricalMachineElement element;
	public ElectricalMachineSlowProcess(ElectricalMachineElement element) {
		this.element = element;
	}
	
	double lastPublishAt = 0,lastUpdate = 0;
	boolean boot = true;
	@Override
	public void process(double time) {
		double P = element.electricalResistor.getP();
		lastUpdate+=time;
		if(!boot)
		{
			if(Math.abs((P-lastPublishAt) / (lastPublishAt + 1.0)) > 1/32.0 && lastUpdate > 0.2)
			{
				element.needPublish();
				lastPublishAt = P;
				lastUpdate = 0;
			}
		}

		
		boot = false;
	}
	
	


}
