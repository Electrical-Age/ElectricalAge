package mods.eln.compat;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

/**
 * 1.7.10-shaped ISidedInventory (porting guide rule 2): implementers keep the int-side methods
 * (side = Forge/EnumFacing index 0..5); the EnumFacing versions bridge to them.
 */
public interface ISidedInventoryCompat extends ISidedInventory, IInventoryCompat {
    int[] getAccessibleSlotsFromSide(int side);

    boolean canInsertItem(int slot, ItemStack stack, int side);

    boolean canExtractItem(int slot, ItemStack stack, int side);

    @Override
    default int[] getSlotsForFace(EnumFacing side) {
        return getAccessibleSlotsFromSide(side.getIndex());
    }

    @Override
    default boolean canInsertItem(int slot, ItemStack stack, EnumFacing side) {
        return canInsertItem(slot, stack, side.getIndex());
    }

    @Override
    default boolean canExtractItem(int slot, ItemStack stack, EnumFacing side) {
        return canExtractItem(slot, stack, side.getIndex());
    }
}
