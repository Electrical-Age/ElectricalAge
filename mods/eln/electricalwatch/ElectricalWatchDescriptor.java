package mods.eln.electricalwatch;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.wiki.Data;

import com.google.common.base.Function;

public class ElectricalWatchDescriptor extends SixNodeDescriptor {


	private Obj3DPart base,cHour,cMin;
	double powerConsumtion;
	public ElectricalWatchDescriptor(String name, Obj3D obj, double powerConsumtion) {
		super(name, ElectricalWatchElement.class, ElectricalWatchRender.class);
		this.obj = obj;
		this.powerConsumtion = powerConsumtion;
		if(obj != null) {
			base = obj.getPart("base");
			cHour = obj.getPart("cHour");
			cMin = obj.getPart("cMin");
		}
	}

	Obj3D obj;

	void draw(float hour,float min) {
		if(base != null) base.draw();
		if(cHour != null) cHour.draw(360*hour, -1, 0, 0);
		if(cMin != null) cMin.draw(360*min, -1, 0, 0);
	}
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		//Data.addSignal(newItemStack());
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		//list.add("Max range : " + (int)maxRange);
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
		draw(0.1f,0.2f);
	}
}
