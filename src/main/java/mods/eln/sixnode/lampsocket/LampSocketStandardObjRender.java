package mods.eln.sixnode.lampsocket;

import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import org.lwjgl.opengl.GL11;

public class LampSocketStandardObjRender implements LampSocketObjRender {

	private Obj3D obj;
	private Obj3DPart socket, socket_unlightable, socket_lightable, lampOn, lampOff;
	ResourceLocation tOn, tOff;
	private boolean onOffModel;

	public LampSocketStandardObjRender(Obj3D obj, boolean onOffModel) {
		this.obj = obj;
		this.onOffModel = onOffModel;
		if (obj != null) {
			socket = obj.getPart("socket");
			lampOn = obj.getPart("lampOn");
			lampOff = obj.getPart("lampOff");
			socket_unlightable = obj.getPart("socket_unlightable");
			socket_lightable = obj.getPart("socket_lightable");
			tOff = obj.getModelResourceLocation(obj.getString("tOff"));
			tOn = obj.getModelResourceLocation(obj.getString("tOn"));
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
		} else if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
			if (descriptor.hasGhostGroup()) {
				GL11.glScalef(0.3f, 0.3f, 0.3f);
				GL11.glRotatef(90, 0, -1, 0);
				GL11.glTranslatef(-0.5f, 0f, -1f);
			}
		}
		draw(LRDU.Up, 0, (byte) 0, true);
	}

	@Override
	public void draw(LampSocketRender render) {
		draw(render.front, render.alphaZ, render.light, render.lampDescriptor != null);
	}

	public void draw(LRDU front, float alphaZ, byte light, boolean hasBulb) {
		front.glRotateOnX();

		UtilsClient.disableCulling();
		if (!onOffModel) {
			if (socket != null) socket.draw();
		} else {
			//
			if (light > 8) {
				UtilsClient.bindTexture(tOn);
			} else
				UtilsClient.bindTexture(tOff);

			if (socket_unlightable != null) socket_unlightable.drawNoBind();

			if (light > 8) {
				UtilsClient.disableLight();
				float l = (light) / 14f;
				GL11.glColor3f(l, l, l);
				if (socket_lightable != null) socket_lightable.drawNoBind();
				GL11.glColor3f(1f, 1f, 1f);
			}

			if (hasBulb) {
				if (light > 8) {
					if (lampOn != null) lampOn.draw();
				} else {
					if (lampOff != null) lampOff.draw();
				}
			}
			if (socket != null) socket.drawNoBind();

			if (light > 8)
				UtilsClient.enableLight();
			//
		}
		UtilsClient.enableCulling();
		/*
		 * GL11.glLineWidth(2f); GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_LIGHTING); GL11.glColor3f(1f,1f,1f); GL11.glBegin(GL11.GL_LINES); GL11.glVertex3d(0f, 0f, 0f); GL11.glVertex3d(Math.cos(alphaZ*Math.PI/180.0), Math.sin(alphaZ*Math.PI/180.0),0.0); GL11.glEnd(); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_LIGHTING);
		 */
	}
}
