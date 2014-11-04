package mods.eln.transparentnode.electricalmachine;

import java.util.List;

import mods.eln.Translator;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.gui.GuiLabel;
import mods.eln.misc.Direction;
import mods.eln.misc.Recipe;
import mods.eln.misc.RecipesList;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalStackMachineProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.SoundCommand;
import mods.eln.wiki.Data;
import mods.eln.wiki.GuiItemStack;
import mods.eln.wiki.GuiVerticalExtender;
import mods.eln.wiki.ItemDefault.IPlugIn;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ElectricalMachineDescriptor extends TransparentNodeDescriptor implements IPlugIn {

	public RecipesList recipe = new RecipesList();

	double nominalU;
	double nominalP;
	//double maximalP;
	ThermalLoadInitializer thermal;
	ElectricalCableDescriptor cable;
	int outStackCount;

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
		
		outStackCount = 4;
		this.nominalP = nominalP;
		this.nominalU = nominalU;
		this.maximalU = maximalU;
		this.cable = cable;
		this.thermal = thermal;
		resistorR = nominalU * nominalU / nominalP;
	//	this.maximalP = nominalP*3;
		//thermal.setMaximalPower(maximalP);
		this.recipe = recipe;
		
	}

	SoundCommand runingSound, endSound;

	public ElectricalMachineDescriptor setRuningSound(SoundCommand runingSound) {
		this.runingSound = runingSound;
		return this;
	}
	@Override
	public boolean use2DIcon() {
		return false;
	}
	public ElectricalMachineDescriptor setEndSound(SoundCommand endSound) {
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add(Translator.translate("eln.core.nominal")+" U : " + nominalU);
		list.add(Translator.translate("eln.core.nominal")+" P : " + nominalP);
	}

	public void applyTo(ElectricalLoad load) {
		cable.applyTo(load);
	}

	public void applyTo(Resistor resistor) {
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

	void refresh(float deltaT, ElectricalMachineRender render, Object handleO, EntityItem inEntity, EntityItem outEntity, float powerFactor, float processState) {

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
		if (defaultHandle == null)
			defaultHandle = newDrawHandle();
		return defaultHandle;
	}

	@Override
	public int top(int y, GuiVerticalExtender extender, ItemStack stack) {
		
		return y;
	}

	@Override
	public int bottom(int y, GuiVerticalExtender extender, ItemStack stack) {

		int counter = -1;

		extender.add(new GuiLabel(6, y, "Can create:"));
		y += 12;
		for (Recipe r : recipe.getRecipes()) {
			if (counter == 0)
				y += (int) (18 * 1.3);
			if (counter == -1)
				counter = 0;
			int x = 6 + counter * 60;

			extender.add(new GuiItemStack(x, y, r.input, extender.helper));
			x += 18 * 2;

			for (ItemStack m : recipe.getMachines()) {
				extender.add(new GuiItemStack(x, y, m, extender.helper));
				x += 18;
			}
			x += 18;
			extender.add(new GuiItemStack(x, y, r.getOutputCopy()[0], extender.helper));
			
			x += 22;
			extender.add(new GuiLabel(x, y+4, Utils.plotEnergy("Cost", r.energy)));
			
			counter = (counter + 1) % 1;
		}
		y += (int) (18 * 1.3);
		
		return y;
	}
}
