package mods.eln.item;

import java.util.List;

import cpw.mods.fml.common.registry.GameRegistry;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.lampsocket.LampSocketType;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalResistor;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Icon;



public class BrushDescriptor  extends GenericItemUsingDamageDescriptor
{

	public BrushDescriptor(	
			String name)
	{
		super( name);
	}
	/*Icon[] iconIndex = new Icon[16];
    public void updateIcons(IconRegister iconRegister)
    {
    	for(int idx = 0;idx<16;idx++)
    	{
    		iconIndex[idx] = iconRegister.registerIcon(IconName + subNames[idx]);
    	}
        
    }*/
   /* 
    public String getNameForColor(int color)
    {
    	return subNames[color] + " " + name;
    }*/
	@Override
	public String getName(ItemStack stack) {
		// TODO Auto-generated method stub
		int color = getColor(stack),life = getLife(stack);
		if(color == 15 && life == 0)
			return "Empty " + super.getName(stack);
		return super.getName(stack);
	}
	/*@Override
	public void getSubItems(List list)
	{
		super.getSubItems(list);
		for(int idx = 0;idx<16;idx++)
    	{
    		ItemStack stack = newItemStack(1);
    		setColor(stack,idx);
    		setLife(stack,4);
	    	list.add(stack);
    	}
	    	
	}*/
	public int getColor(ItemStack stack)
	{
		return stack.getItemDamage() & 0xF;
	}
	public int getLife(ItemStack stack)
	{
		return stack.getTagCompound().getInteger("life");
	}
	public void setColor(ItemStack stack,int color)
	{
		stack.setItemDamage((stack.getItemDamage() & ~0xF) | color);
	}
	public void setLife(ItemStack stack,int life)
	{
		stack.getTagCompound().setInteger("life",life);
	}
	
	@Override
	public NBTTagCompound getDefaultNBT() {
		// TODO Auto-generated method stub
		NBTTagCompound nbt = new NBTTagCompound("painternbt");
		nbt.setInteger("life", 32);
		return nbt;
	}
	
	@Override
	public ItemStack newItemStack(int size) {
		// TODO Auto-generated method stub

		return super.newItemStack(size);
	}
	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add("life : " + itemStack.getTagCompound().getInteger("life"));
		
	}
	
	public boolean use(ItemStack stack)
	{
		int life = stack.getTagCompound().getInteger("life");
		if(life != 0)
		{
			
			if(--life == 0)
				setColor(stack, 15);
			stack.getTagCompound().setInteger("life", life);
			return true;
		}
		
		return false;
	}
}
