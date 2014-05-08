package mods.eln.lampsocket;

import org.lwjgl.opengl.GL11;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;

public class LampSocketStandardObjRender implements LampSocketObjRender {
	private Obj3D obj;
	private Obj3DPart socket,socket_unlightable,socket_lightable;
	ResourceLocation tOn, tOff;
	private boolean onOffModel;

	public LampSocketStandardObjRender(Obj3D obj, boolean onOffModel) {
		this.obj = obj;
		this.onOffModel = onOffModel;
		if (obj != null) {
			socket = obj.getPart("socket");
			socket_unlightable = obj.getPart("socket_unlightable");
			socket_lightable = obj.getPart("socket_lightable");
			tOff = obj.getAlternativeTexture(obj.getString("tOff"));
			tOn = obj.getAlternativeTexture(obj.getString("tOn"));
		}
	}

	@Override
	public void draw(LampSocketDescriptor descriptor, ItemRenderType type) {
		if (type == ItemRenderType.INVENTORY) {
			if (descriptor.hasGhostGroup()) {
				GL11.glScalef(0.5f, 0.5f, 0.5f);
				GL11.glRotatef(90, 0, -1, 0);
				GL11.glTranslatef(-1.5f, 0f, 0f);
			}

		}
		else if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
			if (descriptor.hasGhostGroup()) {
				GL11.glScalef(0.3f, 0.3f, 0.3f);
				GL11.glRotatef(90, 0, -1, 0);
				GL11.glTranslatef(-0.5f, 0f, -1f);
			}
		}
		draw(LRDU.Up, 0, (byte) 0);
	}

	@Override
	public void draw(LampSocketRender render) {
		draw(render.front, render.alphaZ, render.light);
	}

	public void draw(LRDU front, float alphaZ, byte light) {
		front.glRotateOnX();

		GL11.glDisable(GL11.GL_CULL_FACE);
		if (onOffModel == false) {
			if(socket != null) socket.draw();
		}
		else {
			//
			if (light > 5) {
				UtilsClient.bindTexture(tOn);
			}
			else
				UtilsClient.bindTexture(tOff);

			if(socket_unlightable != null) socket_unlightable.drawNoBind();
			
			if (light > 5) {
				UtilsClient.disableLight();
				if(socket_lightable != null) socket_lightable.drawNoBind();
			}
			
			if(socket != null) socket.drawNoBind();
			
			if (light > 5) 
				UtilsClient.enableLight();
			//
		}
		GL11.glEnable(GL11.GL_CULL_FACE);
		/*
		 * GL11.glLineWidth(2f); GL11.glDisable(GL11.GL_TEXTURE_2D);
		 * GL11.glDisable(GL11.GL_LIGHTING); GL11.glColor3f(1f,1f,1f);
		 * GL11.glBegin(GL11.GL_LINES); GL11.glVertex3d(0f, 0f, 0f);
		 * GL11.glVertex3d(Math.cos(alphaZ*Math.PI/180.0),
		 * Math.sin(alphaZ*Math.PI/180.0),0.0); GL11.glEnd();
		 * GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_LIGHTING);
		 */
	}
}
