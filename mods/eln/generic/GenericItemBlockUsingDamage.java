package mods.eln.generic;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class GenericItemBlockUsingDamage<Descriptor extends GenericItemBlockUsingDamageDescriptor> extends ItemBlock {
	public Hashtable<Integer,Descriptor> subItemList = new Hashtable<Integer,Descriptor>();
	public ArrayList<Integer> orderList = new ArrayList<Integer>();
	public ArrayList<Descriptor> descriptors = new ArrayList<Descriptor>();
	
	
	public Descriptor defaultElement = null;
	
	public GenericItemBlockUsingDamage(Block b) {
		super(b);
		setHasSubtypes(true);
		
	}
	
	public void setDefaultElement(Descriptor descriptor)
	{
		defaultElement = descriptor;
	}
	
	public void doubleEntry(int src,int dst){
		subItemList.put(dst,subItemList.get(src));
	}

	public void addDescriptor(int damage,Descriptor descriptor)
	{
		subItemList.put(damage,descriptor);
		ItemStack stack = new ItemStack(this, 1, damage);
		stack.setTagCompound(descriptor.getDefaultNBT());
		LanguageRegistry.addName(stack,descriptor.name);
		orderList.add(damage);
		descriptors.add(descriptor);
		descriptor.setParent(this, damage);
		GameRegistry.registerCustomItemStack(descriptor.name, descriptor.newItemStack(1));
	}
	public void addWithoutRegistry(int damage,Descriptor descriptor)
	{
		subItemList.put(damage,descriptor);
		ItemStack stack = new ItemStack(this, 1, damage);
		stack.setTagCompound(descriptor.getDefaultNBT());
		LanguageRegistry.addName(stack,descriptor.name);
		descriptor.setParent(this, damage);
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
    public String getItemStackDisplayName(ItemStack par1ItemStack)
    {
		Descriptor desc = getDescriptor(par1ItemStack);
		if(desc == null) return "Unknown";
        return desc.getName(par1ItemStack);
    }

	@Override
    public IIcon getIconFromDamage(int damage)
    {
		Descriptor desc = getDescriptor(damage);
		if(desc == null) return null;
    	return desc.getIcon();
    }
	

	
	@Override
    public void registerIcons(IIconRegister iconRegister)
    {
       for(GenericItemBlockUsingDamageDescriptor descriptor : subItemList.values())
       {
    	   descriptor.updateIcons(iconRegister);
       }
    }	
	
    @SideOnly(Side.CLIENT)
    @Override
    public void getSubItems(Item itemID, CreativeTabs tabs, List list){
	// You can also take a more direct approach and do each one individual but I prefer the lazy / right way
    	//for(Entry<Integer, Descriptor> entry : subItemList.entrySet()) 
    	for(int id : orderList)
    	{
    		ItemStack stack = Utils.newItemStack(itemID, 1, id);
    		stack.setTagCompound(subItemList.get(id).getDefaultNBT());
	        list.add(stack);
	    }
	}
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4)
    {
		Descriptor desc = getDescriptor(itemStack);
		if(desc == null) return;
		List listFromDescriptor = new ArrayList();
		desc.addInformation(itemStack, entityPlayer, listFromDescriptor, par4);
		UtilsClient.showItemTooltip(listFromDescriptor,list);
    }
    
    
    public boolean onEntityItemUpdate(EntityItem entityItem)
    {
        Descriptor desc  = getDescriptor(entityItem.getEntityItem());
        if(desc != null) return desc.onEntityItemUpdate(entityItem);
        return false;
    }
    
    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ)
    {
        Descriptor desc  = getDescriptor(stack);
        if(desc != null) return desc.onItemUseFirst(stack,player);
        return false;
    }
}
