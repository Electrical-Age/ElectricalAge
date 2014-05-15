package mods.eln.windturbine;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Direction;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.wiki.Data;

public class WindTurbineDescriptor extends TransparentNodeDescriptor {



	public WindTurbineDescriptor(
			String name,Obj3D obj,
			ElectricalCableDescriptor cable,
			FunctionTable PfW,
			double nominalPower,double nominalWind,
			double maxVoltage, double maxWind,
			int offY,
			int rayX,int rayY,int rayZ,
			int blockMalusMinCount,double blockMalus, 
			String soundName, float nominalVolume
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
		this.soundName = soundName;
		this.nominalVolume = nominalVolume;
		this.PfW = PfW.duplicate(nominalWind, nominalPower);
		this.obj = obj;
		if(obj != null){
			main = obj.getPart("main");
			rot = obj.getPart("rot");
			if(rot != null){
				speed = rot.getFloat("speed");
			}
		}
	}
	
	public void setParent(net.minecraft.item.Item item, int damage)
	{
		super.setParent(item, damage);
		Data.addEnergy(newItemStack());
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
	public String soundName;
	public float nominalVolume;

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
	
	
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		objItemScale(obj);
		Direction.ZN.glRotateXnRef();
		draw(0f);
	}
	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add("Produces power from wind.");
		list.add(Utils.plotVolt("Voltage:", maxVoltage));
		list.add(Utils.plotPower("Power:", nominalPower));
		list.add("Wind area:");
		list.add("  Front: " + rayX);
		list.add("  Up/Down: " + rayY);
		list.add("  Left/Right: " + rayZ);
	}
}

