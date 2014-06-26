package mods.eln.node;

import mods.eln.INBTTReady;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.mna.component.Bipole;
import mods.eln.sim.mna.component.Component;
import net.minecraft.nbt.NBTTagCompound;

public class NodeElectricalLoad extends ElectricalLoad implements INBTTReady{
	String name;

	public NodeElectricalLoad(String name)
	{
		super();
		this.name = name;
	}
		
    public void readFromNBT(NBTTagCompound nbttagcompound, String str)
    {
    	setU(nbttagcompound.getFloat(str + name + "Uc"));	    
    	if(Double.isNaN(getU())) setU(0);
    	if(getU() == Float.NEGATIVE_INFINITY) setU(0);
    	if(getU() == Float.POSITIVE_INFINITY) setU(0);
    }

    public void writeToNBT(NBTTagCompound nbttagcompound, String str)
    {
    	nbttagcompound.setFloat(str + name + "Uc", (float)getU());
    }

	public double getI() {
		double i = 0;
		for(Component c : getConnectedComponents()){
			if(c instanceof Bipole)
				i += Math.abs(((Bipole)c).getCurrent());
		}
		return i*0.5;
	}
    
	public double getCurrent() {
		return getI();
	}
	

}
