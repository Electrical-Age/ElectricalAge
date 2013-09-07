package mods.eln.battery;

import mods.eln.sim.IProcess;

public class BatteryInventoryProcess implements IProcess{

	BatteryElement battery;
	
	public BatteryInventoryProcess(BatteryElement battery) {
		this.battery = battery;
	}
	
	boolean thermalCut = false;
	
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		battery.batteryProcess.life = 1.0;
		
		
		boolean cut = false;
		if(battery.hasOverHeatingProtection())
		{
			if(battery.thermalLoad.Tc* 1.1 > battery.descriptor.thermalHeatTime)
			{
				thermalCut = true;
			}		
			if(battery.thermalLoad.Tc* 1.15 < battery.descriptor.thermalHeatTime)
			{
				thermalCut = false;
			}				
		}
		else
		{
			thermalCut = false;
		}
		if(thermalCut) cut = true;
		
		
		if(battery.hasOverVoltageProtection())
		{
			if(battery.batteryProcess.getU() * 1.1 > battery.batterySlowProcess.getUMax())
			{
				if(battery.batteryProcess.getU() < battery.positiveLoad.Uc - battery.negativeLoad.Uc)
				{
					cut = true;
				}
			}		
			if(battery.batteryProcess.getU() < battery.batteryProcess.uNominal*0.001)
			{
				if(battery.batteryProcess.getU() > battery.positiveLoad.Uc - battery.negativeLoad.Uc)
				{
					cut = true;
				}
			}		
		}
		
		battery.batteryProcess.setCut(cut);

	}

}
