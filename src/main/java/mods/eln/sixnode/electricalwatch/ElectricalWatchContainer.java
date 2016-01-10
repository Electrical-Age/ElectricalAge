package mods.eln.sixnode.electricalwatch;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.electricalitem.BatteryItem;
import mods.eln.misc.BasicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class ElectricalWatchContainer extends BasicContainer {

	public static final int batteryId = 0;

	public ElectricalWatchContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory, new Slot[]{
			new GenericItemUsingDamageSlot(inventory, batteryId, 184 / 2 - 12, 8, 1, BatteryItem.class, SlotSkin.medium,
				new String[]{tr("Portable battery slot")})});
	}
}
/*				new SlotFilter(inventory, 0, 62 + 0, 17, new ItemStackFilter[]{new ItemStackFilter(Block.wood, 0, 0)}),
				new SlotFilter(inventory, 1, 62 + 18, 17, new ItemStackFilter[]{new ItemStackFilter(Item.coal, 0, 0)})
*/
