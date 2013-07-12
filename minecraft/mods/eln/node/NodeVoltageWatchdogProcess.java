package mods.eln.node;

import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.IVoltageWatchdogDescriptor;
import mods.eln.sim.VoltageWatchdogProcess;

public class NodeVoltageWatchdogProcess extends VoltageWatchdogProcess{
	Node node;
	ElectricalLoad voltageLoad;
	IVoltageDestructorDescriptor destructor;
	IVoltageWatchdogDescriptor watchdog;
	public NodeVoltageWatchdogProcess(Node node,IVoltageWatchdogDescriptor watchdog,IVoltageDestructorDescriptor destructor,ElectricalLoad voltageLoad) {
		
		this.node = node;
		this.voltageLoad = voltageLoad;
		this.destructor = destructor;
		this.watchdog = watchdog;
		// TODO Auto-generated constructor stub
	}

	
	@Override
	public double getVoltage() {
		// TODO Auto-generated method stub
		return voltageLoad.Uc;
	}

	@Override
	public IVoltageWatchdogDescriptor getWatchdogDescriptor() {
		// TODO Auto-generated method stub
		return watchdog;
	}

	@Override
	public void voltageOverFlow(double time,double overflow) {
		// TODO Auto-generated method stub
		tryDest(time,overflow);
	}

	@Override
	public void voltageUnderFlow(double time,double overflow) {
		// TODO Auto-generated method stub
		tryDest(time,overflow);
	}
	
	void tryDest(double time,double overflow)
	{
		if(Math.random() < time * overflow * destructor.getVoltageDestructionProbabilityPerOverflow())
		{
			double miaou = overflow * destructor.getVoltageDestructionPerOverflow() + destructor.getVoltageDestructionStart();
			if(miaou > destructor.getVoltageDestructionMax()) miaou = destructor.getVoltageDestructionMax();
			node.physicalSelfDestruction((float)miaou);
		}

	}


}
