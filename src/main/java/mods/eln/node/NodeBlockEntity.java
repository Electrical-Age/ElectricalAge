package mods.eln.node;



import java.awt.JobAttributes.SidesType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import cpw.mods.fml.common.FMLCommonHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import mods.eln.Eln;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUCubeMask;
import mods.eln.misc.LRDUMask;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.server.DelayedBlockRemove;
import mods.eln.sim.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;


public abstract class NodeBlockEntity extends TileEntity implements ITileEntitySpawnClient,INodeEntity{
	
	public static LinkedList<NodeBlockEntity> clientList = new LinkedList<NodeBlockEntity>();
	


	public NodeBlock getBlock(){
		return (NodeBlock) getBlockType();
	}
	
	boolean redstone = false;
	int lastLight = 0xFF; 
	boolean firstUnserialize = true;
	@Override
	public void serverPublishUnserialize(DataInputStream stream)
	{

		int light = 0;
		try {
			if(firstUnserialize){
				firstUnserialize = false;
				Utils.notifyNeighbor(this);

			}
			Byte b = stream.readByte();
			light = b & 0xF;
			boolean newRedstone = (b & 0x10) != 0;
			if(redstone != newRedstone)
			{
				redstone = newRedstone;		
				worldObj.notifyBlockChange(xCoord, yCoord, zCoord, getBlockType());
			}
			else
			{
				redstone = newRedstone;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	/*	if(lastLight == 0xFF) //boot trololol
		{
			lastLight = 15;
			worldObj.updateLightByType(EnumSkyBlock.Block,xCoord,yCoord,zCoord);
		}*/
		
		if(lastLight != light)
		{
			lastLight = light;
			worldObj.updateLightByType(EnumSkyBlock.Block,xCoord,yCoord,zCoord);
		}
		

		
	}
	
	@Override
	public void serverPacketUnserialize(DataInputStream stream)
	{
		
	}
		
	
	
	//abstract public Node newNode();
	//abstract public Node newNode(Direction front,EntityLiving entityLiving,int metadata);
	
	public abstract int isProvidingWeakPower(Direction side);
	//{
		//if(worldObj.isRemote) return 0;
		//return getNode().isProvidingWeakPower(side);
	//}
	
	Node node = null;
	
	@Override
	public Container newContainer(Direction side,EntityPlayer player)
	{
		return null;
	}
	@Override
	public GuiScreen newGuiDraw(Direction side,EntityPlayer player)
	{
		return null;
	}
	
	
	
	public NodeBlockEntity() {

	}


    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox()
    {
    	if(cameraDrawOptimisation())
    	{
            return AxisAlignedBB.getBoundingBox(xCoord-1, yCoord-1, zCoord-1, xCoord+1, yCoord+1, zCoord+1);
    	}
    	else
    	{
    		return INFINITE_EXTENT_AABB;
    	}
    }
    
    public boolean cameraDrawOptimisation()
    {
    	return true;
    }
    
	public int getLightValue()
	{
		if(worldObj.isRemote) 
		{
			if(lastLight == 0xFF) 
			{
				return 0;
			}
			return lastLight;
		}
		else
		{
			Node node =  getNode();
			if(node == null) return 0;
			return getNode().getLightValue();
		}
	}
	
    /**
     * Reads a tile entity from NBT.
     */
    public void readFromNBT(NBTTagCompound nbt)
    {
    	super.readFromNBT(nbt);
    }

    /**
     * Writes a tile entity to NBT.
     */
    public void writeToNBT(NBTTagCompound nbt)
    {
    	super.writeToNBT(nbt);
    }
 

    //max draw distance
    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
    	
    	return 4096.0*(4)*(4);
    }
    
    
    
    
    void onBlockPlacedBy(Direction front,EntityLivingBase entityLiving,int metadata)
    {
    	if(!worldObj.isRemote)
    	{
    		
    	}
	}
    
    /*
    @Override
    public void validate() {
    	
    	super.validate();
    	if(!worldObj.isRemote)
		{
    		if(getNode() == null) //to verrifie todo
    		{
    			worldObj.setBlockAndMetadataWithNotify(xCoord, yCoord, zCoord, 0, 0);
    		}
		}
    }*/
    
    @Override
    public boolean canUpdate() {
    	
    	return true;
    }
    
    boolean updateEntityFirst = true;
    @Override
    public void updateEntity() {
    	if(updateEntityFirst)
    	{
    		updateEntityFirst = false;
    		if(!worldObj.isRemote)
    		{
        		if(getNode() == null) //to verrifie todo
        		{
        			//worldObj.setBlock(xCoord, yCoord, zCoord, 0);//caca1.5.1
        			//Utils.println("ASSERT NODE DESTROYED BY SECURITY updateEntity");
        		}
    		} else {
    			clientList.add(this);
    		}
    	}
    }
    
    
    public void onBlockAdded()
    {    
    	if(!worldObj.isRemote)
		{
    		if(getNode() == null) //to verrifie todo
    		{
    			worldObj.setBlockToAir(xCoord, yCoord, zCoord );
    		}
		}
    }   
    public void onBreakBlock()
    {
    	
    	if(!worldObj.isRemote)
    	{
    		if(getNode() == null)return; 
    		getNode().onBreakBlock();
    	}
    	else
    	{
    	//	onRemove();
    	}
    }
    
    public void onChunkUnload()
    {
    	if(worldObj.isRemote)
    	{
    		destructor();
    	}
    	else
    	{
    		int idx = 0;
    		idx++;
    	}
    }
    
    //client only
    public void destructor()
    {
		clientList.remove(this);
    }
    
    @Override
    public void invalidate() {
    	
    	if(worldObj.isRemote)
    	{
    		destructor();
    	}
    	super.invalidate();
    }
    
    public  boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz)
    {
    	if(!worldObj.isRemote)
    	{
    		if(getNode() == null) return false;
    		getNode().onBlockActivated(entityPlayer, side, vx, vy, vz);
    		return true;
    	}
    	//if(entityPlayer.getCurrentEquippedItem().getItem() instanceof ItemBlock)
		{
    		return true;
		}
    	//return true;
    }
      
