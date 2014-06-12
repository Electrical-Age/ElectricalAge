package mods.eln.electricalmachine;

import java.util.List;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Recipe;
import mods.eln.misc.RecipesList;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ElectricalStackMachineProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.wiki.Data;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ElectricalMachineDescriptor extends TransparentNodeDescriptor {

	public RecipesList recipe = new  RecipesList();

	double nominalU;
	double nominalP;
	double maximalP;
	ThermalLoadInitializer thermal;
	ElectricalCableDescriptor cable;
	
	double resistorR;
	
	double boosterEfficiency = 1.0 / 1.1;
	double boosterSpeedUp = 1.25 / boosterEfficiency;
	public ElectricalMachineDescriptor(
				String name,
				double nominalU, double nominalP,
				double maximalU,
				ThermalLoadInitializer thermal,
				ElectricalCableDescriptor cable,
				RecipesList recipe) {
		super(name, ElectricalMachineElement.class, ElectricalMachineRender.class);
		this.nominalP = nominalP;
		this.nominalU = nominalU;
		this.maximalU = maximalU;
		this.cable = cable;
		this.thermal = thermal;
		resistorR = nominalU * nominalU / nominalP;
		this.maximalP = maximalU * maximalU / resistorR;
		thermal.setMaximalPower(maximalP);
		this.recipe = recipe;
	}
	String runingSound = null,endSound = null;
	double runingSoundLength;
	public ElectricalMachineDescriptor setRuningSound(String runingSound,double length){
		this.runingSound = runingSound;
		this.runingSoundLength = length;
		return this;
	}
	public ElectricalMachineDescriptor setEndSound(String endSound){
		this.endSound = endSound;
		return this;
	}
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		recipe.addMachine(newItemStack(1));
		Data.addMachine(newItemStack(1));
	}

	double maximalU;
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Nominal U : " + nominalU);
		list.add("Nominal P : " + nominalP);
	}
	
	public void applyTo(ElectricalLoad load) {
		cable.applyTo(load, false);
	}
	
	public void applyTo(ElectricalResistor resistor) {
		resistor.setR(resistorR);
	}
	
	public void applyTo(ElectricalStackMachineProcess machine) {
		machine.setResistorValue(resistorR);
	}
	
	public void applyTo(ThermalLoad load) {
		thermal.applyTo(load);
	}
	
	Object newDrawHandle() {
		return null;
	}
	
	Object defaultHandle = null;
	
	void draw(ElectricalMachineRender render, Object handleO, EntityItem inEntity, EntityItem outEntity, float powerFactor, float processState) {
	}
	
	
	public boolean powerLrdu(Direction side, Direction front) {
		return true;
	}
	
	public boolean drawCable() {
		return false;
	}
	
	CableRenderDescriptor getPowerCableRender() {
		return null;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		draw(null, getDefaultHandle(), null, null, 0f, 0f);
	}
	
	Object getDefaultHandle() {
		if(defaultHandle == null) defaultHandle = newDrawHandle();
		return defaultHandle;
	}
}
