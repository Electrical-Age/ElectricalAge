package mods.eln.item;

import mods.eln.Translator;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.misc.Utils;
import mods.eln.sixnode.lampsocket.LampSocketType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class LampSlot extends GenericItemUsingDamageSlot{
	LampSocketType socket;
	public LampSlot(IInventory inventory, int slot, int x, int y,
			int stackLimit,LampSocketType socket) {
		super(inventory, slot, x, y, stackLimit, LampDescriptor.class,SlotSkin.medium,new String[]{Translator.translate("eln.core.lamp.slot.hint")});
		
		this.socket = socket;
		
	}

	@Override
	public boolean isItemValid(ItemStack itemStack) {
		
		if(!super.isItemValid(itemStack)) return false;
		LampDescriptor descriptor = (LampDescriptor)  Utils.getItemObject(itemStack);
		return descriptor.socket == socket;
	}

}
