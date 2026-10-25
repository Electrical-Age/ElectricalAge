package mods.eln.node.six;

import net.minecraft.world.entity.item.ItemEntity;


import mods.eln.sim.process.destruct.Repair;
import mods.eln.compat.WorldCompat;
import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUCubeMask;
import mods.eln.misc.Utils;
import mods.eln.node.ISixNodeCache;
import mods.eln.node.Node;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class SixNode extends Node {

    public static final ArrayList<ISixNodeCache> sixNodeCacheList = new ArrayList<ISixNodeCache>();

    /** Slots: 0..5 the six faces (a device centred on face i), then SLOTS - 6 more for devices a voxel block holds off
     *  centre (SixNodeElement.slot). */
    public static final int SLOTS = 6 + 32;

    public SixNodeElement sideElementList[] = new SixNodeElement[SLOTS];
    public int sideElementIdList[] = new int[SLOTS];
    public ArrayList<ElectricalConnection> internalElectricalConnectionList = new ArrayList<ElectricalConnection>(1);
    public ArrayList<ThermalConnection> internalThermalConnectionList = new ArrayList<ThermalConnection>(1);

    public Block sixNodeCacheBlock = Blocks.AIR;
    public byte sixNodeCacheBlockMeta = 0;
    //public int sixNodeCacheMapId = -1;

    public LRDUCubeMask lrduElementMask = new LRDUCubeMask();

    /**
     * Sides whose saved device could not be built or loaded (a device this version lacks, or a load that threw): the
     * raw {EID, ED} pair, written back unchanged on save so a version that can load it again still finds it. The side
     * stays taken: a device placed on it would silently replace the saved one, so placement is refused. Breaking the
     * whole block discards them.
     */
    private final CompoundTag[] keptSides = new CompoundTag[SLOTS];

    /** Is this side holding a device that could not be loaded (kept as saved data, see keptSides)? */
    public boolean isSideKept(int side) {
        return keptSides[side] != null;
    }

    public SixNodeElement getElement(Direction side) {
        return sideElementList[side.getInt()];
    }

    /** The element in a slot (0..5: centred on that face), or null. */
    public SixNodeElement getElement(int slot) {
        return slot >= 0 && slot < SLOTS ? sideElementList[slot] : null;
    }

    @Override
    public boolean canConnectRedstone() {
        for (SixNodeElement element : sideElementList) {
            if (element != null && element.canConnectRedstone()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int isProvidingWeakPower(Direction side) {
        int value = 0;
        for (SixNodeElement element : sideElementList) {
            if (element != null) {
                int eValue = element.isProvidingWeakPower();
                if (eValue > value)
                    value = eValue;
            }
        }
        return value;
    }

    public SixNode() {
        lrduElementMask.clear();
    }

    public boolean createSubBlock(ItemStack itemStack, Direction direction, Player player) {
        return createSubBlockAt(itemStack, direction.getInt(), direction, 0, 0, 0, player);
    }

    /** A device into a slot: on face direction, its centre (ox, oy, oz) world voxels from the face's centre (0 for the
     *  six face slots). */
    public boolean createSubBlockAt(ItemStack itemStack, int slot, Direction direction, int ox, int oy, int oz, Player player) {
        if (slot < 0 || slot >= SLOTS || slot < 6 && slot != direction.getInt()) return false;
        if (keptSides[slot] != null) {
            if (player != null) Utils.sendMessage(player, "This side holds a device this version of Electrical Age cannot load; its data is kept. Break the block to discard it.");
            return false;
        }
        SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(itemStack);
        if (sideElementList[slot] != null)
            return false;
        try {
            SixNodeElement e = (SixNodeElement) descriptor.ElementClass.getConstructor(SixNode.class, Direction.class, SixNodeDescriptor.class).newInstance(this, direction, descriptor);
            e.slot = slot;
            e.ox = ox;
            e.oy = oy;
            e.oz = oz;
            sideElementList[slot] = e;

            disconnect();
            e.front = descriptor.getFrontFromPlace(direction, player);
            e.initialize();
            sideElementIdList[slot] = descriptor.parentItemDamage;

            connect();

            Utils.println("createSubBlock " + sideElementIdList[slot] + " " + direction + " slot " + slot);

            setNeedPublish(true);
            return true;
        } catch (InstantiationException e) {

            e.printStackTrace();
        } catch (IllegalAccessException e) {

            e.printStackTrace();
        } catch (IllegalArgumentException e) {

            e.printStackTrace();
        } catch (InvocationTargetException e) {

            e.printStackTrace();
        } catch (NoSuchMethodException e) {

            e.printStackTrace();
        } catch (SecurityException e) {

            e.printStackTrace();
        }
        return false;
    }

	/*
        protected void dropItem(ItemStack itemStack)
	    {
	    	
	        if (coordonate.world().getGameRules().getBooleanOr("doTileDrops", false))
	        {
	            float var6 = 0.7F;
	            double var7 = (double)(coordonate.world().rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
	            double var9 = (double)(coordonate.world().rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
	            double var11 = (double)(coordonate.world().rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
	            ItemEntity var13 = new ItemEntity(coordonate.world(), (double)coordonate.x + var7, (double)coordonate.y + var9, (double)coordonate.z + var11, itemStack);
	            var13.setPickupDelay(10);
	            coordonate.world().spawnEntity(var13);
	        }
	    }*/


    public boolean playerAskToBreakSubBlock(ServerPlayer entityPlayer, Direction direction) {
        return playerAskToBreakSlot(entityPlayer, direction.getInt());
    }

    public boolean playerAskToBreakSlot(ServerPlayer entityPlayer, int slot) {
        SixNodeElement e = getElement(slot);
        if (e != null && !e.playerAskToBreak()) return false;
        return deleteSlot(entityPlayer, slot);
    }

    /** The block went another way (an explosion, a command, a piston): every remaining element drops (1.12 breakBlock). */
    @Override
    public void onBreakBlock() {
        if (isAdded()) disconnect();   // first, while the elements are still there
        for (int i = 0; i < SLOTS; i++) {
            if (keptSides[i] == null) continue;
            Eln.LOG.warn("Electrical Age: the block at {} was broken; discarding the saved six-node device {} on side {} that this version could not load",
                coordonate, keptSides[i].getShortOr("EID", (short) 0), Direction.fromInt(i));
            keptSides[i] = null;
        }
        for (int i = 0; i < SLOTS; i++) {
            SixNodeElement e = sideElementList[i];
            if (e == null) continue;
            sideElementList[i] = null;
            sideElementIdList[i] = 0;
            try {
                e.destroy(null);
            } catch (RuntimeException ex) {
                Utils.println("six node element destroy failed: " + ex);
            }
        }
        super.onBreakBlock();
    }

    @Override
    public void onNeighborBlockChange() {
        super.onNeighborBlockChange();
        dropUnsupported();
    }

    /**
     * A device whose wall went (its face toward this block isn't full any more: SixNodeBlock.getIfOtherBlockIsSolid,
     * the placement rule) drops with its contents (Baughn: six nodes need support too). A plain six-node block with
     * nothing left goes; a voxel block decides for itself (afterSubBlockDeleted).
     */
    protected void dropUnsupported() {
        var w = coordonate.world();
        if (w == null || isDestructing()) return;
        var pos = coordonate.pos();
        boolean dropped = false;
        for (int i = 0; i < SLOTS; i++) {
            SixNodeElement e = sideElementList[i];
            if (e != null && !SixNodeBlock.get().getIfOtherBlockIsSolid(w, pos, e.side)) {
                deleteSlot(null, i);
                dropped = true;
            }
        }
        if (dropped && !(this instanceof mods.eln.node.voxel.VoxelNode) && !getIfSideRemain() && !isDestructing())
            w.destroyBlock(pos, false);
    }

    public boolean deleteSubBlock(ServerPlayer entityPlayer, Direction direction) {
        return deleteSlot(entityPlayer, direction.getInt());
    }

    public boolean deleteSlot(ServerPlayer entityPlayer, int slot) {
        if (getElement(slot) == null)
            return false;

        Utils.println("deleteSubBlock slot " + slot);
        try {
            return deleteSubBlockImpl(entityPlayer, slot);
        } finally {
            afterSubBlockDeleted();
        }
    }

    /** After an element went (a voxel node removes its block once nothing at all is left). */
    protected void afterSubBlockDeleted() {
    }

    private boolean deleteSubBlockImpl(ServerPlayer entityPlayer, int slot) {

        disconnect();
        SixNodeElement e = sideElementList[slot];
        sideElementList[slot] = null;
        sideElementIdList[slot] = 0;
        e.destroy(entityPlayer);

        connect();

        recalculateLightValue();
        setNeedPublish(true);
        return true;
    }

    /** Is any side taken? Kept sides count, so the block stays when the last device that loaded goes. */
    public boolean getIfSideRemain() {
        for (int i = 0; i < SLOTS; i++) if (sideElementList[i] != null || keptSides[i] != null) return true;
        return false;
    }

    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt.getCompoundOrEmpty("node"));

        // six-node cover blocks ("cache"): not ported yet (P2); 1.12 saved a numeric block id
        sixNodeCacheBlock = Blocks.AIR;
        sixNodeCacheBlockMeta = nbt.getByteOr("cacheBlockMeta", (byte) 0);
        int idx;
        for (idx = 0; idx < SLOTS; idx++) {

            short sideElementId = nbt.getShortOr("EID" + idx, (short) 0);
            // (slots past the six faces: their face and offset, "EP<slot>" = {face, ox, oy, oz})
            int[] at = idx < 6 ? new int[]{idx, 0, 0, 0} : nbt.getIntArray("EP" + idx).orElse(new int[0]);
            if (at.length != 4 || Direction.fromInt(at[0]) == null) at = null;
            if (sideElementId != 0 && at == null) {
                Eln.LOG.warn("Electrical Age: six-node slot {} at {} has no valid place; dropped from the save", idx, coordonate);
                sideElementId = 0;
            }
            keptSides[idx] = null;
            if (sideElementId == 0) {
                sideElementList[idx] = null;
                sideElementIdList[idx] = 0;
            } else {
                try {
                    SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(sideElementId);
                    if (descriptor == null) throw new IllegalStateException("no six-node device " + sideElementId + " in this version");
                    sideElementIdList[idx] = sideElementId;
                    SixNodeElement e = (SixNodeElement) descriptor.ElementClass.getConstructor(SixNode.class, Direction.class, SixNodeDescriptor.class).newInstance(this, Direction.fromInt(at[0]), descriptor);
                    e.slot = idx;
                    e.ox = at[1];
                    e.oy = at[2];
                    e.oz = at[3];
                    sideElementList[idx] = e;
                    e.readFromNBT(nbt.getCompoundOrEmpty("ED" + idx));
                    sideElementList[idx].initialize();
                } catch (Exception e) {
                    // (a half-built element must not stay: it would run, save and drop as if it had loaded)
                    sideElementList[idx] = null;
                    sideElementIdList[idx] = 0;
                    CompoundTag kept = new CompoundTag();
                    kept.putShort("EID", sideElementId);
                    kept.put("ED", nbt.getCompoundOrEmpty("ED" + idx).copy());
                    if (idx >= 6) kept.putIntArray("EP", at);
                    keptSides[idx] = kept;
                    Eln.LOG.warn("Electrical Age: the six-node device {} on side {} at {} cannot be loaded; keeping its saved data, "
                        + "and the side stays taken", sideElementId, Direction.fromInt(idx), coordonate, e);
                }
            }
        }
        // (no connect here: NodeManager and the other loaders call initializeFromNBT once the node is added; a connect
        // here too ran before addNode, and left the node's loads in the simulator if anything after it threw)
    }

    @Override
    public boolean nodeAutoSave() {

        return false;
    }

    public void writeToNBT(CompoundTag nbt) {
        int idx = 0;
        nbt.putInt("cacheBlockId", 0);
        nbt.putByte("cacheBlockMeta", sixNodeCacheBlockMeta);

        for (SixNodeElement sideElement : sideElementList) {

            if (sideElement == null && keptSides[idx] != null) {
                nbt.putShort("EID" + idx, keptSides[idx].getShortOr("EID", (short) 0));
                nbt.put("ED" + idx, keptSides[idx].getCompoundOrEmpty("ED").copy());
                if (idx >= 6) nbt.putIntArray("EP" + idx, keptSides[idx].getIntArray("EP").orElse(new int[0]));
            } else if (sideElement == null) {
                if (idx < 6) nbt.putShort("EID" + idx, (short) 0);   // (empty slots past the faces: no keys)
            } else {
                nbt.putShort("EID" + idx, (short) sideElementIdList[idx]);
                sideElement.writeToNBT(Utils.newNbtTagCompund(nbt, "ED" + idx));
                if (idx >= 6) nbt.putIntArray("EP" + idx, new int[]{sideElement.side.getInt(), sideElement.ox, sideElement.oy, sideElement.oz});
            }
            idx++;
        }

        CompoundTag nodeNbt = new CompoundTag();
        super.writeToNBT(nodeNbt);
        nbt.put("node", nodeNbt);
    }

    /** Is the side taken (a device, or one kept as saved data)? */
    public boolean getSideEnable(Direction direction) {
        return sideElementList[direction.getInt()] != null || keptSides[direction.getInt()] != null;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        Direction elementSide = side.applyLRDU(lrdu);
        SixNodeElement element = sideElementList[elementSide.getInt()];
        if (element == null || element.isBurnt())
            return null;
        return element.getElectricalLoad(elementSide.getLRDUGoingTo(side));
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        Direction elementSide = side.applyLRDU(lrdu);
        SixNodeElement element = sideElementList[elementSide.getInt()];
        if (element == null || element.isBurnt())
            return null;
        return element.getThermalLoad(elementSide.getLRDUGoingTo(side));
    }

    @Override
    public int getSideConnectionMask(Direction side, LRDU lrdu) {
        Direction elementSide = side.applyLRDU(lrdu);
        SixNodeElement element = sideElementList[elementSide.getInt()];
        if (element == null)
            return 0;
        return element.getConnectionMask(elementSide.getLRDUGoingTo(side));
    }

    @Override
    public String multiMeterString(Direction side) {
        SixNodeElement element = sideElementList[side.getInt()];
        if (element == null)
            return "";
        return element.multiMeterString();
    }

    @Override
    public String thermoMeterString(Direction side) {
        SixNodeElement element = sideElementList[side.getInt()];
        if (element == null)
            return "";
        return element.thermoMeterString();
    }

    @Override
    public void publishSerialize(DataOutputStream stream) {

        super.publishSerialize(stream);
        try {
            publishBeforeElements(stream);
            int idx = 0;
            stream.writeInt(0); // cover block: not ported yet
            stream.writeByte(sixNodeCacheBlockMeta);
            for (idx = 0; idx < 6; idx++) {
                SixNodeElement sideElement = sideElementList[idx];
                if (sideElement == null) {
                    stream.writeShort((byte) 0);
                } else {
                    stream.writeShort((short) sideElementIdList[idx]);
                    sideElement.networkSerialize(stream);
                }
            }
            // the slots past the faces: count, then each slot, face, offset, id and its payload (length-prefixed, so a
            // client without the device's render skips it)
            int n = 0;
            for (idx = 6; idx < SLOTS; idx++) if (sideElementList[idx] != null) n++;
            stream.writeByte(n);
            for (idx = 6; idx < SLOTS; idx++) {
                SixNodeElement e = sideElementList[idx];
                if (e == null) continue;
                stream.writeByte(idx);
                stream.writeByte(e.side.getInt());
                stream.writeByte(e.ox);
                stream.writeByte(e.oy);
                stream.writeByte(e.oz);
                int pins = 0;   // (the edges it connects on: its pins)
                for (LRDU l : LRDU.values()) if (e.getConnectionMask(l) != 0) pins |= 1 << l.toInt();
                stream.writeByte(pins);
                stream.writeShort((short) sideElementIdList[idx]);
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                e.networkSerialize(new DataOutputStream(bos));
                stream.writeInt(bos.size());
                bos.writeTo(stream);
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    /** For a voxel node: its parts, written before the elements (a client without an element's render stops reading at
     *  that element). */
    protected void publishBeforeElements(DataOutputStream stream) throws IOException {
    }

    public void preparePacketForClient(DataOutputStream stream, SixNodeElement e) {
        try {
            super.preparePacketForClient(stream);
            stream.writeByte(e.slot);
            stream.writeShort(e.sixNodeElementDescriptor.parentItemDamage);
        } catch (IOException ex) {

            ex.printStackTrace();
        }
    }

    @Override
    public void initializeFromThat(Direction front, LivingEntity entityLiving,
                                   ItemStack itemStack) {
        neighborBlockRead();


    }

    @Override
    public void initializeFromNBT() {

        connect();
    }

    @Override
    public void connectInit() {
        super.connectInit();
        internalElectricalConnectionList.clear();
        internalThermalConnectionList.clear();

        lrduElementMask.clear();
        for (SixNodeElement e : sideElementList) if (e != null) e.slotMask.set(0);

    }

    @Override
    public void connectJob() {
        super.connectJob();
        connectElements();
    }

    /** The elements' own connect jobs and their links inside the block (a voxel node hosting elements calls this). */
    protected void connectElements() {
        // Burnt elements stay out of the simulation (see SixNodeElement.burn).
        for (SixNodeElement element : sideElementList) {
            if (element != null && !element.isBurnt()) {
                element.connectJob();
            }
        }

        //INTERNAL
        {
            Direction side = Direction.YN;
            SixNodeElement element = sideElementList[side.getInt()];
            if (element != null) {
                for (LRDU lrdu : LRDU.values()) {
                    Direction otherSide = side.applyLRDU(lrdu);
                    SixNodeElement otherElement = sideElementList[otherSide.getInt()];
                    if (otherElement != null) {
                        LRDU otherLRDU = otherSide.getLRDUGoingTo(side);
                        tryConnectTwoInternalElement(side, element, lrdu, otherSide, otherElement, otherLRDU);
                    }
                }
            }
        }
        {
            Direction side = Direction.YP;
            SixNodeElement element = sideElementList[side.getInt()];
            if (element != null) {
                for (LRDU lrdu : LRDU.values()) {
                    Direction otherSide = side.applyLRDU(lrdu);
                    SixNodeElement otherElement = sideElementList[otherSide.getInt()];
                    if (otherElement != null) {
                        LRDU otherLRDU = otherSide.getLRDUGoingTo(side);
                        tryConnectTwoInternalElement(side, element, lrdu, otherSide, otherElement, otherLRDU);
                    }
                }
            }
        }

        {
            Direction side = Direction.XN;
            for (int idx = 0; idx < 4; idx++) {
                Direction otherSide = side.right();
                SixNodeElement element = sideElementList[side.getInt()];
                SixNodeElement otherElement = sideElementList[otherSide.getInt()];
                if (element != null && otherElement != null) {
                    tryConnectTwoInternalElement(side, element, LRDU.Right, otherSide, otherElement, LRDU.Left);
                }

                side = otherSide;
            }
        }

    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        for (SixNodeElement element : sideElementList) {
            if (element != null && !element.isBurnt()) {
                element.disconnectJob();
            }
        }

        Eln.simulator.removeAllElectricalConnection(internalElectricalConnectionList);
        Eln.simulator.removeAllThermalConnection(internalThermalConnectionList);
    }

    @Override
    protected void visualConnectInit() {
        lrduElementMask.clear();
        for (SixNodeElement e : sideElementList) if (e != null) e.slotMask.set(0);
    }

    /** The mask half of connectJob's INTERNAL part (no connections). */
    @Override
    protected void visualConnectInternal() {
        for (Direction side : new Direction[]{Direction.YN, Direction.YP}) {
            SixNodeElement element = sideElementList[side.getInt()];
            if (element == null) continue;
            for (LRDU lrdu : LRDU.values()) {
                Direction otherSide = side.applyLRDU(lrdu);
                SixNodeElement otherElement = sideElementList[otherSide.getInt()];
                if (otherElement != null) visualInternal(side, element, lrdu, otherSide, otherElement, otherSide.getLRDUGoingTo(side));
            }
        }
        Direction side = Direction.XN;
        for (int idx = 0; idx < 4; idx++) {
            Direction otherSide = side.right();
            SixNodeElement element = sideElementList[side.getInt()];
            SixNodeElement otherElement = sideElementList[otherSide.getInt()];
            if (element != null && otherElement != null) visualInternal(side, element, LRDU.Right, otherSide, otherElement, LRDU.Left);
            side = otherSide;
        }
    }

    private void visualInternal(Direction side, SixNodeElement element, LRDU lrdu, Direction otherSide, SixNodeElement otherElement, LRDU otherLRDU) {
        if (compareConnectionMask(element.getConnectionMask(lrdu), otherElement.getConnectionMask(otherLRDU))) {
            lrduElementMask.set(side, lrdu, true);
            lrduElementMask.set(otherSide, otherLRDU, true);
        }
    }

    @Override
    protected int visualHash() {
        int h = super.visualHash();
        for (Direction d : Direction.values()) h = h * 31 + lrduElementMask.get(d).mask;
        for (int i = 6; i < SLOTS; i++) if (sideElementList[i] != null) h = h * 31 + sideElementList[i].slotMask.mask + 16 * i;
        return h;
    }

    public void tryConnectTwoInternalElement(Direction side, SixNodeElement element, LRDU lrdu, Direction otherSide, SixNodeElement otherElement, LRDU otherLRDU) {
        if (compareConnectionMask(element.getConnectionMask(lrdu), otherElement.getConnectionMask(otherLRDU))) {
            lrduElementMask.set(side, lrdu, true);
            lrduElementMask.set(otherSide, otherLRDU, true);
            // A burnt element keeps its (visual) connection but carries nothing.
            if (element.isBurnt() || otherElement.isBurnt()) return;
            ElectricalLoad eLoad;
            if ((eLoad = element.getElectricalLoad(lrdu)) != null) {
                ElectricalLoad otherELoad = otherElement.getElectricalLoad(otherLRDU);
                if (otherELoad != null) {
                    ElectricalConnection eCon;
                    eCon = new ElectricalConnection(eLoad, otherELoad);

                    Eln.simulator.addElectricalComponent(eCon);

                    internalElectricalConnectionList.add(eCon);
                }
            }
            ThermalLoad tLoad;
            if ((tLoad = this.getThermalLoad(side, lrdu)) != null) {

                ThermalLoad otherTLoad = element.getThermalLoad(otherLRDU);
                if (otherTLoad != null) {
                    ThermalConnection tCon;
                    tCon = new ThermalConnection(tLoad, otherTLoad);

                    Eln.simulator.addThermalConnection(tCon);

                    internalThermalConnectionList.add(tCon);
                }

            }
        }
    }

    public void newConnectionAt(Direction side, LRDU lrdu) {
        Direction elementSide = side.applyLRDU(lrdu);
        SixNodeElement element = sideElementList[elementSide.getInt()];
        if (element == null) {
            // (1.12 hung the server here on purpose: incognito crashed 2026-10-17 after a battery blow-up)
            Utils.println("sixnode connection to an empty side " + elementSide + " at " + coordonate + ": ignored");
            return;
        }
        lrduElementMask.set(elementSide, elementSide.getLRDUGoingTo(side), true);

    }

    public void externalDisconnect(Direction side, LRDU lrdu) {
        Direction elementSide = side.applyLRDU(lrdu);
        SixNodeElement element = sideElementList[elementSide.getInt()];
        if (element == null) {
            // (1.12 hung the server here on purpose: incognito crashed 2026-10-17 after a battery blow-up)
            Utils.println("sixnode connection to an empty side " + elementSide + " at " + coordonate + ": ignored");
            return;
        }
        lrduElementMask.set(elementSide, elementSide.getLRDUGoingTo(side), false);
    }

    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        if (sixNodeCacheBlock != Blocks.AIR) {
            return false;
        } else {

            ItemStack stack = entityPlayer.getMainHandItem();

            // 1.12: with the interact key, a block in hand became the node's cover ("cache"); not ported yet (P2)
            boolean accepted = false;
			

			/*if(entityPlayer.isSneaking() == false){
				accepted = false;
			}*/

            if (accepted) {
                return true;
            } else {
                return onSlotActivated(entityPlayer, side.getInt(), vx, vy, vz);
            }
        }
    }

    /** A right-click on the element in a slot: repair, its own use, a meter, its GUI. */
    public boolean onSlotActivated(Player entityPlayer, int slot, float vx, float vy, float vz) {
        SixNodeElement element = getElement(slot);
        if (element == null)
            return false;
        if (element.isBurnt() && Repair.onActivated(element, entityPlayer))
            return true;
        if (element.onBlockActivated(entityPlayer, element.side, vx, vy, vz))
            return true;
        if (meterUseSlot(entityPlayer, slot)) return true;
        if (element.hasGui()) return mods.eln.node.NodeMenus.open(this, entityPlayer, slot);
        return false;
    }

    /** A meter in hand on the element in a slot (the faces' slots: NodeBase.meterUse by side). */
    protected boolean meterUseSlot(Player entityPlayer, int slot) {
        if (slot < 6) return meterUse(entityPlayer, Direction.fromInt(slot));
        SixNodeElement e = getElement(slot);
        return e != null && meterUse(entityPlayer, e::multiMeterString, e::thermoMeterString);
    }

    @Override
    public boolean hasGui(Direction side) {
        if (sideElementList[side.getInt()] == null)
            return false;
        return sideElementList[side.getInt()].hasGui();
    }

    public Container getInventory(Direction side) {
        if (sideElementList[side.getInt()] == null)
            return null;
        return sideElementList[side.getInt()].getInventory();
    }

    public boolean hasGui(int slot) {
        SixNodeElement e = getElement(slot);
        return e != null && e.hasGui();
    }

    public float physicalSelfDestructionExplosionStrength() {
        return 1.0f;
    }

    public void recalculateLightValue() {
        int light = 0;
        for (SixNodeElement element : sideElementList) {
            if (element == null)
                continue;
            int eLight = element.getLightValue();
            if (eLight > light)
                light = eLight;
        }
        setLightValue(light);
    }

    @Override
    public void networkUnserialize(DataInputStream stream, ServerPlayer player) {
        super.networkUnserialize(stream, player);

        // (from a client: a side byte out of range, an empty side or another device's id are ignored)
        try {
            int slot = stream.readByte();
            short id = stream.readShort();
            SixNodeElement element = getElement(slot);
            if (element != null && sideElementIdList[slot] == id) {
                element.networkUnserialize(stream, player);
            } else {
                Utils.println("sixnode unserialize miss");
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public boolean hasVolume() {

        for (SixNodeElement element : sideElementList) {
            if (element != null && element.sixNodeElementDescriptor.hasVolume())
                return true;
        }
        return false;
    }


    @Override
    public String getNodeUuid() {

        return "s";
    }

    @Override
    public void globalBoot() {
        super.globalBoot();
        for (SixNodeElement e : sideElementList) {
            if (e == null) continue;
            e.globalBoot();
        }
    }


    @Override
    public void unload() {
        super.unload();
        for (SixNodeElement e : sideElementList) {
            if (e == null) continue;
            e.unload();
        }
    }
}
