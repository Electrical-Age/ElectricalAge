package mods.eln.node.transparent;

import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;

public class TransparentNodeElementInventory implements ISidedInventory, INBTTReady {
    protected TransparentNodeElementRender transparentNodeRender = null;
    protected TransparentNodeElement transparentNodeElement = null;

    int stackLimit;
    private ItemStack[] inv;

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

    private ItemStack[] getInv() {
        return inv;
    }

    @Override
    public int getSizeInventory() {
        return getInv().length;
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return getInv()[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amt) {
        ItemStack stack = getStackInSlot(slot);
        stack.splitStack(amt);
        return stack;
    }

    @Override
    public ItemStack removeStackFromSlot(int slot) {
        ItemStack stack = getStackInSlot(slot);
        stack.setCount(0);
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, @NotNull ItemStack stack) {
        getInv()[slot] = stack;
        if (stack.getCount() > getInventoryStackLimit()) {
            stack.setCount(getInventoryStackLimit());
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
    public boolean isEmpty() {
        for (ItemStack stack : getInv()) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public void markDirty() {
        if (transparentNodeElement != null && !transparentNodeElement.node.isDestructing())
            transparentNodeElement.inventoryChange(this);
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
            // this is a gem. wat.
            int[] lol = getSlotsForFace(EnumFacing.VALUES[idx]);
            for (int hohoho : lol)
                if (hohoho == i && canInsertItem(i, itemstack, EnumFacing.VALUES[idx]))
                    return true;
        }
        return false;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {}

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {}

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
