package mods.eln.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

public class OverHeatingProtectionDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

    public OverHeatingProtectionDescriptor(String name) {
        super(name);
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add("Useful to prevent overheating with:");
        list.add("  Battery");
    }
}
