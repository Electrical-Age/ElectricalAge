package mods.eln.sixnode.electricalrelay;

import mods.eln.sim.NodeElectricalGateInputHysteresisProcess;
import mods.eln.sim.nbt.NbtElectricalGateInput;

public class ElectricalRelayGateProcess extends NodeElectricalGateInputHysteresisProcess {

	ElectricalRelayElement element;

	public ElectricalRelayGateProcess(ElectricalRelayElement element, String name, NbtElectricalGateInput gate) {
		super(name, gate);
		this.element = element;
	}

	@Override
	protected void setOutput(boolean value) {
		element.setSwitchState(value ^ element.defaultOutput);
	}
}
