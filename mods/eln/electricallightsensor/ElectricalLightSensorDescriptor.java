package mods.eln.electricallightsensor;

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
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.wiki.Data;

import com.google.common.base.Function;

public class ElectricalLightSensorDescriptor extends SixNodeDescriptor {

	private Obj3DPart main;
	public boolean dayLightOnly;
	public ElectricalLightSensorDescriptor(String name, Obj3D obj, boolean dayLightOnly) {
		super(name, ElectricalLightSensorElement.class, ElectricalLightSensorRender.class);
		this.obj = obj;
		this.dayLightOnly = dayLightOnly;
		
		if(obj != null) {
			main = obj.getPart("main");
		}
	}

	Obj3D obj;

	void draw() {
		if(main != null) main.draw();
	}
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addSignal(newItemStack());
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		if(dayLightOnly) {
			list.add("Provides an electrical signal");
			list.add("with strength proportional to");
			list.add("the amount of daylight.");
			list.add("0V at night, " + Eln.SVU + "V at midday.");
		}
		else {
			list.add("Provides an electrical signal");
			list.add("whilst in the presense of light.");		
		}
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		return true;
	}
	@Override
	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		GL11.glScalef(2f, 2f, 2f);
		draw();
	}
}
