package mods.eln.node;



import mods.eln.compat.WorldCompat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.*;
import mods.eln.server.DelayedBlockRemove;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.EnumSkyBlock;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;


public abstract class NodeBlockEntity extends TileEntity implements ITileEntitySpawnClient, INodeEntity, ITickable {

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
                WorldCompat.notifyNeighbours(world, pos.getX(), pos.getY(), pos.getZ(), getBlockType());
            } else {
                redstone = newRedstone;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    /*	if(lastLight == 0xFF) //boot trololol
        {
			lastLight = 15;
			WorldCompat.updateLightByType(world, EnumSkyBlock.BLOCK,pos.getX(),pos.getY(),pos.getZ());
		}*/

        if (lastLight != light) {
            lastLight = light;
            WorldCompat.updateLightByType(world, EnumSkyBlock.BLOCK, pos.getX(), pos.getY(), pos.getZ());
        }


    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) {

    }


    //abstract public Node newNode();
    //abstract public Node newNode(Direction front,EntityLiving entityLiving,int metadata);

    public abstract int isProvidingWeakPower(Direction side);
    //{
    //if(world.isRemote) return 0;
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
            return new AxisAlignedBB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
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
     * Reads a tile entity from NBT.
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

    /** 1.7.10 kept the TE when only the metadata changed; 1.12 would recreate it on any state change. */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }


    //max draw distance
    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 4096.0 * (4) * (4);
    }


    void onBlockPlacedBy(Direction front, EntityLivingBase entityLiving, int metadata) {

    }


    boolean updateEntityFirst = true;

    // 1.7.10 updateEntity() (canUpdate() was true): ITickable.update() in 1.12
    @Override
    public void update() {
        if (updateEntityFirst) {
            updateEntityFirst = false;
            if (!world.isRemote) {
                // WorldCompat.setBlock(world, pos.getX(), pos.getY(), pos.getZ(), 0);
            } else {
                clientList.add(this);
            }
        }
    }


    public void onBlockAdded() {
        if (!world.isRemote && getNode() == null) {
            WorldCompat.setBlockToAir(world, pos.getX(), pos.getY(), pos.getZ());
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
        //if(entityPlayer.getHeldItemMainhand().getItem() instanceof ItemBlock)
        {
            return true;
        }
        //return true;
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
        if (this.world == null) return null;
        if (node == null) {
            NodeBase nodeFromCoordonate = NodeManager.instance.getNodeFromCoordonate(new Coordonate(pos.getX(), pos.getY(), pos.getZ(), world));
            if (nodeFromCoordonate instanceof Node) {
                node = (Node) nodeFromCoordonate;
            } else {
                Utils.println("ASSERT WRONG TYPE public Node getNode " + new Coordonate(pos.getX(), pos.getY(), pos.getZ(), world));
            }
            if (node == null) DelayedBlockRemove.add(new Coordonate(pos.getX(), pos.getY(), pos.getZ(), this.world));
        }
        return node;
    }


    @SideOnly(Side.CLIENT)
    public static NodeBlockEntity getEntity(int x, int y, int z) {
        TileEntity entity;
        if ((entity = WorldCompat.getTileEntity(Minecraft.getMinecraft().world, x, y, z)) != null) {
            if (entity instanceof NodeBlockEntity) {
                return (NodeBlockEntity) entity;
            }
        }
        return null;
    }


    /**
     * 1.7.10 getDescriptionPacket() sent the node's publish packet (EA byte protocol) as a custom payload.
     * 1.12 only syncs TEs through NBT, so the same bytes ride in the update tag / update packet and are fed to
     * the client packet handler on arrival (both handlers run on the client main thread).
     */
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        if (world == null || world.isRemote) return tag;
        Node node = getNode(); //TO DO NULL POINTER
        if (node == null) {
            Utils.println("ASSERT NULL NODE public Packet getDescriptionPacket() nodeblock entity");
            return tag;
        }
        tag.setByteArray(DESCRIPTION_KEY, node.getPublishPacket().toByteArray());
        return tag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound tag = getUpdateTag();
        if (!tag.hasKey(DESCRIPTION_KEY)) return null;
        return new SPacketUpdateTileEntity(pos, 0, tag);
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        if (tag.hasKey(DESCRIPTION_KEY)) Eln.proxy.handleDescriptionPacket(tag.getByteArray(DESCRIPTION_KEY));
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    public static final String DESCRIPTION_KEY = "elnDescription";


    public void preparePacketForServer(DataOutputStream stream) {
        try {
            stream.writeByte(Eln.packetPublishForNode);

            stream.writeInt(pos.getX());
            stream.writeInt(pos.getY());
            stream.writeInt(pos.getZ());

            stream.writeInt(world.provider.getDimension()); // 1.12 port: dimension as int (was byte)

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

    public void clientRefresh(float deltaT) {

    }
}
