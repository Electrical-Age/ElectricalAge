package mods.eln.lampsocket;

import org.lwjgl.opengl.GL11;

import net.minecraft.util.ResourceLocation;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;

public class LampSocketStandardObjRender implements LampSocketObjRender{
	private Obj3D obj;
	private Obj3DPart socket;
	ResourceLocation tOn,tOff;
	private boolean onOffModel;
	
	public LampSocketStandardObjRender(Obj3D obj,boolean onOffModel) {
		this.obj = obj;
		this.onOffModel = onOffModel;
		if(obj != null){
			socket = obj.getPart("socket");
			tOff = obj.getAlternativeTexture(obj.getString("tOff"));
			tOn = obj.getAlternativeTexture(obj.getString("tOn"));
		}
	}	

	@Override
	public void draw(LampSocketDescriptor descriptor) {
		draw(LRDU.Up, 0, (byte) 0);
	}

	@Override
	public void draw(LampSocketRender render) {
		draw(render.front,render.alphaZ,render.light);
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
}
