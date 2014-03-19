package mods.eln.node;

import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.IVoltageWatchdogDescriptor;
import mods.eln.sim.Simulator;
import mods.eln.sim.VoltageWatchdogProcess;

public class NodeVoltageWatchdogProcess extends VoltageWatchdogProcess{
	NodeBase node;
	ElectricalLoad positiveLoad,negativeLoad = ElectricalLoad.groundLoad;
	IVoltageDestructorDescriptor destructor;
	IVoltageWatchdogDescriptor watchdog;
	public NodeVoltageWatchdogProcess(NodeBase node,IVoltageWatchdogDescriptor watchdog,IVoltageDestructorDescriptor destructor,ElectricalLoad positiveLoad) {
		
		this.node = node;
		this.positiveLoad = positiveLoad;
		this.destructor = destructor;
		this.watchdog = watchdog;
		// TODO Auto-generated constructor stub
	}
	public NodeVoltageWatchdogProcess(NodeBase node,ElectricalLoad positiveLoad,ElectricalLoad negativeLoad) {
		
		this.node = node;
		this.positiveLoad = positiveLoad;
		this.negativeLoad = negativeLoad;
	}
	
	public void setVoltage(double max)
	{
		watchdog = new VoltageWatchdogDescriptor(max, -max*0.1);
	}
	public void setDestructor(double strength)
	{
		destructor = new VoltageDestructorDescriptor(strength,strength,0,1/(watchdog.getUmax()*0.02)*0.1);
	}

	
	public static class VoltageWatchdogDescriptor implements IVoltageWatchdogDescriptor{
		double Umax, Umin;
		public VoltageWatchdogDescriptor(double Umax, double Umin) {
			this.Umax = Umax;// TODO Auto-generated constructor stub
			this.Umin = Umin;
		}
		@Override
		public double getUmax() {
			// TODO Auto-generated method stub
			return Umax;
		}

		@Override
		public double getUmin() {
			// TODO Auto-generated method stub
			return Umin;
		}
		
	}
	
	public static class VoltageDestructorDescriptor implements IVoltageDestructorDescriptor{
		double destructionMax, destructionStart,destructionPerOverflow,probabilityPerOverflow;
		public VoltageDestructorDescriptor(double destructionMax, double destructionStart,double destructionPerOverflow,double probabilityPerOverflow) {
			this.destructionMax = destructionMax;// TODO Auto-generated constructor stub
			this.destructionStart = destructionStart;
			this.destructionPerOverflow = destructionPerOverflow;
			this.probabilityPerOverflow = probabilityPerOverflow;
		}
		@Override
		public double getVoltageDestructionMax() {
			// TODO Auto-generated method stub
			return destructionMax;
		}
		@Override
		public double getVoltageDestructionStart() {
			// TODO Auto-generated method stub
			return destructionStart;
		}
		@Override
		public double getVoltageDestructionPerOverflow() {
			// TODO Auto-generated method stub
			return destructionPerOverflow;
		}
		@Override
		public double getVoltageDestructionProbabilityPerOverflow() {
			// TODO Auto-generated method stub
			return probabilityPerOverflow;
		}

		
	}
		
	@Override
	public double getVoltage() {
		// TODO Auto-generated method stub
		return positiveLoad.Uc - negativeLoad.Uc;
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
