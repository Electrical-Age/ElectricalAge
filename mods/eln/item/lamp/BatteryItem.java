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
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;

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
	}

	double energyStorage, dischargePower, chargePower;

	

	
	@Override
	public NBTTagCompound getDefaultNBT() {
		NBTTagCompound nbt = new NBTTagCompound("itemStackNBT");
		nbt.setDouble("energy",0);
		return nbt;
	}
	

	

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add(Utils.plotEnergy("Energy stored", getEnergy(itemStack)) + "(" + (int)(getEnergy(itemStack)/energyStorage*100) + "%)");
	}


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
		return dischargePower;
	}

	@Override
	public int getPriority(ItemStack stack) {
		// TODO Auto-generated method stub
		return priority;
	}




}
