package mods.eln.teleporter;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.TransparentNodeDescriptor;

public class TeleporterDescriptor extends TransparentNodeDescriptor{

	private Obj3D obj;
	private Obj3DPart main;


	public TeleporterDescriptor(
			String name,Obj3D obj,
			ElectricalCableDescriptor cable,
			Coordonate areaCoordonate,
			int areaH,
			Coordonate[] powerCoordonate
			
			) {
		super(name, TeleporterElement.class, TeleporterRender.class);
		this.cable = cable;
		this.obj = obj;
		this.powerCoordonate = powerCoordonate;
		if(obj != null){
			main = obj.getPart("main");
		}
		this.areaCoordonate = areaCoordonate;
		this.areaH = areaH;
	}

	int areaH;
	public Coordonate areaCoordonate;
	
	public AxisAlignedBB getBB(Coordonate c,Direction front){
		Coordonate temp = new Coordonate(areaCoordonate);
		temp.setDimention(c.dimention);
		temp.applyTransformation(front, c);
		
		AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(temp.x,temp.y,temp.z,temp.x+1,temp.y + areaH,temp.z +1);
		return bb;
	}
	
	public Coordonate getTeleportCoordonate(Direction front,Coordonate c) {
		Coordonate temp = new Coordonate(areaCoordonate);
		temp.setDimention(c.dimention);
		temp.applyTransformation(front, c);
		
		return temp;
	}
	
	public ElectricalCableDescriptor cable;


	public void draw() {
		if(main != null) main.draw();
	}

	Coordonate[] powerCoordonate;
	public Coordonate[] getPowerCoordonate(World w) {
		Coordonate[] temp = new Coordonate[powerCoordonate.length];
		for(int idx = 0;idx < temp.length;idx++){
			temp[idx] = new Coordonate(powerCoordonate[idx]);
			temp[idx].setDimention(w.provider.dimensionId);
		}
		return temp;
	}

}
