package mods.eln.electricaldatalogger;

import mods.eln.BasicContainer;
import mods.eln.ItemStackFilter;
import mods.eln.SlotFilter;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.GuiHelper;
import mods.eln.item.LampSlot;
import mods.eln.node.SixNodeItemSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ElectricalDataLoggerContainer extends BasicContainer {

	public static final int paperSlotId = 0;
	public static final int printSlotId = 1;
	
	public ElectricalDataLoggerContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
				new SlotFilter(inventory,paperSlotId,8,8,64,new ItemStackFilter[]{new ItemStackFilter(Item.paper)}),
				new GenericItemUsingDamageSlot(inventory, printSlotId,20,8, 1, DataLogsPrintDescriptor.class)
			});
		
		// TODO Auto-generated constructor stub
	}

}
