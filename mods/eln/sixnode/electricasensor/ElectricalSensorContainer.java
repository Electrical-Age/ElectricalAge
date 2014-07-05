package mods.eln.sixnode.electricasensor;

import mods.eln.BasicContainer;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.node.SixNodeItemSlot;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class ElectricalSensorContainer extends BasicContainer {

	public static final int cableSlotId = 0;
	
	public ElectricalSensorContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
				new SixNodeItemSlot(inventory,cableSlotId,152,62,1,new Class[]{ElectricalCableDescriptor.class},SlotSkin.medium,new String[]{"Electrical Cable Slot"})
			});
		
		// TODO Auto-generated constructor stub
	}

}
