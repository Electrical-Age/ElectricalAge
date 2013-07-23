package mods.eln.wirelesssignal;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;

public class WirelessSignalTxDescriptor extends SixNodeDescriptor{

	private Obj3D obj;
	Obj3DPart main;

	public WirelessSignalTxDescriptor(
			String name,
			Obj3D obj,
			int range
			) {
		super(name, WirelessSignalTxElement.class, WirelessSignalTxRender.class);
		this.range = range;
		this.obj = obj;
		if(obj != null) main = obj.getPart("main");
	}
	int range;
	
	
	public void draw()
	{
		if(main != null) main.draw();
	}
}
