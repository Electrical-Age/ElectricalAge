package mods.eln.item;

import java.util.List;


import mods.eln.generic.GenericItemUsingDamageDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class OverVoltageProtectionDescriptor extends GenericItemUsingDamageDescriptorUpgrade{

	public OverVoltageProtectionDescriptor(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
    		List list, boolean par4) {
    	// TODO Auto-generated method stub
    	super.addInformation(itemStack, entityPlayer, list, par4);
    	list.add("Useful to prevent overvoltage on :");
    	list.add("  Battery");
    }
}
