package mods.eln.node.six;

import mods.eln.Eln;
import mods.eln.debug.DebugType;
import mods.eln.item.IConfigurable;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUCubeMask;
import mods.eln.misc.Utils;
import mods.eln.node.ISixNodeCache;
import mods.eln.node.Node;
import mods.eln.node.NodeConnection;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.Chunk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class SixNode extends Node {

    public static final ArrayList<ISixNodeCache> sixNodeCacheList = new ArrayList<ISixNodeCache>();

    public SixNodeElement sideElementList[] = new SixNodeElement[6];
    public int sideElementIdList[] = new int[6];
    public ArrayList<ElectricalConnection> internalElectricalConnectionList = new ArrayList<ElectricalConnection>(1);
    public ArrayList<ThermalConnection> internalThermalConnectionList = new ArrayList<ThermalConnection>(1);

    public Block sixNodeCacheBlock = Blocks.air;
    public byte sixNodeCacheBlockMeta = 0;
    //public int sixNodeCacheMapId = -1;

    public LRDUCubeMask lrduElementMask = new LRDUCubeMask();

    public SixNodeElement getElement(Direction side) {
        return sideElementList[side.getInt()];
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
        for (int idx = 0; idx < 6; idx++) {
            sideElementList[idx] = null;
            sideElementIdList[idx] = 0;
        }
        lrduElementMask.clear();
    }

    public boolean createSubBlock(ItemStack itemStack, Direction direction, EntityPlayer player) {

        SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(itemStack);
        if (sideElementList[direction.getInt()] != null)
            return false;
        try {
            //Object bool = descriptor.ElementClass.getMethod("canBePlacedOnSide",Direction.class,SixNodeDescriptor.class).invoke(null, direction,descriptor);
            //if((Boolean)bool == false) return false;
            sideElementIdList[direction.getInt()] = itemStack.getItemDamage(); //Je sais c'est moche !
            sideElementList[direction.getInt()] = (SixNodeElement) descriptor.ElementClass.getConstructor(SixNode.class, Direction.class, SixNodeDescriptor.class).newInstance(this, direction, descriptor);
            sideElementIdList[direction.getInt()] = 0;

            disconnect();
            sideElementList[direction.getInt()].front = descriptor.getFrontFromPlace(direction, player);
            sideElementList[direction.getInt()].initialize();
            sideElementIdList[direction.getInt()] = itemStack.getItemDamage();

            connect();

            Eln.dp.println(DebugType.SIX_NODE, "createSubBlock " + sideElementIdList[direction.getInt()] + " " + direction);

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
	    	
	        if (coordonate.world().getGameRules().getGameRuleBooleanValue("doTileDrops"))
	        {
	            float var6 = 0.7F;
	            double var7 = (double)(coordonate.world().rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
	            double var9 = (double)(coordonate.world().rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
	            double var11 = (double)(coordonate.world().rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
	            EntityItem var13 = new EntityItem(coordonate.world(), (double)coordonate.x + var7, (double)coordonate.y + var9, (double)coordonate.z + var11, itemStack);
	            var13.delayBeforeCanPickup = 10;
	            coordonate.world().spawnEntityInWorld(var13);
	        }
	    }*/


    public boolean playerAskToBreakSubBlock(EntityPlayerMP entityPlayer, Direction direction) {

        if (sideElementList[direction.getInt()] == null)
            return deleteSubBlock(entityPlayer, direction);

        if (sideElementList[direction.getInt()].playerAskToBreak()) {
            return deleteSubBlock(entityPlayer, direction);
        } else {
            return false;
        }

    }

    public boolean deleteSubBlock(EntityPlayerMP entityPlayer, Direction direction) {

        if (sideElementList[direction.getInt()] == null)
            return false;

        Eln.dp.println(DebugType.SIX_NODE, "deleteSubBlock " + " " + direction);

        disconnect();
        SixNodeElement e = sideElementList[direction.getInt()];
        sideElementList[direction.getInt()] = null;
        sideElementIdList[direction.getInt()] = 0;
        e.destroy(entityPlayer);

        connect();

        recalculateLightValue();
        setNeedPublish(true);
        return true;
    }

    public boolean getIfSideRemain() {
        for (SixNodeElement sideElement : sideElementList) {
            if (sideElement != null)
                return true;
        }
        return false;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt.getCompoundTag("node"));

        sixNodeCacheBlock = Block.getBlockById(nbt.getInteger("cacheBlockId"));
        sixNodeCacheBlockMeta = nbt.getByte("cacheBlockMeta");
        int idx;
        for (idx = 0; idx < 6; idx++) {

            short sideElementId = nbt.getShort("EID" + idx);
            if (sideElementId == 0) {
                sideElementList[idx] = null;
                sideElementIdList[idx] = 0;
            } else {
                try {
                    SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(sideElementId);
                    sideElementIdList[idx] = sideElementId;
                    sideElementList[idx] = (SixNodeElement) descriptor.ElementClass.getConstructor(SixNode.class, Direction.class, SixNodeDescriptor.class).newInstance(this, Direction.fromInt(idx), descriptor);
                    sideElementList[idx].readFromNBT(nbt.getCompoundTag("ED" + idx));
                    sideElementList[idx].initialize();
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
            }
        }
        initializeFromNBT();

    }

    @Override
    public boolean nodeAutoSave() {

        return false;
    }

    public void writeToNBT(NBTTagCompound nbt) {
        int idx = 0;
        nbt.setInteger("cacheBlockId", Block.getIdFromBlock(sixNodeCacheBlock));
        nbt.setByte("cacheBlockMeta", sixNodeCacheBlockMeta);

        for (SixNodeElement sideElement : sideElementList) {

            if (sideElement == null) {
                nbt.setShort("EID" + idx, (short) 0);
            } else {
                nbt.setShort("EID" + idx, (short) sideElementIdList[idx]);
                sideElement.writeToNBT(Utils.newNbtTagCompund(nbt, "ED" + idx));
            }
            idx++;
        }

        NBTTagCompound nodeNbt = new NBTTagCompound();
        super.writeToNBT(nodeNbt);
        nbt.setTag("node", nodeNbt);
    }

    public boolean getSideEnable(Direction direction) {

        return sideElementList[direction.getInt()] != null;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu, int mask) {
        Direction elementSide = side.applyLRDU(lrdu);
        SixNodeElement element = sideElementList[elementSide.getInt()];
        if (element == null)
            return null;
        return element.getElectricalLoad(elementSide.getLRDUGoingTo(side), mask);
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu, int mask) {
        Direction elementSide = side.applyLRDU(lrdu);
        SixNodeElement element = sideElementList[elementSide.getInt()];
        if (element == null)
            return null;
        return element.getThermalLoad(elementSide.getLRDUGoingTo(side), mask);
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
    public boolean readConfigTool(Direction side, NBTTagCompound tag, EntityPlayer invoker) {
        SixNodeElement element = sideElementList[side.getInt()];
        if(element instanceof IConfigurable) {
            ((IConfigurable) element).readConfigTool(tag, invoker);
            return true;
        }
        return false;
    }

    @Override
    public boolean writeConfigTool(Direction side, NBTTagCompound tag, EntityPlayer invoker) {
        SixNodeElement element = sideElementList[side.getInt()];
        if(element instanceof IConfigurable) {
            ((IConfigurable) element).writeConfigTool(tag, invoker);
            return true;
        }
        return false;
    }

    @Override
    public void publishSerialize(DataOutputStream stream) {

        super.publishSerialize(stream);
        try {
            int idx = 0;
            stream.writeInt(Block.getIdFromBlock(sixNodeCacheBlock));
            stream.writeByte(sixNodeCacheBlockMeta);
            for (SixNodeElement sideElement : sideElementList) {
                if (sideElement == null) {
                    stream.writeShort((byte) 0);
                } else {
                    stream.writeShort((short) sideElementIdList[idx]);
                    sideElement.networkSerialize(stream);
                }
                idx++;
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public void preparePacketForClient(DataOutputStream stream, SixNodeElement e) {
        try {
            super.preparePacketForClient(stream);
            int side = e.side.getInt();
            stream.writeByte(side);
            stream.writeShort(e.sixNodeElementDescriptor.parentItemDamage);
        } catch (IOException ex) {

            ex.printStackTrace();
        }
    }

    @Override
    public void initializeFromThat(Direction front, EntityLivingBase entityLiving,
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

    }

    @Override
    public void connectJob() {

        super.connectJob();
        for (SixNodeElement element : sideElementList) {
            if (element != null) {
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
            if (element != null) {
                element.disconnectJob();
            }
        }

        Eln.simulator.removeAllElectricalConnection(internalElectricalConnectionList);
        Eln.simulator.removeAllThermalConnection(internalThermalConnectionList);
    }

    public void tryConnectTwoInternalElement(Direction side, SixNodeElement element, LRDU lrdu, Direction otherSide, SixNodeElement otherElement, LRDU otherLRDU) {
        Eln.dp.println(DebugType.SIX_NODE, "SixNode.tCTIE:");
        int mskThis = element.getConnectionMask(lrdu);
        int mskOther = otherElement.getConnectionMask(otherLRDU);
        if (compareConnectionMask(mskThis, mskOther)) {
            Eln.dp.println(DebugType.SIX_NODE, "\tConnection OK.");
            lrduElementMask.set(side, lrdu, true);
            lrduElementMask.set(otherSide, otherLRDU, true);
            NodeConnection nodeConnection = new NodeConnection(this, side, lrdu, this, otherSide, otherLRDU);
            nodeConnectionList.add(nodeConnection);
            element.newConnectionAt(nodeConnection, false);
            otherElement.newConnectionAt(nodeConnection, true);
            ElectricalLoad eLoad;
            if ((eLoad = element.getElectricalLoad(lrdu, mskOther)) != null) {
                ElectricalLoad otherELoad = otherElement.getElectricalLoad(otherLRDU, mskThis);
                if (otherELoad != null) {
                    ElectricalConnection eCon;
                    eCon = new ElectricalConnection(eLoad, otherELoad);

                    Eln.simulator.addElectricalComponent(eCon);

                    internalElectricalConnectionList.add(eCon);
                    nodeConnection.addConnection(eCon);
                }
            }
            ThermalLoad tLoad;
            if ((tLoad = this.getThermalLoad(side, lrdu, mskOther)) != null) {

                ThermalLoad otherTLoad = element.getThermalLoad(otherLRDU, mskThis);
                if (otherTLoad != null) {
                    ThermalConnection tCon;
                    tCon = new ThermalConnection(tLoad, otherTLoad);

                    Eln.simulator.addThermalConnection(tCon);

                    internalThermalConnectionList.add(tCon);
                    nodeConnection.addConnection(tCon);
                }

            }
        }
    }

    public void newConnectionAt(NodeConnection connection, boolean isA) {
        Direction side = isA? connection.dir1 : connection.dir2;
        LRDU lrdu = isA? connection.lrdu1 : connection.lrdu2;
        Direction elementSide = side.applyLRDU(lrdu);
        SixNodeElement element = sideElementList[elementSide.getInt()];
        if (element == null) {
            Eln.dp.println(DebugType.SIX_NODE, "sixnode newConnectionAt error");
            while (true)
                ;
        }
        lrduElementMask.set(elementSide, elementSide.getLRDUGoingTo(side), true);
        element.newConnectionAt(connection, isA);

    }

    public void externalDisconnect(Direction side, LRDU lrdu) {
        Direction elementSide = side.applyLRDU(lrdu);
        SixNodeElement element = sideElementList[elementSide.getInt()];
        if (element == null) {
            Eln.dp.println(DebugType.SIX_NODE, "sixnode newConnectionAt error");
            while (true)
                ;
        }
        lrduElementMask.set(elementSide, elementSide.getLRDUGoingTo(side), false);
    }

    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        if (sixNodeCacheBlock != Blocks.air) {
            return false;
        } else {

            ItemStack stack = entityPlayer.getCurrentEquippedItem();

            Block b = Blocks.air;
            if (stack != null)
                b = Block.getBlockFromItem(stack.getItem());

            boolean accepted = false;

            if (Eln.playerManager.get(entityPlayer).getInteractEnable() && stack != null) {
                for (ISixNodeCache a : sixNodeCacheList) {
                    if (a.accept(stack)) {
                        accepted = true;
                        sixNodeCacheBlock = b;
                        sixNodeCacheBlockMeta = (byte) a.getMeta(stack);
                        break;
                    }
                }
            }
			

			/*if(entityPlayer.isSneaking() == false){
				accepted = false;
			}*/

            if (accepted) {
                Eln.dp.println(DebugType.SIX_NODE, "ACACAC");


                setNeedPublish(true);
                if (Utils.isCreative((EntityPlayerMP) entityPlayer) == false)
                    entityPlayer.inventory.decrStackSize(entityPlayer.inventory.currentItem, 1);

                //if(sixNodeCacheMapId != sixNodeCacheMapIdOld)
                {
                    Chunk chunk = coordonate.world().getChunkFromBlockCoords(coordonate.x, coordonate.z);
                    Utils.generateHeightMap(chunk);
                    Utils.updateSkylight(chunk);
                    chunk.generateSkylightMap();
                    Utils.updateAllLightTypes(coordonate.world(), coordonate.x, coordonate.y, coordonate.z);
                }
                return true;
            } else {
                SixNodeElement element = sideElementList[side.getInt()];
                if (element == null)
                    return false;
                if (element.onBlockActivated(entityPlayer, side, vx, vy, vz))
                    return true;
                return super.onBlockActivated(entityPlayer, side, vx, vy, vz);
            }
        }
    }

    @Override
    public boolean hasGui(Direction side) {
        if (sideElementList[side.getInt()] == null)
            return false;
        return sideElementList[side.getInt()].hasGui();
    }

    public IInventory getInventory(Direction side) {
        if (sideElementList[side.getInt()] == null)
            return null;
        return sideElementList[side.getInt()].getInventory();
    }

    public Container newContainer(Direction side, EntityPlayer player) {
        if (sideElementList[side.getInt()] == null)
            return null;
        return sideElementList[side.getInt()].newContainer(side, player);
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
    public void networkUnserialize(DataInputStream stream, EntityPlayerMP player) {
        super.networkUnserialize(stream, player);

        Direction side;
        try {
            side = Direction.fromInt(stream.readByte());
            if (side != null & sideElementIdList[side.getInt()] == stream.readShort()) {
                sideElementList[side.getInt()].networkUnserialize(stream, player);
            } else {
                Eln.dp.println(DebugType.SIX_NODE, "sixnode unserialize miss");
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

        return Eln.sixNodeBlock.getNodeUuid();
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
