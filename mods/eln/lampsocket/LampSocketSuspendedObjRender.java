package mods.eln.lampsocket;

import org.lwjgl.opengl.GL11;

import net.minecraft.util.ResourceLocation;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;

public class LampSocketSuspendedObjRender implements LampSocketObjRender{
	private Obj3D obj;
	private Obj3DPart socket,chain,base;
	ResourceLocation tOn,tOff;
	private boolean onOffModel;
	private int length;
	float baseLength,chainLength,chainFactor;
	
	public LampSocketSuspendedObjRender(Obj3D obj,boolean onOffModel,int length) {
		this.obj = obj;
		this.length = length;
		this.onOffModel = onOffModel;
		if(obj != null){
			socket = obj.getPart("socket");
			chain = obj.getPart("chain");
			base = obj.getPart("base");
			tOff = obj.getAlternativeTexture(obj.getString("tOff"));
			tOn = obj.getAlternativeTexture(obj.getString("tOn"));
			chainLength = chain.getFloat("length");
			chainFactor = chain.getFloat("factor");
			baseLength = base.getFloat("length");
		}
	}	

	@Override
	public void draw(LampSocketDescriptor descriptor) {
		draw(LRDU.Up, 0, (byte) 0,0,0);
	}

	@Override
	public void draw(LampSocketRender render) {
		draw(render.front,render.alphaZ,render.light,render.pertuPy,render.pertuPz);
	}

	public void draw(LRDU front, float alphaZ,byte light,float pertuPy,float pertuPz) {
		//front.glRotateOnX();
		pertuPy /= length;
		pertuPz /= length;
		
		
		base.draw();

		
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glTranslatef(baseLength,0, 0);

		for(int idx = 0;idx < length;idx++){
			GL11.glRotatef(pertuPy, 0, 1, 0);
			GL11.glRotatef(pertuPz, 0, 0, 1);
			chain.draw();
			GL11.glTranslatef(chainLength, 0, 0);
		}
		GL11.glRotatef(pertuPy, 0, 1, 0);
		GL11.glRotatef(pertuPz, 0, 0, 1);
		
		GL11.glEnable(GL11.GL_CULL_FACE);
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
		

	}		
}
