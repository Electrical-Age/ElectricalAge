package mods.eln.sim;

import mods.eln.Eln;

public class ElectricalResistorCableHeatThermalLoad implements IProcess{
	ElectricalResistor electricalResistor;
	ThermalLoad thermalLoad;
	
	public ElectricalResistorCableHeatThermalLoad(ElectricalResistor electricalResistor,ThermalLoad thermalLoad)
	{
		this.electricalResistor = electricalResistor;
		this.thermalLoad = thermalLoad;
	}

	@Override
	public void process(double time) {
		double PMax = Eln.electricalCableDeltaTMax*thermalLoad.C;
		thermalLoad.PcTemp += Math.min(PMax,electricalResistor.getP());
	}
}
