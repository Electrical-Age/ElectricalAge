package mods.eln.sixnode.electricaltimeout;

import java.util.List;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

public class ElectricalTimeoutDescriptor extends SixNodeDescriptor {

	public ElectricalTimeoutDescriptor(String name, Obj3D obj) {
		super(name, ElectricalTimeoutElement.class, ElectricalTimeoutRender.class);
		if(obj != null) {
			main = obj.getPart("main");
			rot = obj.getPart("rot");
			if(rot != null) {
				rotStart = rot.getFloat("rotStart");
				rotEnd = rot.getFloat("rotEnd");
			}
			led  = obj.getPart("led");
		}
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("When the input signal is high this");
		list.add("maintains a high output signal");
		list.add("for a defined time.");
	}
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addSignal(newItemStack());
	}
	
	Obj3D obj;
	Obj3DPart main,rot,led;
	float rotStart,rotEnd;
	
	void draw(float left) {
		if(main != null) main.draw();
		if(rot != null) {
			rot.draw(rotEnd + (rotStart - rotEnd) * left, 1f, 0f, 0f);
		}
		if(led != null) {
			UtilsClient.ledOnOffColor(left != 0f);
			UtilsClient.drawLight(led);
			GL11.glColor3f(1f, 1f, 1f);
		}
	}
	
	String tickSound = null;
	float tickVolume = 0f;
	
	public ElectricalTimeoutDescriptor setTickSound(String tickSound, float tickVolume)
	{
		this.tickSound = tickSound;
		this.tickVolume = tickVolume;
		return this;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}
	@Override
	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if(type == ItemRenderType.INVENTORY) {
			GL11.glScalef(2.2f, 2.2f, 2.2f);
		}
		draw(1f);
	}
}
