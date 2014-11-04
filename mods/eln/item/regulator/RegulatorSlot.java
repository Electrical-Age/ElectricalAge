package mods.eln.item.regulator;

import mods.eln.Eln;
import mods.eln.Translator;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.sim.RegulatorType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class RegulatorSlot extends GenericItemUsingDamageSlot{

	
	RegulatorType[] type;
	public RegulatorSlot(IInventory inventory, int slot, int x, int y,
			int stackLimit,RegulatorType[] type,SlotSkin skin) {
		super(inventory, slot, x, y, stackLimit, IRegulatorDescriptor.class,skin,new String[]{Translator.translate("eln.core.regulator.slot.hint")});
		this.type = type;
	}


	@Override
	public boolean isItemValid(ItemStack itemStack) {
		
		if(!super.isItemValid(itemStack)) return false;
		IRegulatorDescriptor element = (IRegulatorDescriptor) IRegulatorDescriptor.getDescriptor(itemStack);
		for(RegulatorType t : type)
		{
			if(t == element.getType()) return true;
		}
		return false;
	}
}
