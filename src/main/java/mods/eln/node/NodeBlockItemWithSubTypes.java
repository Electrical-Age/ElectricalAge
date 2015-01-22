package mods.eln.node;

import mods.eln.misc.Direction;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class NodeBlockItemWithSubTypes extends NodeBlockItem {

	public NodeBlockItemWithSubTypes(Block b) {
		super(b);
		setHasSubtypes(true);
		setUnlocalizedName("NodeBlockItemWithSubTypes");
	}
	/*//caca1.5.1
	@Override
	public String getItemNameIS(ItemStack itemstack) {
		return getItemName() + "." + itemstack.getItemDamage();
	}
*/
}
