package mods.eln.node;

import mods.eln.compat.IInventoryCompat;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The item array of a node element: fixed size, one stack limit for every slot, saved with the element. Never holds
 * null (empty slots are ItemStack.EMPTY). Out-of-range slots read as empty; a stack written to one is lost, and
 * logged with where the write came from. Subclasses tell their element about changes in {@link #markDirty()}.
 */
public abstract class ElementInventory implements IInventoryCompat, INBTTReady {
    private final ItemStack[] inv;
    private final int stackLimit;

    protected ElementInventory(int size, int stackLimit) {
        inv = new ItemStack[size];
        java.util.Arrays.fill(inv, ItemStack.EMPTY);
        this.stackLimit = stackLimit;
    }

    @Override
    public int getSizeInventory() {
        return inv.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= inv.length) return ItemStack.EMPTY;
        return inv[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amt) {
        ItemStack stack = getStackInSlot(slot);
        if (!Utils.isEmpty(stack)) {
            if (stack.getCount() <= amt) {
                setInventorySlotContents(slot, ItemStack.EMPTY);
            } else {
                stack = stack.split(amt);
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
        if (slot < 0 || slot >= inv.length) {
            if (!Utils.isEmpty(stack))
                mods.eln.Eln.LOG.warn("Electrical Age: {} has no slot {} (size {}); the {} written there is lost",
                    getClass().getSimpleName(), slot, inv.length, stack, new Throwable("written from"));
            return;
        }
        inv[slot] = stack == null ? ItemStack.EMPTY : stack;
        if (!Utils.isEmpty(stack) && stack.getCount() > getInventoryStackLimit()) {
            stack.setCount(getInventoryStackLimit());
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return stackLimit;
    }

    @Override
    public boolean isUsableByPlayer(Player player) {
        return true;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        Utils.readFromNBT(nbt, str, this);
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        Utils.writeToNBT(nbt, str, this);
    }
}
