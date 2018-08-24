package netherclack.client.texture.misc;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;

public interface IAdvancedTexturedBlock extends ITexturedBlock
{
	public IBlockState getBlockState(ItemStack stack);
	
}
