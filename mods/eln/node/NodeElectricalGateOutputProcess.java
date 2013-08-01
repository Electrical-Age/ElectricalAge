package mods.eln.node;

import mods.eln.Eln;
import mods.eln.sim.ElectricalLoad;
import net.minecraft.nbt.NBTTagCompound;

public class NodeElectricalGateOutputProcess extends NodeElectricalSourceWithCurrentLimitationProcess{

	public NodeElectricalGateOutputProcess(String name, ElectricalLoad positiveLoad) {
		super(name, positiveLoad, ElectricalLoad.groundLoad, 0, Eln.instance.gateOutputCurrent);
		// TODO Auto-generated constructor stub
	}

	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
		Imax = Eln.instance.gateOutputCurrent;
	}
	
	public void setOutputNormalized(double value)
	{
		U = value * Eln.SVU;
	}

	public void state(boolean value)
	{
		if(value)
			U = Eln.SVU;
		else
			U = 0.0;
	}


	public double getOutputNormalized() {
		return U/Eln.SVU;
	}
	public boolean getOutputOnOff() {
		// TODO Auto-generated method stub
		return U >= Eln.SVU/2;
	}


	public void setOutputNormalizedSafe(double value) {
		if(value > 1.0) value = 1.0;
		if(value < 0.0)value = 0.0;
		if(Double.isNaN(value)) value = 0.0;
		U = value * Eln.SVU;
	}
}
