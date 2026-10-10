package mods.eln.node.simple;


import mods.eln.compat.WorldCompat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
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
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import mods.eln.node.NodeBlockEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.io.DataInputStream;
import java.io.IOException;

public abstract class SimpleNodeEntity extends TileEntity implements INodeEntity, ITickable {

    private SimpleNode node;

    public SimpleNode getNode() {
        if (world.isRemote) {
            Utils.fatal();
            return null;
        }
        if (this.world == null) return null;
        if (node == null) {
            node = (SimpleNode) NodeManager.instance.getNodeFromCoordonate(new Coordonate(pos.getX(), pos.getY(), pos.getZ(), this.world));
            if (node == null) {
                DelayedBlockRemove.add(new Coordonate(pos.getX(), pos.getY(), pos.getZ(), this.world));
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

    public void onBlockAdded() {
		/*if (!world.isRemote){
			if (getNode() == null) {
				WorldCompat.setBlockToAir(world, pos.getX(), pos.getY(), pos.getZ());
			}
		}*/
    }

    public void onBreakBlock() {
        if (!world.isRemote) {
            if (getNode() == null) return;
            getNode().onBreakBlock();
        }
    }

    public void onChunkUnload() {
        super.onChunkUnload();
        if (world.isRemote) {
            destructor();
        }
    }

    // client only
    public void destructor() {

    }

    @Override
    public void invalidate() {
        if (world.isRemote) {
            destructor();
        }
        super.invalidate();
    }

    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        if (!world.isRemote) {
            if (getNode() == null) return false;
            getNode().onBlockActivated(entityPlayer, side, vx, vy, vz);
            return true;
        }
        return true;
    }

    public void onNeighborBlockChange() {
        if (!world.isRemote) {
            if (getNode() == null) return;
            getNode().onNeighborBlockChange();
        }
    }


    //***************** Descriptor **************************
    public Object getDescriptor() {
        SimpleNodeBlock b = (SimpleNodeBlock) getBlockType();
        return DescriptorManager.get(b.descriptorKey);
    }


    //***************** Network **************************

    public Direction front;

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {
        try {
            if (front != (front = Direction.fromInt(stream.readByte()))) {
                WorldCompat.markBlockForUpdate(world, pos.getX(), pos.getY(), pos.getZ());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) {

    }

    /** 1.7.10 getDescriptionPacket(): see NodeBlockEntity.getUpdateTag for the 1.12 mapping. */
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        if (world == null || world.isRemote) return tag;
        SimpleNode node = getNode();
        if (node == null) {
            Utils.println("ASSERT NULL NODE public Packet getDescriptionPacket() nodeblock entity");
            return tag;
        }
        tag.setByteArray(NodeBlockEntity.DESCRIPTION_KEY, node.getPublishPacket().toByteArray());
        return tag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound tag = getUpdateTag();
        if (!tag.hasKey(NodeBlockEntity.DESCRIPTION_KEY)) return null;
        return new SPacketUpdateTileEntity(pos, 0, tag);
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        if (tag.hasKey(NodeBlockEntity.DESCRIPTION_KEY))
            Eln.proxy.handleDescriptionPacket(tag.getByteArray(NodeBlockEntity.DESCRIPTION_KEY));
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    /** 1.7.10 kept the TE when only the metadata changed; 1.12 would recreate it on any state change. */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    /** 1.7.10 TEs ticked by default (updateEntity); subclasses override update(). */
    @Override
    public void update() {
    }


    public NodeEntityClientSender sender = new NodeEntityClientSender(this, getNodeUuid());


    //*********************** GUI ***************************
    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return null;
    }


}
