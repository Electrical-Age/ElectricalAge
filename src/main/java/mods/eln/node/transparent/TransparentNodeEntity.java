package mods.eln.node.transparent;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Coordinate;
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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;


public class TransparentNodeEntity extends NodeBlockEntity implements ISidedInventory { // boolean[] syncronizedSideEnable = new boolean[6];
    TransparentNodeElementRender elementRender = null;
    private short elementRenderId;


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

        } catch (IOException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
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

    public int getDamageValue(World world, BlockPos pos) {
        if (world.isRemote) {
            return elementRenderId;
        }
        return 0;
    }

    @Override
    public void tileEntityNeighborSpawn() {

        if (elementRender != null) elementRender.notifyNeighborSpawn();
    }

    public void addCollisionBoxesToList(AxisAlignedBB axisAlignedBB, List<AxisAlignedBB> list, Coordinate blockCoord) {
        TransparentNodeDescriptor desc = null;
        if (world.isRemote) {
            desc = elementRender == null ? null : elementRender.transparentNodedescriptor;
        } else {
            TransparentNode node = (TransparentNode) getNode();
            desc = node == null ? null : node.element.transparentNodeDescriptor;
        }
        BlockPos pos;
        if (blockCoord != null) {
            pos = blockCoord.pos;
        } else {
            pos = this.pos;
        }
        if (desc == null) {
            AxisAlignedBB bb = new AxisAlignedBB(pos);
            if (axisAlignedBB.intersects(bb)) list.add(bb);
        } else {
            desc.addCollisionBoxesToList(axisAlignedBB, list, pos);
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
        if (world.isRemote) {
            if (elementRender == null) return FakeSideInventory.getInstance();
            IInventory i = elementRender.getInventory();
            if (i instanceof ISidedInventory) {
                return (ISidedInventory) i;
            }
        } else {
            Node node = getNode();
            if (node instanceof TransparentNode) {
                TransparentNode tn = (TransparentNode) node;
                IInventory i = tn.getInventory(null);
                if (i instanceof ISidedInventory) {
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

    @NotNull
    @Override
    public ItemStack getStackInSlot(int var1) {
        return getSidedInventory().getStackInSlot(var1);
    }

    @NotNull
    @Override
    public ItemStack decrStackSize(int var1, int var2) {
        return getSidedInventory().decrStackSize(var1, var2);
    }

    @NotNull
    @Override
    public ItemStack removeStackFromSlot(int var1) {
        return getSidedInventory().removeStackFromSlot(var1);
    }

    @Override
    public void setInventorySlotContents(int var1, @NotNull ItemStack var2) {
        getSidedInventory().setInventorySlotContents(var1, var2);
    }

    @NotNull
    @Override
    public String getName() {
        return getSidedInventory().getName();
    }

    @Override
    public boolean hasCustomName() {
        return getSidedInventory().hasCustomName();
    }

    @Override
    public int getInventoryStackLimit() {
        return getSidedInventory().getInventoryStackLimit();
    }

    @Override
    public boolean isEmpty() {
        return getSidedInventory().isEmpty();
    }

    @Override
    public boolean isUsableByPlayer(@NotNull EntityPlayer player) {
        return getSidedInventory().isUsableByPlayer(player);
    }

    @Override
    public void openInventory(EntityPlayer player) {
        getSidedInventory().openInventory(player);
    }

    @Override
    public void closeInventory(EntityPlayer player) { getSidedInventory().closeInventory(player); }

    @Override
    public boolean isItemValidForSlot(int var1, ItemStack stack) {
        return getSidedInventory().isItemValidForSlot(var1, stack);
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
    }

    @Override
    public int[] getSlotsForFace(@NotNull EnumFacing facing) {
        return getSidedInventory().getSlotsForFace(facing);
    }

    @Override
    public boolean canInsertItem(int var1, @NotNull ItemStack stack, @NotNull EnumFacing facing) {
        return getSidedInventory().canInsertItem(var1, stack, facing);
    }

    @Override
    public boolean canExtractItem(int var1, @NotNull ItemStack stack, @NotNull EnumFacing facing) {
        return getSidedInventory().canExtractItem(var1, stack, facing);
    }
}
