package mods.eln.sim;



public class ElectricalLoadHeatThermalLoadProcess implements IProcess {
	ElectricalLoad 	eLoad;
	ThermalLoad		tLoad;
	
	public ElectricalLoadHeatThermalLoadProcess(ElectricalLoad eLoad,ThermalLoad tLoad)
	{
		this.eLoad = eLoad;
		this.tLoad = tLoad;
	}

	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		tLoad.PcTemp += (eLoad.IrsPow2*eLoad.getRs() + eLoad.Uc*eLoad.Uc/eLoad.getRp());
		
	}
	
	

}
