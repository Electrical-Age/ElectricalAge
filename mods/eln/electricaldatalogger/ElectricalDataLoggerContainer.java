package mods.eln.electricaldatalogger;

import mods.eln.BasicContainer;
import mods.eln.ItemStackFilter;
import mods.eln.SlotFilter;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.LampSlot;
import mods.eln.node.SixNodeItemSlot;
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
				new SlotFilter(inventory, paperSlotId, 176 / 2 - 44, 125, 64, new ItemStackFilter[]{new ItemStackFilter(Items.paper)}, SlotSkin.medium, new String[]{"Paper Slot"}),
				new GenericItemUsingDamageSlot(inventory, printSlotId, 176 / 2 + 45 - 17, 125, 1, DataLogsPrintDescriptor.class, SlotSkin.medium, new String[]{})
			});
	}
}
