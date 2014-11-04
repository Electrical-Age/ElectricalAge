package mods.eln.item.electricalitem;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.Translator;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.wiki.Data;

public class ElectricalLampItem extends LampItem implements IItemEnergyBattery{

	public ElectricalLampItem(
			String name,
			int lightMin,int rangeMin,double dischargeMin,int lightMax,int rangeMax,double dischargeMax,
			double energyStorage,double chargePower
			
			) {
		super(name);
		this.lightMin = lightMin;
		this.rangeMin = rangeMin;
		this.lightMax = lightMax;
		this.rangeMax = rangeMax;
		this.chargePower = chargePower;
		this.dischargeMin = dischargeMin;
		this.dischargeMax = dischargeMax;
		this.energyStorage = energyStorage;
		on = new ResourceLocation("eln", "textures/items/" + name.replace(" ", "").toLowerCase() + "on.png");
		off = new ResourceLocation("eln", "textures/items/" + name.replace(" ", "").toLowerCase() + "off.png");
	//	off = new ResourceLocation("eln", "/model/StoneFurnace/all.png");
	}
	int lightMin,rangeMin;
	int lightMax,rangeMax;
	double energyStorage, dischargeMin,dischargeMax, chargePower;

	ResourceLocation on,off;
	@Override
	public void setParent(Item item, int damage) {
		
		super.setParent(item, damage);
		Data.addPortable(newItemStack());
		Data.addLight(newItemStack());
	}
	
	@Override
	int getRange(ItemStack stack) {
		
		return getLightState(stack) == 1 ? rangeMin : rangeMax;
	}
	
	@Override
	int getLight(ItemStack stack) {
		double energy = getEnergy(stack);
		int state = getLightState(stack);
		double power = 0;
		switch (state) {
		case 1:
			power = dischargeMin;
			break;
		case 2:
			power = dischargeMax;
			break;
		}
		if(energy > power){
			//setEnergy(stack, energy - power);
			return getLightLevel(stack);
		}
		else{
			//setEnergy(stack,0);
			return 0;
		}
	}
	
	@Override
	public NBTTagCompound getDefaultNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setDouble("energy",0);
		nbt.setBoolean("powerOn",false);
		nbt.setInteger("rand", (int) (Math.random()*0xFFFFFFF));
		return nbt;
	}
	

	
	int getLightState(ItemStack stack)
	{
		return getNbt(stack).getInteger("LightState");
	}
	void setLightState(ItemStack stack,int value)
	{
		getNbt(stack).setInteger("LightState",value);
	}
	
	int getLightLevel(ItemStack stack)
	{
		return getLightState(stack) == 1 ? lightMin : lightMax;
	}
	/*
	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int par4,
			boolean par5) {
		
		if(world.isRemote == false && entity instanceof EntityPlayer && ((EntityPlayer) entity).inventory.getCurrentItem() == stack && Eln.playerManager.get((EntityPlayer) entity).getInteractRise()){
			int lightState = getLightState(stack) + 1;
			if(lightState > 1) lightState = 0;
			//((EntityPlayer) entity).addChatMessage("Flashlight !!!");
			switch (lightState) {
			case 0:
				Utils.addChatMessage((EntityPlayerMP)entity,"Flashlight OFF");
				break;
			case 1:
				Utils.addChatMessage((EntityPlayerMP)entity,"Flashlight ON");
				break;
			case 2:
				Utils.addChatMessage((EntityPlayerMP)entity,"Flashlight ON-2");
				break;

			default:
				break;
			}
			setLightState(stack, lightState);
		}
		super.onUpdate(stack, world, entity, par4, par5);
	}*/

	@Override
	public ItemStack onItemRightClick(ItemStack s, World w, EntityPlayer p) {
		if(w.isRemote == false){
			int lightState = getLightState(s) + 1;
			if(lightState > 1) lightState = 0;
			//((EntityPlayer) entity).addChatMessage("Flashlight !!!");
			switch (lightState) {
			case 0:
				Utils.addChatMessage((EntityPlayerMP)p,"Flashlight OFF");
				break;
			case 1:
				Utils.addChatMessage((EntityPlayerMP)p,"Flashlight ON");
				break;
			case 2:
				Utils.addChatMessage((EntityPlayerMP)p,"Flashlight ON-2");
				break;

			default:
				break;
			}
			setLightState(s, lightState);
		}

		return s;
	}

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		
		super.addInformation(itemStack, entityPlayer, list, par4);

		list.add(Translator.translate("eln.core.chargespeed")+": " + (int) chargePower + "W");
		list.add(Utils.plotEnergy(Translator.translate("eln.core.energystored")+":", getEnergy(itemStack)) + "(" + (int)(getEnergy(itemStack)/energyStorage*100) + "%)");
		list.add(Translator.translate("eln.core.lamp.state")+": " + (getLightState(itemStack) != 0 ? Translator.translate("eln.core.state.on") : Translator.translate("eln.core.state.off")));
	}
/*
	@Override
	public double putEnergy(ItemStack stack, double energy,double time) {
		double hit = Math.min(energy,Math.min(energyStorage - getEnergy(stack), chargePower*time));
		setEnergy(stack, getEnergy(stack) + hit);
		return energy - hit;
	}

	@Override
	public boolean isFull(ItemStack stack) {
		
		return getEnergy(stack) == energyStorage;
	}
*/

	public double getEnergy(ItemStack stack)
	{
		return getNbt(stack).getDouble("energy");
	}
	public void setEnergy(ItemStack stack,double value)
	{
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
		
		return 0;
	}

	@Override
	public int getPriority(ItemStack stack) {
		
		return 0;
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
		UtilsClient.drawIcon(type,(getLight(item) != 0 && getLightState(item) != 0? on : off));
	}

	@Override
	public void electricalItemUpdate(ItemStack stack,double time) {
		
	
		double energy = getEnergy(stack);
		int state = getLightState(stack);
		double power = 0;
		switch (state) {
		case 1:
			power = dischargeMin*time;
			break;
		case 2:
			power = dischargeMax*time;
			break;
		}
		
		if(energy > power)
			setEnergy(stack, energy - power);
		else
			setEnergy(stack,0);
	}
}
