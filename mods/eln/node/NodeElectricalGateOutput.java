package mods.eln.node;

import mods.eln.Eln;
import mods.eln.misc.Utils;

public class NodeElectricalGateOutput extends NodeElectricalLoad{

	public NodeElectricalGateOutput(String name) {
		super(name);
		Eln.instance.signalCableDescriptor.applyTo(this);
	}
	public String plot(String str)
	{
		return str + " " + Utils.plotVolt("", getU()) + Utils.plotAmpere("", getCurrent());
	}
	
}
