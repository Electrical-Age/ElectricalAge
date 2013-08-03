package mods.eln.ghost;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
/*
public class GhostEntity extends TileEntity {
	
	public GhostEntity() {
		// TODO Auto-generated constructor stub
	}
	
    @Override
    public boolean canUpdate() {
    	// TODO Auto-generated method stub
    	return true;
    }
    
    int updateCounter = 0;
    @Override
    public void updateEntity() {
    	if(updateCounter != 20)
    	{
    		updateCounter++;
    	}
    	else
    	{
    		if(!worldObj.isRemote)
    		{
        		if(getGhost() == null) //to verrifie todo
        		{
        //			worldObj.setBlock(xCoord, yCoord, zCoord, 0);
        		}
    		}    		
    	}
    }
    
    
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
    	// TODO Auto-generated method stub
    	super.readFromNBT(nbt); 	
    }
    
    @Override
    public void writeToNBT(NBTTagCompound nbt) {
    	// TODO Auto-generated method stub
    	super.writeToNBT(nbt);
    }
    
    public GhostElement getGhost()
    {
    	return Eln.ghostManager.getGhost(new Coordonate(this));
    }
}*/
