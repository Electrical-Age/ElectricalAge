package mods.eln.sim;

import mods.eln.INBTTReady;
import net.minecraft.nbt.NBTTagCompound;

public class RegulatorThermalLoadToElectricalResistor extends RegulatorProcess{

	ThermalLoad thermalLoad;
	ElectricalResistor electricalResistor;
	
	double Rmin;
	
	public void setRmin(double Rmin)
	{
		this.Rmin = Rmin;
	}
	
	public RegulatorThermalLoadToElectricalResistor(String name,ThermalLoad thermalLoad,ElectricalResistor electricalResistor) 
	{
		super(name);
		this.thermalLoad = thermalLoad;
		this.electricalResistor = electricalResistor;
	}
	@Override
	protected double getHit() {
		// TODO Auto-generated method stub
		return thermalLoad.Tc;
	}

	@Override
	protected void setCmd(double cmd) {
		if(cmd <= 0.001)
		{
			electricalResistor.highImpedance();
		}
		else if(cmd >= 1.0)
		{
			electricalResistor.setR(Rmin);
		}
		else
		{
			electricalResistor.setR(Rmin/cmd);
		}
	}

}
