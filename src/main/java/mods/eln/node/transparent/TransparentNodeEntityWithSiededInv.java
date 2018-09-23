package mods.eln.node.transparent;

import mods.eln.misc.FakeSideInventory;
import mods.eln.node.Node;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class TransparentNodeEntityWithSiededInv extends TransparentNodeEntity implements ISidedInventory { // boolean[] syncronizedSideEnable = new boolean[6];
    ISidedInventory getSidedInventory() {
        if (worldObj.isRemote) {
            if (elementRender == null) return FakeSideInventory.getInstance();
            IInventory i = elementRender.getInventory();
            if (i != null && i instanceof ISidedInventory) {
                return (ISidedInventory) i;
            }
        } else {
            Node node = getNode();
            if (node != null && node instanceof TransparentNode) {
                TransparentNode tn = (TransparentNode) node;
                IInventory i = tn.getInventory(null);
                ;
                if (i != null && i instanceof ISidedInventory) {
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
    public ItemStack removeStackFromSlot(int var1) {
        return getSidedInventory().removeStackFromSlot(var1);
    }

    @Override
    public void setInventorySlotContents(int var1, ItemStack var2) {
        getSidedInventory().setInventorySlotContents(var1, var2);
    }

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
    public boolean isUseableByPlayer(EntityPlayer var1) {
        return getSidedInventory().isUseableByPlayer(var1);
    }

    @Override
    public void openInventory(EntityPlayer var1) {
        getSidedInventory().openInventory(var1);
    }

    @Override
    public void closeInventory(EntityPlayer var1) {
        getSidedInventory().closeInventory(var1);
    }

    @Override
    public boolean isItemValidForSlot(int var1, ItemStack var2) {
        return getSidedInventory().isItemValidForSlot(var1, var2);
    }

    @Override
    public int[] getSlotsForFace(EnumFacing var1) {
        return getSidedInventory().getSlotsForFace(var1);
    }

    @Override
    public boolean canInsertItem(int var1, ItemStack var2, EnumFacing var3) {
        return getSidedInventory().canInsertItem(var1, var2, var3);
    }

    @Override
    public boolean canExtractItem(int var1, ItemStack var2, EnumFacing var3) {
        return getSidedInventory().canExtractItem(var1, var2, var3);
    }
}
// && 
