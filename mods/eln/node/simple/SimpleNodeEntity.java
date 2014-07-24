package mods.eln.node.simple;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.Node;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.EnumSkyBlock;

public class SimpleNodeEntity extends TileEntity {

	private SimpleNode node;

	public SimpleNode getNode() {
		if (worldObj.isRemote) Utils.fatal();
		if (node == null) node = (SimpleNode) NodeManager.instance.getNodeFromCoordonate(new Coordonate(xCoord, yCoord, zCoord, this.worldObj));
		return node;
	}

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

}
