package mods.eln.lampsocket;

import java.util.List;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

public class LampSocketDescriptor extends SixNodeDescriptor{
	public LampSocketType socketType;
	private Obj3D obj;
	private Obj3DPart socket;
	public LampSocketDescriptor(String name, Obj3D obj,boolean onOffModel,
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
		this.onOffModel = onOffModel;
		this.obj = obj;
		
		if(obj != null){
			socket = obj.getPart("socket");
			tOff = obj.getAlternativeTexture(obj.getString("tOff"));
			tOn = obj.getAlternativeTexture(obj.getString("tOn"));
		}
	}

	ResourceLocation tOn,tOff;
	public int range;
	public String modelName;
	float alphaZMin,alphaZMax,alphaZBoot;
	boolean onOffModel;
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
		draw(LRDU.Up, 0, (byte) 0);
	}
	public void draw(LRDU front, float alphaZ,byte light) {
		front.glRotateOnX();
		
		GL11.glDisable(GL11.GL_CULL_FACE);
		if(onOffModel == false){
			socket.draw();
		}
		else{
			if(light > 5)
				Utils.bindTexture(tOn);
			else
				Utils.bindTexture(tOff);
			socket.drawNoBind();
		}
		GL11.glEnable(GL11.GL_CULL_FACE);
	/*	GL11.glLineWidth(2f);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
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
	

