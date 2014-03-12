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
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.util.StatCollector;
import net.minecraft.util.StringTranslate;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

public class GenericItemUsingDamage<Descriptor extends GenericItemUsingDamageDescriptor> extends Item implements IGenericItemUsingDamage{
	Hashtable<Integer,Descriptor> subItemList = new Hashtable<Integer,Descriptor>();
	ArrayList<Integer> orderList = new ArrayList<Integer>();

	Descriptor defaultElement = null;
	
	public GenericItemUsingDamage(int par1) {
		super(par1);
		setHasSubtypes(true);
		// TODO Auto-generated constructor stub
	}
	
	
	
	public void setDefaultElement(Descriptor descriptor)
	{
		defaultElement = descriptor;
	}
	
	
	public void addElement(int damage,Descriptor descriptor)
	{
		subItemList.put(damage,descriptor);
		ItemStack stack = new ItemStack(this, 1, damage);
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
	
	 /*//caca1.5.1
	@Override
	@SideOnly(Side.CLIENT)
	public int getIconFromDamage(int damage) {
		return getDescriptor(damage).getIconId();
		
	}
	@Override
	public String getTextureFile () {
		return CommonProxy.ITEMS_PNG;
	}
	@Override
	public String getItemNameIS(ItemStack itemstack) {
		return getItemName() + "." + getDescriptor(itemstack).name;
	}
	
*/
	@Override  //1.6.4
    public String getUnlocalizedNameInefficiently(ItemStack par1ItemStack)
    {
		return "trololol";
    }

    
	@Override
    public String getStatName()
    {
        return "troloo2";
    }
	
    
	@Override
    public String getItemDisplayName(ItemStack par1ItemStack)
    {
		Descriptor desc = getDescriptor(par1ItemStack);
		if(desc == null) return "NullItem";
        return desc.getName(par1ItemStack);
    }


    public Icon getIconFromDamage(int damage)
    {
    	GenericItemUsingDamageDescriptor desc = getDescriptor(damage);
    	if(desc != null)
    	{
    		return getDescriptor(damage).getIcon();
    	}
    	return null;
    }
	

	
	@Override
    public void registerIcons(IconRegister iconRegister)
    {
       for(GenericItemUsingDamageDescriptor descriptor : subItemList.values())
       {
    	   descriptor.updateIcons(iconRegister);
       }
    }
	
	
    @SideOnly(Side.CLIENT)
    public void getSubItems(int itemID, CreativeTabs tabs, List list){
	// You can also take a more direct approach and do each one individual but I prefer the lazy / right way
    	for(int id : orderList)
    	{
    		subItemList.get(id).getSubItems(list);
	    }
	}
    
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4)
    {
		Descriptor desc = getDescriptor(itemStack);
		if(desc == null) return;
		desc.addInformation(itemStack, entityPlayer, list, par4);
    }
   
    
    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
	@Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float vx, float vy, float vz)
    {
		GenericItemUsingDamageDescriptor d = getDescriptor(stack); if(d == null) return false;
        return d.onItemUse(stack, player, world, x, y, z, side, vx, vy, vz);
    }
    
   
   public void onUpdate(ItemStack stack, World world, Entity entity, int par4, boolean par5)
   {
	   if(world.isRemote){
		   return;
	   }
	   GenericItemUsingDamageDescriptor d = getDescriptor(stack); if(d == null) return ;
	   d.onUpdate(stack, world, entity, par4, par5);
   }

   @Override
   public float getStrVsBlock(ItemStack stack, Block block)
   {
	   GenericItemUsingDamageDescriptor d = getDescriptor(stack); if(d == null) return 0.2f;
	   return d.getStrVsBlock(stack, block);
   }


   @Override
   public boolean canHarvestBlock(Block par1Block)
   {
       return true;
   }

   @Override
   public boolean onBlockDestroyed(ItemStack stack, World w, int blockId, int x, int y, int z, EntityLivingBase entity)
   {
	   if(w.isRemote){
		   return false;
	   }
	   GenericItemUsingDamageDescriptor d = getDescriptor(stack); if(d == null) return true;
	   return d.onBlockDestroyed( stack,  w, blockId, x,  y,  z,  entity);
   }
   

}
