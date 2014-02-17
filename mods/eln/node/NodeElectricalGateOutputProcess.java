package mods.eln.node;

import mods.eln.Eln;
import mods.eln.sim.ElectricalLoad;
import net.minecraft.nbt.NBTTagCompound;

public class NodeElectricalGateOutputProcess extends NodeElectricalSourceWithCurrentLimitationProcess{

	public NodeElectricalGateOutputProcess(String name, ElectricalLoad positiveLoad) {
		super(name, positiveLoad, ElectricalLoad.groundLoad, 0, Eln.instance.gateOutputCurrent);
		// TODO Auto-generated constructor stub
	}
	
	public void setHighImpedance(boolean enable){
		this.highImpedance = enable;
		if(highImpedance)
			Imax = 0;
		else
			Imax = Eln.instance.gateOutputCurrent;
	}
	public boolean isHighImpedance(){
		return highImpedance;
	}
	
	boolean highImpedance = false;
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
		setHighImpedance(nbt.getBoolean(name + "highImpedance"));
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setBoolean(name + "highImpedance", highImpedance);
	}
	
	public void setOutputNormalized(double value)
	{
		setOutputNormalizedSafe(value);
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
