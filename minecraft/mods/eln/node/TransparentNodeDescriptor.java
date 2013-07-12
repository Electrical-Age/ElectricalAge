package mods.eln.node;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.TransparentNode.FrontType;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IRenderContextHandler;

public class TransparentNodeDescriptor extends GenericItemBlockUsingDamageDescriptor implements IItemRenderer{
	public Class ElementClass,RenderClass;
	public TransparentNodeDescriptor(  String name,
							 Class ElementClass,Class RenderClass) {
		super( name);
		this.ElementClass = ElementClass;
		this.RenderClass = RenderClass;
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		// TODO Auto-generated method stub
		
	}
	
	
	public FrontType getFrontType()
	{
		return FrontType.PlayerViewHorizontal;
	}
	public boolean mustHaveFloor()
	{
		return true;
	}
	
	public boolean mustHaveCeiling()
	{
		return false;
	}
	public boolean mustHaveWall()
	{
		return false;
	}
	public boolean mustHaveWallFrontInverse()
	{
		return false;
	}
	public boolean checkCanPlace(Coordonate coord,Direction front) {
		Block block;
		boolean needDestroy = false;
		if(mustHaveFloor())
		{
			Coordonate temp = new Coordonate(coord);
			temp.move(Direction.YN);
			block = temp.getBlock();
			if(block == null || ! block.isOpaqueCube()) needDestroy = true;
		}
		if(mustHaveCeiling())
		{
			Coordonate temp = new Coordonate(coord);
			temp.move(Direction.YP);
			block = temp.getBlock();
			if(block == null || ! block.isOpaqueCube()) needDestroy = true;
		}
		if(mustHaveWallFrontInverse())
		{
			Coordonate temp = new Coordonate(coord);
			temp.move(front.getInverse());
			block = temp.getBlock();
			if(block == null || ! block.isOpaqueCube()) needDestroy = true;
		}
		if(mustHaveWall())
		{
			Coordonate temp;
			boolean wall = false;
			temp = new Coordonate(coord);
			temp.move(Direction.XN);
			block = temp.getBlock();
			if(block != null && block.isOpaqueCube()) wall = true;
			temp = new Coordonate(coord);
			temp.move(Direction.XP);
			block = temp.getBlock();
			if(block != null && block.isOpaqueCube()) wall = true;
			temp = new Coordonate(coord);
			temp.move(Direction.ZN);
			block = temp.getBlock();
			if(block != null && block.isOpaqueCube()) wall = true;
			temp = new Coordonate(coord);
			temp.move(Direction.ZP);
			block = temp.getBlock();
			if(block != null && block.isOpaqueCube()) wall = true;
			
			if(! wall) needDestroy = true;
		}
		
		return ! needDestroy;
	}

	
	public Direction getFrontFromPlace(Direction side,EntityLivingBase entityLiving)
	{
		Direction front = Direction.XN;
		switch(getFrontType())
		{
		case BlockSide:
			front = side;
			break;
		case PlayerView:
			front = Utils.entityLivingViewDirection(entityLiving).getInverse();
			break;
		case PlayerViewHorizontal:
			front = Utils.entityLivingHorizontalViewDirection(entityLiving).getInverse();
			break;
		
		}
		return front;
	}
}
