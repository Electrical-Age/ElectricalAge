package mods.eln.item.lamp;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import mods.eln.Eln;
import mods.eln.PlayerManager;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;

public class ElectricalLampItem extends LampItem implements IItemEnergyBattery{

	public ElectricalLampItem(
			String name,
			int light,int range,
			double energyStorage,double autonomy,double chargeTime
			
			) {
		super(name);
		this.light = light;
		this.range = range;
		this.chargePower = energyStorage/chargeTime;
		this.dischargePower = energyStorage/autonomy;
		this.energyStorage = energyStorage;
	}
	int light,range;
	double energyStorage, dischargePower, chargePower;

	

	
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
		return 0;
	}
	
	@Override
	public NBTTagCompound getDefaultNBT() {
		NBTTagCompound nbt = new NBTTagCompound("itemStackNBT");
		nbt.setDouble("energy",0);
		nbt.setBoolean("powerOn",false);
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
	
	double getEnergy(ItemStack stack)
	{
		return getNbt(stack).getDouble("energy");
	}
	void setEnergy(ItemStack stack,double value)
	{
		getNbt(stack).setDouble("energy",value);
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

	@Override
	public double putEnergy(ItemStack stack, double energy,double time) {
		double hit = Math.min(energy,Math.min(energyStorage - getEnergy(stack), chargePower*time));
		setEnergy(stack, getEnergy(stack) + hit);
		return energy - hit;
	}



}
