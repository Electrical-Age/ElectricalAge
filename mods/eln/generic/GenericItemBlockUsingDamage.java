package mods.eln.generic;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.eln.CommonProxy;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.util.StringTranslate;

public class GenericItemBlockUsingDamage<Descriptor extends GenericItemBlockUsingDamageDescriptor> extends ItemBlock {
	Hashtable<Integer,Descriptor> subItemList = new Hashtable<Integer,Descriptor>();
	ArrayList<Integer> orderList = new ArrayList<Integer>();
	
	
	Descriptor defaultElement = null;
	
	public GenericItemBlockUsingDamage(int par1) {
		super(par1);
		setHasSubtypes(true);
		// TODO Auto-generated constructor stub
	}
	
	public void setDefaultElement(Descriptor descriptor)
	{
		defaultElement = descriptor;
	}
	

	public void addDescriptor(int damage,Descriptor descriptor)
	{
		subItemList.put(damage,descriptor);
		ItemStack stack = new ItemStack(this, 1, damage);
		stack.setTagCompound(descriptor.getDefaultNBT());
		LanguageRegistry.addName(stack,descriptor.name);
		orderList.add(damage);
		descriptor.setParent(this, damage);
		GameRegistry.registerCustomItemStack(descriptor.name, descriptor.newItemStack(1));
	}
	
	public Descriptor getDescriptor(int damage)
	{
		return subItemList.get(damage);
	}
	
	public Descriptor getDescriptor(ItemStack itemStack)
	{
		if(itemStack == null) return defaultElement;
		if(itemStack.getItem() != this) return defaultElement;
		return getDescriptor(itemStack.getItemDamage());
	}
	
	

	/*
	@Override
	@SideOnly(Side.CLIENT)
	public int getIconFromDamage(int damage) {
		return getDescriptor(damage).getIconId();
		
	}
	//caca1.5.1
	@Override
	public String getTextureFile () {
		return CommonProxy.ITEMS_PNG;
	}
	@Override
	public String getItemNameIS(ItemStack itemstack) {
		return getItemName() + "." + getDescriptor(itemstack).name;
	}
	*/
	
	
	@Override
    public String getItemDisplayName(ItemStack par1ItemStack)
    {
		Descriptor desc = getDescriptor(par1ItemStack);
		if(desc == null) return "Unknow";
        return desc.getName(par1ItemStack);
    }


    public Icon getIconFromDamage(int damage)
    {
		Descriptor desc = getDescriptor(damage);
		if(desc == null) return null;
    	return desc.getIcon();
    }
	

	
	@Override
    public void registerIcons(IconRegister iconRegister)
    {
       for(GenericItemBlockUsingDamageDescriptor descriptor : subItemList.values())
       {
    	   descriptor.updateIcons(iconRegister);
       }
    }	
	
    @SideOnly(Side.CLIENT)
    public void getSubItems(int itemID, CreativeTabs tabs, List list){
	// You can also take a more direct approach and do each one individual but I prefer the lazy / right way
    	//for(Entry<Integer, Descriptor> entry : subItemList.entrySet()) 
    	for(int id : orderList)
    	{
    		ItemStack stack = new ItemStack(itemID, 1, id);
    		stack.setTagCompound(subItemList.get(id).getDefaultNBT());
	        list.add(stack);
	    }
	}
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4)
    {
		Descriptor desc = getDescriptor(itemStack);
		if(desc == null) return;
		desc.addInformation(itemStack, entityPlayer, list, par4);
    }
}
