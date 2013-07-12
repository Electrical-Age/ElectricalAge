package mods.eln.sim;

public class ThermalRegulator implements IProcess{
	
	//public enum RegulatorType{none,onOff,analog,mechanical};
	
	public RegulatorType type = RegulatorType.none;

	ElectricalResistor electricalResistor;
	ThermalLoad thermalLoad;
	
	public double thermalTarget = 0;
	
	public double OnOffResistance = 1000000000.0;
	public double OnOffDelta = 10;
	
	public ThermalRegulator(ThermalLoad thermalLoad,ElectricalResistor electricalResistor) 
	{
		this.thermalLoad = thermalLoad;
		this.electricalResistor = electricalResistor;
	}

	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		switch (type) {
		case analog:
			electricalResistor.highImpedance();
			break;
		case none:
			electricalResistor.setR(OnOffResistance);
			break;
		case onOff:
			if(thermalLoad.Tc > thermalTarget + OnOffDelta) electricalResistor.highImpedance();
			if(thermalLoad.Tc < thermalTarget - OnOffDelta) electricalResistor.setR(OnOffResistance);
			break;
		default:
			break;

		}
	}
	


}
