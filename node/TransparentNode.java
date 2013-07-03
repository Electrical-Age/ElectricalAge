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
import net.minecraft.client.Minecraft;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class TransparentNode extends Node {


	public TransparentNodeElement element;
	public int elementId;
	
	
	@Override
	public boolean nodeAutoSave() {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	
	@Override
	public void onNeighborBlockChange() {
		super.onNeighborBlockChange();
		element.onNeighborBlockChange();
	}

    public void readFromNBT(NBTTagCompound nbt,String str)
    {
    	super.readFromNBT(nbt,str + "node");
    	elementId = nbt.getShort(str + "eid");
		try {
			TransparentNodeDescriptor descriptor = Eln.transparentNodeItem.getDescriptor(elementId);
			element = (TransparentNodeElement) descriptor.ElementClass.getConstructor(TransparentNode.class,TransparentNodeDescriptor.class).newInstance(this,descriptor);
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
    	element.readFromNBT(nbt,str + "element");
       

    }

    


    public void writeToNBT(NBTTagCompound nbt,String str)
    {	
    	super.writeToNBT(nbt,str + "node");
    	nbt.setShort(str + "eid", (short) elementId);
    	element.writeToNBT(nbt,str + "element");
    	

    }
	
	
    @Override
    public void onBreakBlock() {
    	// TODO Auto-generated method stub
    	element.onBreakElement();
    	super.onBreakBlock();
    }




	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		return element.getElectricalLoad(side,lrdu);
	}


	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return element.getThermalLoad(side,lrdu);
	}


	@Override
	public int getSideConnectionMask(Direction side, LRDU lrdu) {
		return element.getConnectionMask(side,lrdu);
	}



	@Override
	public String multiMeterString(Direction side) {
		return element.multiMeterString(side);
	}


	@Override
	public String thermoMeterString(Direction side) {
		return element.thermoMeterString(side);
	}



	@Override
	public void networkSerialize(DataOutputStream stream)  {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		
		try {
			stream.writeShort(this.elementId);
			element.networkSerialize(stream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public enum FrontType{BlockSide,PlayerView,PlayerViewHorizontal};
	

	@Override
	public void initializeFromThat(Direction side, EntityLiving entityLiving,ItemStack itemStack) {
		try {
			//Direction front = null;
			TransparentNodeDescriptor descriptor = Eln.transparentNodeItem.getDescriptor(itemStack);
			/*switch(descriptor.getFrontType())
			{
			case BlockSide:
				front = side;
				break;
			case PlayerView:
				front = Utils.entityLivingViewDirection(entityLiving).getInverse();
				break;
			case PlayerViewHorizontal:
				front = Utils.entityLivingHorizontalViewDirection(entityLiving).getInverse();
				break;
			
			}*/
	
			int metadata = itemStack.getItemDamage();
			elementId = metadata;
			element = (TransparentNodeElement) descriptor.ElementClass.getConstructor(TransparentNode.class,TransparentNodeDescriptor.class).newInstance(this,descriptor);
			element.initializeFromThat(side, entityLiving, itemStack.getTagCompound());
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


	@Override
	public void initializeFromNBT() {
		// TODO Auto-generated method stub

		element.initialize();

	}

 
	@Override
	public short getBlockId() {
		// TODO Auto-generated method stub
		return (short) Eln.transparentNodeBlock.blockID;
	}


	

	
	
	
	public  boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,float vx,float vy,float vz)
	{
    	if(element.onBlockActivated( entityPlayer,  side, vx, vy, vz)) return true;
    	return super.onBlockActivated(entityPlayer, side, vx, vy, vz);
	}

	@Override
	public boolean hasGui(Direction side) {
		if(element == null) return false;
		return element.hasGui();
	}
	public IInventory getInventory(Direction side)
	{
		if(element == null) return null;
		return element.getInventory();		
	}
	
	public Container newContainer(Direction side,EntityPlayer player)
	{
		if(element == null) return null;
		return element.newContainer(side, player);
	}

	
	
	@Override
	public int getBlockMetadata() {
		return ((int)(element.getLightOpacity()*3)) & 3;
	}
	
	
	
	@Override
	public void networkUnserialize(DataInputStream stream,Player player) {
		super.networkUnserialize(stream,player);
		
		Direction side;
		try {
			if(elementId == stream.readShort())
			{
				element.networkUnserialize(stream,player);
			}
			else
			{
				System.out.println("Transparent node unserialize miss");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void connectJob() {
		super.connectJob();
		element.connectJob();
	}
	
	@Override
	public void disconnectJob() {
		super.disconnectJob();
		element.disconnectJob();
	}
	

	@Override
	public void checkCanStay(boolean onCreate) {
		// TODO Auto-generated method stub
		super.checkCanStay(onCreate);
		element.checkCanStay(onCreate);
	}
	
	
}
