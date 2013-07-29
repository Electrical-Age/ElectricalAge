package mods.eln.node;

import java.awt.List;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.Player;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.SixNodeCacheItem;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUCubeMask;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

public class SixNode extends Node {

	public SixNodeElement sideElementList[] = new SixNodeElement[6];
	public int sideElementIdList[] = new int[6];
	public ArrayList<ElectricalConnection> internalElectricalConnectionList = new ArrayList<ElectricalConnection>(1);
	public ArrayList<ThermalConnection> internalThermalConnectionList = new ArrayList<ThermalConnection>(1);

	public int sixNodeCacheMapId = -1;
	
	public LRDUCubeMask lrduElementMask = new LRDUCubeMask();

	
	public SixNodeElement getElement(Direction side)
	{
		return sideElementList[side.getInt()];
	}
	@Override
	public boolean canConnectRedstone() {
		for(SixNodeElement element : sideElementList)
		{
			if(element != null)
			{
				if(element.canConnectRedstone()) return true;
			}
		}
		return false;
	}
	@Override
	int isProvidingWeakPower(Direction side)
	{
		int value = 0;
		for(SixNodeElement element : sideElementList)
		{
			if(element != null)
			{
				int eValue = element.isProvidingWeakPower();
				if(eValue > value) value = eValue;
			}
		}
		return value;
	}

