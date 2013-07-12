package mods.eln.electricalvumeter;

import mods.eln.Eln;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;

import com.google.common.base.Function;


public class ElectricalVuMeterDescriptor extends SixNodeDescriptor{


	public ElectricalVuMeterDescriptor(
			String name,
			String objName
			) {
		super(name, ElectricalVuMeterElement.class,ElectricalVuMeterRender.class);
		obj = Eln.instance.obj.getObj(objName);
		
	}

	Obj3D obj;

	void draw(float factor)
	{
		if(factor < 0.0) factor = 0.0f;
		if(factor > 1.0) factor = 1.0f;
		if(obj.getString("type").equals("rot"))
		{
			obj.draw("Vumeter");
			Obj3DPart pointer = obj.getPart("Pointer");
			float alphaOff,alphaOn;
			alphaOff = pointer.getFloat("alphaOff");
			alphaOn = pointer.getFloat("alphaOn");
			pointer.draw((factor*(alphaOn-alphaOff) + alphaOff), 1.0f, 0, 0);
		}
	}
	
}
