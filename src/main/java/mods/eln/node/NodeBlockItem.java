package mods.eln.node;

import mods.eln.misc.Coordinate;
import mods.eln.misc.Direction;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
        World w = coord.world();
        BlockPos pos = coord.pos;
    	/*if(w.isRemote) return false;
        Direction direction = Direction.fromIntMinecraftSide(side).getInverse();

    	NodeBase node = (NodeBase) getBlock().newNodeBase();
		node.onBlockPlacedBy(new Coordinate(pos ,w),direction,player,stack);
		
		w.setBlockState(pos, getBlock(), node.getBlockMetadata(),0x03);//caca1.5.1
    	getBlock().onBlockPlacedBy(w, pos, direction, player,metadata);
    	
    	node.checkCanStay(true);
        */
        return false;

    }
}
