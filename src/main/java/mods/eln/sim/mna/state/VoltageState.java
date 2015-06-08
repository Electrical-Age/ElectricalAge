package mods.eln.sim.mna.state;

import mods.eln.sim.mna.primitives.Voltage;

public class VoltageState extends State {

	public double getU() {
		return state;
	}

	public void setU(double state) {
		this.state = state;
	}
}
