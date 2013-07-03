package mods.eln.node;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import mods.eln.CommonProxy;
import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;

public class TransparentNodeItem extends GenericItemBlockUsingDamage<TransparentNodeDescriptor> implements IItemRenderer{

	
	
	public TransparentNodeItem(int id) {
		super(id);
		setHasSubtypes(true);
		setUnlocalizedName("TransparentNodeItem");
	}
	
	@Override
	public int getMetadata (int damageValue) {
		return damageValue;
	}

    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata)
    {
    	if(world.isRemote) return false;
    	Direction direction = Direction.fromIntMinecraftSide(side).getInverse();
    	Coordonate coord = new Coordonate(x,y,z,world);
    	TransparentNodeDescriptor descriptor = getDescriptor(stack);
    	Direction front = descriptor.getFrontFromPlace(direction, player);
		try {
			if(descriptor.checkCanPlace(coord, front) == false)
			{
				player.sendChatToPlayer("You can't place this block at this side");
				return false;
			}
			
        	TransparentNode node =  (TransparentNode) NodeManager.UUIDToClass[getBlockID()].getConstructor().newInstance();
			node.onBlockPlacedBy(new Coordonate(x, y, z,world),front,player,stack);
			
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

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return getDescriptor(item).handleRenderType(item, type);
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return getDescriptor(item).shouldUseRenderHelper(type, item, helper);
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		
		

		switch(type)
		{
		case ENTITY:
			GL11.glTranslatef(0.00f, 0.3f, 0.0f);
			break;
		case EQUIPPED:
			GL11.glTranslatef(0.50f, 1, 0.5f);
			break;
		case FIRST_PERSON_MAP:
			break;
		case INVENTORY:
			GL11.glRotatef(90, 0, 1, 0);
			break;
		default:
			break;
		}
		getDescriptor(item).renderItem(type, item, data);
		
	}	
}
