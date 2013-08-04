package mods.eln.sim;

import mods.eln.Eln;


public class ElectricalConnection{
	public ElectricalConnection(ElectricalLoad L1,ElectricalLoad L2)
	{
		this.L1 = L1;
		this.L2 = L2;
	}
	public ElectricalLoad L1;
	public ElectricalLoad L2;
	
	public double serialConductance; //simulator use only

	public void removeFromSimulator() {
		Eln.simulator.removeElectricalConnection(this);	
	}

	public void addToSimulator() {
		Eln.simulator.addElectricalConnection(this);	
	}
}
