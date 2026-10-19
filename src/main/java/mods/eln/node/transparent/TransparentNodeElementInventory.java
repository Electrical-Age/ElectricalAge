package mods.eln.node.transparent;

import mods.eln.compat.ISidedInventoryCompat;
import mods.eln.node.ElementInventory;
import net.minecraft.world.item.ItemStack;

/**
 * Inventory of a transparent-node element. The sided rules (which slots a face may insert into or extract from)
 * expose nothing here; machines that feed from hoppers and pipes override them.
 */
public class TransparentNodeElementInventory extends ElementInventory implements ISidedInventoryCompat {
    protected final TransparentNodeElement transparentNodeElement;

    public TransparentNodeElementInventory(int size, int stackLimit, TransparentNodeElement transparentNodeElement) {
        super(size, stackLimit);
        this.transparentNodeElement = transparentNodeElement;
    }

    @Override
    public String getInventoryName() {
        return "tco.TransparentNodeInventory";
    }

    @Override
    public void markDirty() {
        if (transparentNodeElement != null && !transparentNodeElement.node.isDestructing()) {
            transparentNodeElement.inventoryChange(this);
        }
    }

    /** Valid if some face may insert it. */
    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        for (int side = 0; side < 6; side++) {
            for (int slot : getAccessibleSlotsFromSide(side)) {
                if (slot == i && canInsertItem(i, itemstack, side)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return false;
    }
}
