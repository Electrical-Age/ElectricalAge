package mods.eln.lampsocket;

import java.util.List;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.misc.LRDU;
import mods.eln.node.SixNodeDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

public class LampSocketDescriptor extends SixNodeDescriptor{
	public LampSocketType socketType;
	public LampSocketDescriptor(String name, String modelName,
								LampSocketType socketType,
								int range,
								float alphaZMin,float alphaZMax,
								float alphaZBoot
			) 
	{
		super(name, LampSocketElement.class,LampSocketRender.class);
		this.socketType = socketType;
		this.modelName = modelName;
		this.range = range;
		this.alphaZMin = alphaZMin;
		this.alphaZMax = alphaZMax;
		this.alphaZBoot = alphaZBoot;
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
		Eln.obj.draw(modelName, "socket");
	}
	public void draw(LRDU front, float alphaZ) {
		front.glRotateOnX();
		
		GL11.glDisable(GL11.GL_CULL_FACE);
		Eln.obj.draw(modelName, "socket");
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glLineWidth(2f);
	/*	GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glColor3f(1f,1f,1f);
		GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex3d(0f, 0f, 0f);
			GL11.glVertex3d(Math.cos(alphaZ*Math.PI/180.0), Math.sin(alphaZ*Math.PI/180.0),0.0);
		GL11.glEnd();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_LIGHTING);*/
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
	

