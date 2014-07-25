package mods.eln.node.simple;

import java.io.DataInputStream;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.DescriptorManager;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.INodeEntity;
import mods.eln.node.NodeEntityClientSender;
import mods.eln.node.NodeManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.tileentity.TileEntity;

public abstract class SimpleNodeEntity extends TileEntity implements INodeEntity{

	private SimpleNode node;

	public SimpleNode getNode() {
		if (worldObj.isRemote) Utils.fatal();
		if (node == null) node = (SimpleNode) NodeManager.instance.getNodeFromCoordonate(new Coordonate(xCoord, yCoord, zCoord, this.worldObj));
		return node;
	}

	
	
	//***************** Wrapping **************************
	
	public void onBlockPlacedBy(Direction front, EntityLivingBase entityLiving, int metadata) {
	
	}



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
			getNode().onBlockActivated(entityPlayer, side, vx, vy, vz);
			return true;
		}
		return true;
	}

	public void onNeighborBlockChange(){
		if (!worldObj.isRemote){
			getNode().onNeighborBlockChange();
		}
	}


	//***************** Descriptor **************************
	protected Object getDescriptor(){
		SimpleNodeBlock b = (SimpleNodeBlock) getBlockType();
		return DescriptorManager.get(b.descriptorKey);
	}
	
	
	
	
	
	
	//***************** Network **************************
	@Override
	public void serverPublishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void serverPacketUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		
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
    	//return null;
    }

    
    public NodeEntityClientSender sender = new NodeEntityClientSender(this, getNodeUuid());
}
