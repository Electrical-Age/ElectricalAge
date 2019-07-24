package mods.eln.node.six;

import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.jetbrains.annotations.NotNull;

public class SixNodeElementInventory implements IInventory, INBTTReady {
    SixNodeElementRender sixnodeRender = null;
    SixNodeElement sixNodeElement = null;
    int stackLimit;
    private ItemStack[] inv;

    public SixNodeElementInventory(int size, int stackLimit, SixNodeElementRender sixnodeRender) {
        inv = new ItemStack[size];
        this.stackLimit = stackLimit;
        this.sixnodeRender = sixnodeRender;
    }

    public SixNodeElementInventory(int size, int stackLimit, SixNodeElement sixNodeElement) {
        inv = new ItemStack[size];
        this.stackLimit = stackLimit;
        this.sixNodeElement = sixNodeElement;
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
        if (slot >= getInv().length) return null;
        return getInv()[slot];
    }

    @NotNull
    @Override
    public ItemStack decrStackSize(int slot, int amt) {
        ItemStack stack = getStackInSlot(slot);
        stack.splitStack(amt);
        return stack;
    }

    @NotNull
    @Override
    public ItemStack removeStackFromSlot(int slot) {
        ItemStack stack = getStackInSlot(slot);
        stack.setCount(0);
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, @NotNull ItemStack stack) {
        getInv()[slot] = stack;
        int stackLimit = getInventoryStackLimit();
        if (stack.getCount() > stackLimit) stack.setCount(stackLimit);
    }

    @NotNull
    @Override
    public String getName() {
        return "tco.SixNodeInventory";
    }

    @Override
    public int getInventoryStackLimit() {
        return stackLimit;
    }

    @Override
    public boolean isUsableByPlayer(@NotNull EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(@NotNull EntityPlayer player) {}

    @Override
    public void closeInventory(@NotNull EntityPlayer player) {}

    @Override
    public void markDirty() {
        if (sixNodeElement != null && !sixNodeElement.sixNode.isDestructing()) {
            sixNodeElement.inventoryChanged();
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
    public boolean isItemValidForSlot(int i, @NotNull ItemStack itemstack) {
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

    @NotNull
    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString("SixNodeInventory");
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : inv) {
            if (itemStack.getCount() > 0) return false;
        }
        return true;
    }
}
