package mods.eln.electricalregulator;

import mods.eln.sim.ElectricalSourceProcess;
import mods.eln.sim.RegulatorProcess;

public class ElectricalRegulatorFastProcess extends RegulatorProcess {

	public ElectricalRegulatorFastProcess(ElectricalRegulatorElement element) {
		super("TheFastProcess");
		this.source = element.outputGateProcess;
		this.Umax = element.descriptor.outputGateUmax;
		this.Umin = element.descriptor.outputGateUmin;
		this.Udelta = Umax - Umin;
	}
	
	ElectricalRegulatorElement element;
	
	ElectricalSourceProcess source;
	double Umax, Umin, Udelta;

	@Override
	protected  double getHit() {
		return 0;
	}

	@Override
	protected void setCmd(double cmd) {
		source.U = Umin + Udelta * cmd;
	}
}
