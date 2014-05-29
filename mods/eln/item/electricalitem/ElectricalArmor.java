package mods.eln.item.electricalitem;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import net.minecraftforge.common.ISpecialArmor;
import mods.eln.generic.genericArmorItem;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;
import mods.eln.wiki.Data;

public class ElectricalArmor extends genericArmorItem implements IItemEnergyBattery ,ISpecialArmor{

	public ElectricalArmor(ArmorMaterial par2EnumArmorMaterial,
			int par3, int par4, 
			String t1, String t2,//String icon,
			double energyStorage,double chargePower,
			double ratioMax,double ratioMaxEnergy,
			double energyPerDamage
			) {
		super(par2EnumArmorMaterial, par3, par4, t1, t2);
		//rIcon = new ResourceLocation("eln",icon);
		this.chargePower = chargePower;
		this.energyStorage = energyStorage;
		this.ratioMax = ratioMax;
		this.ratioMaxEnergy = ratioMaxEnergy;
		this.energyPerDamage = energyPerDamage;
		Data.addPortable(new ItemStack(this));
	}
	double ratioMax, ratioMaxEnergy,energyPerDamage;
	@Override
	public ArmorProperties getProperties(EntityLivingBase player,
			ItemStack armor, DamageSource source, double damage, int slot) {
		// TODO Auto-generated method stub
		return new ArmorProperties(100, Math.min(1.0, getEnergy(armor)/ratioMaxEnergy)*ratioMax, (int) (getEnergy(armor)/energyPerDamage*25D));
	}

	@Override
	public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
		// TODO Auto-generated method stub
		return (int)(Math.min(1.0, getEnergy(armor)/ratioMaxEnergy)*ratioMax*20);
	}

	@Override
	public void damageArmor(EntityLivingBase entity, ItemStack stack,
			DamageSource source, int damage, int slot) {
		double e = getEnergy(stack);
		e = Math.max(0.0, e - damage*energyPerDamage);
		setEnergy(stack, e);
		Utils.println("armor hit  damage=" + damage + " energy=" + e + " energyLost=" + damage*energyPerDamage);
	}
	double energyStorage, chargePower;



	
    public boolean getIsRepairable(ItemStack par1ItemStack, ItemStack par2ItemStack)
    {
    	return false;
    }
    
    @Override
    public boolean hasColor(ItemStack par1ItemStack) {
    	// TODO Auto-generated method stub
    	return false;
    }

    
	
	
	public NBTTagCompound getDefaultNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setDouble("energy",0);
		nbt.setBoolean("powerOn",false);
		nbt.setInteger("rand", (int) (Math.random()*0xFFFFFFF));
		return nbt;
	}
	
	protected NBTTagCompound getNbt(ItemStack stack){
		NBTTagCompound nbt = stack.getTagCompound();
		if(nbt == null){
			stack.setTagCompound(nbt = getDefaultNBT());
		}
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
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add(Utils.plotEnergy("Energy Stored:", getEnergy(itemStack)) + "(" + (int)(getEnergy(itemStack)/energyStorage*100) + "%)");
		//list.add("Power button is " + (getPowerOn(itemStack) ? "ON" : "OFF"));
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
		return 0;
	}

	@Override
	public int getPriority(ItemStack stack) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void electricalItemUpdate(ItemStack stack,double time) {
		// TODO Auto-generated method stub
		
	}



 
}
