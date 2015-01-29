package mods.eln.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class OverVoltageProtectionDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

	public OverVoltageProtectionDescriptor(String name) {
		super(name);
	}

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
    	super.addInformation(itemStack, entityPlayer, list, par4);
    	list.add("Useful to prevent overvoltage with:");
    	list.add("  Battery");
    }
}
