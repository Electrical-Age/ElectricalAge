package mods.eln.node;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.item.ItemStack;

public class SixNodeCacheStd implements ISixNodeCache{

	@Override
	public boolean accept(ItemStack stack) {
		// TODO Auto-generated method stub
		Block b = Block.getBlockFromItem(stack.getItem());
		if(b == null) return false;
		if(b instanceof BlockContainer) return false;
		return b.getRenderType() == 0 && stack.getItem() instanceof SixNodeItem == false;
	}

	@Override
	public int getMeta(ItemStack stack) {
		// TODO Auto-generated method stub
		return stack.getItemDamage();
	}

}
