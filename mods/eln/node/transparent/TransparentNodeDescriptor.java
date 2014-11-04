package mods.eln.node.transparent;

import java.util.List;

import org.lwjgl.opengl.GL11;

import mods.eln.Translator;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNode.FrontType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHopper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IRenderContextHandler;

public class TransparentNodeDescriptor extends GenericItemBlockUsingDamageDescriptor implements IItemRenderer{
	public Class ElementClass,RenderClass;
	public TransparentNodeDescriptor(  String name,
							 Class ElementClass,Class RenderClass) {
		super( name);
		this.ElementClass = ElementClass;
		this.RenderClass = RenderClass;
		
	}
	
	
	protected GhostGroup ghostGroup = null;
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		
		return false;
	}
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		
		return false;
	}
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		
		
	}
	
	public void objItemScale(Obj3D obj)
	{
		if(obj == null) return;
		float factor = obj.yDim*0.6f;
		//factor = obj.dimMaxInv*1.0f;
		factor = Math.max(factor, (Math.max(obj.zMax,-obj.xMin) + Math.max(obj.xMax,-obj.zMin))*0.7f);
		factor = 1f/factor;
		
		GL11.glScalef(factor,factor,factor);
		//GL11.glTranslatef((Math.max(obj.zMax,-obj.xMin) - Math.max(obj.xMax,-obj.zMin))*0.5f,-((obj.yMax + Math.max(-obj.xMin,obj.zMax)*0.3f) + (obj.yMin + Math.min(obj.zMin,-obj.xMax)*0.3f))*0.4f,0.0f);
		GL11.glTranslatef((Math.min(obj.zMin,obj.xMin) + Math.max(obj.xMax,obj.zMax))/2 -(obj.xMax+obj.xMin)/2,1.0f -(obj.xMax+obj.xMin)/2-(obj.zMax+obj.zMin)/2 -(obj.yMax+obj.yMin)/2,0.0f);
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
	public String checkCanPlace(Coordonate coord,Direction front) {
		Block block;
		boolean needDestroy = false;
		if(mustHaveFloor())
		{
			Coordonate temp = new Coordonate(coord);
			temp.move(Direction.YN);
			block = temp.getBlock();
			if(block == null || ((! block.isOpaqueCube()) && block instanceof BlockHopper == false)) return Translator.translate("eln.core.tile.block.cantplaceside");
		}
		if(mustHaveCeiling())
		{
			Coordonate temp = new Coordonate(coord);
			temp.move(Direction.YP);
			block = temp.getBlock();
			if(block == null || ! block.isOpaqueCube()) return Translator.translate("eln.core.tile.block.cantplaceside");
		}
		if(mustHaveWallFrontInverse())
		{
			Coordonate temp = new Coordonate(coord);
			temp.move(front.getInverse());
			block = temp.getBlock();
			if(block == null || ! block.isOpaqueCube()) return Translator.translate("eln.core.tile.block.cantplaceside");
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
			
			if(! wall) return Translator.translate("eln.core.tile.block.cantplaceside");
		}
		
		GhostGroup ghostGroup = getGhostGroup(front);
		if(ghostGroup != null && ghostGroup.canBePloted(coord) == false) return Translator.translate("eln.core.tile.block.nonenoughspace");
		return null;
	}

	
	public Direction getFrontFromPlace(Direction side,EntityLivingBase entityLiving)
	{
		Direction front = Direction.XN;
		switch(getFrontType())
		{
		case BlockSide:
			front = side;
			break;
		case BlockSideInv:
			front = side.getInverse();
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
	public boolean hasGhostGroup()
	{
		return ghostGroup != null;
	}
	public GhostGroup getGhostGroup(Direction front) {
		if(ghostGroup == null) return null;
		return ghostGroup.newRotate(front);
	}
	public int getGhostGroupUuid() {
		
		return -1;
	}
	public int getSpawnDeltaX() {
		
		return 0;
	}
	public int getSpawnDeltaY() {
		
		return 0;
	}
	public int getSpawnDeltaZ() {
		
		return 0;
	}
	public void addCollisionBoxesToList(AxisAlignedBB par5AxisAlignedBB,List list, TransparentNodeEntity entity) {
		AxisAlignedBB bb = Blocks.stone.getCollisionBoundingBoxFromPool(entity.getWorldObj(),entity.xCoord,entity.yCoord,entity.zCoord);
		if(par5AxisAlignedBB.intersectsWith(bb)) list.add(bb);
	}
	
	public void setGhostGroup(GhostGroup ghostGroup)
	{
		this.ghostGroup = ghostGroup;
	}
}
