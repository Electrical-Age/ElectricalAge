package mods.eln.heatfurnace;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.sim.FurnaceProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.ThermalLoadInitializerByPowerDrop;

public class HeatFurnaceDescriptor extends TransparentNodeDescriptor{

	public HeatFurnaceDescriptor(
			String name,String modelName,
			double nominalPower, double nominalCombustibleEnergy,
			int combustionChamberMax,double combustionChamberPower,
			ThermalLoadInitializerByPowerDrop thermal
			) {
		super(name,HeatFurnaceElement.class,HeatFurnaceRender.class);
		this.thermal = thermal;
		this.nominalCombustibleEnergy = nominalCombustibleEnergy;
		this.combustionChamberMax = combustionChamberMax;
		this.combustionChamberPower = combustionChamberPower;
		this.nominalPower = nominalPower;
		obj = Eln.obj.getObj(modelName);
		if(obj != null)
		{
			tiroir = obj.getPart("tiroir");
			main = obj.getPart("main");
			
			if(tiroir != null)
			{
				alphaClose = tiroir.getFloat("alphaClose");
				alphaOpen = tiroir.getFloat("alphaOpen");
			}
	
			if(main != null)
			{
				flameStartX = main.getFloat("flameStartX");
				flameStartY = main.getFloat("flameStartY");
				flameStartZ = main.getFloat("flameStartZ");
				
				flameEndX = main.getFloat("flameEndX");
				flameEndY = main.getFloat("flameEndY");
				flameEndZ = main.getFloat("flameEndZ");
				
				
				flameDeltaX = flameEndX - flameStartX;
				flameDeltaY = flameEndY - flameStartY;
				flameDeltaZ = flameEndZ - flameStartZ;
				flamePopRate = main.getFloat("flamePopRate");
			}
		}
		thermal.setMaximalPower(nominalPower);
		
		
	}
	Obj3D obj;
	Obj3DPart tiroir,main;
	float alphaClose,alphaOpen;
	public void applyTo(ThermalLoad load)
	{
		thermal.applyTo(load);
	}
	public void applyTo(FurnaceProcess process)
	{
		process.nominalCombustibleEnergy = nominalCombustibleEnergy;
		process.nominalPower = nominalPower;
	}
	
	double nominalPower;
	double nominalCombustibleEnergy;
	int combustionChamberMax;
	double combustionChamberPower;
	ThermalLoadInitializerByPowerDrop thermal;

	double flameStartX,flameStartY,flameStartZ;
	double flameEndX,flameEndY,flameEndZ;
	double flameDeltaX,flameDeltaY,flameDeltaZ;
	double flamePopRate;
	void draw(float tiroirFactor)
	{
		//GL11.glDisable(GL11.GL_T4$5EXTURE_2D);
		//GL11.glDisable(GL11.GL_CULL_FACE);
		//GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
	//	GL11.glColor3f(0.5f,0.5f,0.5f);
		if(main != null) main.draw();
		if(tiroir != null) tiroir.draw(alphaClose + tiroirFactor * (alphaOpen - alphaClose),0f, 0f, 1f);
		//GL11.glEnable(GL11.GL_TEXTURE_2D);
		//GL11.glCullFace(GL11.GL_BACK);
	}
	
	
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		draw(1.0f);
	}
	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Provide heat when");
		list.add("consume fuel");
		list.add(Utils.plotPower("Power :", nominalPower));
		list.add(Utils.plotCelsius("Tmax :",thermal.warmLimit));
	}
}
