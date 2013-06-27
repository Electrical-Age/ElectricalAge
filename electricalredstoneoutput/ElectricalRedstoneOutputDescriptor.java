package mods.eln.electricalredstoneoutput;

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


public class ElectricalRedstoneOutputDescriptor extends SixNodeDescriptor{


	public ElectricalRedstoneOutputDescriptor(
			String name,
			String objName
			) {
		super(name, ElectricalRedstoneOutputElement.class,ElectricalRedstoneOutputRender.class);
		//obj = Eln.instance.obj.getObj(objName);
		
	}

	Obj3D obj;

	void draw(float factor)
	{

	}
	
}
