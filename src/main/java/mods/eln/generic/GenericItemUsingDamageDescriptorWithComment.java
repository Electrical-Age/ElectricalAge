package mods.eln.generic;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

public class GenericItemUsingDamageDescriptorWithComment extends GenericItemUsingDamageDescriptor {

    String[] description;

    public GenericItemUsingDamageDescriptorWithComment(String name, String[] description) {
        super(name);
        this.description = description;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        for (String str : description) {
            list.add(str);
        }
    }
}
