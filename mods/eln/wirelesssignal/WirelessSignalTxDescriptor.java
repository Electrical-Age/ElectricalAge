package mods.eln.wirelesssignal;

import mods.eln.node.SixNodeDescriptor;

public class WirelessSignalTxDescriptor extends SixNodeDescriptor{

	public WirelessSignalTxDescriptor(
			String name,
			int range
			) {
		super(name, WirelessSignalTxElement.class, WirelessSignalTxRender.class);
		this.range = range;
	}
	int range;
}
