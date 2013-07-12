package mods.eln.sim;

public interface ITemperatureWatchdogDescriptorForInventory {
	public double getTmax();
	public double getTmin();
	public double getBreakPropPerKelvinOverflow();
}
