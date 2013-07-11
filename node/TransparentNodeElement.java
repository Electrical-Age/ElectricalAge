package mods.eln.node;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.CaseFormat;

import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.LanguageRegistry;


import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.lampsocket.LampSocketElement;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public abstract class TransparentNodeElement implements INBTTReady {

	public ArrayList<IProcess> slowProcessList  = new ArrayList<IProcess>(4);

	public ArrayList<IProcess> electricalProcessList = new ArrayList<IProcess>(4);
	public ArrayList<ElectricalConnection> electricalConnectionList = new ArrayList<ElectricalConnection>(4);
	public ArrayList<NodeElectricalLoad> electricalLoadList = new ArrayList<NodeElectricalLoad>(4);
	
	public ArrayList<IProcess> thermalProcessList = new ArrayList<IProcess>(4);
	public ArrayList<ThermalConnection> thermalConnectionList = new ArrayList<ThermalConnection>(4);
	public ArrayList<NodeThermalLoad> thermalLoadList = new ArrayList<NodeThermalLoad>(4);
	
	
	public static final byte unserializeGroundedId = -127;
	public static final byte unserializeNulldId = -128;
	TransparentNodeDescriptor transparentNodeDescriptor;
	protected void serialiseItemStack(DataOutputStream stream,ItemStack stack) throws IOException
	{
		Utils.serialiseItemStack(stream,stack);
	}
	protected int serialiseItemStackSize()
	{
		return 4;
	}
	public void connectJob()
	{
		Eln.simulator.addAllSlowProcess(slowProcessList);
		
		Eln.simulator.addAllElectricalConnection(electricalConnectionList);
		for(NodeElectricalLoad load : electricalLoadList)Eln.simulator.addElectricalLoad(load);
		Eln.simulator.addAllElectricalProcess(electricalProcessList);
		
		Eln.simulator.addAllThermalConnection(thermalConnectionList);
		for(NodeThermalLoad load : thermalLoadList)Eln.simulator.addThermalLoad(load);
		Eln.simulator.addAllThermalProcess(thermalProcessList);
	}
	public void disconnectJob()
	{
		Eln.simulator.removeAllSlowProcess(slowProcessList);
		
		Eln.simulator.removeAllElectricalConnection(electricalConnectionList);
		for(NodeElectricalLoad load : electricalLoadList)Eln.simulator.removeElectricalLoad(load);
		Eln.simulator.removeAllElectricalProcess(electricalProcessList);
		
		Eln.simulator.removeAllThermalConnection(thermalConnectionList);
		for(NodeThermalLoad load : thermalLoadList)Eln.simulator.removeThermalLoad(load);
		Eln.simulator.removeAllThermalProcess(thermalProcessList);
	}

	public TransparentNode node;
	public Direction front;
	public boolean grounded = true;
	
	
	public void onGroundedChangedByClient()
	{
		needPublish();
	}

	public void networkUnserialize(DataInputStream stream,Player player) 
	{
		networkUnserialize(stream);
	}
	public byte networkUnserialize(DataInputStream stream) 
	{
		byte readed;
		try {
			switch(readed = stream.readByte())
			{
			case unserializeGroundedId:
				grounded = stream.readByte() != 0 ? true : false;
				onGroundedChangedByClient();
				return unserializeNulldId;
				default:
					return readed;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return unserializeNulldId;
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

	public TransparentNodeElement(TransparentNode transparentNode,TransparentNodeDescriptor descriptor)
	{
		this.node = transparentNode;
		this.transparentNodeDescriptor = descriptor;
	}
	

	
	
	public void onNeighborBlockChange() 
	{
		checkCanStay(false);
	}
	
	
	public void checkCanStay(boolean onCreate) {
		Block block;
		boolean needDestroy = false;
		if(transparentNodeDescriptor.mustHaveFloor())
		{
			block = Block.blocksList[node.neighborBlock[Direction.YN.getInt()]];
			if(block == null || ! block.isOpaqueCube()) needDestroy = true;
		}
		if(transparentNodeDescriptor.mustHaveCeiling())
		{
			block = Block.blocksList[node.neighborBlock[Direction.YP.getInt()]];
			if(block == null || ! block.isOpaqueCube()) needDestroy = true;
		}
		if(transparentNodeDescriptor.mustHaveWallFrontInverse())
		{
			block = Block.blocksList[node.neighborBlock[front.getInverse().getInt()]];
			if(block == null || ! block.isOpaqueCube()) needDestroy = true;
		}
		if(transparentNodeDescriptor.mustHaveWall())
		{
			boolean wall = false;
			block = Block.blocksList[node.neighborBlock[Direction.XN.getInt()]];
			if(block != null && block.isOpaqueCube()) wall = true;
			block = Block.blocksList[node.neighborBlock[Direction.XP.getInt()]];
			if(block != null && block.isOpaqueCube()) wall = true;
			block = Block.blocksList[node.neighborBlock[Direction.ZN.getInt()]];
			if(block != null && block.isOpaqueCube()) wall = true;
			block = Block.blocksList[node.neighborBlock[Direction.ZP.getInt()]];
			if(block != null && block.isOpaqueCube()) wall = true;
			
			if(! wall) needDestroy = true;
		}
		
		if(needDestroy)
		{
			selfDestroy();
		}
	}
	public void selfDestroy()
	{
		node.physicalSelfDestruction(0f);
	}
	/*
	public static boolean canBePlacedOnSide(Direction side,int type)
	{
		return true;
	}
	*/
	public void onBreakElement()
	{
		node.dropInventory(getInventory());
		node.dropItem(getDropItemStack());
	}
	public ItemStack getDropItemStack()
	{
		ItemStack itemStack =  new ItemStack(Eln.transparentNodeBlock, 1, node.elementId);
		itemStack.setTagCompound(getItemStackNBT());
		return itemStack;
	}	
	
	public NBTTagCompound getItemStackNBT()
	{
		return null;
	}


	
	
	
    public abstract ElectricalLoad getElectricalLoad(Direction side,LRDU lrdu);
	public abstract ThermalLoad getThermalLoad(Direction side,LRDU lrdu);
	
	public abstract int getConnectionMask(Direction side,LRDU lrdu);
	
	
	
	public abstract String multiMeterString(Direction side);
	public abstract String thermoMeterString(Direction side);
	

	public void networkSerialize(DataOutputStream stream)
	{
		try {
			stream.writeByte(front.getInt() + (grounded ? 8 : 0));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
    public void initializeFromThat(Direction front, EntityLiving entityLiving,NBTTagCompound itemStackNbt)
    {
    	this.front = front;
    	readItemStackNBT(itemStackNbt);
    	initialize();
    }
    public abstract void initialize();
    
    public void readItemStackNBT(NBTTagCompound nbt)
    {
    	
    }
    
    
  //  public abstract void destroyFrom(SixNode sixNode);

	public abstract boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz);

	
		
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
			
		
		byte b = nbt.getByte(str + "others");
		front = Direction.fromInt(b & 0x7);		
		grounded = (b & 8) != 0;
	}
	
	    
	    

    public void writeToNBT(NBTTagCompound nbt,String str)
    {
        int idx = 0;
        
        IInventory inv = getInventory();
        if(inv != null)
        {
        	Utils.writeToNBT(nbt, str + "inv", inv);
        }
        
		for(NodeElectricalLoad electricalLoad : electricalLoadList) 
		{
			electricalLoad.writeToNBT(nbt,str );
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


        nbt.setByte(str + "others",(byte) (front.getInt() + (grounded ? 8 : 0))) ;
    }
    
    public void reconnect()
    {
    	node.reconnect();
    }
    
    public void needPublish()
    {
    	node.setNeedPublish(true);
    }
    
    
    public void connect()
    {
    	node.connect();
    }
    public void disconnect()
    {
    	node.disconnect();
    }
    
    
    public void inventoryChange(IInventory inventory)
    {
    	
    }
    
	public float getLightOpacity() {
		// TODO Auto-generated method stub
		return 0f;
	}
    
    

    

}
