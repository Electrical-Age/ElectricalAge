package mods.eln.gui;

import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

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

	public boolean tryItemStack(ItemStack itemStack) {// caca1.5.1
		if (Utils.getItemId(itemStack) != itemId)
			return false;
		if ((itemStack.getItemDamage() & damageMask) != damageValue)
			return false;
		return true;
	}
}
