package mods.eln.node;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.*;
import mods.eln.server.DelayedBlockRemove;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;


public abstract class NodeBlockEntity extends TileEntity implements ITileEntitySpawnClient, INodeEntity {

    public static final LinkedList<NodeBlockEntity> clientList = new LinkedList<NodeBlockEntity>();


    public NodeBlock getBlock() {
        return (NodeBlock) getBlockType();
    }

    boolean redstone = false;
    int lastLight = 0xFF;
    boolean firstUnserialize = true;

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {

        int light = 0;
        try {
            if (firstUnserialize) {
                firstUnserialize = false;
                Utils.notifyNeighbor(this);

            }
            Byte b = stream.readByte();
            light = b & 0xF;
            boolean newRedstone = (b & 0x10) != 0;
            if (redstone != newRedstone) {
                redstone = newRedstone;
                worldObj.notifyNeighborsRespectDebug(getPos(), getBlockType());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (lastLight != light) {
            lastLight = light;
            worldObj.checkLightFor(EnumSkyBlock.BLOCK, getPos());
        }

    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) {

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
    public Container newContainer(Direction side, EntityPlayer player) {
        return null;
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return null;
    }


    public NodeBlockEntity() {
    }


    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        if (cameraDrawOptimisation()) {
            // TODO(1.10): This may not be correct.
            return new AxisAlignedBB(pos);
        } else {
            return INFINITE_EXTENT_AABB;
        }
    }

    public boolean cameraDrawOptimisation() {
        return true;
    }

    public int getLightValue() {
        if (worldObj.isRemote) {
            if (lastLight == 0xFF) {
                return 0;
            }
            return lastLight;
        } else {
            Node node = getNode();
            if (node == null) return 0;
            return getNode().getLightValue();
        }
    }

    /**
     * Reads a tile entity fromFacing NBT.
     */
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
    }

    /**
     * Writes a tile entity to NBT.
     */
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        return super.writeToNBT(nbt);
    }


    //max draw distance
    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 4096.0 * (4) * (4);
    }


    void onBlockPlacedBy(Direction front, EntityLivingBase entityLiving, IBlockState state) {

    }


    @Override
    public void onLoad()
    {
        if (!worldObj.isRemote) {
            // worldObj.setBlock(xCoord, yCoord, zCoord, 0);
        } else {
            clientList.add(this);
        }
    }



    public void onBlockAdded() {
        if (!worldObj.isRemote && getNode() == null) {
            worldObj.setBlockToAir(pos);
        }
    }

    public void onBreakBlock() {
        if (!worldObj.isRemote) {
            if (getNode() == null) return;
            getNode().onBreakBlock();
        }
    }

    public void onChunkUnload() {
        if (worldObj.isRemote) {
            destructor();
        }
    }

    //client only
    public void destructor() {
        clientList.remove(this);
    }

    @Override
    public void invalidate() {

        if (worldObj.isRemote) {
            destructor();
        }
        super.invalidate();
    }

    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        if (!worldObj.isRemote) {
            if (getNode() == null) return false;
            getNode().onBlockActivated(entityPlayer, side, vx, vy, vz);
            return true;
        }
        //if(entityPlayer.getCurrentEquippedItem().getItem() instanceof ItemBlock)
        {
            return true;
        }
        //return true;
    }

    public void onNeighborBlockChange() {
        if (!worldObj.isRemote) {
            if (getNode() == null) return;
            getNode().onNeighborBlockChange();
        }
    }


    public Node getNode() {
        if (worldObj.isRemote) {
            Utils.fatal();
            return null;
        }
        if (this.worldObj == null) return null;
        if (node == null) {
            NodeBase nodeFromCoordinate = NodeManager.instance.getNodeFromCoordinate(new Coordinate(pos, worldObj));
            if (nodeFromCoordinate instanceof Node) {
                node = (Node) nodeFromCoordinate;
            } else {
                Utils.println("ASSERT WRONG TYPE public Node getNode " + new Coordinate(pos, worldObj));
            }
            if (node == null) DelayedBlockRemove.add(new Coordinate(pos, this.worldObj));
        }
        return node;
    }


    public static NodeBlockEntity getEntity(BlockPos pos) {
        TileEntity entity;
        if ((entity = Minecraft.getMinecraft().theWorld.getTileEntity(pos)) != null) {
            if (entity instanceof NodeBlockEntity) {
                return (NodeBlockEntity) entity;
            }
        }
        return null;
    }

    //TODO: FIX PACKETS
    @Override
    public Packet getDescriptionPacket() {
        Node node = getNode(); //TO DO NULL POINTER
        if (node == null) {
            Utils.println("ASSERT NULL NODE public Packet getDescriptionPacket() nodeblock entity");
            return null;
        }

        ByteBuf buffer = Unpooled.buffer();
        return new SPacketCustomPayload(Eln.channelName, new PacketBuffer(buffer.writeBytes(node.getPublishPacket().toByteArray())));
        //return null;
    }


    public void preparePacketForServer(DataOutputStream stream) {
        try {
            stream.writeByte(Eln.packetPublishForNode);

            stream.writeInt(pos.getX());
            stream.writeInt(pos.getY());
            stream.writeInt(pos.getZ());

            stream.writeByte(worldObj.provider.getDimension());

            stream.writeUTF(getNodeUuid());


        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public void sendPacketToServer(ByteArrayOutputStream bos) {
        UtilsClient.sendPacketToServer(new PacketBuffer(Unpooled.buffer().readBytes(bos.toByteArray())));
    }


    public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
        return null;
    }

    public int getCableDry(Direction side, LRDU lrdu) {
        return 0;
    }

    public boolean canConnectRedstone(Direction xn) {
        if (worldObj.isRemote)
            return redstone;
        else {
            if (getNode() == null) return false;
            return getNode().canConnectRedstone();
        }
    }

    public void clientRefresh(float deltaT) {

    }
}
