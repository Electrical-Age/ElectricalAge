package mods.eln.sim;

import mods.eln.Eln;



public class ElectricalLoadCableHeatThermalLoadProcess implements IProcess {
	ElectricalLoad 	eLoad;
	ThermalLoad		tLoad;
	

	public ElectricalLoadCableHeatThermalLoadProcess(ElectricalLoad eLoad,ThermalLoad tLoad)
	{
		this.eLoad = eLoad;
		this.tLoad = tLoad;
	}
	
	@Override
	public void process(double time) {
		double PMax = Eln.electricalCableDeltaTMax*tLoad.C;
		tLoad.PcTemp += Math.min(PMax,(eLoad.IrsPow2*eLoad.getRs() + eLoad.Uc*eLoad.Uc/eLoad.getRp()));
		
	}
	

}
