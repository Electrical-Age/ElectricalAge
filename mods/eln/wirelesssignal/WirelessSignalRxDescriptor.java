package mods.eln.wirelesssignal;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;

public class WirelessSignalRxDescriptor extends SixNodeDescriptor{

	public boolean repeater;
	public int range;
	private Obj3D obj;
	Obj3DPart main,led;

	public WirelessSignalRxDescriptor(
			String name,
			Obj3D obj,
			boolean repeater,int range
			) {
		super(name, WirelessSignalRxElement.class, WirelessSignalRxRender.class);
		this.repeater = repeater;
		this.range = range;
		
		this.obj = obj;
		if(obj != null){
			main = obj.getPart("main");
			led = obj.getPart("led");
		}
	}
	
	
	
	public void draw(boolean connection)
	{
		if(main != null) main.draw();
		
		if(led != null){
			Utils.ledOnOffColor(connection);
			Utils.drawLight(led);
		}
	}

}
