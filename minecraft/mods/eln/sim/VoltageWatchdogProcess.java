package mods.eln.sim;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public abstract class VoltageWatchdogProcess implements IProcess{


	
	@Override
	public void process(double time) {
		IVoltageWatchdogDescriptor descriptor = getWatchdogDescriptor();
		if(descriptor == null) return;
		double voltage = getVoltage();
		if(voltage < descriptor.getUmin()) voltageUnderFlow(time,descriptor.getUmin() - voltage);
		if(voltage > descriptor.getUmax()) voltageOverFlow(time,voltage - descriptor.getUmax());

		
	}
	
	public abstract double getVoltage();
	public abstract IVoltageWatchdogDescriptor getWatchdogDescriptor();
	public abstract void voltageOverFlow(double time,double overflow);
	public abstract void voltageUnderFlow(double time,double overflow);
	
}


