package mods.eln.generic;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class GenericItemUsingDamageDescriptorWithComment extends GenericItemUsingDamageDescriptor{
	String[] description;
	public GenericItemUsingDamageDescriptorWithComment( String name,String[] description) {
		super( name);
		this.description = description;
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		for(String str : description)
		{
			list.add(str);
		}
	}

}
