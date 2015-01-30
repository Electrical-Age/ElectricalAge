package mods.eln.sim.nbt;

import mods.eln.Eln;
import mods.eln.misc.Utils;

public class NbtElectricalGateOutput extends NbtElectricalLoad {

	public NbtElectricalGateOutput(String name) {
		super(name);
		Eln.instance.signalCableDescriptor.applyTo(this);
	}

	public String plot(String str) {
		return Utils.plotSignal(getU(), getCurrent()); //return //str + " " + Utils.plotVolt("", getU()) + Utils.plotAmpere("", getCurrent());
	}
}
