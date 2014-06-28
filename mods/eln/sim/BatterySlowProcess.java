package mods.eln.sim;

import mods.eln.SaveConfig;

public abstract class BatterySlowProcess implements IProcess{
	BatteryProcess batteryProcess;
	
	ThermalLoad thermalLoad;
	
	public double lifeNominalCurrent,lifeNominalLost;

	public BatterySlowProcess(BatteryProcess batteryProcess,ThermalLoad thermalLoad)
	{
		this.batteryProcess = batteryProcess;
		this.thermalLoad = thermalLoad;
	}
	
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		double U = batteryProcess.getU();
		if(U < -0.1 * batteryProcess.uNominal) {destroy();return; }
		if(U > getUMax()) {destroy();return; }
		if(SaveConfig.instance.batteryAging){
			double newLife = batteryProcess.life;
			double normalisedCurrent = Math.abs(batteryProcess.getDischargeCurrent()) / lifeNominalCurrent;
			newLife -= normalisedCurrent*normalisedCurrent * lifeNominalLost * time;
				
			if(newLife < 0.1) newLife = 0.1;
			batteryProcess.changeLife(newLife);
		}

		
	}
	
	
	public double getUMax()
	{
		return 1.3 * batteryProcess.uNominal;
	}
	
	public abstract void destroy();

	
}
