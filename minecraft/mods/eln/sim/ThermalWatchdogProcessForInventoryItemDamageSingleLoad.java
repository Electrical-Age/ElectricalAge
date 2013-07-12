package mods.eln.sim;

import mods.eln.generic.GenericItemUsingDamage;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class ThermalWatchdogProcessForInventoryItemDamageSingleLoad extends ThermalWatchdogProcessForInventory{

	IInventory inventory;
	int slotId;
	ThermalLoad thermallLoad;
	

	
	public ThermalWatchdogProcessForInventoryItemDamageSingleLoad(IInventory inventory,int slotId,ThermalLoad thermallLoad) {
		this.inventory = inventory;
		this.slotId = slotId;
		this.thermallLoad = thermallLoad;
	}
	@Override
	public double getTemperature() {
		// TODO Auto-generated method stub
		return thermallLoad.Tc;
	}

	@Override
	public ITemperatureWatchdogDescriptorForInventory getWatchdogDescriptor() {
		ItemStack stack = inventory.getStackInSlot(slotId);
		if(stack == null) return null;
		GenericItemUsingDamage item = (GenericItemUsingDamage) stack.getItem();
		return (ITemperatureWatchdogDescriptorForInventory) item.getDescriptor(stack);
	}
	@Override 
	public void temperatureOverFlow(double time,double overflow) {
		// TODO Auto-generated method stub
		inventory.setInventorySlotContents(slotId, null);
	}
	@Override
	public void temperatureUnderFlow(double time,double overflow) {
		// TODO Auto-generated method stub
		inventory.setInventorySlotContents(slotId, null);
		
	}


}
