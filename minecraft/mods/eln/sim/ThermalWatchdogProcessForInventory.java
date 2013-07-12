package mods.eln.sim;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public abstract class ThermalWatchdogProcessForInventory implements IProcess{


	
	@Override
	public void process(double time) {
		ITemperatureWatchdogDescriptorForInventory descriptor = getWatchdogDescriptor();
		if(descriptor == null) return;
		double temperature = getTemperature();
		if(temperature < descriptor.getTmin() && Math.random() < time * (descriptor.getTmin() - temperature) * descriptor.getBreakPropPerKelvinOverflow()) temperatureUnderFlow(time,descriptor.getTmin() - temperature);
		if(temperature > descriptor.getTmax() && Math.random() < time * (temperature - descriptor.getTmax()) * descriptor.getBreakPropPerKelvinOverflow()) temperatureOverFlow(time,temperature - descriptor.getTmax());

		
	}
	
	public abstract double getTemperature();
	public abstract ITemperatureWatchdogDescriptorForInventory getWatchdogDescriptor();
	public abstract void temperatureOverFlow(double time,double overflow);
	public abstract void temperatureUnderFlow(double time,double overflow);
	
}


