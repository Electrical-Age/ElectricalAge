package mods.eln.sixnode.electricalalarm;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.Eln;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.misc.IFunction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.wiki.Data;

import com.google.common.base.Function;

public class ElectricalAlarmDescriptor extends SixNodeDescriptor {

	public float[] pinDistance;

	public ElectricalAlarmDescriptor(
			String name,
			Obj3D obj,
			int light,
			String soundName, double soundTime, float soundLevel) {
		super(name, ElectricalAlarmElement.class, ElectricalAlarmRender.class);
		this.obj = obj;
		this.soundName = soundName;
		this.soundTime = soundTime;
		this.soundLevel = soundLevel;
		this.light = light;
		if(obj != null) {
			main = obj.getPart("main");
			rot = obj.getPart("rot");
			lightPart = obj.getPart("light");
			
			onTexture = obj.getAlternativeTexture(obj.getString("onTexture"));
			offTexture = obj.getAlternativeTexture(obj.getString("offTexture"));
			if(rot != null) {
				rotSpeed = rot.getFloat("speed");
			}
			pinDistance = Utils.getSixNodePinDistance(main);
		}
	}
	int light;
	Obj3D obj;
	Obj3DPart main, rot, lightPart;
	
	ResourceLocation onTexture, offTexture;
	String soundName;
	double soundTime;
	float soundLevel;
	public float rotSpeed = 0f;
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addUtilities(newItemStack());
	}
	
	void draw(boolean warm, float rotAlpha) {
		if(warm) UtilsClient.bindTexture(onTexture);
		else UtilsClient.bindTexture(offTexture);
		if(main != null) main.drawNoBind();
		if(rot != null) {
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glColor3f(1.0f, 1.0f, 1.0f);
			if(warm) UtilsClient.disableLight();
			else GL11.glDisable(GL11.GL_LIGHTING);
			rot.drawNoBind(rotAlpha,1f,0f,0f);
			if(warm) UtilsClient.enableLight();
			else GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_CULL_FACE);
		}
		if(lightPart != null) {
			UtilsClient.drawLightNoBind(lightPart);
		}
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		return true;
	}
	@Override
	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		
		return true;
	}
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if(type == ItemRenderType.INVENTORY) {
			GL11.glScalef(1.6f, 1.6f, 1.6f);
			GL11.glTranslatef(-0.1f, 0.0f, 0f);
			LRDU.Up.glRotateOnX();
		}
		draw(true,0.0f);
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Emit a sonor alarm when");
		list.add("the input signal is high");
	}
}
