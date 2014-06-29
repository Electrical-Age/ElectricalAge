package mods.eln.item;

import net.minecraft.item.Item;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.wiki.Data;

public class MiningPipeDescriptor extends GenericItemUsingDamageDescriptorUpgrade{

	public MiningPipeDescriptor(
			String name
			) {
		super(name);
		
	}
	
	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		
	}

}
