package mods.eln.sim;

import mods.eln.misc.FunctionTable;
import mods.eln.misc.IFunction;
import mods.eln.misc.Utils;

public class TurbineThermalProcess implements IProcess{
	ThermalLoad warmLoad,coolLoad;
	ElectricalSourceECProcess electricalSource;
	
	
	public IFunction TtoU;
	public IFunction PintoPout;
	public double nominalDeltaT = 1,nominalU = 1; 
	//public double baseEfficiency = 1.0;
	
	public TurbineThermalProcess(ElectricalSourceECProcess electricalSource,ThermalLoad warmLoad,ThermalLoad coolLoad) {
		this.electricalSource = electricalSource;
		this.warmLoad = warmLoad;
		this.coolLoad = coolLoad;
	}
	
	double timeCounter = 0;
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		double deltaT = warmLoad.Tc - coolLoad.Tc;
		double energy = electricalSource.energyCounter;
		double efficiency = 0.01;
		double enginEfficiency = PintoPout.getValue(Math.abs(energy / time));
		if(deltaT >= 0)
		{
			efficiency = Math.abs(1 - (coolLoad.Tc + PhysicalConstant.Tref)/(warmLoad.Tc + PhysicalConstant.Tref));
			if(energy > 0) 
			{
				efficiency *= enginEfficiency;
			}
			else 		  
			{
				efficiency /= enginEfficiency;
			}
			if(efficiency<0.01)efficiency = 0.01;
				
			warmLoad.PcTemp -= energy / efficiency /time;
			coolLoad.PcTemp += (energy / efficiency - energy) /time;
		}
		if(deltaT < 0)
		{
			efficiency = Math.abs(1 - (warmLoad.Tc + PhysicalConstant.Tref)/(coolLoad.Tc + PhysicalConstant.Tref));
			
			if(energy > 0.0) 
			{
				efficiency *= enginEfficiency;
			}
			else 		  
			{
				efficiency /= enginEfficiency;
			}
			
			if(efficiency<0.01)efficiency = 0.01;
				
			coolLoad.PcTemp -= energy / efficiency /time;
			warmLoad.PcTemp += (energy / efficiency - energy) /time;
		}
		double U = 0;
		if(TtoU != null)
		{
			U = Math.signum(deltaT)*TtoU.getValue(Math.signum(deltaT)*deltaT/nominalDeltaT)*nominalU;
		}
		
		electricalSource.U = U;		
		
		if(timeCounter > 1.0)
		{
			timeCounter -= 1.0;
			Utils.println("Eletrical : " + (int)(electricalSource.energyCounter / time) + "  thermal : " + (int)(( energy / efficiency) / time) + "  target % : " + (int)(100*efficiency) +  "  hit % : " + (int)(100*electricalSource.energyCounter/( energy / efficiency))  );
		}
		electricalSource.energyCounter = 0;
		timeCounter += time;

	}

}

/*
 * 		if(energy!=0)
		{
			if(energy>0)
			{
				efficiency *= baseEfficiency;
			}
			else
			{
				efficiency /= baseEfficiency; 
			}*/
