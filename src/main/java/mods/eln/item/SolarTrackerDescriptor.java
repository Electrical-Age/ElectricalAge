package mods.eln.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

public class SolarTrackerDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

    public SolarTrackerDescriptor(String name) {
        super(name);
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add("Upgrade for the Solar Panel.");
    }
}
