package mods.eln.sixnode.electricalredstoneoutput;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalRedstoneOutputDescriptor extends SixNodeDescriptor {

	public float[] pinDistance;

    Obj3D obj;
    Obj3DPart main, led;

	public ElectricalRedstoneOutputDescriptor(String name, Obj3D obj) {
		super(name, ElectricalRedstoneOutputElement.class, ElectricalRedstoneOutputRender.class);
		this.obj = obj;
		if (obj != null) {
			main = obj.getPart("main");
			led = obj.getPart("led");

			pinDistance = Utils.getSixNodePinDistance(main);
		}

		voltageLevelColor = VoltageLevelColor.SignalVoltage;
	}

	void draw(int redstone) {
		//LRDU.Down.glRotateOnX();
		if (main != null) main.draw();
		
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
	public boolean use2DIcon() {
		return true;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return type != ItemRenderType.INVENTORY;
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}

	@Override
	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return type != ItemRenderType.INVENTORY;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if (type == ItemRenderType.INVENTORY) {
			super.renderItem(type, item, data);
		} else {
			draw(15);
		}
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		Collections.addAll(list, tr("Converts electrical voltage\ninto a Redstone signal.").split("\n"));
	}

	@Override
	public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
		return super.getFrontFromPlace(side, player).right();
	}
}
