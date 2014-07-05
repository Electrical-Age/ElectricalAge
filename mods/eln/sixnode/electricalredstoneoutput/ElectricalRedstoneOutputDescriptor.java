package mods.eln.sixnode.electricalredstoneoutput;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.Eln;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.wiki.Data;

import com.google.common.base.Function;

public class ElectricalRedstoneOutputDescriptor extends SixNodeDescriptor {

	public float[] pinDistance;

	public ElectricalRedstoneOutputDescriptor(String name, Obj3D obj) {
		super(name, ElectricalRedstoneOutputElement.class, ElectricalRedstoneOutputRender.class);
		this.obj = obj;
		if(obj != null) {
			main = obj.getPart("main");
			led = obj.getPart("led");
			

			pinDistance = Utils.getSixNodePinDistance(main);
		}
	}

	Obj3D obj;
	Obj3DPart main,led;
	
	void draw(int redstone) {
		//LRDU.Down.glRotateOnX();
		if(main != null) main.draw();
		
		float light = redstone / 15f;
		GL11.glColor4f(light, light, light, 1f);
		UtilsClient.drawLight(led);
	}
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addSignal(newItemStack());
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
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if(type == ItemRenderType.INVENTORY) GL11.glScalef(2.8f, 2.8f, 2.8f);
		draw(15);
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Converts voltage signal");
		list.add("to redstone signal.");
	}
}
