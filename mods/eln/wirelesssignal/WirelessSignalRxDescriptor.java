package mods.eln.wirelesssignal;

import mods.eln.node.SixNodeDescriptor;

public class WirelessSignalRxDescriptor extends SixNodeDescriptor{

	public boolean repeater;
	public int range;

	public WirelessSignalRxDescriptor(
			String name,
			boolean repeater,int range
			) {
		super(name, WirelessSignalRxElement.class, WirelessSignalRxRender.class);
		this.repeater = repeater;
		this.range = range;
	}

}
