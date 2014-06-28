package mods.eln.electricalrelay;

import mods.eln.node.NodeElectricalGateInput;
import mods.eln.sim.NodeElectricalGateInputHysteresisProcess;

public class ElectricalRelayGateProcess extends NodeElectricalGateInputHysteresisProcess {

	ElectricalRelayElement element;
	public ElectricalRelayGateProcess(ElectricalRelayElement element, String name, NodeElectricalGateInput gate) {
		super(name, gate);
		this.element = element;
	}

	@Override
	protected void setOutput(boolean value) {
		element.setSwitchState(value ^ element.defaultOutput);
	}
}
