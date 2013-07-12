package mods.eln.turbine;

import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalPowerSource;
import mods.eln.sim.IProcess;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.ThermalLoad;


public class TurbineInOutProcess implements IProcess{
	TurbineElement turbine;
	double timeCounter = 0,energyCounterGlobal = 0;
	static int staticId = 0;
	int id;
	public TurbineInOutProcess(TurbineElement turbine) {
		this.turbine = turbine;
		id = staticId++;
	}
	
	@Override
	public void process(double time) {
		TurbineDescriptor descriptor = turbine.descriptor;
		double deltaT = turbine.warmLoad.Tc - turbine.coolLoad.Tc;
		if(deltaT < 0) return;
		double deltaU = turbine.positiveLoad.Uc - turbine.negativeLoad.Uc;
		double targetU = descriptor.TtoU.getValue(deltaT);

		ElectricalPowerSource eps = turbine.electricalPowerSourceProcess;
		eps.setUmax(targetU);
		if(targetU - deltaU > 0)
		{
			eps.setP((targetU - deltaU) * descriptor.powerOutPerDeltaU);
		}
		else
		{
			eps.setP(0);
		}
		
		double eff  = Math.abs(1 - (turbine.coolLoad.Tc + PhysicalConstant.Tref)/(turbine.warmLoad.Tc + PhysicalConstant.Tref));
		if(eff < 0.05) eff = 0.05;
		//eff = 0.4;
		double E = eps.getEnergyCounter();
		energyCounterGlobal += E;
		double Pout = E/time;
		double Pin = descriptor.PoutToPin.getValue(Pout) / eff;
		turbine.warmLoad.movePowerTo(-Pin);
		turbine.coolLoad.movePowerTo(Pin * (1 - eff));
		//ThermalLoad.movePower(Pin, turbine.warmLoad, turbine.coolLoad);		
		eps.clearEnergyCounter();
		timeCounter+=time;
		if(timeCounter >= 1.0){
			timeCounter -= 1.0;
			System.out.println("Turbine " + id + " : " + Utils.plotPower("Pin : ", Pin) + Utils.plotPower("Pout : ", Pout) + Utils.plotEnergy("Pavg", energyCounterGlobal));
			energyCounterGlobal = 0;
		}
	}

}
