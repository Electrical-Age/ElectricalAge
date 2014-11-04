package mods.eln.sixnode.electricaldatalogger;

import mods.eln.Translator;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.ItemStackFilter;
import mods.eln.gui.SlotFilter;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.misc.BasicContainer;
import mods.eln.node.six.SixNodeItemSlot;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ElectricalDataLoggerContainer extends BasicContainer {

	public static final int paperSlotId = 0;
	public static final int printSlotId = 1;
	
	public ElectricalDataLoggerContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory, new Slot[]{
				new SlotFilter(inventory, paperSlotId, 176 / 2 - 44, 125, 64, new ItemStackFilter[]{new ItemStackFilter(Items.paper)}, SlotSkin.medium, new String[]{Translator.translate("eln.core.paper.slot.name")}),
				new GenericItemUsingDamageSlot(inventory, printSlotId, 176 / 2 + 45 - 17, 125, 1, DataLogsPrintDescriptor.class, SlotSkin.medium, new String[]{})
			});
	}
}
