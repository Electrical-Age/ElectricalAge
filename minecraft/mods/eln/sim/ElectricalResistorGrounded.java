package mods.eln.sim;

public class ElectricalResistorGrounded extends ElectricalResistor{

	
	public ElectricalResistorGrounded(ElectricalLoad a) {
		super(a, ElectricalLoad.groundLoad);
	}
	
	
}
