package mods.eln.mppt;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.IFunction;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;

public class MpptDescriptor extends TransparentNodeDescriptor{



	public MpptDescriptor(
			String name,
			double inUmin,double inUmax,
			double outUmin,double outUmax,
			
			double designedPout,	
			FunctionTable PoutfPin,
			double electricalLoadDropFactor,
			
			double inResistorLowHighTime,double inResistorNormalTime,
			double inResistorStepFactor,
			double inResistorMin,double inResistorMax
			) {
		super(name, MpptElement.class, MpptRender.class);
		
		this.inResistorLowHighTime = inResistorLowHighTime;
		this.inResistorMax = inResistorMax;
		this.inResistorMin = inResistorMin;
		this.inResistorNormalTime = inResistorNormalTime;
		this.inResistorStepFactor = inResistorStepFactor;
		this.inUmax = inUmax;
		this.inUmin = inUmin;
		
		this.outUmax = outUmax;
		this.outUmin = outUmin;
		
		this.designedPout = designedPout;
		this.PoutfPin = PoutfPin.duplicate(designedPout, designedPout);
		
		
		inElectricalLoadRs = inUmax*inUmax/designedPout*electricalLoadDropFactor/3;
		outElectricalLoadRs = outUmax*outUmax/designedPout*electricalLoadDropFactor/3;
		
		
	}
	public double inElectricalLoadRs,outElectricalLoadRs;
	
	public double inUmin,inUmax;
	public double outUmin,outUmax;
	
	public double designedPout;	
	public IFunction PoutfPin;
	
	public double inResistorLowHighTime,inResistorNormalTime;
	public double inResistorStepFactor;
	public double inResistorMin,inResistorMax;
	
	public void applylToIn(ElectricalLoad load,boolean grounded)
	{
		load.setRs(inElectricalLoadRs);
		load.setMinimalC(Eln.simulator);
		load.grounded(grounded);
	}
	public void applylToOut(ElectricalLoad load,boolean grounded)
	{
		load.setRs(outElectricalLoadRs);
		load.setMinimalC(Eln.simulator);
		load.grounded(grounded);
	}
}
