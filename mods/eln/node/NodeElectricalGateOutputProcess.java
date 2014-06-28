package mods.eln.node;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.NodeVoltageState;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.Capacitor;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;

public class NodeElectricalGateOutputProcess extends Capacitor implements INBTTReady{
	

	double U;
	String name;
	public NodeElectricalGateOutputProcess(String name,ElectricalLoad positiveLoad) {
		super(positiveLoad, null);
		this.name = name;
		setHighImpedance(false);
	}
		
	public void setHighImpedance(boolean enable){
		this.highImpedance = enable;
		double baseC = Eln.instance.gateOutputCurrent/Eln.instance.electricalFrequancy/Eln.SVU;
		if(enable){
			setC(baseC/1000);
		} else {
			setC(baseC);
		}
	}
	
	@Override
	public void simProcessI(SubSystem s) {
		aPin.state = U;
		super.simProcessI(s);
	}
	
	public boolean isHighImpedance(){
		return highImpedance;
	}
	
	boolean highImpedance = false;
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		setHighImpedance(nbt.getBoolean(name + "highImpedance"));
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
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

	public void setU(double U) {
		this.U = U;
	}
	
	

	
	
}
