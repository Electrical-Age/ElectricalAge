package mods.eln.item;

import net.minecraft.item.Item;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.wiki.Data;

public class GenericItemUsingDamageDescriptorUpgrade extends GenericItemUsingDamageDescriptor {

	public GenericItemUsingDamageDescriptorUpgrade(String name) {
		super(name);
	}

	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addUpgrade(newItemStack());
	}
}
