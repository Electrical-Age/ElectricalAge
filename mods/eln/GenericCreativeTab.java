package mods.eln;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class GenericCreativeTab extends CreativeTabs {
	public Item item;

	public GenericCreativeTab(String label, Item item) {
		super(label);
		this.item = item;
	}

	@Override
	public ItemStack getIconItemStack() {
		return new ItemStack(item);
	}

}