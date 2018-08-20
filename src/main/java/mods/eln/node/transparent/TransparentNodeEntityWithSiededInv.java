package mods.eln.node.transparent;

import mods.eln.misc.FakeSideInventory;
import mods.eln.node.Node;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

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
// && 
