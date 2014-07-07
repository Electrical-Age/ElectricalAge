package mods.eln.sim.nbt;

import mods.eln.Eln;
import mods.eln.misc.Utils;

public class NbtElectricalGateOutput extends NbtElectricalLoad{

	public NbtElectricalGateOutput(String name) {
		super(name);
		Eln.instance.signalCableDescriptor.applyTo(this);
	}
	public String plot(String str)
	{
		return str + " " + Utils.plotVolt("", getU()) + Utils.plotAmpere("", getCurrent());
	}
	
}
