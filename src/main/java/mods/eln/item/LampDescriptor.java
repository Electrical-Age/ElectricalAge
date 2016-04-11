package mods.eln.item;

import mods.eln.Eln;
import mods.eln.misc.IConfigSharing;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.lampsocket.LampSocketType;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class LampDescriptor extends GenericItemUsingDamageDescriptorUpgrade implements IConfigSharing {

	public enum Type {Incandescent, eco, LED}
	public double nominalP,nominalLight, nominalLife;
	public String name, description;
	public Type type;
	public LampSocketType socket;

	public double nominalU, minimalU;
	public double stableU, stableUNormalised, stableTime, vegetableGrowRate;

	double serverNominalLife;
	
	public LampDescriptor(	
			String name,String iconName,
			Type type, LampSocketType socket,
			double nominalU, double nominalP, double nominalLight, double nominalLife,
			double vegetableGrowRate) {
		super(name);
		changeDefaultIcon(iconName);
		this.type = type;
		this.socket = socket;
		this.nominalU = nominalU;
		this.nominalP = nominalP;
		this.nominalLight = nominalLight;
		this.nominalLife = nominalLife;
		this.vegetableGrowRate = vegetableGrowRate;

		switch (type) {
			case Incandescent:
				minimalU = nominalU * 0.5;
				break;

			case eco:
				stableUNormalised = 0.75;
				minimalU = nominalU * 0.5;
				stableU = nominalU * stableUNormalised;
				stableTime = 4;
				break;

			case LED:
				minimalU = nominalU * 0.75;
				break;

			default:
				break;
		}
		
		Eln.instance.configShared.add(this);
		voltageLevelColor = VoltageLevelColor.fromVoltage(nominalU);
	}
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addLight(newItemStack());
	}
	
	public double getR() {
		return nominalU * nominalU / nominalP;
	}

	public double getLifeInTag(ItemStack stack) {
		if(!stack.hasTagCompound())
			stack.setTagCompound(getDefaultNBT());
		if(stack.getTagCompound().hasKey("life"))
			return stack.getTagCompound().getDouble("life");
		return Utils.rand(0.75, 1.50);
	}
	
	public void setLifeInTag(ItemStack stack, double life) {
		if(!stack.hasTagCompound())
			stack.setTagCompound(getDefaultNBT());
		stack.getTagCompound().setDouble("life", life);
	}
	
	@Override
	public NBTTagCompound getDefaultNBT() {
		return new NBTTagCompound();
	}
	
	@Override
	public ItemStack newItemStack(int size) {
		return super.newItemStack(size);
	}
	
	public void applyTo(Resistor resistor) {
		resistor.setR(getR());
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);

		list.add(tr("Technology: %1$", type));
		list.add(tr("Range: %1$ blocks", (int)(nominalLight * 15)));
		list.add(tr("Power: %1$W", (int)nominalP));
		list.add(tr("Resistance: %1$Ω", getR()));
		list.add(tr("Nominal lifetime: %1$h", serverNominalLife));
		if(!itemStack.getTagCompound().hasKey("life"))
			list.add(tr("Condition:") + " " + tr("New"));
		else if(getLifeInTag(itemStack) > 0.5)
			list.add(tr("Condition:") + " " + tr("Good"));
		else if(getLifeInTag(itemStack) > 0.2)
			list.add(tr("Condition:") + " " + tr("Used"));
		else if(getLifeInTag(itemStack) > 0.1)
			list.add(tr("Condition:") + " " + tr("End of life"));
		else 
			list.add(tr("Condition:") + " " + tr("Bad"));
	}

	@Override
	public void serializeConfig(DataOutputStream stream) throws IOException {
		stream.writeDouble(nominalLife);
	}

	@Override
	public void deserialize(DataInputStream stream) throws IOException {
		serverNominalLife = stream.readDouble();
	}
}
