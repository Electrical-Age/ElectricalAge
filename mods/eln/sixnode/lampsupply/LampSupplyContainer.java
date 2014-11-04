package mods.eln.sixnode.lampsupply;

import mods.eln.Translator;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.LampSlot;
import mods.eln.misc.BasicContainer;
import mods.eln.node.six.SixNodeItemSlot;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class LampSupplyContainer extends BasicContainer {


	public static final int cableSlotId = 0;
	
	public LampSupplyContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
				new SixNodeItemSlot(inventory,cableSlotId,151,6,64, new Class[]{ElectricalCableDescriptor.class},SlotSkin.medium,new String[]{Translator.translate("eln.core.cable.slot.hint"),Translator.translate("eln.core.tile.supply.hint0"),Translator.translate("eln.core.tile.supply.hint1"),Translator.translate("eln.core.tile.supply.hint2")})
			});
		
		
	}

}
