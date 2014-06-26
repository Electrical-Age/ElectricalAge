package mods.eln.item;

import net.minecraft.item.Item;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.wiki.Data;

public class GenericItemUsingDamageDescriptorResource extends GenericItemUsingDamageDescriptor{

	public GenericItemUsingDamageDescriptorResource(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	
	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addResource(newItemStack());
	}

}
