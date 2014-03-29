package mods.eln.electricalbreaker;

import mods.eln.BasicContainer;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.LampSlot;
import mods.eln.node.SixNodeItemSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class ElectricalBreakerContainer extends BasicContainer {

	public static final int cableSlotId = 0;
	
	public ElectricalBreakerContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
				new SixNodeItemSlot(inventory, cableSlotId, 150, 21, 1, new Class[]{ElectricalCableDescriptor.class}, SlotSkin.medium, new String[]{"Electrical Cable Slot"})
			});
	}
}
