package mods.eln.node.simple;

import java.io.DataInputStream;
import java.io.IOException;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import mods.eln.Eln;
import mods.eln.misc.Coordinate;
import mods.eln.misc.DescriptorManager;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.INodeEntity;
import mods.eln.node.NodeEntityClientSender;
import mods.eln.node.NodeManager;
import mods.eln.server.DelayedBlockRemove;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.tileentity.TileEntity;

public abstract class SimpleNodeEntity extends TileEntity implements INodeEntity{

	private SimpleNode node;

	public SimpleNode getNode() {
		if (worldObj.isRemote){
			Utils.fatal();
			return null;
		}
		if(this.worldObj == null) return null;
		if (node == null){ 
			node = (SimpleNode) NodeManager.instance.getNodeFromCoordonate(new Coordinate(xCoord, yCoord, zCoord, this.worldObj));
			if(node == null){
				DelayedBlockRemove.add(new Coordinate(xCoord, yCoord, zCoord, this.worldObj));
				return null;
			}
		}
		return node;
	}

	
	
	//***************** Wrapping **************************
	/*
	public void onBlockPlacedBy(Direction front, EntityLivingBase entityLiving, int metadata) {
	
	}
*/

	public void onBlockAdded(){
		/*if (!worldObj.isRemote){
			if (getNode() == null) {
				worldObj.setBlockToAir(xCoord, yCoord, zCoord);
			}
		}*/
	}

	public void onBreakBlock(){
		if (!worldObj.isRemote){
			if (getNode() == null) return;
			getNode().onBreakBlock();
		}
	}

	public void onChunkUnload(){
		super.onChunkUnload();
		if (worldObj.isRemote){
			destructor();
		}
	}

	// client only
	public void destructor(){

	}

	@Override
	public void invalidate() {
		if (worldObj.isRemote){
			destructor();
		}
		super.invalidate();
	}

	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz){
		if (!worldObj.isRemote){
			if (getNode() == null) return false;
			getNode().onBlockActivated(entityPlayer, side, vx, vy, vz);
			return true;
		}
		return true;
	}

	public void onNeighborBlockChange(){
		if (!worldObj.isRemote){
			if (getNode() == null) return;
			getNode().onNeighborBlockChange();
		}
	}


	//***************** Descriptor **************************
	public Object getDescriptor(){
		SimpleNodeBlock b = (SimpleNodeBlock) getBlockType();
		return DescriptorManager.get(b.descriptorKey);
	}
	
	
	
	
	
	
	//***************** Network **************************
	
	public Direction front;
	@Override
	public void serverPublishUnserialize(DataInputStream stream) {
		try {
			if(front != (front = Direction.fromInt(stream.readByte()))){
				worldObj.markBlockForUpdate(xCoord,yCoord,zCoord);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void serverPacketUnserialize(DataInputStream stream) {

	}
	
    @Override
    public Packet getDescriptionPacket()
    {	
    	SimpleNode node = getNode(); 
    	if(node == null){
    		Utils.println("ASSERT NULL NODE public Packet getDescriptionPacket() nodeblock entity");
    		return null;
    	}
    	return new S3FPacketCustomPayload(Eln.channelName,node.getPublishPacket().toByteArray());
    }

    
    public NodeEntityClientSender sender = new NodeEntityClientSender(this, getNodeUuid());
    
    
    
    
    //*********************** GUI ***************************
	@Override
	public Container newContainer(Direction side,EntityPlayer player)
	{
		return null;
	}
	@Override
    @SideOnly(Side.CLIENT)
	public GuiScreen newGuiDraw(Direction side,EntityPlayer player)
	{
		return null;
	}
	
	
}
