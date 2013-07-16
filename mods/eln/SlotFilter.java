package mods.eln;

import mods.eln.gui.SlotWithSkin;
import mods.eln.gui.SlotWithSkinAndComment;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SlotFilter extends SlotWithSkinAndComment {
	ItemStackFilter[] itemStackFilter;
	int stackLimit;

	public SlotFilter(IInventory par1iInventory, int slot, int x, int y,
			int stackLimit, ItemStackFilter[] itemStackFilter, SlotSkin skin,
			String[] comment) {
		super(par1iInventory, slot, x, y, skin, comment);
		// TODO Auto-generated constructor stub
		this.stackLimit = stackLimit;
		this.itemStackFilter = itemStackFilter;
	}

	/**
	 * Check if the stack is a valid item for this slot. Always true beside for
	 * the armor slots.
	 */
	public boolean isItemValid(ItemStack itemStack) {
		for (ItemStackFilter filter : itemStackFilter) {
			if (filter.tryItemStack(itemStack))
				return true;
		}
		return false;
	}

	@Override
	public int getSlotStackLimit() {
		// TODO Auto-generated method stub
		// return super.getSlotStackLimit();
		return stackLimit;
	}
}
