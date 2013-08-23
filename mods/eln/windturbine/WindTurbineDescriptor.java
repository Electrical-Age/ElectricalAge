package mods.eln.windturbine;

import net.minecraft.entity.EntityLivingBase;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Direction;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.TransparentNodeDescriptor;

public class WindTurbineDescriptor extends TransparentNodeDescriptor {



	public WindTurbineDescriptor(
			String name,Obj3D obj,
			ElectricalCableDescriptor cable,
			FunctionTable PfW,
			double nominalPower,double nominalWind,
			double maxVoltage, double maxWind,
			int offY,
			int rayX,int rayY,int rayZ,
			int blockMalusMinCount,double blockMalus
			) {
		super(name, WindTurbineElement.class, WindTurbineRender.class);

		this.cable = cable;
		this.nominalPower = nominalPower;
		this.nominalWind = nominalWind;
		this.maxVoltage = maxVoltage;
		this.maxWind = maxWind;
		this.offY = offY;
		this.rayX = rayX;
		this.rayY = rayY;
		this.rayZ = rayZ;
		this.blockMalusSubCount = blockMalusMinCount + 1;
		this.blockMalus = blockMalus;
		this.PfW = PfW.duplicate(nominalWind, nominalPower);
		
		if(obj != null){
			main = obj.getPart("main");
			rot = obj.getPart("rot");
			if(rot != null){
				speed = rot.getFloat("speed");
			}
		}
	}
	
	public double speed;
	
	public void setGhostGroup(GhostGroup ghostGroup)
	{
		blockMalusSubCount += ghostGroup.size();
		this.ghostGroup = ghostGroup;
	}
	
	
	Obj3DPart main,rot;
	
	Obj3D obj;
	public ElectricalCableDescriptor cable;
	public double nominalPower, nominalWind;
	public double maxVoltage,  maxWind;
	public int offY,rayX, rayY, rayZ;
	public int blockMalusSubCount;
	public double blockMalus;

	public FunctionTable PfW;
	
	public void draw(float alpha) {
		if(main != null) main.draw();
		if(rot != null) rot.draw(alpha,1f,0f,0f);
	}


	
	@Override
	public Direction getFrontFromPlace(Direction side,
			EntityLivingBase entityLiving) {
		return Direction.XN;
	}
}
