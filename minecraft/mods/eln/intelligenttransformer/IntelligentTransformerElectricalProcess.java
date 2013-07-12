package mods.eln.intelligenttransformer;

import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;

public class IntelligentTransformerElectricalProcess implements IProcess{
	
	ElectricalLoad positivePrimaryLoad;
	ElectricalLoad negativePrimaryLoad;
	ElectricalLoad positiveSecondaryLoad;
	ElectricalLoad negativeSecondaryLoad;
	
	public IntelligentTransformerElectricalProcess(ElectricalLoad positivePrimaryLoad,ElectricalLoad negativePrimaryLoad,ElectricalLoad positiveSecondaryLoad,ElectricalLoad negativeSecondaryLoad) {
		this.positivePrimaryLoad = positivePrimaryLoad;
		this.negativePrimaryLoad = negativePrimaryLoad;
		this.positiveSecondaryLoad = positiveSecondaryLoad;
		this.negativeSecondaryLoad = negativeSecondaryLoad;
				
	}
	
	double primaryUmin;
	double secondaryUmin;
	double powerMax;
	
	public void setMinMin(double primaryMin,double secondaryMin)
	{
		this.primaryUmin = primaryMin;
		this.secondaryUmin = secondaryMin;
	}

	public void setPowerMax(double powerMax)
	{
		this.powerMax = powerMax;
	}
	
	@Override
	public void process(double time) {
		double primaryU = positivePrimaryLoad.Uc - negativePrimaryLoad.Uc;
		double secondaryU = positiveSecondaryLoad.Uc - negativeSecondaryLoad.Uc;
		double primaryE = 0.5 * 0.5 * positivePrimaryLoad.getC() * (primaryU * primaryU);	
		double secondaryE = 0.5 * 0.5 * positiveSecondaryLoad.getC() * (secondaryU * secondaryU);	
		
		double energy = 0;
		if(primaryU > primaryUmin && secondaryU < secondaryUmin)
		{
			energy =		      0.5 * 0.5 * positiveSecondaryLoad.getC()  
					*	(secondaryUmin * secondaryUmin - secondaryU * secondaryU);			
			if(energy / time > powerMax) energy = powerMax * time;			
		}
		if(primaryU < primaryUmin && secondaryU > secondaryUmin)
		{
			energy =		      - 0.5 * 0.5 * positivePrimaryLoad.getC()  
					*	(primaryUmin * primaryUmin - primaryU * primaryU);			
			if(energy / time < -powerMax) energy = - powerMax * time;	
		}
		
		double primaryI = (Math.sqrt(4 * (primaryE - energy) * positivePrimaryLoad.invC) - primaryU)
				* positivePrimaryLoad.getC() * 0.5 / time;
		double secondaryI = (Math.sqrt(4 * (secondaryE + energy) * positiveSecondaryLoad.invC) - secondaryU)
				* positiveSecondaryLoad.getC() *0.5 / time;

		ElectricalLoad.moveCurrent(primaryI, negativePrimaryLoad, positivePrimaryLoad);
		ElectricalLoad.moveCurrent(secondaryI, negativeSecondaryLoad, positiveSecondaryLoad);
	}

}
