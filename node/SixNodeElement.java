package mods.eln.node;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.LanguageRegistry;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public abstract class SixNodeElement implements INBTTReady {
	//private static Class[] idToClass = new Class[256];
	//private static Class[] idToRenderClass = new Class[256];
	

	public ArrayList<IProcess> slowProcessList  = new ArrayList<IProcess>(4);

	public ArrayList<IProcess> electricalProcessList = new ArrayList<IProcess>(4);
	public ArrayList<ElectricalConnection> electricalConnectionList = new ArrayList<ElectricalConnection>(4);
	public ArrayList<NodeElectricalLoad> electricalLoadList = new ArrayList<NodeElectricalLoad>(4);
	
	public ArrayList<IProcess> thermalProcessList = new ArrayList<IProcess>(4);
	public ArrayList<ThermalConnection> thermalConnectionList = new ArrayList<ThermalConnection>(4);
	public ArrayList<NodeThermalLoad> thermalLoadList = new ArrayList<NodeThermalLoad>(4);
	
	
	public SixNode sixNode;
	public Direction side;
	public SixNodeDescriptor sixNodeElementDescriptor;
	public int isProvidingWeakPower()
	{
		return 0;
	}
	protected void inventoryChanged() {
		// TODO Auto-generated method stub

	}
	
	public void sendPacketToAllClient(ByteArrayOutputStream bos) {
		// TODO Auto-generated method stub
		sixNode.sendPacketToAllClient(bos);
	}

    public void sendPacketToClient(ByteArrayOutputStream bos,Player player)
    {
    	sixNode.sendPacketToClient(bos, player);
    } 
	public void notifyNeighbor()
	{
		sixNode.notifyNeighbor();
	}
	
	public void connectJob()
	{
		Eln.simulator.addAllElectricalConnection(electricalConnectionList);
		Eln.simulator.addAllThermalConnection(thermalConnectionList);

		for(NodeElectricalLoad load : electricalLoadList)Eln.simulator.addElectricalLoad(load);
		for(NodeThermalLoad load : thermalLoadList)Eln.simulator.addThermalLoad(load);

		for(IProcess process : slowProcessList)Eln.simulator.addSlowProcess(process);
		for(IProcess process : electricalProcessList)Eln.simulator.addElectricalProcess(process);
		for(IProcess process : thermalProcessList)Eln.simulator.addThermalProcess(process);				

	}
		
	public void networkUnserialize(DataInputStream stream) 
	{

	}
	public void networkUnserialize(DataInputStream stream,Player player) 
	{
		networkUnserialize(stream);
	}
	
	public int getLightValue() 
	{
		return 0;
	}
	
	public boolean hasGui()
	{
		return false;
	}
	
	public IInventory getInventory()
	{
		return null;
	}
	
	public Container newContainer(Direction side,EntityPlayer player)
	{
		return null;
	}

	public SixNodeElement(SixNode sixNode,Direction side,SixNodeDescriptor descriptor)
	{
		this.sixNode = sixNode;
		this.side = side;
		this.sixNodeElementDescriptor = descriptor;
	}
	
	public static boolean canBePlacedOnSide(Direction side,SixNodeDescriptor descriptor)
	{
		return true;
	}
    public void preparePacketForClient(DataOutputStream stream)
    {
    	sixNode.preparePacketForClient(stream, this);    	
    }
/*	static boolean  staticInitialized = false;
	public static void staticInitialize()
	{
		for(int idx = 0;idx<256;idx++)
		{
			idToClass[idx] = null;
			idToRenderClass[idx] = null;
		}
		staticInitialized = true;
	}*/
	/*
	public static void registerElement(int id,Class sixNodeElementClass,Class sixNodeElementRenderClass)
	{
		if(!staticInitialized) staticInitialize();
		if(id == 0)
		{
			System.out.println("Reserved ID sixenodeelement");
			while(true);
		}
		if(idToClass[id] != null)
		{
			System.out.println("used ID sixenodeelement");
			while(true);	
		}


		idToClass[id] = sixNodeElementClass;
		idToRenderClass[id] = sixNodeElementRenderClass;
	}*/
	

	


	
    public abstract ElectricalLoad getElectricalLoad(LRDU lrdu);
	public abstract ThermalLoad getThermalLoad(LRDU lrdu);
	
	public abstract int getConnectionMask(LRDU lrdu);
	
	
	
	public abstract String multiMeterString();
	public abstract String thermoMeterString();
	
	public LRDU front = LRDU.Up;
	public  void networkSerialize(DataOutputStream stream)
	{
		
		try {
			stream.writeByte(sixNode.lrduElementMask.get(side).mask  + (front.dir<<4));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
   /* public void initializeFromThat(SixNodeDescriptor descriptor)
    {
    	this.descriptor = descriptor;
    	initialize();
    }*/
    public abstract void initialize();
    
    public void destroy()
    {
    	sixNode.dropInventory(getInventory());
    	sixNode.dropItem(getDropItemStack());
    }

	public abstract boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz);

	/*
	public void onBreakElement()
	{
		sixNode.dropInventory(getInventory());
		
	}*/
	
	public ItemStack getDropItemStack()
	{
		return new ItemStack(Eln.sixNodeBlock, 1, sixNode.sideElementIdList[side.getInt()]);
	}	
	
	public void readFromNBT(NBTTagCompound nbt,String str)
    {
    	
        int idx;
        
        IInventory inv = getInventory();
        if(inv != null)
        {
        	Utils.readFromNBT(nbt, str + "inv", inv);
        }
        

        
        idx = 0;
		for(NodeElectricalLoad electricalLoad : electricalLoadList) 
		{
			electricalLoad.readFromNBT(nbt,str);
		}

		for(NodeThermalLoad thermalLoad : thermalLoadList) 
		{
			thermalLoad.readFromNBT(nbt,str);
		}
		
		for(IProcess process : slowProcessList) 
		{
			if(process instanceof INBTTReady) ((INBTTReady)process).readFromNBT(nbt,str);
		}
		for(IProcess process : electricalProcessList) 
		{
			if(process instanceof INBTTReady) ((INBTTReady)process).readFromNBT(nbt,str);
		}
		for(IProcess process : thermalProcessList) 
		{
			if(process instanceof INBTTReady) ((INBTTReady)process).readFromNBT(nbt,str);
		}
		
    }

    
    

    public void writeToNBT(NBTTagCompound nbt,String str)
    {
      
        int idx;

        IInventory inv = getInventory();
        if(inv != null)
        {
        	Utils.writeToNBT(nbt, str + "inv", inv);
        }
        

        
        
        idx = 0;
		for(NodeElectricalLoad electricalLoad : electricalLoadList) 
		{
			electricalLoad.writeToNBT(nbt,str);
		}

		for(NodeThermalLoad thermalLoad : thermalLoadList) 
		{
			thermalLoad.writeToNBT(nbt,str);
		}
		
		for(IProcess process : slowProcessList) 
		{
			if(process instanceof INBTTReady) ((INBTTReady)process).writeToNBT(nbt,str);
		}
		for(IProcess process : electricalProcessList) 
		{
			if(process instanceof INBTTReady) ((INBTTReady)process).writeToNBT(nbt,str);
		}
		for(IProcess process : thermalProcessList) 
		{
			if(process instanceof INBTTReady) ((INBTTReady)process).writeToNBT(nbt,str);
		}
           
    }
    
    public void reconnect()
    {
    	sixNode.reconnect();
    }
    
    public void needPublish()
    {
    	sixNode.setNeedPublish(true);
    }
    
    public void disconnectJob()
    {
		Eln.simulator.removeAllElectricalConnection(electricalConnectionList);
		Eln.simulator.removeAllThermalConnection(thermalConnectionList);

		for(NodeElectricalLoad load : electricalLoadList)Eln.simulator.removeElectricalLoad(load);
		for(NodeThermalLoad load : thermalLoadList)Eln.simulator.removeThermalLoad(load);

		for(IProcess process : slowProcessList)Eln.simulator.removeSlowProcess(process);
		for(IProcess process : electricalProcessList)Eln.simulator.removeElectricalProcess(process);
		for(IProcess process : thermalProcessList)Eln.simulator.removeThermalProcess(process);				

    }
	public boolean canConnectRedstone() {
		// TODO Auto-generated method stub
		return false;
	}
    

}
