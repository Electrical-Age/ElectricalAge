package mods.eln.sim;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public abstract class ThermalWatchdogProcess implements IProcess{


	
	@Override
	public void process(double time) {
		ITemperatureWatchdogDescriptor descriptor = getWatchdogDescriptor();
		if(descriptor == null) return;
		double temperature = getTemperature();
		if(temperature < descriptor.getTmin()) temperatureUnderFlow(time,descriptor.getTmin() - temperature);
		if(temperature > descriptor.getTmax()) temperatureOverFlow(time,temperature - descriptor.getTmax());
	//	if(voltage < descriptor.getTmin() && Math.random() < time * (descriptor.getTmin() - voltage) * descriptor.getBreakPropPerKelvinOverflow()) temperatureUnderFlow();
//		if(voltage > descriptor.getTmax() && Math.random() < time * (voltage - descriptor.getTmax()) * descriptor.getBreakPropPerKelvinOverflow()) temperatureOverFlow();

		
	}
	
	public abstract double getTemperature();
	public abstract ITemperatureWatchdogDescriptor getWatchdogDescriptor();
	public abstract void temperatureOverFlow(double time,double overflow);
	public abstract void temperatureUnderFlow(double time,double overflow);
	
}


