package mods.eln.compat;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 1.7.10-shaped sided inventory on 26.x: implementers keep the int-side methods (side = 3D index 0..5 = DOWN, UP,
 * NORTH, SOUTH, WEST, EAST), the WorldlyContainer methods bridge to them.
 */
public interface ISidedInventoryCompat extends WorldlyContainer, IInventoryCompat {
    int[] getAccessibleSlotsFromSide(int side);

    boolean canInsertItem(int slot, ItemStack stack, int side);

    boolean canExtractItem(int slot, ItemStack stack, int side);

    @Override
    default int[] getSlotsForFace(Direction side) {
        return getAccessibleSlotsFromSide(side.get3DDataValue());
    }

    @Override
    default boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canInsertItem(slot, stack, side == null ? 0 : side.get3DDataValue());
    }

    @Override
    default boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canExtractItem(slot, stack, side.get3DDataValue());
    }
}
