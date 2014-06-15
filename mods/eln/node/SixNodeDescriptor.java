package mods.eln.node;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Random;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.ItemRender;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.ForgeHooks;

public class SixNodeDescriptor extends GenericItemBlockUsingDamageDescriptor implements IItemRenderer{
	public Class ElementClass,RenderClass;
	public SixNodeDescriptor(String name,
							 Class ElementClass,Class RenderClass) {
		super(name);
		this.ElementClass = ElementClass;
		this.RenderClass = RenderClass;
		// TODO Auto-generated constructor stub
	}
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		if(type == ItemRenderType.INVENTORY)
			return false;
		return true;
		//return false;
	}

	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		//RenderBlocks b = (RenderBlocks)data[0];
		//MinecraftForgeClient.getItemRenderer(item, type)
		// IItemRenderer r = MinecraftForgeClient.getItemRenderer(new ItemStack(Items.bread), IItemRenderer.ItemRenderType.ENTITY);
		// int i = 0;
		
		
		//if(type == ItemRenderType.ENTITY)
			//ForgeHooksClient.renderEntityItem((EntityItem)data[1], item, 0, 0, new Random(), Minecraft.getMinecraft().getTextureManager(),(RenderBlocks) data[0], 1);
		//new ItemRenderer(Minecraft.getMinecraft()).renderItem(Minecraft.getMinecraft().thePlayer,item,1,ItemRenderType.ENTITY);
		
		UtilsClient.drawIcon(type,getIcon().getIconName().replace("eln:", "textures/blocks/")+".png");

	//	Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation("eln", getIcon().getIconName().replace("eln:", "textures/blocks/")+".png"));
		/*UtilsClient.disableCulling();
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(1f, 0f);
		GL11.glVertex3f(0f,16f, 0f);
		GL11.glTexCoord2f(0f, 0f);
		GL11.glVertex3f(0f,0f, 0f);
		GL11.glTexCoord2f(0f, 1f);
		GL11.glVertex3f(0f,0f, 16f);
		GL11.glTexCoord2f(1f, 1f);
		GL11.glVertex3f(0f,16f, 16f);
		GL11.glEnd();
		UtilsClient.enableCulling();*/
	}
	public boolean hasVolume() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean canBePlacedOnSide(EntityPlayer player,Coordonate c,Direction side){
		return canBePlacedOnSide(player,side);
	}
	
	public boolean canBePlacedOnSide(EntityPlayer player,Direction side)
	{
		if(placeDirection != null){
			boolean ok = false;
			for(Direction d : placeDirection){
				if(d == side) {
					return true;
				}
			}
			Utils.addChatMessage(player,"You can't place this block at this side");
			return false;
		}
		return true;
	}

	
	public void setGhostGroup(GhostGroup ghostGroup)
	{
		this.ghostGroup = ghostGroup;
	}
	
	protected GhostGroup ghostGroup = null;
	
	public boolean hasGhostGroup()
	{
		return ghostGroup != null;
	}
	public GhostGroup getGhostGroup(Direction side,LRDU front) {
		if(ghostGroup == null) return null;
		return ghostGroup.newRotate(side,front);
	}
	public int getGhostGroupUuid() {
		// TODO Auto-generated method stub
		return -1;
	}
	
	public void setPlaceDirection(Direction d)
	{
		placeDirection = new Direction[]{d};
	}	
	public void setPlaceDirection(Direction []d)
	{
		placeDirection = d;
	}
	
	protected Direction[] placeDirection = null;
	public String checkCanPlace(Coordonate coord, Direction direction,LRDU front) {
		if(placeDirection != null){
			boolean ok = false;
			for(Direction d : placeDirection){
				if(d == direction) {
					ok = true;
					break;
				}
			}
			if(ok == false)
				return "Can not be placed at this side";
		}
		GhostGroup ghostGroup = getGhostGroup(direction,front);
		if(ghostGroup != null && ghostGroup.canBePloted(coord) == false) return "Not enough space for this block";
		return null;
	}
	
	
}
