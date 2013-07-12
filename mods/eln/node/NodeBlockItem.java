package mods.eln.node;

import java.lang.reflect.InvocationTargetException;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class NodeBlockItem extends ItemBlock {

	public NodeBlockItem(int id) {
		super(id);
		setUnlocalizedName("NodeBlockItem");
	}

	
	@Override
	public int getMetadata (int damageValue) {
		return damageValue;
	}
	

    public boolean placeBlockAt(ItemStack stack, EntityLivingBase player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata)
    {
    	if(world.isRemote) return false;
    	Direction direction = Direction.fromIntMinecraftSide(side).getInverse();
    	
		try {

        	Node node = (Node) NodeManager.UUIDToClass[getBlockID()].getConstructor().newInstance();
			node.onBlockPlacedBy(new Coordonate(x, y, z,world),direction,player,stack);
			
			world.setBlock(x, y, z, getBlockID(), node.getBlockMetadata(),0x03);//caca1.5.1
        	((NodeBlock)Block.blocksList[getBlockID()]).onBlockPlacedBy(world, x, y, z,direction, player,metadata);
        	
        	node.checkCanStay(true);
        	
        	return true;
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return false;
    }
}
