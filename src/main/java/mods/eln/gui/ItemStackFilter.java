package mods.eln.gui;

import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;

public class ItemStackFilter {

	int itemId;
	int damageMask;
	int damageValue;

	public ItemStackFilter(Item item, int damageMask, int damageValue) {
		this.itemId = Item.getIdFromItem(item);
		this.damageMask = damageMask;
		this.damageValue = damageValue;
	}

	public ItemStackFilter(Block block, int damageMask, int damageValue) {
		this.itemId = Utils.getItemId(block);
		this.damageMask = damageMask;
		this.damageValue = damageValue;
	}

	public ItemStackFilter(Item item) {
		this.itemId = Item.getIdFromItem(item);
		this.damageMask = 0;
		this.damageValue = 0;
	}

	public ItemStackFilter(Block block) {
		this.itemId = Utils.getItemId(block);
		this.damageMask = 0;
		this.damageValue = 0;
	}

	public static ItemStackFilter[] OreDict(String name) {
		final ArrayList<ItemStack> ores = OreDictionary.getOres(name);
		ItemStackFilter[] filters = new ItemStackFilter[ores.size()];
		for (int i = 0; i < ores.size(); i++) {
			filters[i] = new ItemStackFilter(ores.get(i).getItem(), 0xff, ores.get(i).getItemDamage());
		}
		return filters;
	}

	public boolean tryItemStack(ItemStack itemStack) {// caca1.5.1
		if (Utils.getItemId(itemStack) != itemId)
			return false;
		if ((itemStack.getItemDamage() & damageMask) != damageValue)
			return false;
		return true;
	}
}
