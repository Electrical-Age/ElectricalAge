package mods.eln.item;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.FunctionTable;
import mods.eln.sim.ElectricalLoad;

public class DynamoDescriptor extends GenericItemUsingDamageDescriptor{

	public DynamoDescriptor(
			String name,
			FunctionTable PoutfPin,
			FunctionTable UfPout,
			double electricalMaxU,double electricalMaxP,double electricalDropFactor
			) {
		super(name);
		this.electricalMaxP = electricalMaxP;
		this.electricalMaxU = electricalMaxU;
		
		this.PoutfPin = PoutfPin.duplicate(electricalMaxP, electricalMaxP);
		this.UfPout = UfPout.duplicate(electricalMaxP, electricalMaxU);
		electricalRs = electricalMaxU*electricalMaxU*electricalDropFactor/(electricalMaxP)/2;
	}

	public double electricalMaxU, electricalMaxP;
	public double electricalRs;
	public FunctionTable UfPout,PoutfPin;
	public void applyTo(ElectricalLoad load,boolean grounded)
	{

		load.setRs(electricalRs);
		load.setMinimalC(Eln.simulator);
		load.grounded(grounded);
	}
}
