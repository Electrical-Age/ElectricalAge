package mods.eln.electricalmath;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;

public class ElectricalMathDescriptor extends SixNodeDescriptor {

	public ElectricalMathDescriptor(
			String name,
			Obj3D obj
			) {
		super(name, ElectricalMathElement.class, ElectricalMathRender.class);
		this.obj = obj;
		if(obj != null){
			main = obj.getPart("main");
		}
	}

	Obj3D obj;
	Obj3DPart main;
	
	void draw()
	{
		if(main != null) main.draw();
	}
	
}
