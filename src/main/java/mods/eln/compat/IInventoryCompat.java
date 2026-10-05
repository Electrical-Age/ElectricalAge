package mods.eln.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * 1.7.10-shaped IInventory (porting guide rule 2): implementers keep getInventoryName / hasCustomInventoryName /
 * openInventory() / closeInventory(); the 1.12 additions get defaults here.
 * getStackInSlot must return ItemStack.EMPTY, never null (rule 3).
 */
public interface IInventoryCompat extends IInventory {
    String getInventoryName();

    boolean hasCustomInventoryName();

    default void openInventory() {
    }

    default void closeInventory() {
    }

    @Override
    default String getName() {
        return getInventoryName();
    }

    @Override
    default boolean hasCustomName() {
        return hasCustomInventoryName();
    }

    @Override
    default ITextComponent getDisplayName() {
        return new TextComponentString(getName());
    }

    @Override
    default void openInventory(EntityPlayer player) {
        openInventory();
    }

    @Override
    default void closeInventory(EntityPlayer player) {
        closeInventory();
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
    default int getField(int id) {
        return 0;
    }

    @Override
    default void setField(int id, int value) {
    }

    @Override
    default int getFieldCount() {
        return 0;
    }

    @Override
    default void clear() {
        for (int i = 0; i < getSizeInventory(); i++) {
            setInventorySlotContents(i, ItemStack.EMPTY);
        }
    }
}
