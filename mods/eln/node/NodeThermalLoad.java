package mods.eln.node;

import mods.eln.INBTTReady;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.NBTTagCompound;

public class NodeThermalLoad extends ThermalLoad implements INBTTReady{
	String name;
	public NodeThermalLoad(String name,double Tc,double Rp,double Rs,double C)
	{
		super(Tc,Rp,Rs,C);
		this.name = name;
	}
	public NodeThermalLoad(String name)
	{
		super();
		this.name = name;
	}
		
	
    public void readFromNBT(NBTTagCompound nbttagcompound,String str)
    {
    	Tc = nbttagcompound.getFloat(str + name + "Tc");	      
    	if(Double.isNaN(Tc)) Tc = 0;
    	if(Tc == Float.NEGATIVE_INFINITY) Tc = 0;
    	if(Tc == Float.POSITIVE_INFINITY) Tc = 0;
    }

    public void writeToNBT(NBTTagCompound nbttagcompound,String str)
    {
    	nbttagcompound.setFloat(str + name + "Tc", (float)Tc);
    }
}
