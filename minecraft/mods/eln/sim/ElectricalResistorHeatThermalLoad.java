package mods.eln.sim;

public class ElectricalResistorHeatThermalLoad implements IProcess{
	ElectricalResistor electricalResistor;
	ThermalLoad thermalLoad;
	
	public ElectricalResistorHeatThermalLoad(ElectricalResistor electricalResistor,ThermalLoad thermalLoad)
	{
		this.electricalResistor = electricalResistor;
		this.thermalLoad = thermalLoad;
	}

	@Override
	public void process(double time) {
		thermalLoad.PcTemp += electricalResistor.getP();
	}
}
