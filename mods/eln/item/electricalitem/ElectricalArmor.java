package mods.eln.item.electricalitem;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumArmorMaterial;
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

	public ElectricalArmor(int par1, EnumArmorMaterial par2EnumArmorMaterial,
			int par3, int par4, 
			String t1, String t2,//String icon,
			double energyStorage,double chargePower,
			double ratioMax,double ratioMaxEnergy,
			double energyPerDamage
			) {
		super(par1, par2EnumArmorMaterial, par3, par4, t1, t2);
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
		System.out.println("armor hit  damage=" + damage + " energy=" + e + " energyLost=" + damage*energyPerDamage);
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
    @Override
    public void onArmorTickUpdate(World world, EntityPlayer player,
    		ItemStack itemStack) {
    	// TODO Auto-generated method stub
    	super.onArmorTickUpdate(world, player, itemStack);
    	
	//	int maxDamage = getArmorMaterial().getDurability(armorType);
		
	//	System.out.println("maxDamage=" + maxDamage + " Damage=" + itemStack.getItemDamage());
    }
    
    
	
	
	public NBTTagCompound getDefaultNBT() {
		NBTTagCompound nbt = new NBTTagCompound("itemStackNBT");
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



/*
	@Override
	public void onUpdate(ItemStack par1ItemStack, World par2World,
			Entity par3Entity, int par4, boolean par5) {
		// TODO Auto-generated method stub
		//super.onUpdate(par1ItemStack, par2World, par3Entity, par4, par5);
		int maxDamage = getArmorMaterial().getDurability(armorType);
		
		System.out.println("maxDamage=" + maxDamage + " Damage=" + par1ItemStack.getItemDamage());
	}
	*/
	
	
/*

	
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
		Utils.drawIcon(type,rIcon);
	}*/
 
}
