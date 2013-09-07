package mods.eln.thermaldissipatorpassive;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.sim.ITemperatureWatchdogDescriptor;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.wiki.Data;

public class ThermalDissipatorPassiveDescriptor extends TransparentNodeDescriptor implements ITemperatureWatchdogDescriptor ,IThermalDestructorDescriptor{
	public double thermalRs,thermalRp,thermalC;
	private Obj3D obj;
	Obj3DPart main;
	public ThermalDissipatorPassiveDescriptor(
			String name,
			Obj3D obj,
			double warmLimit,double coolLimit,
			double nominalP,double nominalT,
			double nominalTao,double nominalConnectionDrop
			) {
		super(name, ThermalDissipatorPassiveElement.class, ThermalDissipatorPassiveRender.class);
		thermalC = nominalP * nominalTao / nominalT;
		thermalRp = nominalT / nominalP;
		thermalRs = nominalConnectionDrop / nominalP;
		this.coolLimit = coolLimit;
		this.warmLimit = warmLimit;
		this.nominalP = nominalP;
		this.nominalT = nominalT;
		Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC);
		this.obj = obj;
		if(obj != null) main = obj.getPart("main");
	}
	double warmLimit,coolLimit;
	double nominalP, nominalT;
	public void applyTo(ThermalLoad load)
	{
		load.set(thermalRs, thermalRp, thermalC);
	}

	public void setParent(net.minecraft.item.Item item, int damage)
	{
		super.setParent(item, damage);
		Data.addThermal(newItemStack());
	}
	@Override
	public double getThermalDestructionMax() {
		// TODO Auto-generated method stub
		return 2;
	}


	@Override
	public double getThermalDestructionStart() {
		// TODO Auto-generated method stub
		return 1;
	}


	@Override
	public double getThermalDestructionPerOverflow() {
		// TODO Auto-generated method stub
		return 0.1;
	}


	@Override
	public double getThermalDestructionProbabilityPerOverflow() {
		// TODO Auto-generated method stub
		return 0.05;
	}


	@Override
	public double getTmax() {
		// TODO Auto-generated method stub
		return warmLimit;
	}


	@Override
	public double getTmin() {
		// TODO Auto-generated method stub
		return coolLimit;
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Dissipates heat in air");
		list.add("Useful for cooling turbine");
		list.add(Utils.plotCelsius("Tmax :", warmLimit));
		list.add("Nominal usage ->");
		list.add(Utils.plotCelsius("  Temperature :", nominalT));
		list.add(Utils.plotPower("  Cooling :", nominalP));

	}
	
	
	public void draw()
	{
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
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		// TODO Auto-generated method stub
		draw();
	}
}
