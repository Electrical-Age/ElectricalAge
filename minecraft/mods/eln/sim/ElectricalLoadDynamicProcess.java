package mods.eln.sim;

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
		if(electricalLoad.getRs() < RsMin) electricalLoad.setRp(RsMin);
		if(Math.abs(electricalLoad.Uc) < dielectricVoltage)
		{
			electricalLoad.setRp(RpOffset);
		}
		else
		{
			double RpTh  = (dielectricBreakOhm - Math.abs(electricalLoad.Uc - dielectricVoltage)*dielectricBreakOhmPerVolt);
			if(RpTh < dielectricBreakOhmMin) RpTh = dielectricBreakOhmMin;		
			
			electricalLoad.setRp(1/(1/RpOffset + 1/RpTh));
		}
		
	}
	
}
