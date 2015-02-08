package mods.eln.sixnode.electricalwatch;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ElectricalWatchDescriptor extends SixNodeDescriptor {

	private Obj3DPart base, cHour, cMin;
	double powerConsumtion;

    Obj3D obj;

	public ElectricalWatchDescriptor(String name, Obj3D obj, double powerConsumtion) {
		super(name, ElectricalWatchElement.class, ElectricalWatchRender.class);
		this.obj = obj;
		this.powerConsumtion = powerConsumtion;
		if (obj != null) {
			base = obj.getPart("base");
			cHour = obj.getPart("cHour");
			cMin = obj.getPart("cMin");
		}
	}

	void draw(float hour, float min) {
		if (base != null) base.draw();
		if (cHour != null) cHour.draw(360 * hour, -1, 0, 0);
		if (cMin != null) cMin.draw(360 * min, -1, 0, 0);
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
	public boolean use2DIcon() {
		return false;
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
	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		draw(0.1f, 0.2f);
	}
}
