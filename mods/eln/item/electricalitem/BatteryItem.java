package mods.eln.item.electricalitem;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.Translator;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.wiki.Data;

public class BatteryItem extends GenericItemUsingDamageDescriptor implements IItemEnergyBattery{

	private int priority;




	public BatteryItem(
			String name,
			double energyStorage,double chargePower,double dischargePower, 
			int priority
			
			) {
		super(name);
		this.priority = priority;
		this.chargePower = chargePower;
		this.dischargePower = dischargePower;
		this.energyStorage = energyStorage;
		iconRessource = new ResourceLocation("eln", "textures/items/" + name.replace(" ", "").toLowerCase() + ".png");
	}
	ResourceLocation iconRessource;
	double energyStorage, dischargePower, chargePower;

	
	@Override
	public void setParent(Item item, int damage) {
		
		super.setParent(item, damage);
		Data.addPortable(newItemStack());
	}
	
	@Override
	public NBTTagCompound getDefaultNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setDouble("energy",0);
		return nbt;
	}
	

	

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add(Translator.translate("eln.core.chargespeed")+": " + (int) chargePower + "W");
		list.add(Translator.translate("eln.core.dischargespeed")+": " + (int) dischargePower + "W");
		list.add(Utils.plotEnergy(Translator.translate("eln.core.energystored")+":", getEnergy(itemStack)) + "(" + (int)(getEnergy(itemStack)/energyStorage*100) + "%)");
	}


	public double getEnergy(ItemStack stack)
	{
		return getNbt(stack).getDouble("energy");
	}
	public void setEnergy(ItemStack stack,double value)
	{
		if(value < 0) value = 0;
		getNbt(stack).setDouble("energy",value);
	}

	@Override
	public double getEnergyMax(ItemStack stack) {
		
		return energyStorage;
	}

	@Override
	public double getChargePower(ItemStack stack) {
		
		return chargePower;
	}

	@Override
	public double getDischagePower(ItemStack stack) {
		
		return dischargePower;
	}

	@Override
	public int getPriority(ItemStack stack) {
		
		return priority;
	}

	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		if(type == ItemRenderType.INVENTORY)
			return false;
		return true;
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {		
		if(type == ItemRenderType.INVENTORY)		
			UtilsClient.drawEnergyBare(type,(float) (getEnergy(item)/getEnergyMax(item)));
		UtilsClient.drawIcon(type,iconRessource);
	}

	@Override
	public void electricalItemUpdate(ItemStack stack,double time) {
		
		
	}

}
