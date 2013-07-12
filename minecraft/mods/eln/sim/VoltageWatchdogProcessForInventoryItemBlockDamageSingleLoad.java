package mods.eln.sim;

import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.generic.GenericItemUsingDamage;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class VoltageWatchdogProcessForInventoryItemBlockDamageSingleLoad extends VoltageWatchdogProcessForInventory{

	IInventory inventory;
	int slotId;
	ElectricalLoad electricalLoad;
	

	
	public VoltageWatchdogProcessForInventoryItemBlockDamageSingleLoad(IInventory inventory,int slotId,ElectricalLoad electricalLoad) {
		this.inventory = inventory;
		this.slotId = slotId;
		this.electricalLoad = electricalLoad;
	}
	@Override
	public double getVoltage() {
		// TODO Auto-generated method stub
		return electricalLoad.Uc;
	}

	@Override
	public IVoltageWatchdogDescriptorForInventory getWatchdogDescriptor() {
		ItemStack stack = inventory.getStackInSlot(slotId);
		if(stack == null) return null;
		GenericItemBlockUsingDamage item = (GenericItemBlockUsingDamage) stack.getItem();
		return (IVoltageWatchdogDescriptorForInventory) item.getDescriptor(stack);
	}
	@Override 
	public void voltageOverFlow(double time,double overflow) {
		// TODO Auto-generated method stub
		inventory.setInventorySlotContents(slotId, null);
	}
	@Override
	public void voltageUnderFlow(double time,double overflow) {
		// TODO Auto-generated method stub
		inventory.setInventorySlotContents(slotId, null);
		
	}


}
