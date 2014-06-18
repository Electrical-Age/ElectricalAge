package mods.eln.node;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import mods.eln.CommonProxy;
import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;

public class TransparentNodeItem extends GenericItemBlockUsingDamage<TransparentNodeDescriptor> implements IItemRenderer{

	
	
	public TransparentNodeItem(Block b) {
		super(b);
		setHasSubtypes(true);
		setUnlocalizedName("TransparentNodeItem");
	}
	
	@Override
	public int getMetadata (int damageValue) {
		return damageValue;
	}
	
	

	@Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata)
	{
    	if(world.isRemote) return false;
    	TransparentNodeDescriptor descriptor = getDescriptor(stack);
    	Direction direction = Direction.fromIntMinecraftSide(side).getInverse();
    	Direction front = descriptor.getFrontFromPlace(direction, player);
    	int[]v = new int[]{descriptor.getSpawnDeltaX(),descriptor.getSpawnDeltaY(),descriptor.getSpawnDeltaZ()};
    	front.rotateFromXN(v);
    	x += v[0];
    	y += v[1];
    	z += v[2];
    	
    	Block bb = world.getBlock(x, y, z);
    	if(bb.isReplaceable(world, x, y, z));
    	//if(world.getBlock(x, y, z) != Blocks.air) return false;
    	
    	Coordonate coord = new Coordonate(x,y,z,world);
    	
    	
		String error;
		if((error = descriptor.checkCanPlace(coord, front)) != null)
		{
			Utils.addChatMessage(player,error);
			return false;
		}
		
		GhostGroup ghostgroup = descriptor.getGhostGroup(front);
		if(ghostgroup != null) ghostgroup.plot(coord, coord, descriptor.getGhostGroupUuid());
		
    	TransparentNode node =  new TransparentNode();
		node.onBlockPlacedBy(new Coordonate(x, y, z,world),front,player,stack);
		
		world.setBlock(x, y, z, Block.getBlockFromItem(this), node.getBlockMetadata(),0x03);//caca1.5.1
    	((NodeBlock)Block.getBlockFromItem(this)).onBlockPlacedBy(world, x, y, z,direction, player,metadata);
    	
    	
    	
    	node.checkCanStay(true);
    	
    	return true;

    }

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		TransparentNodeDescriptor d = getDescriptor(item);
		if(Utils.nullCheck(d)) return false;
		return d.handleRenderType(item, type);
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return getDescriptor(item).shouldUseRenderHelper(type, item, helper);
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		Minecraft.getMinecraft().mcProfiler.startSection("TransparentNodeItem");
		
		switch(type)
		{
		case ENTITY:
			GL11.glTranslatef(0.00f, 0.3f, 0.0f);
			break;
		case EQUIPPED_FIRST_PERSON:
			GL11.glTranslatef(0.50f, 1, 0.5f);
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
		
		Minecraft.getMinecraft().mcProfiler.endSection();

	}	
}
