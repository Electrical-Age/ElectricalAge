package mods.eln.node;

import mods.eln.Eln;
import mods.eln.GuiHandler;
import mods.eln.ghost.GhostBlock;
import mods.eln.misc.*;
import mods.eln.node.six.SixNode;
import mods.eln.server.PlayerManager;
import mods.eln.sim.*;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public abstract class NodeBase {

    public static final int maskElectricalPower = 1 << 0;
    public static final int maskThermal = 1 << 1;

    public static final int maskElectricalGate = (1 << 2);
    public static final int maskElectricalAll = maskElectricalPower | maskElectricalGate;

    public static final int maskElectricalInputGate = maskElectricalGate;
    public static final int maskElectricalOutputGate = maskElectricalGate;

    public static final int maskWire = 0;
    public static final int maskElectricalWire = (1 << 3);
    public static final int maskThermalWire = maskWire + maskThermal;

    public static final int maskSignal = (1 << 9);
    public static final int maskRs485 = (1 << 10);

    public static final int maskColorData = 0xF << 16;
    public static final int maskColorShift = 16;
    public static final int maskColorCareShift = 20;
    public static final int maskColorCareData = 1 << 20;

    public static final double networkSerializeUFactor = 10.0;
    public static final double networkSerializeIFactor = 100.0;
    public static final double networkSerializeTFactor = 10.0;

    public byte neighborOpaque;
    public byte neighborWrapable;

    public static int teststatic;

    public Coordinate coordinate;

    public ArrayList<NodeConnection> nodeConnectionList = new ArrayList<NodeConnection>(4);

    private boolean initialized = false;

    private boolean isAdded = false;

    private boolean needPublish = false;

    // public static boolean canBePlacedOn(ItemStack itemStack,Direction side)

    public boolean mustBeSaved() {
        return true;
    }

    public int getBlockMetadata() {
        return 0;
    }

    public void networkUnserialize(DataInputStream stream, EntityPlayerMP player) {

    }

    public void notifyNeighbor() {
        coordinate.world().notifyNeighborsRespectDebug(coordinate.pos, coordinate.getBlockState().getBlock());
    }

    //public abstract Block getBlock();
    public abstract String getNodeUuid();

    public LRDUCubeMask lrduCubeMask = new LRDUCubeMask();

    public void neighborBlockRead() {
        int[] vector = new int[3];
        World world = coordinate.world();

        neighborOpaque = 0;
        neighborWrapable = 0;
        for (Direction direction : Direction.values()) {
            BlockPos.MutableBlockPos pos = coordinate.pos;
            vector[0] = pos.getX();
            vector[1] = pos.getY();
            vector[2] = pos.getZ();

            direction.applyTo(vector, 1);

            Block b = world.getBlockState(pos).getBlock();
            neighborOpaque |= 1 << direction.getInt();
            if (isBlockWrappable(b, world, pos)) {
                neighborWrapable |= 1 << direction.getInt();
            }
        }
    }

    public boolean hasGui(Direction side) {
        return false;
    }

    public void onNeighborBlockChange() {
        neighborBlockRead();
        if (isAdded) {
            reconnect();
        }
    }

    public boolean isBlockWrappable(Direction direction) {
        return ((neighborWrapable >> direction.getInt()) & 1) != 0;
    }

    public boolean isBlockOpaque(Direction direction) {
        return ((neighborOpaque >> direction.getInt()) & 1) != 0;
    }

    public static boolean isBlockWrappable(Block block, World w, BlockPos pos) {
        Block[] blocks = {Eln.sixNodeBlock,
            Blocks.TORCH,
            Blocks.REDSTONE_TORCH,
            Blocks.UNLIT_REDSTONE_TORCH,
            Blocks.REDSTONE_WIRE};
        if (block.isReplaceable(w, pos)) return true;
        if (w.isAirBlock(pos)) return true;
        for (int i = 0; i < blocks.length; i++) {
            if (block == blocks[i]) return true;
        }

        if (block instanceof GhostBlock) return true;

        return false;
    }

    public NodeBase() {
        coordinate = new Coordinate();
    }

    boolean destructed = false;

    public boolean isDestructing() {
        return destructed;
    }

    public void physicalSelfDestruction(float explosionStrength) {
        if (destructed == true) return;
        destructed = true;
        if (Eln.instance.explosionEnable == false) explosionStrength = 0;
        disconnect();
        World world = coordinate.world();
        BlockPos.MutableBlockPos pos = coordinate.pos;
        world.setBlockToAir(pos);
        NodeManager.instance.removeNode(this);
        if (explosionStrength != 0) {
            world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), explosionStrength, true);
        }
    }

    // NodeBaseTodo
    public void onBlockPlacedBy(Coordinate coordinate, Direction front, EntityLivingBase entityLiving, ItemStack itemStack) {
        // this.entity = entity;
        this.coordinate = coordinate;
        neighborBlockRead();
        NodeManager.instance.addNode(this);

        initializeFromThat(front, entityLiving, itemStack);

        if (itemStack != null)
            Utils.println("Node::constructor( meta = " + itemStack.getItemDamage() + ")");
    }

    abstract public void initializeFromThat(Direction front,
                                            EntityLivingBase entityLiving, ItemStack itemStack);

    public NodeBase getNeighbor(Direction direction) {
        BlockPos neighbour = direction.applied(coordinate.pos, 1);
        Coordinate coordinate = new Coordinate(neighbour, this.coordinate.getDimension());
        return NodeManager.instance.getNodeFromCoordinate(coordinate);
    }

    // leaf
    public void onBreakBlock() {
        destructed = true;
        disconnect();
        NodeManager.instance.removeNode(this);
        Utils.println("Node::onBreakBlock()");
    }

    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        if (!entityPlayer.worldObj.isRemote && entityPlayer.getHeldItemMainhand() != null) {
            if (Eln.multiMeterElement.checkSameItemStack(entityPlayer.getHeldItemMainhand())) {
                String str = multiMeterString(side);
                if (str != null)
                    Utils.addChatMessage(entityPlayer, str);
                return true;
            }
            if (Eln.thermometerElement.checkSameItemStack(entityPlayer.getHeldItemMainhand())) {
                String str = thermoMeterString(side);
                if (str != null)
                    Utils.addChatMessage(entityPlayer, str);
                return true;
            }
            if (Eln.allMeterElement.checkSameItemStack(entityPlayer.getHeldItemMainhand())) {
                String str1 = multiMeterString(side);
                String str2 = thermoMeterString(side);
                String str = "";
                if (str1 != null)
                    str += str1;
                if (str2 != null)
                    str += str2;
                if (str.equals("") == false)
                    Utils.addChatMessage(entityPlayer, str);
                return true;
            }
        }
        if (hasGui(side)) {
            entityPlayer.openGui(Eln.instance, GuiHandler.nodeBaseOpen + side.getInt(), coordinate.world(), coordinate.pos.getX(), coordinate.pos.getY(), coordinate.pos.getZ());
            return true;
        }

        return false;
    }

    public void reconnect() {
        disconnect();
        connect();
    }

    public static void tryConnectTwoNode(NodeBase nodeA, Direction directionA, LRDU lrduA, NodeBase nodeB, Direction directionB, LRDU lrduB) {
        if (compareConnectionMask(nodeA.getSideConnectionMask(directionA, lrduA), nodeB.getSideConnectionMask(directionB, lrduB))) {
            ElectricalConnection eCon = null;
            ThermalConnection tCon = null;

            nodeA.lrduCubeMask.set(directionA, lrduA, true);
            nodeB.lrduCubeMask.set(directionB, lrduB, true);

            nodeA.newConnectionAt(directionA, lrduA);
            nodeB.newConnectionAt(directionB, lrduB);

            ElectricalLoad eLoad;
            if ((eLoad = nodeA.getElectricalLoad(directionA, lrduA)) != null) {

                ElectricalLoad otherELoad = nodeB.getElectricalLoad(directionB, lrduB);
                if (otherELoad != null) {
                    eCon = new ElectricalConnection(eLoad, otherELoad);

                    Eln.simulator.addElectricalComponent(eCon);
                }
            }
            ThermalLoad tLoad;
            if ((tLoad = nodeA.getThermalLoad(directionA, lrduA)) != null) {

                ThermalLoad otherTLoad = nodeB.getThermalLoad(directionB, lrduB);
                if (otherTLoad != null) {
                    tCon = new ThermalConnection(tLoad, otherTLoad);

                    Eln.simulator.addThermalConnection(tCon);
                }

            }
            NodeConnection nodeConnection = new NodeConnection(nodeA, directionA, lrduA, nodeB, directionB, lrduB, eCon, tCon);

            nodeA.nodeConnectionList.add(nodeConnection);
            nodeB.nodeConnectionList.add(nodeConnection);

            nodeA.setNeedPublish(true);
            nodeB.setNeedPublish(true);
        }
    }

    public abstract int getSideConnectionMask(Direction directionA, LRDU lrduA);

    public abstract ThermalLoad getThermalLoad(Direction directionA, LRDU lrduA);

    public abstract ElectricalLoad getElectricalLoad(Direction directionB, LRDU lrduB);

    public void checkCanStay(boolean onCreate) {

    }

    public void connectJob() {
        // EXTERNAL OTHERS SIXNODE
        {
            //int[] emptyBlockCoord = new int[3];
            //int[] otherBlockCoord = new int[3];
            for (Direction direction : Direction.values()) {
                if (isBlockWrappable(direction)) {
                    BlockPos.MutableBlockPos emptyBlockCoord = new BlockPos.MutableBlockPos(coordinate.pos);
                    direction.applyTo(emptyBlockCoord, 1);
                    for (LRDU lrdu : LRDU.values()) {
                        Direction elementSide = direction.applyLRDU(lrdu);
                        BlockPos.MutableBlockPos otherBlockCoord = new BlockPos.MutableBlockPos(emptyBlockCoord);
                        elementSide.applyTo(otherBlockCoord, 1);
                        NodeBase otherNode = NodeManager.instance.getNodeFromCoordinate(new Coordinate(otherBlockCoord, coordinate.getDimension()));
                        if (otherNode == null) continue;
                        Direction otherDirection = elementSide.getInverse();
                        LRDU otherLRDU = otherDirection.getLRDUGoingTo(direction).inverse();
                        if (this instanceof SixNode || otherNode instanceof SixNode) {
                            tryConnectTwoNode(this, direction, lrdu, otherNode, otherDirection, otherLRDU);
                        }
                    }
                }
            }
        }

        {
            for (Direction dir : Direction.values()) {
                NodeBase otherNode = getNeighbor(dir);
                if (otherNode != null && otherNode.isAdded) {
                    for (LRDU lrdu : LRDU.values()) {
                        tryConnectTwoNode(this, dir, lrdu, otherNode, dir.getInverse(), lrdu.inverseIfLR());
                    }
                }

            }
        }

    }

    public void disconnectJob() {

        for (NodeConnection c : nodeConnectionList) {

            if (c.N1 != this) {
                c.N1.nodeConnectionList.remove(c);
                c.N1.setNeedPublish(true);
                c.N1.lrduCubeMask.set(c.dir1, c.lrdu1, false);
            } else {
                c.N2.nodeConnectionList.remove(c);
                c.N2.setNeedPublish(true);
                c.N2.lrduCubeMask.set(c.dir2, c.lrdu2, false);
            }
            c.destroy();
        }

        lrduCubeMask.clear();

        nodeConnectionList.clear();
    }

    public static boolean compareConnectionMask(int mask1, int mask2) {
        if (((mask1 & 0xFFFF) & (mask2 & 0xFFFF)) == 0) return false;
        if (((mask1 & maskColorCareData) & (mask2 & maskColorCareData)) == 0) return true;
        if ((mask1 & maskColorData) == (mask2 & maskColorData)) return true;
        return false;
    }

    public void externalDisconnect(Direction side, LRDU lrdu) {
    }

    public void newConnectionAt(Direction side, LRDU lrdu) {
    }

    public void connectInit() {
        lrduCubeMask.clear();
        nodeConnectionList.clear();
    }

    public void connect() {

        if (isAdded) {
            disconnect();
        }

        connectInit();
        connectJob();

        isAdded = true;

        setNeedPublish(true);

    }

    public void disconnect() {
        if (!isAdded) {
            Utils.println("Node destroy error already destroy");
            return;
        }

        disconnectJob();

        isAdded = false;
    }

    public boolean nodeAutoSave() {
        return true;
    }

    public void readFromNBT(NBTTagCompound nbt) {

        coordinate.readFromNBT(nbt, "c");

        neighborOpaque = nbt.getByte("NBOpaque");
        neighborWrapable = nbt.getByte("NBWrap");

        initialized = true;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {

        coordinate.writeToNBT(nbt, "c");

        int idx;

        nbt.setByte("NBOpaque", neighborOpaque);
        nbt.setByte("NBWrap", neighborWrapable);
        return nbt;
    }

    public String multiMeterString(Direction side) {
        return "";
    }

    public String thermoMeterString(Direction side) {
        return "";
    }

    public void setNeedPublish(boolean needPublish) {
        this.needPublish = needPublish;
    }

    public boolean getNeedPublish() {
        return needPublish;
    }

    private boolean isINodeProcess(IProcess process) {
        for (Class c : process.getClass().getInterfaces()) {
            if (c == INBTTReady.class) return true;
        }
        return false;
    }

    boolean needNotify = false;

    public void publishSerialize(DataOutputStream stream) {

    }

    public void preparePacketForClient(DataOutputStream stream) {
        try {
            stream.writeByte(Eln.packetForClientNode);

            BlockPos pos = coordinate.pos;
            stream.writeInt(pos.getX());
            stream.writeInt(pos.getY());
            stream.writeInt(pos.getZ());

            stream.writeByte(coordinate.getDimension());

            stream.writeUTF(getNodeUuid());

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public void sendPacketToClient(ByteArrayOutputStream bos, EntityPlayerMP player) {
        Utils.sendPacketToClient(bos, player);
    }


    public void sendPacketToAllClient(ByteArrayOutputStream bos) {
        sendPacketToAllClient(bos, 100000);
    }

    public void sendPacketToAllClient(ByteArrayOutputStream bos, double range) {
        //Profiler p = new Profiler();

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        //TODO: Find new way to get isPlayerWatchingChunk()

        for (Object obj : server.getEntityWorld().playerEntities) {

            EntityPlayerMP player = (EntityPlayerMP) obj;
            WorldServer worldServer = (WorldServer) server.worldServerForDimension(player.dimension);
            PlayerManager playerManager = worldServer.getPlayerManager();
            if (player.dimension != this.coordinate.getDimension()) continue;
            if (!playerManager.isPlayerWatchingChunk(player, coordinate.pos.getX() / 16, coordinate.pos.getZ() / 16)) continue;
            if (coordinate.distanceTo(player) > range) continue;

            Utils.sendPacketToClient(bos, player);
        }

    }

    public ByteArrayOutputStream getPublishPacket() {

        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(bos);

        try {

            stream.writeByte(Eln.packetNodeSingleSerialized);

            BlockPos pos = coordinate.pos;
            stream.writeInt(pos.getX());
            stream.writeInt(pos.getY());
            stream.writeInt(pos.getZ());
            stream.writeByte(coordinate.getDimension());

            stream.writeUTF(getNodeUuid());

            publishSerialize(stream);

            return bos;
        } catch (IOException e) {

            e.printStackTrace();

        }
        return null;
    }

    public void publishToAllPlayer() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        for (Object obj : server.getEntityWorld().playerEntities) {
            EntityPlayerMP player = (EntityPlayerMP) obj;
            WorldServer worldServer = (WorldServer) server.worldServerForDimension(player.dimension);
            PlayerManager playerManager = worldServer.getPlayerManager();
            if (player.dimension != this.coordinate.getDimension()) continue;
            if (!playerManager.isPlayerWatchingChunk(player, coordinate.pos.getX() / 16, coordinate.pos.getZ() / 16)) continue;

            Utils.sendPacketToClient(getPublishPacket(), player);
        }
        if (needNotify) {
            needNotify = false;
            notifyNeighbor();
        }
        needPublish = false;
    }

    public void publishToPlayer(EntityPlayerMP player) {
        Utils.sendPacketToClient(getPublishPacket(), player);
    }

    public void dropItem(ItemStack itemStack) {
        if (itemStack == null) return;
        World w = coordinate.world();
        BlockPos pos = coordinate.pos;
        if (w.getGameRules().getBoolean("doTileDrops")) {
            float var6 = 0.7F;
            double var7 = (double) (w.rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
            double var9 = (double) (w.rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
            double var11 = (double) (w.rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
            EntityItem var13 = new EntityItem(w, (double) pos.getX() + var7, (double) pos.getY() + var9, (double) pos.getZ() + var11, itemStack);
            var13.setPickupDelay(10);
            w.spawnEntityInWorld(var13);
        }
    }

    public void dropInventory(IInventory inventory) {
        if (inventory == null) return;
        for (int idx = 0; idx < inventory.getSizeInventory(); idx++) {
            dropItem(inventory.getStackInSlot(idx));
        }
    }

    public abstract void initializeFromNBT();

    public void globalBoot() {

    }

    public void needPublish() {
        setNeedPublish(true);
    }

    public void unload() {
        disconnect();
    }
}
