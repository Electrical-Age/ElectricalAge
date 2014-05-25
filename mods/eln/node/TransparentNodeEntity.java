package mods.eln.node;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;




import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class TransparentNodeEntity extends NodeBlockEntity {	//boolean[] syncronizedSideEnable = new boolean[6];
	TransparentNodeElementRender elementRender = null;
	short elementRenderId;
	
	public TransparentNodeEntity()
	{

	}
	/* caca
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction direction) {
		// TODO Auto-generated method stub
		//Utils.println("onBlockActivated " + direction);
		
		return getNode().onBlockActivated(entityPlayer, direction);
	}
	*/
	@Override
	public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		if(elementRender == null) return null;
		return elementRender.getCableRender(side, lrdu);
	}
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		try {
			Short id = stream.readShort();
			if(id == 0)
			{
				elementRenderId = (byte)0;
				elementRender = null;
			}
			else
			{
				if(id != elementRenderId)
				{
					elementRenderId = id;
					TransparentNodeDescriptor descriptor = Eln.transparentNodeItem.getDescriptor(id);
					elementRender = (TransparentNodeElementRender) descriptor.RenderClass.getConstructor(TransparentNodeEntity.class,TransparentNodeDescriptor.class).newInstance(this,descriptor);				
				}	
				elementRender.networkUnserialize(stream);
			}		
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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


	
	
	public Container newContainer(Direction side,EntityPlayer player)
	{	
		return ((TransparentNode)getNode()).newContainer(side,player);
	}
	public GuiScreen newGuiDraw(Direction side,EntityPlayer player)
	{
		return elementRender.newGuiDraw(side, player);
	}
		
	
    public void preparePacketForServer(DataOutputStream stream)
    {
    	try {
    		super.preparePacketForServer(stream);
    		
    		stream.writeShort(elementRenderId);
    		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	    	
    }
	
    public void sendPacketToServer(ByteArrayOutputStream bos)
    {
    	super.sendPacketToServer(bos);
    }
	
    
    public boolean cameraDrawOptimisation()
    {
    	if(elementRender == null) return super.cameraDrawOptimisation();
    	return elementRender.cameraDrawOptimisation();
    }
	public int getDamageValue(World world, int x, int y, int z) {
		if(world.isRemote)
		{
			return elementRenderId;
		}
		return 0;
	}
	@Override
	public void tileEntityNeighborSpawn() {
		// TODO Auto-generated method stub
		if(elementRender != null) elementRender.notifyNeighborSpawn();
	}
	public void addCollisionBoxesToList(AxisAlignedBB par5AxisAlignedBB,List list) {
		if(worldObj.isRemote){
			if(elementRender == null){
				AxisAlignedBB bb = Block.stone.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
				if(par5AxisAlignedBB.intersectsWith(bb)) list.add(bb);
			}
			else{
				elementRender.transparentNodedescriptor.addCollisionBoxesToList( par5AxisAlignedBB, list,  this);
			}
		}
		else{
			TransparentNode node = (TransparentNode) getNode();
			if(node == null){
				AxisAlignedBB bb = Block.stone.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
				if(par5AxisAlignedBB.intersectsWith(bb)) list.add(bb);
			}
			else{
				node.element.transparentNodeDescriptor.addCollisionBoxesToList( par5AxisAlignedBB, list,  this);
			}
		}
	}
	
	public void serverPacketUnserialize(DataInputStream stream)
	{
		super.serverPacketUnserialize(stream);
		if(elementRender != null)
			elementRender.serverPacketUnserialize(stream);
	}


	
}
// && 