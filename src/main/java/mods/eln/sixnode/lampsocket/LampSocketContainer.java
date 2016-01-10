package mods.eln.sixnode.lampsocket;

import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.LampSlot;
import mods.eln.misc.BasicContainer;
import mods.eln.node.six.SixNodeItemSlot;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class LampSocketContainer extends BasicContainer {

	public static final int lampSlotId = 0;
	public static final int cableSlotId = 1;
	
	public LampSocketContainer(EntityPlayer player, IInventory inventory, LampSocketDescriptor descriptor) {
		super(player, inventory, new Slot[]{
			new LampSlot(inventory, lampSlotId, 70 + 0, 57, 1, descriptor.socketType),
			//new SixNodeItemSlot(inventory, 0, 1, 62 + 0, 17, new Class[]{ElectricalCableDescriptor.class}),
			new SixNodeItemSlot(inventory, cableSlotId, 70 + 18, 57, 1, new Class[]{ElectricalCableDescriptor.class},
				SlotSkin.medium, new String[]{tr("Electrical cable slot")})
		});
	}
}
