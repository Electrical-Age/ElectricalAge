package mods.eln.generic;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class GenericCreativeTab extends CreativeTabs {
	public Item item;

	public GenericCreativeTab(String label, Item item) {
		super(label);
		this.item = item;
	}



	@Override
	@SideOnly(Side.CLIENT)
	public Item getTabIconItem()
	{
		return (item);
	}


}