	public SixNode()
	{
		for(int idx = 0;idx<6;idx++)
		{
			sideElementList[idx] = null;
			sideElementIdList[idx] = 0;
		}
		lrduElementMask.clear();
	}
	

	
	public boolean createSubBlock(ItemStack itemStack, Direction direction) {
		// TODO Auto-generated method stub
		SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(itemStack);
		if(sideElementList[direction.getInt()]  != null) return false;
		try {
			//Object bool = descriptor.ElementClass.getMethod("canBePlacedOnSide",Direction.class,SixNodeDescriptor.class).invoke(null, direction,descriptor);
			//if((Boolean)bool == false) return false;
			sideElementList[direction.getInt()] =  (SixNodeElement) descriptor.ElementClass.getConstructor(SixNode.class,Direction.class,SixNodeDescriptor.class).newInstance(this,direction,descriptor);	
			
			disconnect();
			sideElementList[direction.getInt()].initialize();
			sideElementIdList[direction.getInt()] = itemStack.getItemDamage();
			connect();
			
			System.out.println("createSubBlock " + sideElementIdList[direction.getInt()] + " " + direction);
			
			setNeedPublish(true);
			return true;
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
/*
    protected void dropItem(ItemStack itemStack)
    {
    	
        if (coordonate.world().getGameRules().getGameRuleBooleanValue("doTileDrops"))
        {
            float var6 = 0.7F;
            double var7 = (double)(coordonate.world().rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
            double var9 = (double)(coordonate.world().rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
            double var11 = (double)(coordonate.world().rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
            EntityItem var13 = new EntityItem(coordonate.world(), (double)coordonate.x + var7, (double)coordonate.y + var9, (double)coordonate.z + var11, itemStack);
            var13.delayBeforeCanPickup = 10;
            coordonate.world().spawnEntityInWorld(var13);
        }
    }*/
    
	public boolean deleteSubBlock(Direction direction) {
		// TODO Auto-generated method stub
		if(sideElementList[direction.getInt()] == null) return false;
		
		
		
		System.out.println("deleteSubBlock "+ " " + direction);
		/*
		if(sideElementList[direction.getInt()].dropItems())
		{	
			dropItem(new ItemStack(Eln.sixNodeBlock, 1, sideElementIdList[direction.getInt()] + (sideElementList[direction.getInt()].type<<8)));
		}*/
		
		disconnect();
		sideElementList[direction.getInt()].destroy();
		sideElementList[direction.getInt()] = null;
		sideElementIdList[direction.getInt()] = 0;
		connect();
		
		recalculateLightValue();
		setNeedPublish(true);
		return true;
	}

	public boolean getIfSideRemain() {
		for(SixNodeElement sideElement  : sideElementList)
		{
			if(sideElement != null) return true;
		}
		return false;
	}

	

    public void readFromNBT(NBTTagCompound nbt,String str)
    {
    	super.readFromNBT(nbt,str + "node");
    	
    	sixNodeCacheMapId = nbt.getByte(str + "cacheId");
    	if(sixNodeCacheMapId == 0) sixNodeCacheMapId = -1;
    	int idx = 0;
		for(idx = 0;idx<6;idx++)
		{
			
			short sideElementId =  nbt.getShort(str + "EID"+idx);
			if(sideElementId == 0)
			{
				sideElementList[idx] = null;
				sideElementIdList[idx] = 0;
			}
			else
			{
				try{
					SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(sideElementId);
					sideElementIdList[idx] = sideElementId;
					sideElementList[idx] = (SixNodeElement) descriptor.ElementClass.getConstructor(SixNode.class,Direction.class,SixNodeDescriptor.class).newInstance(this,Direction.fromInt(idx),descriptor);	
					sideElementList[idx].readFromNBT(nbt, str + "ED" + idx);
					sideElementList[idx].initialize();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}		
		}
		initializeFromNBT();

    }

    @Override
    public boolean nodeAutoSave() {
    	// TODO Auto-generated method stub
    	return false;
    }
    

    public void writeToNBT(NBTTagCompound nbt,String str)
    {
    	int idx = 0;
    	nbt.setByte(str + "cacheId",(byte) sixNodeCacheMapId);
		for(SixNodeElement sideElement  : sideElementList)
		{

			if(sideElement == null)
			{
				nbt.setShort(str + "EID"+idx, (short) 0);
			}
			else
			{
				nbt.setShort(str + "EID"+idx, (short) sideElementIdList[idx]);
				sideElement.writeToNBT(nbt, str + "ED"+idx);			
			}		
			idx++;
		}


		
    	super.writeToNBT(nbt,str + "node");
    }
	
	

	public boolean getSideEnable(Direction direction) {
		// TODO Auto-generated method stub
		return sideElementList[direction.getInt()] != null;
	}




	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		Direction elementSide = side.applyLRDU(lrdu);
		SixNodeElement element = sideElementList[elementSide.getInt()];
		if(element == null) return null;
		return element.getElectricalLoad(elementSide.getLRDUGoingTo(side));
	}


	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		Direction elementSide = side.applyLRDU(lrdu);
		SixNodeElement element = sideElementList[elementSide.getInt()];
		if(element == null) return null;
		return element.getThermalLoad(elementSide.getLRDUGoingTo(side));
	}


	@Override
	public int getSideConnectionMask(Direction side, LRDU lrdu) {
		Direction elementSide = side.applyLRDU(lrdu);
		SixNodeElement element = sideElementList[elementSide.getInt()];
		if(element == null) return 0;
		return element.getConnectionMask(elementSide.getLRDUGoingTo(side));
	}



	@Override
	public String multiMeterString(Direction side) {
		SixNodeElement element = sideElementList[side.getInt()];
		if(element == null) return "";
		return element.multiMeterString();
	}


	@Override
	public String thermoMeterString(Direction side) {
		SixNodeElement element = sideElementList[side.getInt()];
		if(element == null) return "";
		return element.thermoMeterString();
	}




	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
	    	int idx = 0;
	    	stream.writeByte(sixNodeCacheMapId);
			for(SixNodeElement sideElement  : sideElementList)
			{
				if(sideElement == null)
				{
					stream.writeShort((byte) 0);
				}
				else
				{
					stream.writeShort((short) sideElementIdList[idx]);
					sideElement.networkSerialize(stream);			
				}		
				idx++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

    public void preparePacketForClient(DataOutputStream stream,SixNodeElement e)
    {
    	try {
    		super.preparePacketForClient(stream);
    		int side = e.side.getInt();
       		stream.writeByte(side);
       		stream.writeShort(e.sixNodeElementDescriptor.parentItemDamage);	    	
		} catch (IOException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}	    	
    }

	@Override
	public void initializeFromThat(Direction front, EntityLivingBase entityLiving,
			ItemStack itemStack) {
		neighborBlockRead();
		// TODO Auto-generated method stub
		
	}


	@Override
	public void initializeFromNBT() {
		// TODO Auto-generated method stub
		connect();
	}

 
	@Override
	public short getBlockId() {
		// TODO Auto-generated method stub
		return (short) Eln.sixNodeBlock.blockID;
	}

	@Override
	public void connectInit()
	{
		super.connectInit();
		internalElectricalConnectionList.clear();
		internalThermalConnectionList.clear();
		
		lrduElementMask.clear();
		
	}
	
	@Override
	public void connectJob() {
		// TODO Auto-generated method stub
		super.connectJob();
		for(SixNodeElement element : sideElementList)
		{
			if(element != null)
			{
				element.connectJob();
			}
		}
	
		
		//INTERNAL
		{
			Direction side = Direction.YN;
			SixNodeElement element = sideElementList[side.getInt()];
			if(element != null)
			{
				for(LRDU lrdu : LRDU.values())
				{
					Direction otherSide = side.applyLRDU(lrdu);
					SixNodeElement otherElement = sideElementList[otherSide.getInt()];
					if(otherElement != null)
					{
						LRDU otherLRDU = otherSide.getLRDUGoingTo(side);
						tryConnectTwoInternalElement(side,element,lrdu,otherSide,otherElement,otherLRDU);						
					}
				}
			}			
		}
		{
			Direction side = Direction.YP;
			SixNodeElement element = sideElementList[side.getInt()];
			if(element != null)
			{
				for(LRDU lrdu : LRDU.values())
				{
					Direction otherSide = side.applyLRDU(lrdu);
					SixNodeElement otherElement = sideElementList[otherSide.getInt()];
					if(otherElement != null)
					{
						LRDU otherLRDU = otherSide.getLRDUGoingTo(side);
						tryConnectTwoInternalElement(side,element,lrdu,otherSide,otherElement,otherLRDU);						
					}
				}
			}			
		}
	
		{
			Direction side = Direction.XN;
			for(int idx = 0;idx<4;idx++)
			{
				Direction otherSide = side.right();
				SixNodeElement element = sideElementList[side.getInt()];
				SixNodeElement otherElement = sideElementList[otherSide.getInt()];
				if(element != null && otherElement != null)
				{
					tryConnectTwoInternalElement(side,element,LRDU.Right,otherSide,otherElement,LRDU.Left);
				}
				
				side = otherSide;
			}
		}
		
		

	}
	
	
	
	@Override
	public void disconnectJob() {
		super.disconnectJob();
		for(SixNodeElement element : sideElementList)
		{
			if(element != null)
			{
				element.disconnectJob();
			}
		}
		
		Eln.simulator.removeAllElectricalConnection(internalElectricalConnectionList);
		Eln.simulator.removeAllThermalConnection(internalThermalConnectionList);
	}
	
	public void tryConnectTwoInternalElement(Direction side,SixNodeElement element,LRDU lrdu,Direction otherSide,SixNodeElement otherElement,LRDU otherLRDU)
	{
		if(compareConnectionMask(element.getConnectionMask(lrdu) , otherElement.getConnectionMask(otherLRDU)))
		{
			lrduElementMask.set(side,lrdu,true);		
			lrduElementMask.set(otherSide,otherLRDU,true);		
			ElectricalLoad eLoad;
			if((eLoad = element.getElectricalLoad(lrdu)) != null)
			{			
				ElectricalLoad otherELoad = otherElement.getElectricalLoad(otherLRDU);
				if(otherELoad != null) 
				{
					ElectricalConnection eCon;
					eCon = new ElectricalConnection(eLoad,otherELoad);
					
					Eln.simulator.addElectricalConnection(eCon);				
					
					internalElectricalConnectionList.add(eCon);
				}
			}
			ThermalLoad tLoad;
			if((tLoad = this.getThermalLoad(side,lrdu)) != null)
			{
				
				ThermalLoad otherTLoad = element.getThermalLoad(otherLRDU);
				if(otherTLoad != null)
				{
					ThermalConnection tCon;
					tCon = new ThermalConnection(tLoad,otherTLoad);
					
					Eln.simulator.addThermalConnection(tCon);
					
					internalThermalConnectionList.add(tCon);
				}
				
			}	
		}
	}
	
	public void newConnectionAt(Direction side,LRDU lrdu)
	{
		Direction elementSide = side.applyLRDU(lrdu);
		SixNodeElement element = sideElementList[elementSide.getInt()];
		if(element == null)
		{
			System.out.println("sixnode newConnectionAt error");
			while(true);
		}
		lrduElementMask.set(elementSide,elementSide.getLRDUGoingTo(side),true);

	}
	
	public void externalDisconnect(Direction side,LRDU lrdu)
	{
		Direction elementSide = side.applyLRDU(lrdu);
		SixNodeElement element = sideElementList[elementSide.getInt()];
		if(element == null)
		{
			System.out.println("sixnode newConnectionAt error");
			while(true);
		}
		lrduElementMask.set(elementSide,elementSide.getLRDUGoingTo(side),false);		
	}
	
	
	public  boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,float vx,float vy,float vz)
	{
		if(sixNodeCacheMapId >= 0)
		{
			return false;
		}
		else
		{
			
			ItemStack stack = entityPlayer.getCurrentEquippedItem();
			GenericItemUsingDamageDescriptor descriptor = SixNodeCacheItem.getDescriptor(stack);
			if(descriptor instanceof SixNodeCacheItem)
			{
				if(sixNodeCacheMapId != ((SixNodeCacheItem)descriptor).mapIndex)
				sixNodeCacheMapId = ((SixNodeCacheItem)descriptor).mapIndex;
				setNeedPublish(true);
				entityPlayer.inventory.decrStackSize(entityPlayer.inventory.currentItem, 1);
				
				//if(sixNodeCacheMapId != sixNodeCacheMapIdOld)
				{
					Chunk chunk = coordonate.world().getChunkFromBlockCoords(coordonate.x, coordonate.z);
					chunk.generateHeightMap();
					chunk.updateSkylight();
					chunk.generateSkylightMap();
					coordonate.world().updateAllLightTypes(coordonate.x,coordonate.y,coordonate.z);
				}
				return true;
			}
			else
			{
				SixNodeElement element = sideElementList[side.getInt()];
		    	if(element == null) return false;
		    	if(element.onBlockActivated( entityPlayer,  side, vx, vy, vz)) return true;
		    	return super.onBlockActivated(entityPlayer, side, vx, vy, vz);
			}
		}
	}

	@Override
	public boolean hasGui(Direction side) {
		if(sideElementList[side.getInt()] == null) return false;
		return sideElementList[side.getInt()].hasGui();
	}
	public IInventory getInventory(Direction side)
	{
		if(sideElementList[side.getInt()] == null) return null;
		return sideElementList[side.getInt()].getInventory();		
	}
	
	public Container newContainer(Direction side,EntityPlayer player)
	{
		if(sideElementList[side.getInt()] == null) return null;
		return sideElementList[side.getInt()].newContainer(side, player);
	}

	
	public float physicalSelfDestructionExplosionStrength()
	{
		return 1.0f;
	}
	
	
	public void recalculateLightValue()
	{
		int light = 0;
		for(SixNodeElement element : sideElementList)
		{
			if(element == null) continue;
			int eLight = element.getLightValue();
			if(eLight > light) light = eLight;
		}
		setLightValue(light);
	}
	
	
	
	@Override
	public void networkUnserialize(DataInputStream stream,Player player) {
		super.networkUnserialize(stream,player);
		
		Direction side;
		try {
			side = Direction.fromInt(stream.readByte());	
			if(side != null & sideElementIdList[side.getInt()] == stream.readShort())
			{
				sideElementList[side.getInt()].networkUnserialize(stream,player);
			}
			else
			{
				System.out.println("sixnode unserialize miss");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public boolean hasVolume() {
		// TODO Auto-generated method stub
		for(SixNodeElement element : sideElementList)
		{
			if(element != null && element.sixNodeElementDescriptor.hasVolume()) return true;
		}
		return false;
	}
	
	
	
}
