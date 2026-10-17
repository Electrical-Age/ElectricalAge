package mods.eln.node;



import net.minecraft.core.BlockPos;
import mods.eln.Eln;
import mods.eln.misc.*;
import mods.eln.node.six.SixNode;
import mods.eln.sim.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

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

    public Coordonate coordonate;

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

    public void networkUnserialize(DataInputStream stream, ServerPlayer player) {

    }

    public void notifyNeighbor() {
        Utils.notifyNeighbours(coordonate.world(), coordonate.pos());
    }

    //public abstract Block getBlock();
    public abstract String getNodeUuid();

    public LRDUCubeMask lrduCubeMask = new LRDUCubeMask();

    public void neighborBlockRead() {
        int[] vector = new int[3];
        Level world = coordonate.world();

        neighborOpaque = 0;
        neighborWrapable = 0;
        for (Direction direction : Direction.values()) {
            vector[0] = coordonate.x;
            vector[1] = coordonate.y;
            vector[2] = coordonate.z;

            direction.applyTo(vector, 1);

            BlockPos np = new BlockPos(vector[0], vector[1], vector[2]);
            net.minecraft.world.level.block.state.BlockState bs = world.getBlockState(np);
            Block b = bs.getBlock();
            // 1.12 port, known bug fixed: 1.7.10 had `if (b.isSolidRender()) ;` (empty statement), so every neighbour
            // counted as opaque and TransparentNodeElement.checkCanStay never removed unsupported devices. Same rule as
            // TransparentNodeDescriptor.checkCanPlace (default-state opaque cube; a hopper counts as a floor).
            if (bs.isSolidRender() || (direction == Direction.YN && b instanceof net.minecraft.world.level.block.HopperBlock))
                neighborOpaque |= 1 << direction.getInt();
            if (isBlockWrappable(bs, world, np))
                neighborWrapable |= 1 << direction.getInt();
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

    public static boolean isBlockWrappable(net.minecraft.world.level.block.state.BlockState state, Level w, BlockPos pos) {
        Block block = state.getBlock();
        if (state.canBeReplaced()) return true;
        if (state.isAir()) return true;
        if (block == Eln.sixNodeBlock) return true;
        if (block == Eln.ghostBlock) return true;
        if (block == Blocks.TORCH || block == Blocks.WALL_TORCH) return true;
        if (block == Blocks.REDSTONE_TORCH || block == Blocks.REDSTONE_WALL_TORCH) return true; // 26.x: lit is a state
        if (block == Blocks.REDSTONE_WIRE) return true;

        return false;
    }

    public NodeBase() {
        coordonate = new Coordonate();
    }

    boolean destructed = false;

    public boolean isDestructing() {
        return destructed;
    }

    public void physicalSelfDestruction(float explosionStrength) {
        if (destructed == true) return;
        destructed = true;
        if (!Eln.explosionEnable) explosionStrength = 0;
        disconnect();
        coordonate.world().removeBlock(coordonate.pos(), false);
        NodeManager.instance.removeNode(this);
        if (explosionStrength != 0) {
            coordonate.world().explode(null, coordonate.x, coordonate.y, coordonate.z, explosionStrength, Level.ExplosionInteraction.BLOCK);
        }
    }

    // NodeBaseTodo
    public void onBlockPlacedBy(Coordonate coordonate, Direction front, LivingEntity entityLiving, ItemStack itemStack) {
        // this.entity = entity;
        this.coordonate = coordonate;
        neighborBlockRead();
        NodeManager.instance.addNode(this);

        initializeFromThat(front, entityLiving, itemStack);

        if (!Utils.isEmpty(itemStack))
            Utils.println("Node::constructor( " + itemStack + ")");
    }

    abstract public void initializeFromThat(Direction front,
                                            LivingEntity entityLiving, ItemStack itemStack);

    public NodeBase getNeighbor(Direction direction) {
        int[] position = new int[3];
        position[0] = coordonate.x;
        position[1] = coordonate.y;
        position[2] = coordonate.z;
        direction.applyTo(position, 1);
        Coordonate nodeCoordonate = new Coordonate(position[0], position[1], position[2], coordonate.dimention);
        return NodeManager.instance.getNodeFromCoordonate(nodeCoordonate);
    }

    // leaf
    public void onBreakBlock() {
        destructed = true;
        disconnect();
        NodeManager.instance.removeNode(this);
        Utils.println("Node::onBreakBlock()");
    }

    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        ItemStack held = entityPlayer.getMainHandItem();
        if (!entityPlayer.level().isClientSide() && !Utils.isEmpty(held)) {
            // meters (MeterItems is filled when the meter items are ported)
            boolean multi = MeterItems.isMultiMeter(held), thermo = MeterItems.isThermometer(held), all = MeterItems.isAllMeter(held);
            if (multi || thermo || all) {
                String str = "";
                if (multi || all) str += nullToEmpty(multiMeterString(side));
                if (thermo || all) str += nullToEmpty(thermoMeterString(side));
                if (!str.isEmpty()) Utils.sendMessage(entityPlayer, str);
                return true;
            }
        }
        if (hasGui(side)) {
            // 1.12 opened GuiHandler.nodeBaseOpen + side; 26.x: the eln:node menu (P4)
            return NodeMenus.open(this, entityPlayer, side);
        }

        return false;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
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
            int[] emptyBlockCoord = new int[3];
            int[] otherBlockCoord = new int[3];
            for (Direction direction : Direction.values()) {
                if (isBlockWrappable(direction)) {
                    emptyBlockCoord[0] = coordonate.x;
                    emptyBlockCoord[1] = coordonate.y;
                    emptyBlockCoord[2] = coordonate.z;
                    direction.applyTo(emptyBlockCoord, 1);
                    for (LRDU lrdu : LRDU.values()) {
                        Direction elementSide = direction.applyLRDU(lrdu);
                        otherBlockCoord[0] = emptyBlockCoord[0];
                        otherBlockCoord[1] = emptyBlockCoord[1];
                        otherBlockCoord[2] = emptyBlockCoord[2];
                        elementSide.applyTo(otherBlockCoord, 1);
                        NodeBase otherNode = NodeManager.instance.getNodeFromCoordonate(new Coordonate(otherBlockCoord[0], otherBlockCoord[1], otherBlockCoord[2], coordonate.dimention));
                        if (otherNode == null) continue;
                        Direction otherDirection = elementSide.getInverse();
                        LRDU otherLRDU = otherDirection.getLRDUGoingTo(direction).inverse();
                        if (this instanceof SixNode || otherNode instanceof SixNode) {
                            // a frozen neighbour (Stasis) gets no link, only the look of one: a real link into a frozen
                            // node outlived its parts and hung incognito (2026-10-17); without the look, cables at the
                            // tower yard's edge seemed cut (Baughn)
                            if (otherNode.isAdded) tryConnectTwoNode(this, direction, lrdu, otherNode, otherDirection, otherLRDU);
                            else visualPair(direction, lrdu, otherNode, otherDirection, otherLRDU);
                        }
                    }
                }
            }
        }

        {
            for (Direction dir : Direction.values()) {
                NodeBase otherNode = getNeighbor(dir);
                if (otherNode != null) {
                    for (LRDU lrdu : LRDU.values()) {
                        if (otherNode.isAdded) tryConnectTwoNode(this, dir, lrdu, otherNode, dir.getInverse(), lrdu.inverseIfLR());
                        else visualPair(dir, lrdu, otherNode, dir.getInverse(), lrdu.inverseIfLR());   // (frozen: the look only)
                    }
                }

            }
        }

    }

    /**
     * 26.x, for Stasis Delvers: a node taken out of the simulation (time stopped) still shows the connections it would
     * make, so frozen cables don't look cut. Masks only, on this node only; no electrical or thermal connection. Returns
     * true if what the client sees changed (and then marks the node for publishing).
     */
    public boolean visualConnect() {
        int before = visualHash();
        lrduCubeMask.clear();
        visualConnectInit();
        int[] emptyBlockCoord = new int[3];
        int[] otherBlockCoord = new int[3];
        for (Direction direction : Direction.values()) {
            if (!isBlockWrappable(direction)) continue;
            emptyBlockCoord[0] = coordonate.x;
            emptyBlockCoord[1] = coordonate.y;
            emptyBlockCoord[2] = coordonate.z;
            direction.applyTo(emptyBlockCoord, 1);
            for (LRDU lrdu : LRDU.values()) {
                Direction elementSide = direction.applyLRDU(lrdu);
                otherBlockCoord[0] = emptyBlockCoord[0];
                otherBlockCoord[1] = emptyBlockCoord[1];
                otherBlockCoord[2] = emptyBlockCoord[2];
                elementSide.applyTo(otherBlockCoord, 1);
                NodeBase otherNode = NodeManager.instance.getNodeFromCoordonate(new Coordonate(otherBlockCoord[0], otherBlockCoord[1], otherBlockCoord[2], coordonate.dimention));
                if (otherNode == null) continue;
                Direction otherDirection = elementSide.getInverse();
                LRDU otherLRDU = otherDirection.getLRDUGoingTo(direction).inverse();
                if (this instanceof SixNode || otherNode instanceof SixNode) visualPair(direction, lrdu, otherNode, otherDirection, otherLRDU);
            }
        }
        for (Direction dir : Direction.values()) {
            NodeBase otherNode = getNeighbor(dir);
            if (otherNode == null) continue;
            for (LRDU lrdu : LRDU.values()) visualPair(dir, lrdu, otherNode, dir.getInverse(), lrdu.inverseIfLR());
        }
        visualConnectInternal();
        boolean changed = visualHash() != before;
        if (changed) setNeedPublish(true);
        return changed;
    }

    private void visualPair(Direction directionA, LRDU lrduA, NodeBase nodeB, Direction directionB, LRDU lrduB) {
        if (compareConnectionMask(getSideConnectionMask(directionA, lrduA), nodeB.getSideConnectionMask(directionB, lrduB))) {
            lrduCubeMask.set(directionA, lrduA, true);
            newConnectionAt(directionA, lrduA);
        }
    }

    /** Clear what visualConnect recomputes (subclasses: their element masks). */
    protected void visualConnectInit() {
    }

    /** Internal (within the block) visual connections, for subclasses. */
    protected void visualConnectInternal() {
    }

    /** A hash of what the client sees of the connections. */
    protected int visualHash() {
        int h = 0;
        for (Direction d : Direction.values()) h = h * 31 + lrduCubeMask.get(d).mask;
        return h;
    }

    public void disconnectJob() {
        dropConnections();
    }

    /** Remove every NodeConnection of this node, on both ends. */
    protected void dropConnections() {
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

    /**
     * 26.x, for Stasis Delvers: if set and true for a node, connect() keeps it out of the simulation and only draws its
     * connections (visualConnect). EA reconnects nodes from many places (a second device in a block, a switch toggled,
     * a burn or repair); without this a frozen node could slip back into the simulation until the next freezer pass, and
     * what got linked would depend on the order things happened (Baughn).
     */
    public static volatile java.util.function.Predicate<NodeBase> simulationVeto;

    /** Is the node in the simulation (connected)? */
    public boolean isAdded() {
        return isAdded;
    }

    public void connect() {

        if (isAdded) {
            disconnect();
        }

        var veto = simulationVeto;
        if (veto != null && veto.test(this)) {
            visualConnect();
            return;
        }

        connectInit();
        connectJob();

        isAdded = true;

        setNeedPublish(true);

    }

    public void disconnect() {
        if (!isAdded) {
            // Not in the simulation (frozen by Stasis, or never connected), but a running neighbour may still have linked
            // to it: drop those links too, or a part removed later leaves a link to an empty side (incognito hung on it,
            // 2026-10-17: a frozen node's part mined after a tower collapse).
            dropConnections();
            return;
        }

        disconnectJob();

        isAdded = false;
    }

    public boolean nodeAutoSave() {
        return true;
    }

    public void readFromNBT(CompoundTag nbt) {

        coordonate.readFromNBT(nbt, "c");

        neighborOpaque = nbt.getByteOr("NBOpaque", (byte) 0);
        neighborWrapable = nbt.getByteOr("NBWrap", (byte) 0);

        initialized = true;
    }

    public void writeToNBT(CompoundTag nbt) {

        coordonate.writeToNBT(nbt, "c");

        int idx;

        nbt.putByte("NBOpaque", neighborOpaque);
        nbt.putByte("NBWrap", neighborWrapable);

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

            stream.writeInt(coordonate.x);
            stream.writeInt(coordonate.y);
            stream.writeInt(coordonate.z);

            stream.writeInt(coordonate.dimention); // 1.12 port: dimension as int (was byte)

            stream.writeUTF(getNodeUuid());

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public void sendPacketToClient(ByteArrayOutputStream bos, ServerPlayer player) {
        Utils.sendPacketToClient(bos, player);
    }


    public void sendPacketToAllClient(ByteArrayOutputStream bos) {
        sendPacketToAllClient(bos, 100000);
    }

    public void sendPacketToAllClient(ByteArrayOutputStream bos, double range) {
        // 1.12: every player in the dimension who watches the chunk and is within range
        Level w = coordonate.world();
        if (!(w instanceof ServerLevel)) return;
        for (ServerPlayer player : ((ServerLevel) w).players()) {
            if (coordonate.distanceTo(player) > range) continue;
            if (!((ServerLevel) w).getChunkSource().chunkMap.isChunkTracked(player, coordonate.x >> 4, coordonate.z >> 4)) continue;
            Utils.sendPacketToClient(bos, player);
        }
    }

    public ByteArrayOutputStream getPublishPacket() {

        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(bos);

        try {

            stream.writeByte(Eln.packetNodeSingleSerialized);

            stream.writeInt(coordonate.x);
            stream.writeInt(coordonate.y);
            stream.writeInt(coordonate.z);
            stream.writeInt(coordonate.dimention); // 1.12 port: dimension as int (was byte)

            stream.writeUTF(getNodeUuid());

            publishSerialize(stream);

            return bos;
        } catch (IOException e) {

            e.printStackTrace();

        }
        return null;
    }

    public void publishToAllPlayer() {
        Level w = coordonate.world();
        if (w instanceof ServerLevel) {
            ByteArrayOutputStream bos = getPublishPacket();
            if (bos != null) mods.eln.network.ElnNet.sendToTracking((ServerLevel) w, coordonate.x, coordonate.z, bos);
        }
        if (needNotify) {
            needNotify = false;
            notifyNeighbor();
        }
        needPublish = false;
    }

    public void publishToPlayer(ServerPlayer player) {
        Utils.sendPacketToClient(getPublishPacket(), player);
    }

    public void dropItem(ItemStack itemStack) {
        if (Utils.isEmpty(itemStack)) return;
        Level w = coordonate.world();
        if (w instanceof ServerLevel && ((ServerLevel) w).getGameRules().get(net.minecraft.world.level.gamerules.GameRules.BLOCK_DROPS)) {
            net.minecraft.world.level.block.Block.popResource(w, coordonate.pos(), itemStack);
        }
    }

    public void dropInventory(Container inventory) {
        if (inventory == null) return;
        for (int idx = 0; idx < inventory.getContainerSize(); idx++) {
            dropItem(inventory.getItem(idx));
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
