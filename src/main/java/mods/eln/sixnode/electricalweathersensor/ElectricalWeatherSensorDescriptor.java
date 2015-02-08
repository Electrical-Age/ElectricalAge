package mods.eln.sixnode.electricalweathersensor;

import java.util.List;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

public class ElectricalWeatherSensorDescriptor extends SixNodeDescriptor {

	private Obj3DPart main;
	public float[] pinDistance;

    Obj3D obj;

	public ElectricalWeatherSensorDescriptor(String name, Obj3D obj) {
		super(name, ElectricalWeatherSensorElement.class, ElectricalWeatherSensorRender.class);
		this.obj = obj;
		
		if (obj != null) {
			main = obj.getPart("main");

			pinDistance = Utils.getSixNodePinDistance(main);
		}
	}

	void draw() {
		UtilsClient.disableCulling();
		if (main != null) main.draw();
		UtilsClient.enableCulling();
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
		list.add("0V -> Clear");
		list.add(Eln.SVU / 2 + "V -> Rain");
		list.add(Eln.SVU + "V -> Thunder");
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
		GL11.glScalef(2f, 2f, 2f);
		draw();
	}
}
