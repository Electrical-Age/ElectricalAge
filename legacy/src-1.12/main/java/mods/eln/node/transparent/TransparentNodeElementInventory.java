package mods.eln.node.transparent;

import mods.eln.compat.ISidedInventoryCompat;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class TransparentNodeElementInventory implements ISidedInventoryCompat, INBTTReady {
    protected TransparentNodeElementRender transparentNodeRender = null;
    protected TransparentNodeElement transparentNodeElement = null;

    int stackLimit;

    public TransparentNodeElementInventory(int size, int stackLimit, TransparentNodeElementRender TransparentnodeRender) {
        inv = new ItemStack[size];
        java.util.Arrays.fill(inv, ItemStack.EMPTY);
        this.stackLimit = stackLimit;
        this.transparentNodeRender = TransparentnodeRender;
    }

    public TransparentNodeElementInventory(int size, int stackLimit, TransparentNodeElement TransparentNodeElement) {
        inv = new ItemStack[size];
        java.util.Arrays.fill(inv, ItemStack.EMPTY);
        this.stackLimit = stackLimit;
        this.transparentNodeElement = TransparentNodeElement;
    }

    private ItemStack[] inv;

    private ItemStack[] getInv() {
        return inv;
    }

    @Override
    public int getSizeInventory() {

        return getInv().length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {

        return getInv()[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amt) {
        ItemStack stack = getStackInSlot(slot);
        if (!Utils.isEmpty(stack)) {
            if (stack.getCount() <= amt) {
                setInventorySlotContents(slot, ItemStack.EMPTY);
            } else {
                stack = stack.splitStack(amt);
                if (stack.getCount() == 0) {
                    setInventorySlotContents(slot, ItemStack.EMPTY);
                }
            }
        }
        return stack;
    }

    @Override
    public ItemStack removeStackFromSlot(int slot) {
        ItemStack stack = getStackInSlot(slot);
        if (!Utils.isEmpty(stack)) {
            setInventorySlotContents(slot, ItemStack.EMPTY);
        }
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        getInv()[slot] = stack == null ? ItemStack.EMPTY : stack; // never store null (1.12)
        if (!Utils.isEmpty(stack) && stack.getCount() > getInventoryStackLimit()) {
            stack.setCount(getInventoryStackLimit());
        }
    }

    @Override
    public String getInventoryName() {
        return "tco.TransparentNodeInventory";
    }

    @Override
    public int getInventoryStackLimit() {

        return stackLimit;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {

		/*
         * if(transparentNodeElement != null) { if(NodeManager.instance.getNodeFromCoordonate(transparentNodeElement.node.coordonate) != transparentNodeElement.node) return false; return player.getDistance(transparentNodeElement.node.coordonate.x + 0.5, transparentNodeElement.node.coordonate.y + 0.5, transparentNodeElement.node.coordonate.z + 0.5) < 10; }
		 */
        return true;
        // return player.getDistanceSq(transparentNodeRender.tileEntity.getPos().getX() + 0.5, transparentNodeRender.tileEntity.getPos().getY() + 0.5, transparentNodeRender.tileEntity.getPos().getZ() + 0.5) < 18;
    }

    @Override
    public void openInventory() {

    }

    @Override
    public void closeInventory() {

    }

    @Override
    public void markDirty() {
        if (transparentNodeElement != null && !transparentNodeElement.node.isDestructing()) {
            transparentNodeElement.inventoryChange(this);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {

        Utils.readFromNBT(nbt, str, this);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {

        Utils.writeToNBT(nbt, str, this);
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        for (int idx = 0; idx < 6; idx++) {
            int[] lol = getAccessibleSlotsFromSide(idx);
            for (int hohoho : lol) {
                if (hohoho == i && canInsertItem(i, itemstack, idx)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasCustomInventoryName() {

        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int var1) {
        return new int[]{};
    }

    @Override
    public boolean canInsertItem(int var1, ItemStack var2, int var3) {
        return false;
    }

    @Override
    public boolean canExtractItem(int var1, ItemStack var2, int var3) {
        return false;
    }

}
