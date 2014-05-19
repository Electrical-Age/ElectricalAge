package mods.eln.waterturbine;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.wiki.Data;

public class WaterTurbineDescriptor extends TransparentNodeDescriptor {



	public WaterTurbineDescriptor(
			String name,Obj3D obj,
			ElectricalCableDescriptor cable,
			double nominalPower,
			double maxVoltage,
			Coordonate waterCoord,
			String soundName,
			float nominalVolume
			) {
		super(name, WaterTurbineElement.class, WaterTurbineRender.class);

		this.cable = cable;
		this.nominalPower = nominalPower;
		this.maxVoltage = maxVoltage;
		this.waterCoord = waterCoord;
		this.soundName = soundName;
		this.nominalVolume = nominalVolume;
		
		this.obj = obj;
		if(obj != null){
			wheel = obj.getPart("Wheel");
			support = obj.getPart("Support");
			generator = obj.getPart("Generator");
			speed = 60;
		}
	}
	
	Coordonate waterCoord;
	
	public void setParent(net.minecraft.item.Item item, int damage)
	{
		super.setParent(item, damage);
		Data.addEnergy(newItemStack());
	}

	

	
	Obj3DPart wheel,support,generator;
	
	Obj3D obj;
	public ElectricalCableDescriptor cable;
	public double nominalPower;


	public double maxVoltage;
	
	public float speed;
	
	public String soundName;
	public float nominalVolume;


	public FunctionTable PfW;
	
	public void draw(float alpha) {
		if(support != null) support.draw();
		if(generator != null) generator.draw();
		if(wheel != null) wheel.draw(alpha,1f,0f,0f);
	}


	
	@Override
	public Direction getFrontFromPlace(Direction side,
			EntityLivingBase entityLiving) {
		return super.getFrontFromPlace(side, entityLiving);
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
		
		list.add("Produces power from water.");
		list.add(Utils.plotVolt("Voltage:", cable.electricalNominalVoltage));
		list.add(Utils.plotPower("Power:", nominalPower));

	}
	
	public Coordonate getWaterCoordonate(World w) {
		Coordonate coord = new Coordonate(waterCoord);
		coord.setDimention(w.provider.dimensionId);
		return coord;
	}
	
	
	
	@Override
	public String checkCanPlace(Coordonate coord, Direction front) {
		// TODO Auto-generated method stub
		String str = super.checkCanPlace(coord, front);
		if(str != null) return str;
		if(checkCanPlaceWater(coord, front) == false) return "No place for water";
		return str;
	}
	
	

	public boolean checkCanPlaceWater(Coordonate coord, Direction front) {
		Coordonate water = new Coordonate(waterCoord);
		water.applyTransformation(front, coord);
		if(coord.getBlockExist() == false) return true;
		if(water.getBlock() == null || Utils.isWater(water)) return true;
		return false;
	}
}

