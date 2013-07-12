package mods.eln.node;

import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalWatchdogProcess;

public class NodeThermalWatchdogProcess extends ThermalWatchdogProcess{
	Node node;
	ThermalLoad thermalLoad;
	IThermalDestructorDescriptor destructor;
	ITemperatureWatchdogDescriptor watchdog;
	public NodeThermalWatchdogProcess(Node node,ITemperatureWatchdogDescriptor watchdog,IThermalDestructorDescriptor destructor,ThermalLoad thermalLoad) {
		
		this.node = node;
		this.thermalLoad = thermalLoad;
		this.destructor = destructor;
		this.watchdog = watchdog;
		// TODO Auto-generated constructor stub
	}

	
	@Override
	public double getTemperature() {
		// TODO Auto-generated method stub
		return thermalLoad.Tc;
	}

	@Override
	public ITemperatureWatchdogDescriptor getWatchdogDescriptor() {
		// TODO Auto-generated method stub
		return watchdog;
	}

	@Override
	public void temperatureOverFlow(double time,double overflow) {
		// TODO Auto-generated method stub
		tryDest(time,overflow);
	}

	@Override
	public void temperatureUnderFlow(double time,double overflow) {
		// TODO Auto-generated method stub
		tryDest(time,overflow);
	}
	
	void tryDest(double time,double overflow)
	{
		if(Math.random() < time * overflow * destructor.getThermalDestructionProbabilityPerOverflow())
		{
			double miaou = overflow * destructor.getThermalDestructionPerOverflow() + destructor.getThermalDestructionStart();
			if(miaou > destructor.getThermalDestructionMax()) miaou = destructor.getThermalDestructionMax();
			node.physicalSelfDestruction((float)miaou);
		}

	}


}
