package mods.eln.sim;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public abstract class VoltageWatchdogProcessForInventory implements IProcess{


	
	@Override
	public void process(double time) {
		IVoltageWatchdogDescriptorForInventory descriptor = getWatchdogDescriptor();
		if(descriptor == null) return;
		double voltage = getVoltage();
		if(voltage < descriptor.getUmin() && Math.random() < time * (descriptor.getUmin() - voltage) * descriptor.getBreakPropPerVoltOverflow()) voltageUnderFlow(time,descriptor.getUmin() - voltage);
		if(voltage > descriptor.getUmax() && Math.random() < time * (voltage - descriptor.getUmax()) * descriptor.getBreakPropPerVoltOverflow()) voltageOverFlow(time,voltage - descriptor.getUmax());

		
	}
	
	public abstract double getVoltage();
	public abstract IVoltageWatchdogDescriptorForInventory getWatchdogDescriptor();
	public abstract void voltageOverFlow(double time,double overflow);
	public abstract void voltageUnderFlow(double time,double overflow);
	
}


