package mods.eln.item;

import java.util.List;

import mods.eln.Translator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class CombustionChamber extends GenericItemUsingDamageDescriptorUpgrade{

	public CombustionChamber(String name) {
		super( name);
		
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add(Translator.translate("eln.core.shfupgrade.hint"));
	}
}
