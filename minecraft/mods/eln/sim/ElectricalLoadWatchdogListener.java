package mods.eln.sim;

public interface ElectricalLoadWatchdogListener {
	public void overVoltage(double time,double overflow);
	public void underVoltage(double time,double overflow);
}
