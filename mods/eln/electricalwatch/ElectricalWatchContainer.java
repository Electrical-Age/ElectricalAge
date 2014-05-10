package mods.eln.electricalwatch;

import mods.eln.BasicContainer;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.SlotWithSkin;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.item.HeatingCorpElement;
import mods.eln.item.MaceratorSorterDescriptor;
import mods.eln.item.MachineBoosterDescriptor;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.electricalitem.BatteryItem;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.RegulatorSlot;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import mods.eln.sim.RegulatorType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class ElectricalWatchContainer extends BasicContainer {

	public static final int batteryId = 0;
	public ElectricalWatchContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory, new Slot[]{
				new GenericItemUsingDamageSlot(inventory, batteryId, 184 / 2 - 12, 8, 1,
												BatteryItem.class,
												SlotSkin.medium,
												new String[]{"Portable battery slot"}),
			});
	}
}
/*				new SlotFilter(inventory, 0, 62 + 0, 17, new ItemStackFilter[]{new ItemStackFilter(Block.wood, 0, 0)}),
				new SlotFilter(inventory, 1, 62 + 18, 17, new ItemStackFilter[]{new ItemStackFilter(Item.coal, 0, 0)})
*/