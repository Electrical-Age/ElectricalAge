package mods.eln.node;

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
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.io.*;
import java.util.LinkedList;

public abstract class NodeBlockEntity extends TileEntity implements ITileEntitySpawnClient, INodeEntity {

    public static final LinkedList<NodeBlockEntity> clientList = new LinkedList<NodeBlockEntity>();

    boolean redstone = false;
    int lastLight = 0xFF;
    boolean firstUnserialize = true;
    Node node = null;

    public NodeBlock getBlock() {
        return (NodeBlock) getBlockType();
    }

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
                world.notifyNeighborsRespectDebug(getPos(), getBlockType(), true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (lastLight != light) {
            lastLight = light;
            world.checkLightFor(EnumSkyBlock.BLOCK, getPos());
        }
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) {}

    //abstract public Node newNode();
    //abstract public Node newNode(Direction front,EntityLiving entityLiving,int metadata);

    public abstract int isProvidingWeakPower(Direction side);
    //{
    //if(world.isRemote) return 0;
    //return getNode().isProvidingWeakPower(side);
    //}

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return null;
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return null;
    }

    public NodeBlockEntity() {}

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
        if (world.isRemote) {
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

    void onBlockPlacedBy(Direction front, EntityLivingBase entityLiving, IBlockState state) {}

    @Override
    public void onLoad()
    {
        if (!world.isRemote) {
            // world.setBlock(xCoord, yCoord, zCoord, 0);
        } else {
            clientList.add(this);
        }
    }

    public void onBlockAdded() {
        if (!world.isRemote && getNode() == null) {
            world.setBlockToAir(pos);
        }
    }

    public void onBreakBlock() {
        if (!world.isRemote) {
            if (getNode() == null) return;
            getNode().onBreakBlock();
        }
    }

    public void onChunkUnload() {
        if (world.isRemote) {
            destructor();
        }
    }

    //client only
    public void destructor() {
        clientList.remove(this);
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
        //if(entityPlayer.getCurrentEquippedItem().getItem() instanceof ItemBlock)
        return true;
    }

    public void onNeighborBlockChange() {
        if (!world.isRemote) {
            if (getNode() == null) return;
            getNode().onNeighborBlockChange();
        }
    }

    public Node getNode() {
        if (world.isRemote) {
            Utils.fatal();
            return null;
        }
        if (node == null) {
            NodeBase nodeFromCoordinate = NodeManager.instance.getNodeFromCoordinate(new Coordinate(pos, world));
            if (nodeFromCoordinate instanceof Node) {
                node = (Node) nodeFromCoordinate;
            } else {
                Utils.println("ASSERT WRONG TYPE public Node getNode " + new Coordinate(pos, world));
            }
            if (node == null) DelayedBlockRemove.add(new Coordinate(pos, this.world));
        }
        return node;
    }

    public static NodeBlockEntity getEntity(BlockPos pos) {
        TileEntity entity;
        if ((entity = Minecraft.getMinecraft().world.getTileEntity(pos)) != null) {
            if (entity instanceof NodeBlockEntity) {
                return (NodeBlockEntity) entity;
            }
        }
        return null;
    }

    // TODO(1.10): Packets are probably still broken somehow!
    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        Node node = getNode();
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

    public void preparePacketForServer(DataOutputStream stream) {
        try {
            stream.writeByte(Eln.PACKET_PUBLISH_FOR_NODE);
            stream.writeInt(pos.getX());
            stream.writeInt(pos.getY());
            stream.writeInt(pos.getZ());
            stream.writeByte(world.provider.getDimension());
            stream.writeUTF(getNodeUuid());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPacketToServer(ByteArrayOutputStream bos) {
        UtilsClient.sendPacketToServer(bos);
    }

    public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
        return null;
    }

    public int getCableDry(Direction side, LRDU lrdu) {
        return 0;
    }

    public boolean canConnectRedstone(Direction xn) {
        if (world.isRemote)
            return redstone;
        else {
            if (getNode() == null) return false;
            return getNode().canConnectRedstone();
        }
    }

    public void clientRefresh(float deltaT) {}
}
