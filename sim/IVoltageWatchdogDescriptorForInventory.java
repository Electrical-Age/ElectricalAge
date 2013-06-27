package mods.eln.sim;

public interface IVoltageWatchdogDescriptorForInventory {
	public double getUmax();
	public double getUmin();
	public double getBreakPropPerVoltOverflow();
}
