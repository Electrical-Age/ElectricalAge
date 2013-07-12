package mods.eln.sim;

public class ElectricalLoadWatchdogProcess implements IProcess{
	ElectricalLoad electricalLoad;
	public double positiveLimit =  Double.POSITIVE_INFINITY;
	public double negativeLimit =  Double.NEGATIVE_INFINITY;
	
	public ElectricalLoadWatchdogListener listener;
	public ElectricalLoadWatchdogProcess(ElectricalLoad electricalLoad,ElectricalLoadWatchdogListener listener)
	{
		this.electricalLoad = electricalLoad;
		this.listener = listener;
	}
	
	@Override
	public void process(double time) {
		if(electricalLoad.Uc < negativeLimit) 
		{
			underVoltage(time,negativeLimit-electricalLoad.Uc);
			return; 
		}
		if(electricalLoad.Uc > positiveLimit) 
		{
			overVoltage(time,electricalLoad.Uc - positiveLimit);
			return; 
		}
	}
	
	
	public void overVoltage(double time,double overflow)
	{
		if(listener != null)listener.overVoltage(time,overflow);
	}
	public void underVoltage(double time,double overflow)
	{
		if(listener != null)listener.underVoltage(time,overflow);
	}
	
}
