package mods.eln.node.six;

import mods.eln.node.ISixNodeCache;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.item.ItemStack;

public class SixNodeCacheStd implements ISixNodeCache{

	@Override
	public boolean accept(ItemStack stack) {
		
		Block b = Block.getBlockFromItem(stack.getItem());
		if(b == null) return false;
		if(b instanceof BlockContainer) return false;
		return b.getRenderType() == 0 && stack.getItem() instanceof SixNodeItem == false;
	}

	@Override
	public int getMeta(ItemStack stack) {
		
		return stack.getItemDamage();
	}

}
