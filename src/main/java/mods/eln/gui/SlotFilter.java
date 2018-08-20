package mods.eln.gui;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SlotFilter extends SlotWithSkinAndComment {

    IItemStackFilter[] itemStackFilter;
    int stackLimit;

    public SlotFilter(IInventory par1iInventory, int slot, int x, int y,
                      int stackLimit, IItemStackFilter[] itemStackFilter, SlotSkin skin, String[] comment) {
        super(par1iInventory, slot, x, y, skin, comment);

        this.stackLimit = stackLimit;
        this.itemStackFilter = itemStackFilter;
    }

    /**
     * Check if the stack is a valid item for this slot. Always true beside for
     * the armor slots.
     */
    public boolean isItemValid(ItemStack itemStack) {
        for (IItemStackFilter filter : itemStackFilter) {
            if (filter.tryItemStack(itemStack))
                return true;
        }
        return false;
    }

    @Override
    public int getSlotStackLimit() {
        // return super.getSlotStackLimit();
        return stackLimit;
    }
}
