package mods.eln.lampsocket;

import mods.eln.BasicContainer;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.LampSlot;
import mods.eln.node.SixNodeItemSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class LampSocketContainer extends BasicContainer {

	public static final int lampSlotId = 0;
	public static final int cableSlotId = 1;
	
	public LampSocketContainer(EntityPlayer player, IInventory inventory,LampSocketDescriptor descriptor) {
		super(player, inventory,new Slot[]{
				new LampSlot(inventory,lampSlotId,70 +  0,50,1,descriptor.socketType),
				//new SixNodeItemSlot(inventory,0,1,62 + 0,17,new Class[]{ElectricalCableDescriptor.class}),
				new SixNodeItemSlot(inventory,cableSlotId,70 + 18,50,1,new Class[]{ElectricalCableDescriptor.class},SlotSkin.medium,new String[]{"Electrical cable slot"})
			});
		
		// TODO Auto-generated constructor stub
	}

}
