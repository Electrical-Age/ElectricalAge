package mods.eln.node;

import net.minecraft.block.Block;

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
