package mods.eln.node;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import cpw.mods.fml.common.FMLCommonHandler;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUCubeMask;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public abstract class Node extends NodeBase {

	
	private int lastLight = 0;
	public void setLightValue(int light)
	{
		if(light>15)light = 15;
		if(light<0)light = 0;
		if(lastLight != light)
		{
			lastLight = light;
			coordonate.world().updateLightByType(EnumSkyBlock.Block,coordonate.x, coordonate.y, coordonate.z);
			setNeedPublish(true);
		}
		
	}
	
	public int getLightValue()
	{
		return lastLight;
	}
	


    public void readFromNBT(NBTTagCompound nbt)
    {
    	super.readFromNBT(nbt);
        
        lastLight = nbt.getByte("lastLight");        
    }

    
    

    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        nbt.setByte("lastLight",(byte) lastLight);
    }
	
    boolean oldSendedRedstone = false;

    public void networkSerialize(DataOutputStream stream) {
    	super.networkSerialize(stream);
    	
    	try {
    		boolean redstone = canConnectRedstone();
			stream.writeByte(lastLight | (redstone ? 0x10 : 0x00));
			if(redstone != oldSendedRedstone)
				needNotify = true;
			oldSendedRedstone = redstone;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    
    
    public NodeBlockEntity getEntity()
    {
    	return (NodeBlockEntity) coordonate.world().getTileEntity(coordonate.x, coordonate.y, coordonate.z);
    }
    
	int isProvidingWeakPower(Direction side) {
		// TODO Auto-generated method stub
		return 0;
	}
	public boolean canConnectRedstone()
	{
		return false;
	}


}