    public void onNeighborBlockChange()
    {
    	if(!worldObj.isRemote)
    	{
    		if(getNode() == null) return;
    		getNode().onNeighborBlockChange();
    	}
    }
    
    
    
    
    
 
    public Node getNode()
    {
    	if(worldObj.isRemote){Utils.fatal(); 
    		return null;
    	}
    	if(this.worldObj == null) return null;
    	if(node == null){
    		node = (Node)NodeManager.instance.getNodeFromCoordonate(new Coordonate(xCoord, yCoord, zCoord,this.worldObj));
    		if(node == null) DelayedBlockRemove.add(new Coordonate(xCoord, yCoord, zCoord,this.worldObj));
    	}
    	return node;
    }
    
    
    public static NodeBlockEntity getEntity(int x, int y, int z)
    {
    	TileEntity entity;
    	if((entity = Minecraft.getMinecraft().theWorld.getTileEntity(x, y, z)) != null)
    	{
    		if(entity instanceof NodeBlockEntity)
    		{
    			return (NodeBlockEntity)entity;
    		}
    	}
    	return null;
    }
    
    
   

    @Override
    public Packet getDescriptionPacket()
    {	
    	Node node = getNode(); //TO DO NULL POINTER
    	if(node == null)
    	{
    		Utils.println("ASSERT NULL NODE public Packet getDescriptionPacket() nodeblock entity");
    		return null;
    	}
    	return new S3FPacketCustomPayload(Eln.channelName,node.getPublishPacket().toByteArray());
    	//return null;
    }

    
    
    public void preparePacketForServer(DataOutputStream stream)
    {
    	try {
    		stream.writeByte(Eln.packetPublishForNode);
    		 		
			stream.writeInt(xCoord);
	    	stream.writeInt(yCoord);
	    	stream.writeInt(zCoord);
	    	
	    	stream.writeByte(worldObj.provider.dimensionId);
	    	
	    	stream.writeUTF(getNodeUuid());
	    		    	
	    	
		} catch (IOException e) {
			
			e.printStackTrace();
		}	    	
    }
	
    public void sendPacketToServer(ByteArrayOutputStream bos)
    {
    	UtilsClient.sendPacketToServer(bos);
    }
    
    
    
    public CableRenderDescriptor getCableRender(Direction side,LRDU lrdu)
    {
    	return null;
    }
    public int getCableDry(Direction side,LRDU lrdu)
    {
    	return 0;
    }

	public boolean canConnectRedstone(Direction xn) {
		
		if(worldObj.isRemote)
			return redstone;
		else
		{
			if(getNode() == null) return false;
			return getNode().canConnectRedstone(); 
		}
	}
	public void clientRefresh(float deltaT) {
		
	}
}
