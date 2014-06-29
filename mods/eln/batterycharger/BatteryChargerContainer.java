package mods.eln.batterycharger;

import mods.eln.BasicContainer;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.SlotWithSkinAndComment;
import mods.eln.item.MachineBoosterDescriptor;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class BatteryChargerContainer extends BasicContainer {

	static class BatterySlot extends SlotWithSkinAndComment {

		public BatterySlot(IInventory par1iInventory, int slot, int x, int y) {
			super(par1iInventory, slot, x, y, SlotSkin.medium, new String[]{"Battery Slot"});
		}
		
		public boolean isItemValid(ItemStack itemStack) {
			Object d = Utils.getItemObject(itemStack);
			if(d instanceof IItemEnergyBattery) {
				return true;
			}
			return false;
		}
		
		@Override
		public int getSlotStackLimit() {
			return 1;
		}
	}
	
	public static final int boosterSlotId = 4;
	
	public BatteryChargerContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory, new Slot[]{
				new BatterySlot(inventory, 0, 26 - 18 + 0, 6 + 0),
				new BatterySlot(inventory, 1, 26 - 18 + 18, 6 + 0),
				new BatterySlot(inventory, 2, 26 - 18 + 0, 6 + 18),
				new BatterySlot(inventory, 3, 26 - 18 + 18, 6 + 18),
				new GenericItemUsingDamageSlot(inventory, boosterSlotId, 80 - 18, 6 + 18 / 2, 5,
						MachineBoosterDescriptor.class,
						SlotSkin.medium,
						new String[]{"Booster Slot"})
			});
	}
}
