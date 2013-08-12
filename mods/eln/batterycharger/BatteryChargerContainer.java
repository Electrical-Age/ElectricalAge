package mods.eln.batterycharger;

import mods.eln.BasicContainer;
import mods.eln.ItemStackFilter;
import mods.eln.SlotFilter;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.generic.SharedItem;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.SlotWithSkinAndComment;
import mods.eln.item.LampSlot;
import mods.eln.item.MachineBoosterDescriptor;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.node.SixNodeItemSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class BatteryChargerContainer extends BasicContainer {

	static class BatterySlot extends SlotWithSkinAndComment
	{

		public BatterySlot(IInventory par1iInventory, int slot, int x,int y) {
			super(par1iInventory, slot, x, y, SlotSkin.medium, new String[]{"Battery slot"});
			// TODO Auto-generated constructor stub
		}
		
		public boolean isItemValid(ItemStack itemStack) {
			Item i = itemStack.getItem();
			if(i instanceof GenericItemUsingDamage){
				GenericItemUsingDamage ii = (GenericItemUsingDamage)i;
				GenericItemUsingDamageDescriptor d = ii.getDescriptor(itemStack);
				if(d instanceof IItemEnergyBattery){
					return true;
				}
			}
			return false;
		}
		
		@Override
		public int getSlotStackLimit() {
			// TODO Auto-generated method stub
			return 1;
		}
		
		
	}
	public static final int boosterSlotId = 4;
	
	public BatteryChargerContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory,new Slot[]{
				new BatterySlot(inventory, 0, 0, 0),
				new BatterySlot(inventory, 18, 0, 1),
				new BatterySlot(inventory, 0, 18, 2),
				new BatterySlot(inventory, 18, 18, 3),
				new GenericItemUsingDamageSlot(inventory,boosterSlotId,50,12,5,
						MachineBoosterDescriptor.class,
						SlotSkin.medium,
						new String[]{"Booster slot"})
			});
		
		// TODO Auto-generated constructor stub
	}

}
