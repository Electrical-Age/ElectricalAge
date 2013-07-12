package mods.eln.node;

import mods.eln.INBTTReady;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import net.minecraft.nbt.NBTTagCompound;

public class NodeElectricalLoad extends ElectricalLoad implements INBTTReady{
	String name;
	public NodeElectricalLoad(String name,double Uc,double Rp,double Rs,double C)
	{
		super(Uc,Rp,Rs,C);
		this.name = name;
	}
	public NodeElectricalLoad(String name)
	{
		super();
		this.name = name;
	}
		
    public void readFromNBT(NBTTagCompound nbttagcompound, String str)
    {
    	Uc = nbttagcompound.getFloat(str + name + "Uc");	    
    	if(Double.isNaN(Uc)) Uc = 0;
    	if(Uc == Float.NEGATIVE_INFINITY) Uc = 0;
    	if(Uc == Float.POSITIVE_INFINITY) Uc = 0;
    }

    public void writeToNBT(NBTTagCompound nbttagcompound, String str)
    {
    	nbttagcompound.setFloat(str + name + "Uc", (float)Uc);
    }
    

}
