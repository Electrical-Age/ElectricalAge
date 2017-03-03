package mods.eln.gui;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SlotFilter extends SlotWithSkinAndComment {

    ItemStackFilter itemStackFilter;
    int stackLimit;

    public SlotFilter(IInventory par1iInventory, int slot, int x, int y,
                      int stackLimit, ItemStackFilter itemStackFilter, SlotSkin skin, String[] comment) {
        super(par1iInventory, slot, x, y, skin, comment);

        this.stackLimit = stackLimit;
        this.itemStackFilter = itemStackFilter;
    }

    /**
     * Check if the stack is a valid item for this slot. Always true beside for
     * the armor slots.
     */
    public boolean isItemValid(ItemStack itemStack) {
        return itemStackFilter.tryItemStack(itemStack);
    }

    @Override
    public int getSlotStackLimit() {
        // return super.getSlotStackLimit();
        return stackLimit;
    }
}
