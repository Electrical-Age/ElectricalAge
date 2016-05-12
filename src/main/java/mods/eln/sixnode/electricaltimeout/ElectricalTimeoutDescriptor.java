package mods.eln.sixnode.electricaltimeout;

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

public class ElectricalTimeoutDescriptor extends SixNodeDescriptor {

    Obj3D obj;
    Obj3DPart main, rot, led;
    float rotStart, rotEnd;

    String tickSound = null;
    float tickVolume = 0f;

	public ElectricalTimeoutDescriptor(String name, Obj3D obj) {
		super(name, ElectricalTimeoutElement.class, ElectricalTimeoutRender.class);
		if (obj != null) {
			main = obj.getPart("main");
			rot = obj.getPart("rot");
			if (rot != null) {
				rotStart = rot.getFloat("rotStart");
				rotEnd = rot.getFloat("rotEnd");
			}
			led  = obj.getPart("led");
		}

		voltageLevelColor = VoltageLevelColor.SignalVoltage;
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		Collections.addAll(list, tr("Upon application of a high signal,\nthe timer maintains the output high for\na configurable interval. Can be re-triggered.").split("\n"));
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

	void draw(float left) {
		if (main != null) main.draw();
		if (rot != null) {
			rot.draw(rotEnd + (rotStart - rotEnd) * left, 1f, 0f, 0f);
		}
		if (led != null) {
			UtilsClient.ledOnOffColor(left != 0f);
			UtilsClient.drawLight(led);
			GL11.glColor3f(1f, 1f, 1f);
		}
	}

	public ElectricalTimeoutDescriptor setTickSound(String tickSound, float tickVolume) {
		this.tickSound = tickSound;
		this.tickVolume = tickVolume;
		return this;
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
		}
		draw(1f);
	}

	@Override
	public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
		return super.getFrontFromPlace(side, player).right();
	}
}
