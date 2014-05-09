package mods.eln.electricalentitysensor;

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

public class ElectricalEntitySensorDescriptor extends SixNodeDescriptor {

	boolean useEntitySpeed = true;
	double speedFactor = 1 / 0.10;
	private Obj3DPart detector,haloMask;
	double maxRange;
	public ElectricalEntitySensorDescriptor(String name, Obj3D obj, double maxRange) {
		super(name, ElectricalEntitySensorElement.class, ElectricalEntitySensorRender.class);
		this.obj = obj;
		this.maxRange = maxRange;
		if(obj != null) {
			detector = obj.getPart("Detector");
			haloMask = obj.getPart("HaloMask");
		}
	}

	Obj3D obj;

	void draw(boolean state,EntitySensorFilterDescriptor filter) {
		if(detector != null) detector.draw();
		if(state){
			if(filter == null){
				GL11.glColor3f(1f, 1f, 0f);
			}
			else{
				filter.glColor();
			}
			UtilsClient.drawLight(haloMask);
		}
	}
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addSignal(newItemStack());
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Max range : " + (int)maxRange);
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
		draw(false,null);
	}
}
