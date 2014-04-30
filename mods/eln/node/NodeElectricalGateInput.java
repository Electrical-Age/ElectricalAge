package mods.eln.node;

import mods.eln.Eln;
import mods.eln.misc.Utils;

public class NodeElectricalGateInput extends NodeElectricalLoad{

	public NodeElectricalGateInput(String name) {
		super(name);
		Eln.instance.signalCableDescriptor.applyTo(this,false);
		//this.setRp(Eln.instance.SVU/Eln.instance.SVII);
	}

	public String plot(String str)
	{
		return str  + " "+ Utils.plotVolt("", Uc) + Utils.plotAmpere("", getCurrent());
	}
	
	
	public boolean stateHigh()
	{
		return Uc > Eln.SVU * 0.6;
	}
	public boolean stateLow()
	{
		return Uc < Eln.SVU * 0.2;
	}
	
	public double getNormalized()
	{
		double norm =  Uc * Eln.SVUinv;
		if(norm < 0.0) norm = 0.0;
		if(norm > 1.0) norm = 1.0;
		return norm;
	}
	
	public double getBornedU()
	{
		double U = this.Uc;
		if(U < 0.0) U = 0.0;
		if(U > Eln.SVU) U = Eln.SVU;
		return U;
	}
}
