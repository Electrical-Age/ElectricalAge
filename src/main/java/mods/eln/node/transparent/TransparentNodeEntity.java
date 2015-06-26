package mods.eln.node.transparent;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.FakeSideInventory;
import mods.eln.misc.LRDU;
import mods.eln.node.Node;
import mods.eln.node.NodeBlockEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class TransparentNodeEntity extends NodeBlockEntity implements ISidedInventory { // boolean[] syncronizedSideEnable = new boolean[6];
    TransparentNodeElementRender elementRender = null;
    short elementRenderId;


    @Override
    public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
        if (elementRender == null) return null;
        return elementRender.getCableRender(side, lrdu);
    }

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {
        super.serverPublishUnserialize(stream);

        try {
            Short id = stream.readShort();
            if (id == 0) {
                elementRenderId = (byte) 0;
                elementRender = null;
            } else {
                if (id != elementRenderId) {
                    elementRenderId = id;
                    TransparentNodeDescriptor descriptor = Eln.transparentNodeItem.getDescriptor(id);
                    elementRender = (TransparentNodeElementRender) descriptor.RenderClass.getConstructor(TransparentNodeEntity.class, TransparentNodeDescriptor.class).newInstance(this, descriptor);
                }
                elementRender.networkUnserialize(stream);
            }

        } catch (IOException e) {

            e.printStackTrace();
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

    public Container newContainer(Direction side, EntityPlayer player) {
        TransparentNode n = (TransparentNode) getNode();
        if (n == null) return null;
        return n.newContainer(side, player);
    }

    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return elementRender.newGuiDraw(side, player);
    }

    public void preparePacketForServer(DataOutputStream stream) {
        try {
            super.preparePacketForServer(stream);

            stream.writeShort(elementRenderId);

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public void sendPacketToServer(ByteArrayOutputStream bos) {
        super.sendPacketToServer(bos);
    }

    public boolean cameraDrawOptimisation() {
        if (elementRender == null) return super.cameraDrawOptimisation();
        return elementRender.cameraDrawOptimisation();
    }

    public int getDamageValue(World world, int x, int y, int z) {
        if (world.isRemote) {
            return elementRenderId;
        }
        return 0;
    }

    @Override
    public void tileEntityNeighborSpawn() {

        if (elementRender != null) elementRender.notifyNeighborSpawn();
    }

    public void addCollisionBoxesToList(AxisAlignedBB par5AxisAlignedBB, List list) {
        if (worldObj.isRemote) {
            if (elementRender == null) {
                AxisAlignedBB bb = Blocks.stone.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
                if (par5AxisAlignedBB.intersectsWith(bb)) list.add(bb);
            } else {
                elementRender.transparentNodedescriptor.addCollisionBoxesToList(par5AxisAlignedBB, list, this);
            }
        } else {
            TransparentNode node = (TransparentNode) getNode();
            if (node == null) {
                AxisAlignedBB bb = Blocks.stone.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
                if (par5AxisAlignedBB.intersectsWith(bb)) list.add(bb);
            } else {
                node.element.transparentNodeDescriptor.addCollisionBoxesToList(par5AxisAlignedBB, list, this);
            }
        }
    }

    public void serverPacketUnserialize(DataInputStream stream) {
        super.serverPacketUnserialize(stream);
        if (elementRender != null)
            elementRender.serverPacketUnserialize(stream);
    }

    @Override
    public String getNodeUuid() {

        return Eln.transparentNodeBlock.getNodeUuid();
    }

    @Override
    public void destructor() {
        if (elementRender != null)
            elementRender.destructor();
        super.destructor();
    }

    @Override
    public void clientRefresh(float deltaT) {
        if (elementRender != null) {
            elementRender.refresh(deltaT);
        }
    }

    @Override
    public int isProvidingWeakPower(Direction side) {
        return 0;
    }

    ISidedInventory getSidedInventory() {
        if (worldObj.isRemote) {
            if (elementRender == null) return FakeSideInventory.getInstance();
            IInventory i = elementRender.getInventory();
            if (i != null) {
                if (i instanceof ISidedInventory)
                    return (ISidedInventory) i;
            }
        } else {
            Node node = getNode();
            if (node != null && node instanceof TransparentNode) {
                TransparentNode tn = (TransparentNode) node;
                IInventory i = tn.getInventory(null);
                ;
                if (i != null) {
                    if (i instanceof ISidedInventory)
                        return (ISidedInventory) i;
                }
            }
        }
        return FakeSideInventory.getInstance();
    }

    @Override
    public int getSizeInventory() {
        return getSidedInventory().getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int var1) {
        return getSidedInventory().getStackInSlot(var1);
    }

    @Override
    public ItemStack decrStackSize(int var1, int var2) {
        return getSidedInventory().decrStackSize(var1, var2);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int var1) {
        return getSidedInventory().getStackInSlotOnClosing(var1);
    }

    @Override
    public void setInventorySlotContents(int var1, ItemStack var2) {
        getSidedInventory().setInventorySlotContents(var1, var2);
    }

    @Override
    public String getInventoryName() {
        return getSidedInventory().getInventoryName();
    }

    @Override
    public boolean hasCustomInventoryName() {
        return getSidedInventory().hasCustomInventoryName();
    }

    @Override
    public int getInventoryStackLimit() {
        return getSidedInventory().getInventoryStackLimit();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer var1) {
        return getSidedInventory().isUseableByPlayer(var1);
    }

    @Override
    public void openInventory() {
        getSidedInventory().openInventory();
    }

    @Override
    public void closeInventory() {
        getSidedInventory().closeInventory();
    }

    @Override
    public boolean isItemValidForSlot(int var1, ItemStack var2) {
        return getSidedInventory().isItemValidForSlot(var1, var2);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int var1) {
        return getSidedInventory().getAccessibleSlotsFromSide(var1);
    }

    @Override
    public boolean canInsertItem(int var1, ItemStack var2, int var3) {
        return getSidedInventory().canInsertItem(var1, var2, var3);
    }

    @Override
    public boolean canExtractItem(int var1, ItemStack var2, int var3) {
        return getSidedInventory().canExtractItem(var1, var2, var3);
    }
}