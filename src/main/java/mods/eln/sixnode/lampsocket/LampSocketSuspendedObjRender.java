package mods.eln.sixnode.lampsocket;

import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import org.lwjgl.opengl.GL11;

public class LampSocketSuspendedObjRender implements LampSocketObjRender {

	private Obj3D obj;
	private Obj3DPart socket, chain, base;
	ResourceLocation tOn, tOff;
	private boolean onOffModel;
	private int length;
	float baseLength, chainLength, chainFactor;

	public LampSocketSuspendedObjRender(Obj3D obj, boolean onOffModel, int length) {
		this.obj = obj;
		this.length = length;
		this.onOffModel = onOffModel;
		if (obj != null) {
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
	public void draw(LampSocketDescriptor descriptor, ItemRenderType type) {
		if (type == ItemRenderType.INVENTORY) {
			GL11.glScalef(0.5f, 0.5f, 0.5f);
			GL11.glRotatef(90, 0, 1, 0);
			GL11.glTranslatef(-1.5f, 0f, 0f);
		} else if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
			GL11.glScalef(0.3f, 0.3f, 0.3f);
			GL11.glRotatef(45, 0, 1, 0);
			GL11.glTranslatef(-1.5f, 0f, 0.4f);
		}
		draw(LRDU.Up, 0, (byte) 0, 0, 0);
	}

	@Override
	public void draw(LampSocketRender render) {
		draw(render.front, render.alphaZ, render.light, render.pertuPy, render.pertuPz);
	}

	public void draw(LRDU front, float alphaZ, byte light, float pertuPy, float pertuPz) {
		// front.glRotateOnX();
		pertuPy /= length;
		pertuPz /= length;

		base.draw();

		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glTranslatef(baseLength, 0, 0);

		for (int idx = 0; idx < length; idx++) {
			GL11.glRotatef(pertuPy, 0, 1, 0);
			GL11.glRotatef(pertuPz, 0, 0, 1);
			chain.draw();
			GL11.glTranslatef(chainLength, 0, 0);
		}
		GL11.glRotatef(pertuPy, 0, 1, 0);
		GL11.glRotatef(pertuPz, 0, 0, 1);

		GL11.glEnable(GL11.GL_CULL_FACE);
		if (!onOffModel) {
			socket.draw();
		} else {
			if (light > 8) {
				float l = (light) / 14f;
				GL11.glColor3f(l, l, l);

				UtilsClient.bindTexture(tOn);
			} else
				UtilsClient.bindTexture(tOff);
			socket.drawNoBind();

			if (light > 8) {
				UtilsClient.disableLight();

			}

			if (socket != null) socket.drawNoBind();

			if (light > 8) {
				UtilsClient.enableLight();
				GL11.glColor3f(1f, 1f, 1f);
			}
		}
	}
}
