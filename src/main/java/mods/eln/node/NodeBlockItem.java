package mods.eln.node;

import mods.eln.misc.Coordinate;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class NodeBlockItem extends ItemBlock {

    public NodeBlockItem(Block b) {
        super(b);
        setUnlocalizedName("NodeBlockItem");
    }


    @Override
    public int getMetadata(int damageValue) {
        return damageValue;
    }


    /*int getBlockID(){
        return Block.getIdFromBlock(getBlock());
    }*/
    public NodeBlock getBlock() {
        return (NodeBlock) Block.getBlockFromItem(this);
    }

    public boolean placeBlockAt(ItemStack stack, EntityLivingBase player, Coordinate coord, float hitX, float hitY, float hitZ, int metadata) {
    /*	if(world.isRemote) return false;
        Direction direction = Direction.fromIntMinecraftSide(side).getInverse();
    	

    	NodeBase node = (NodeBase) getBlock().newNodeBase();
		node.onBlockPlacedBy(new Coordonate(x, y, z,world),direction,player,stack);
		
		world.setBlock(x, y, z, getBlock(), node.getBlockMetadata(),0x03);//caca1.5.1
    	getBlock().onBlockPlacedBy(world, x, y, z,direction, player,metadata);
    	
    	node.checkCanStay(true);
    	*/
        return false;

    }
}
