package mods.eln.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import mods.eln.Eln;
import mods.eln.misc.IConfigSharing;
import mods.eln.misc.Utils;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.lampsocket.LampSocketType;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class LampDescriptor extends GenericItemUsingDamageDescriptorUpgrade implements IConfigSharing {

	public enum Type {Incandescent, eco}
	public double nominalP,nominalLight, nominalLife;
	public String name, description;
	public Type type;
	public LampSocketType socket;
	
	public int textureId;

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
		//this.description = description;
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
			default:
				break;
		}
		
		Eln.instance.configShared.add(this);
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
		NBTTagCompound nbt = new NBTTagCompound();
		//nbt.setDouble("life", Utils.rand(0.75, 1.50));
		return nbt;
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
		
		//list.add("Socket : " + socket);
		list.add("Tech  : " + type);
		list.add("Light   : " + (int)(nominalLight * 15) + " blocks");
		list.add("Power : " + (int)nominalP + "W");
		list.add(Utils.plotOhm("Resistance :", getR()));
		//list.add(Utils.plotOhm("Resistance", this.getR()));
		list.add(Utils.plotTime("Nominal life : ", serverNominalLife));
		if(!itemStack.getTagCompound().hasKey("life"))
			list.add("Seem that nobody has used it before");
		else if(getLifeInTag(itemStack) > 0.5)
			list.add("Seem in good condition");
		else if(getLifeInTag(itemStack) > 0.2)
			list.add("seems a bit worn");
		else if(getLifeInTag(itemStack) > 0.1)
			list.add("Seem in end of life");
		else 
			list.add("Seem that can break in the hours");

		//list.add(Utils.plotTime("Life    : ", getLifeInTag(itemStack) * nominalLife));
	}

	@Override
	public void serializeConfig(DataOutputStream stream) throws IOException {
		// TODO Auto-generated method stub
		stream.writeDouble(nominalLife);
	}

	@Override
	public void deserialize(DataInputStream stream) throws IOException {
		// TODO Auto-generated method stub
		serverNominalLife = stream.readDouble();
	}
}
