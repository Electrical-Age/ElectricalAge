package mods.eln.sixnode.powersocket;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class PowerSocketDescriptor extends SixNodeDescriptor {

	private Obj3D obj;
	private Obj3DPart base;
	private Obj3DPart socket;
	private int subID;

    public int range;

    public PowerSocketDescriptor(int subID, String name, Obj3D obj, int range) {
		super(name, PowerSocketElement.class, PowerSocketRender.class);
		this.subID = subID;
		this.range = range;
		this.obj = obj;
		if (obj != null) {
			base = obj.getPart("SocketBase");
			switch (subID) {
				case 1:
					socket = obj.getPart("Socket50V");
					voltageLevelColor = VoltageLevelColor.LowVoltage;
					break;
				case 2:
					socket = obj.getPart("Socket200V");
					voltageLevelColor = VoltageLevelColor.MediumVoltage;
					break;
				default:
					socket = null;
			}
		}

	}

	@Override
	public boolean use2DIcon() {
		return true;
	}

	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addLight(newItemStack(1));
	}
	
	public void draw() {
		//GL11.glRotatef(90.f,1.f,0.f,0.f);
		if(base != null)
			base.draw();
		if(socket != null)
			socket.draw();
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
			draw();
		}
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		Collections.addAll(list, tr("Supplies any device\nplugged in with energy.").split("\n"));
	}

	@Override
	public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
		return LRDU.Down;
	}
}
