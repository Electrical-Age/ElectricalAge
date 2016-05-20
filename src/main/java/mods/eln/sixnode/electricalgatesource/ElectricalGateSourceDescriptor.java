package mods.eln.sixnode.electricalgatesource;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalGateSourceDescriptor extends SixNodeDescriptor {

	public boolean onOffOnly;

	public boolean autoReset = false;

    enum ObjType {Pot, Button}
    ObjType objType;
    float leverTx;
    ElectricalGateSourceRenderObj render;

	public ElectricalGateSourceDescriptor(String name, ElectricalGateSourceRenderObj render, boolean onOffOnly,
										  String iconName) {
		super(name, ElectricalGateSourceElement.class, ElectricalGateSourceRender.class, iconName);
		this.render = render;
		this.onOffOnly = onOffOnly;

		voltageLevelColor = VoltageLevelColor.SignalVoltage;
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		Collections.addAll(list, tr("Provides configurable signal\nvoltage.").split("\n"));
	}

	public void setWithAutoReset() {
		autoReset = true;
	}

	void draw(float factor, float distance, TileEntity e) {
		render.draw(factor, distance, e);
	}

	@Override
	public boolean use2DIcon() {
		return true;
	}

	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addSignal(newItemStack());
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
		if (type != ItemRenderType.INVENTORY) {
			GL11.glScalef(1.5f, 1.5f, 1.5f);
			//if (type == ItemRenderType.INVENTORY) GL11.glScalef(1.5f, 1.5f, 1.5f);
			draw(0f, 1f, null);
		} else {
			super.renderItem(type, item, data);
		}
	}

	@Override
	public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
		return super.getFrontFromPlace(side, player).inverse();
	}
}
