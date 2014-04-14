package mods.eln.electricalweathersensor;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

public class ElectricalWeatherSensorDescriptor extends SixNodeDescriptor {

	private Obj3DPart main;

	public ElectricalWeatherSensorDescriptor(String name, Obj3D obj) {
		super(name, ElectricalWeatherSensorElement.class,ElectricalWeatherSensorRender.class);
		this.obj = obj;
		
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
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Provides an electrical signal");
		list.add("dependant on weather type.");
		list.add("0V -> clear ");
		list.add(Eln.SVU/2 + "V -> rain ");
		list.add(Eln.SVU + "V -> thunder ");
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		GL11.glScalef(2f, 2f, 2f);
		draw();
	}
}
