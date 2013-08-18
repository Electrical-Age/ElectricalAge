package mods.eln.item.electricalitem;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.generic.genericArmorItem;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;
/*
public class ElectricalArmor extends genericArmorItem implements IItemEnergyBattery{

	public ElectricalArmor(int par1, EnumArmorMaterial par2EnumArmorMaterial,
			int par3, int par4, String t1, String t2,String icon) {
		super(par1, par2EnumArmorMaterial, par3, par4, t1, t2);
		rIcon = new ResourceLocation("eln",icon);
	}


	double energyStorage, chargePower;


	ResourceLocation rIcon;
	
	
	
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
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add(Utils.plotEnergy("Energy stored", getEnergy(itemStack)) + "(" + (int)(getEnergy(itemStack)/energyStorage*100) + "%)");
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
	}
 
}
*/