package mods.eln.turbine;

import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.FunctionTable;
import mods.eln.sim.TurbineThermalProcess;
import mods.eln.thermalcable.ThermalCableDescriptor;

public class TurbineCoreDescriptor extends GenericItemUsingDamageDescriptor{

	public TurbineCoreDescriptor(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
/*
	public TurbineCoreDescriptor(
					String name,String description,
					FunctionTable TtoU,
					double nominalDeltaT, double nominalU,
					double baseEfficiency				
					) 
	{
		super( name);

	}

	public FunctionTable TtoU;
	public double nominalDeltaT = 1,nominalU = 1; 
	public double baseEfficiency = 1.0;

	public ElectricalCableDescriptor electricalCable;
	
	public void applyTo(TurbineThermalProcess turbine)
	{
		turbine.TtoU = TtoU;
		turbine.nominalDeltaT = nominalDeltaT;
		turbine.nominalU = nominalU;
		turbine.baseEfficiency = baseEfficiency;
	}*/
}
