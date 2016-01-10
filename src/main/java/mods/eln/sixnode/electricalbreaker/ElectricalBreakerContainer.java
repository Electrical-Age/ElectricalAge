package mods.eln.sixnode.electricalbreaker;

import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.misc.BasicContainer;
import mods.eln.node.six.SixNodeItemSlot;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class ElectricalBreakerContainer extends BasicContainer {

	public static final int cableSlotId = 0;
	
	public ElectricalBreakerContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory, new Slot[]{
			new SixNodeItemSlot(inventory, cableSlotId, 150, 21, 1, new Class[]{ElectricalCableDescriptor.class}, SlotSkin.medium, new String[]{tr("Electrical cable slot")})
		});
	}
}
