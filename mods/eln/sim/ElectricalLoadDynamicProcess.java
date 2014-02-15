package mods.eln.sim;

import mods.eln.Eln;

public class ElectricalLoadDynamicProcess implements IProcess {

	ElectricalLoad electricalLoad;	
	ThermalLoad thermalLoad;
	
	public double RsOffset = Double.POSITIVE_INFINITY;
	public double RpOffset = Double.POSITIVE_INFINITY;
	
	public double RsPerDegree = 0;
	public double dielectricBreakOhmPerVolt = 0;
	public double dielectricBreakOhm = Double.POSITIVE_INFINITY;
	public double dielectricVoltage = 0;
	
	public double dielectricBreakOhmMin = 1.0;
	public double RsMin = 0.001;
	
	public ElectricalLoadDynamicProcess(ElectricalLoad electricalLoad,ThermalLoad thermalLoad)
	{
		this.electricalLoad = electricalLoad;
		this.thermalLoad = thermalLoad;
	}
	
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		electricalLoad.setRs(RsOffset + RsPerDegree * thermalLoad.Tc);
		if(electricalLoad.getRs() < RsMin) electricalLoad.setRs(RsMin);
		
		if(electricalLoad.getSimplifyAuthorized() == true){
			if(Math.abs(electricalLoad.Uc) > dielectricVoltage*1.05){
				electricalLoad.setSimplifyAuthorized(false);
				Eln.simulator.workingGenerated = false;
			}
		}
		else{
			if(Math.abs(electricalLoad.Uc) < dielectricVoltage*1.00){
				electricalLoad.setSimplifyAuthorized(true);
				Eln.simulator.workingGenerated = false;
			}			
		}
		
		
		if(electricalLoad.getSimplifyAuthorized() == true){
			electricalLoad.setRp(RpOffset);
		}
		else {

			double RpTh  = dielectricBreakOhm*Math.pow(dielectricBreakOhmPerVolt,Math.abs(electricalLoad.Uc) - dielectricVoltage*1.00);//(dielectricBreakOhm - Math.abs(electricalLoad.Uc - dielectricVoltage*1.00)*dielectricBreakOhmPerVolt);
		//	if(RpTh > dielectricBreakOhmMin) RpTh = dielectricBreakOhmMin;		
			
			RpTh = Math.max(RsMin*3,1/(1/RpOffset + 1/RpTh));
			electricalLoad.setRp(RpTh);
		}
		
	}
	
}
