package mods.eln.sim;

public class DiodeHeatingThermalLoadProcess implements IProcess{

	DiodeProcess diode;
	ThermalLoad load;
	public DiodeHeatingThermalLoadProcess(DiodeProcess diode,ThermalLoad load) {
		this.diode = diode;
		this.load = load;
	}
	
	
	@Override
	public void process(double time) {
		load.PcTemp += diode.getDissipatedPower();
	}

}
