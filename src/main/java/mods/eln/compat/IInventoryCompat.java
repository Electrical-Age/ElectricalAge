package mods.eln.compat;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10/1.12-shaped inventory on 26.x: EA inventories keep implementing the old method names (getSizeInventory,
 * getStackInSlot, decrStackSize, setInventorySlotContents, markDirty...), and the 26.x Container methods delegate to
 * them. getStackInSlot must return ItemStack.EMPTY, never null.
 */
public interface IInventoryCompat extends Container {
    int getSizeInventory();

    ItemStack getStackInSlot(int slot);

    ItemStack decrStackSize(int slot, int amount);

    ItemStack removeStackFromSlot(int slot);

    void setInventorySlotContents(int slot, ItemStack stack);

    int getInventoryStackLimit();

    boolean isUsableByPlayer(Player player);

    void markDirty();

    default boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    String getInventoryName();

    boolean hasCustomInventoryName();

    default void openInventory() {
    }

    default void closeInventory() {
    }

    // ---- 26.x Container, delegating ----

    @Override
    default int getContainerSize() {
        return getSizeInventory();
    }

    @Override
    default boolean isEmpty() {
        for (int i = 0; i < getSizeInventory(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (stack != null && !stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    default ItemStack getItem(int slot) {
        ItemStack s = getStackInSlot(slot);
        return s == null ? ItemStack.EMPTY : s;
    }

    @Override
    default ItemStack removeItem(int slot, int amount) {
        return decrStackSize(slot, amount);
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        return removeStackFromSlot(slot);
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        setInventorySlotContents(slot, stack);
    }

    @Override
    default int getMaxStackSize() {
        return getInventoryStackLimit();
    }

    @Override
    default void setChanged() {
        markDirty();
    }

    @Override
    default boolean stillValid(Player player) {
        return isUsableByPlayer(player);
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    default void startOpen(net.minecraft.world.entity.ContainerUser user) {
        openInventory();
    }

    @Override
    default void stopOpen(net.minecraft.world.entity.ContainerUser user) {
        closeInventory();
    }

    @Override
    default void clearContent() {
        for (int i = 0; i < getSizeInventory(); i++) setInventorySlotContents(i, ItemStack.EMPTY);
    }
}
