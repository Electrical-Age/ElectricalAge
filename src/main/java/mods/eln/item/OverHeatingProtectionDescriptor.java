package mods.eln.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import static mods.eln.i18n.I18N.tr;

public class OverHeatingProtectionDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

	public OverHeatingProtectionDescriptor(String name) {
		super(name);
	}

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
    	super.addInformation(itemStack, entityPlayer, list, par4);
    	list.add(tr("Useful to prevent overheating of"));
    	list.add(tr("Batteries"));
    }
}
