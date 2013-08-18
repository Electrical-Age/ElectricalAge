package mods.eln.item.electricalitem;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.Eln;
import mods.eln.PlayerManager;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;

public class ElectricalLampItem extends LampItem implements IItemEnergyBattery{

	public ElectricalLampItem(
			String name,
			int light,int range,
			double energyStorage,double dischargePower,double chargePower
			
			) {
		super(name);
		this.light = light;
		this.range = range;
		this.chargePower = chargePower;
		this.dischargePower = dischargePower;
		this.energyStorage = energyStorage;
		on = new ResourceLocation("eln", "/textures/items/" + name.replace(" ", "").toLowerCase() + "on.png");
		off = new ResourceLocation("eln", "/textures/items/" + name.replace(" ", "").toLowerCase() + "off.png");
	//	off = new ResourceLocation("eln", "/model/StoneFurnace/all.png");
	}
	int light,range;
	double energyStorage, dischargePower, chargePower;

	ResourceLocation on,off;

	
	@Override
	int getRange(ItemStack stack) {
		// TODO Auto-generated method stub
		return range;
	}
	
	@Override
	int getLight(ItemStack stack) {
		double energy = getEnergy(stack);
		if(energy > dischargePower*0.05){
			setEnergy(stack, energy - dischargePower*0.05);
			return light;
		}
		else{
			setEnergy(stack,0);
			return 0;
		}
	}
	
	@Override
	public NBTTagCompound getDefaultNBT() {
		NBTTagCompound nbt = new NBTTagCompound("itemStackNBT");
		nbt.setDouble("energy",0);
		nbt.setBoolean("powerOn",false);
		nbt.setInteger("rand", (int) (Math.random()*0xFFFFFFF));
		return nbt;
	}
	

	
	boolean getPowerOn(ItemStack stack)
	{
		return getNbt(stack).getBoolean("powerOn");
	}
	void setPowerOn(ItemStack stack,boolean value)
	{
		getNbt(stack).setBoolean("powerOn",value);
	}
	

	
	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int par4,
			boolean par5) {
		
		if(world.isRemote == false && entity instanceof EntityPlayer && ((EntityPlayer) entity).inventory.getCurrentItem() == stack && Eln.playerManager.get((EntityPlayer) entity).getInteractRise()){
			boolean status = ! getPowerOn(stack);
			if(status)
				((EntityPlayer) entity).addChatMessage("Flashlight ON");
			else
				((EntityPlayer) entity).addChatMessage("Flashlight OFF");
			setPowerOn(stack, status);
		}
		super.onUpdate(stack, world, entity, par4, par5);
	}

	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add(Utils.plotEnergy("Energy stored", getEnergy(itemStack)) + "(" + (int)(getEnergy(itemStack)/energyStorage*100) + "%)");
		list.add("Power button is " + (getPowerOn(itemStack) ? "ON" : "OFF"));
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
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return energyStorage;
	}

	@Override
	public double getChargePower(ItemStack stack) {
		// TODO Auto-generated method stub
		return chargePower;
	}

	@Override
	public double getDischagePower(ItemStack stack) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPriority(ItemStack stack) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {		
		if(type == ItemRenderType.INVENTORY)		
			Utils.drawEnergyBare(type,(float) (getEnergy(item)/getEnergyMax(item)));
		Utils.drawIcon(type,(getPowerOn(item) ? on : off));
	}
}
