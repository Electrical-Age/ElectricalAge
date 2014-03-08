package mods.eln.lampsocket;

import java.util.List;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

public class LampSocketDescriptor extends SixNodeDescriptor{
	public LampSocketType socketType;
	LampSocketObjRender render;

	public LampSocketDescriptor(String name,LampSocketObjRender render,
								LampSocketType socketType,
								int range,
								float alphaZMin,float alphaZMax,
								float alphaZBoot
			) 
	{
		super(name, LampSocketElement.class,LampSocketRender.class);
		this.socketType = socketType;
		this.range = range;
		this.alphaZMin = alphaZMin;
		this.alphaZMax = alphaZMax;
		this.alphaZBoot = alphaZBoot;
		this.render = render;

	}

	public void setParent(net.minecraft.item.Item item, int damage)
	{
		super.setParent(item, damage);
		Data.addLight(newItemStack());
	}
	

	public int range;
	public String modelName;
	float alphaZMin,alphaZMax,alphaZBoot;

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		// TODO Auto-generated method stub
		if(type == ItemRenderType.INVENTORY){
			if(hasGhostGroup()){
				GL11.glScalef(0.5f, 0.5f, 0.5f);
				GL11.glRotatef(90, 0, -1, 0);
				GL11.glTranslatef(-1.5f,0f, 0f);
			}
			
		}
		else if(type == ItemRenderType.EQUIPPED_FIRST_PERSON){
			if(hasGhostGroup()){
				GL11.glScalef(0.3f, 0.3f, 0.3f);
				GL11.glRotatef(90, 0, -1, 0);
				GL11.glTranslatef(-0.5f,0f, -1f);
			}
		}
		render.draw(this);
	}

	@Override
	public boolean hasVolume() {
		// TODO Auto-generated method stub
		return hasGhostGroup();
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Socket type : " + socketType.toString());
		
		if(range != 0 || alphaZMin != alphaZMax){
			list.add("Projector");
			if(range != 0){
				list.add("  range : " + range + " Blocks");
			}
			if(alphaZMin != alphaZMax){
				list.add("  angle  : " + ((int)alphaZMin) + "\u00B0 to " + ((int)alphaZMax) + "\u00B0");
			}
		}
	}
}	
	

