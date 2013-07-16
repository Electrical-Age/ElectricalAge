package mods.eln;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemStackFilter {
	int itemId;
	int damageMask;
	int damageValue;

	public ItemStackFilter(Item item, int damageMask, int damageValue) {
		this.itemId = item.itemID;// caca1.5.1
		this.damageMask = damageMask;
		this.damageValue = damageValue;
	}

	public ItemStackFilter(Block block, int damageMask, int damageValue) {
		this.itemId = block.blockID;
		this.damageMask = damageMask;
		this.damageValue = damageValue;
	}

	public ItemStackFilter(Item item) {
		this.itemId = item.itemID;// caca1.5.1
		this.damageMask = 0;
		this.damageValue = 0;
	}

	public ItemStackFilter(Block block) {
		this.itemId = block.blockID;
		this.damageMask = 0;
		this.damageValue = 0;
	}

	public boolean tryItemStack(ItemStack itemStack) {// caca1.5.1
		if (itemStack.getItem().itemID != itemId)
			return false;
		if ((itemStack.getItemDamage() & damageMask) != damageValue)
			return false;
		return true;
	}
}
