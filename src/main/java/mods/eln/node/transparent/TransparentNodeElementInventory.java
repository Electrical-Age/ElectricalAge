package mods.eln.node.transparent;

import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;

public class TransparentNodeElementInventory implements ISidedInventory, INBTTReady {
    protected TransparentNodeElementRender transparentNodeRender = null;
    protected TransparentNodeElement transparentNodeElement = null;

    int stackLimit;

    public TransparentNodeElementInventory(int size, int stackLimit, TransparentNodeElementRender TransparentnodeRender) {
        inv = new ItemStack[size];
        this.stackLimit = stackLimit;
        this.transparentNodeRender = TransparentnodeRender;
    }

    public TransparentNodeElementInventory(int size, int stackLimit, TransparentNodeElement TransparentNodeElement) {
        inv = new ItemStack[size];
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
        if (stack != null) {
            if (stack.stackSize <= amt) {
                setInventorySlotContents(slot, null);
            } else {
                stack = stack.splitStack(amt);
                if (stack.stackSize == 0) {
                    setInventorySlotContents(slot, null);
                }
            }
        }
        return stack;
    }

    @Override
    public ItemStack removeStackFromSlot(int slot) {
        ItemStack stack = getStackInSlot(slot);
        if (stack != null) {
            setInventorySlotContents(slot, null);
        }
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        getInv()[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
    }

    @Override
    public String getName() {
        return "tco.TransparentNodeInventory";
    }

    @Override
    public int getInventoryStackLimit() {

        return stackLimit;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {

		/*
         * if(transparentNodeElement != null) { if(NodeManager.instance.getNodeFromCoordonate(transparentNodeElement.node.coordinate) != transparentNodeElement.node) return false; return player.getDistance(transparentNodeElement.node.coordinate.x + 0.5, transparentNodeElement.node.coordinate.y + 0.5, transparentNodeElement.node.coordinate.z + 0.5) < 10; }
		 */
        return true;
        // return player.getDistanceSq(transparentNodeRender.tileEntity.xCoord + 0.5, transparentNodeRender.tileEntity.yCoord + 0.5, transparentNodeRender.tileEntity.zCoord + 0.5) < 18;
    }

    @Override
    public void openInventory(EntityPlayer player) {

    }

    @Override
    public void closeInventory(EntityPlayer player) {

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
    public NBTTagCompound writeToNBT(NBTTagCompound nbt, String str) {

        return Utils.writeToNBT(nbt, str, this);
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        for (int idx = 0; idx < 6; idx++) {
            int[] lol = getSlotsForFace(EnumFacing.getFront(idx));
            for (int hohoho : lol) {
                if (hohoho == i && canInsertItem(i, itemstack, EnumFacing.getFront(idx))) {
                    return true;
                }
            }
        }
        return false;
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
    public boolean hasCustomName() {

        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return null;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing var1) {
        return new int[]{};
    }

    @Override
    public boolean canInsertItem(int var1, ItemStack var2, EnumFacing var3) {
        return false;
    }

    @Override
    public boolean canExtractItem(int var1, ItemStack var2, EnumFacing var3) {
        return false;
    }

}
