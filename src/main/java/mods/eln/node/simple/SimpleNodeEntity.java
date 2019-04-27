package mods.eln.node.simple;

import mods.eln.Eln;
import mods.eln.misc.Coordinate;
import mods.eln.misc.DescriptorManager;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.INodeEntity;
import mods.eln.node.NodeEntityClientSender;
import mods.eln.node.NodeManager;
import mods.eln.server.DelayedBlockRemove;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public abstract class SimpleNodeEntity extends TileEntity implements INodeEntity, ITickable {

    private SimpleNode node;

    public SimpleNode getNode() {
        if (world.isRemote) {
            Utils.fatal();
            return null;
        }
        if (node == null) {
            node = (SimpleNode) NodeManager.instance.getNodeFromCoordinate(new Coordinate(pos, world));
            if (node == null) {
                DelayedBlockRemove.add(new Coordinate(pos, this.world));
                return null;
            }
        }
        return node;
    }


    //***************** Wrapping **************************

    void onBlockAdded() {
		/*if (!world.isRemote){
			if (getNode() == null) {
				world.setBlockToAir(xCoord, yCoord, zCoord);
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
    void destructor() {
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

    void onNeighborBlockChange() {
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

    // TODO(1.10): Packets are probably still broken somehow!
    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        SimpleNode node = getNode();
        if (node == null) {
            Utils.println("ASSERT NULL NODE public Packet getDescriptionPacket() nodeblock entity");
            return null;
        }
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setByteArray("eln", node.getPublishPacket().toByteArray());
        return new SPacketUpdateTileEntity(
            getPos(),
            getBlockMetadata(),
            tagCompound
        );
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        assert(world.isRemote);
        byte[] bytes = pkt.getNbtCompound().getByteArray("eln");
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
        Eln.packetHandler.packetRx(dataInputStream, net, Minecraft.getMinecraft().player);
    }

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {
        try {
            if (front != (front = Direction.fromInt(stream.readByte()))) {
                IBlockState state = this.world.getBlockState(this.pos);
                world.notifyBlockUpdate(getPos(), state, state, 0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) {
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
