package mods.eln.item;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.lampsocket.LampSocketType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class LampSlot extends GenericItemUsingDamageSlot{
	LampSocketType socket;
	public LampSlot(IInventory inventory, int slot, int x, int y,
			int stackLimit,LampSocketType socket) {
		super(inventory, slot, x, y, stackLimit, LampDescriptor.class,SlotSkin.medium,new String[]{"Lamp slot"});
		
		this.socket = socket;
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean isItemValid(ItemStack itemStack) {
		// TODO Auto-generated method stub
		if(!super.isItemValid(itemStack)) return false;
		LampDescriptor descriptor = (LampDescriptor) Eln.sharedItem.getDescriptor(itemStack);
		return descriptor.socket == socket;
	}

}
