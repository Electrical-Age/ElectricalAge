package mods.eln.turbine;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.TurbineThermalProcess;
import mods.eln.sound.SoundCommand;
import mods.eln.sound.SoundTrack;
import mods.eln.wiki.Data;

public class TurbineDescriptor extends TransparentNodeDescriptor{

	CableRenderDescriptor eRender;
	public TurbineDescriptor(String name,String modelName,String description,
			CableRenderDescriptor eRender,
			FunctionTable TtoU,
			FunctionTable PoutToPin,
			double nominalDeltaT, double nominalU,double nominalP,double nominalPowerLost,
			double maxU,
			double electricalRs,double electricalRp,double electricalC,
			double thermalC,double DeltaTForInput,
			double powerOutPerDeltaU,
			SoundCommand sound
			) 
	{
		super(name, TurbineElement.class, TurbineRender.class);
		double nominalEff =  Math.abs(1 - (0 + PhysicalConstant.Tref)/(nominalDeltaT + PhysicalConstant.Tref));
		this.TtoU = TtoU;
		this.PoutToPin = PoutToPin;
		this.nominalDeltaT = nominalDeltaT;
		this.nominalU = nominalU; 
		this.nominalP = nominalP;
		this.thermalC = thermalC;
		this.thermalRs = DeltaTForInput/(nominalP / nominalEff);
		this.thermalRp = nominalDeltaT/nominalPowerLost;
		this.electricalRs = electricalRs;
		this.electricalRp = electricalRp;
		this.electricalC = electricalC;
		this.powerOutPerDeltaU = powerOutPerDeltaU;
		this.eRender = eRender;
		this.maxU = maxU;
		this.sound = sound;
		obj = Eln.obj.getObj(modelName);
		if(obj != null)
		{
			main = obj.getPart("main");
		}
	}
	Obj3D obj;
	Obj3DPart main;
	
	public double powerOutPerDeltaU;
	public FunctionTable TtoU;
	public FunctionTable PoutToPin;
	public double nominalDeltaT,nominalU; 
	double nominalP;
	public double thermalC,thermalRs,thermalRp;
	public double maxU;
	double electricalRs,electricalRp,electricalC;
	public SoundCommand sound;
	/*
	public void applyTo(TurbineThermalProcess turbine)
	{
		turbine.TtoU = TtoU;
		turbine.nominalDeltaT = nominalDeltaT;
		turbine.nominalU = nominalU;
		turbine.PintoPout = PintoPout;
	}*/
	public void applyTo(ThermalLoad load)
	{
		load.C = thermalC;
		load.Rp = thermalRp;
		load.Rs = thermalRs;	
	}
	
	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addThermal(newItemStack());
		Data.addEnergy(newItemStack());
	}
	
	public void applyTo(ElectricalLoad load,boolean grounded)
	{
		
		if(grounded) load.setAll(electricalRs, electricalRs, electricalC);
		else load.setAll(electricalRs, electricalRp, electricalC);
	}
	
	void draw()
	{
		
		//GL11.glTranslatef(0f, 0.5f, 0f);
		//GL11.glScalef(1f, 2f, 1f);
		if(main != null) main.draw();
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
		draw();
	}
	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Converts heat into electricity.");
		list.add("Nominal usage ->");
		list.add("  Delta T : " +  ((int)nominalDeltaT) + "\u00B0C");
		list.add(Utils.plotVolt("  Voltage out :", nominalU));
		list.add(Utils.plotPower("  Power out :", nominalP));

	}
}
