package mods.eln.sixnode.thermalsensor;

import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.LampSlot;
import mods.eln.misc.BasicContainer;
import mods.eln.node.six.SixNodeItemSlot;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.thermalcable.ThermalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class ThermalSensorContainer extends BasicContainer {

	public static final int cableSlotId = 0;
	
	public ThermalSensorContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
				new SixNodeItemSlot(inventory,cableSlotId,152,62,1,new Class[]{ThermalCableDescriptor.class, ElectricalCableDescriptor.class},SlotSkin.medium,new String[]{"Cable Slot"})
			});
		
		
	}

}
